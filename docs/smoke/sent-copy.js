// Sent-copy probe for the provider presets (`autoSavesSent`, mail roadmap P3.2 follow-up).
//
// Run by the host device test `MailScriptSmokeDeviceTest#realProviderScript` through
// `.python/run_host_script_smoke.py <PROFILE> <serial> --script docs/smoke/sent-copy.js`.
// The test prepends `var MAIL_SMOKE = { provider, address, password | accessToken, workDir,
// reportPath }`.
//
// Steps: connect -> folders (the sent folder: special-use `sent`, else the preset's `sentFolder`)
// -> send to self with `saveToSent: false` and count the copies of that subject in the sent folder
// for up to 90 s (a copy means the server files sent mail itself) -> send to self with the preset
// default and count again (exactly one copy expected, two means the preset is wrong) -> delete the
// probe messages from the sent folder and INBOX -> close. The report never carries secrets.

var report = { ok: false, steps: [], timings: {} };
var secret = MAIL_SMOKE.password || MAIL_SMOKE.accessToken || '';
var settleMs = 90000;
var pollMs = 5000;
var duplicateGraceMs = 20000;

function redact(value) {
    var text = String(value);
    return secret ? text.split(secret).join('****') : text;
}

function check(condition, message) {
    if (!condition) {
        throw new Error('check failed: ' + message);
    }
}

function step(name, action) {
    var started = Date.now();
    var value = action();
    report.timings[name] = Date.now() - started;
    report.steps.push(name);
    return value;
}

function specialUseFolder(folders, role) {
    for (var i = 0; i < folders.length; i++) {
        if (folders[i].specialUse === role && folders[i].selectable) {
            return folders[i].path;
        }
        var nested = specialUseFolder(folders[i].children || [], role);
        if (nested) {
            return nested;
        }
    }
    return null;
}

function folderExists(folders, path) {
    for (var i = 0; i < folders.length; i++) {
        if (folders[i].path === path || folderExists(folders[i].children || [], path)) {
            return true;
        }
    }
    return false;
}

function paths(folders) {
    var result = [];
    for (var i = 0; i < folders.length; i++) {
        result.push(folders[i].path + (folders[i].specialUse ? ' (' + folders[i].specialUse + ')' : ''));
        result = result.concat(paths(folders[i].children || []));
    }
    return result;
}

var client = null;
var sentPath = null;
var subjects = [];

function copiesOf(subject, folder) {
    return client.search({ subject: subject }, { folder: folder, limit: 10, fallback: 'always' });
}

function waitForCopies(subject, folder) {
    var started = Date.now();
    var found = [];
    while (Date.now() - started < settleMs) {
        found = copiesOf(subject, folder);
        if (found.length > 0) {
            break;
        }
        sleep(pollMs);
    }
    if (found.length > 0) {
        // A late duplicate (plugin append plus server copy) shows up within a few seconds.
        sleep(duplicateGraceMs);
        found = copiesOf(subject, folder);
    }
    return { copies: found.length, waitedMs: Date.now() - started };
}

function probe(name, options) {
    var subject = 'AutoJs6 mail sent-copy ' + name + ' ' + Date.now();
    subjects.push(subject);
    var sent = step('send-' + name, function () {
        return client.send({ to: MAIL_SMOKE.address, subject: subject, text: 'sent-copy probe ' + name }, options);
    });
    check(sent.accepted.length === 1 && sent.rejected.length === 0, 'one accepted recipient');
    var result = { sentCopy: sent.sentCopy, savedToSent: sent.savedToSent, sentFolder: sent.sentFolder || null, saveError: sent.saveError ? sent.saveError.code : null };
    if (sentPath) {
        var counted = step('count-' + name, function () { return waitForCopies(subject, sentPath); });
        result.copies = counted.copies;
        result.waitedMs = counted.waitedMs;
    }
    return result;
}

try {
    var preset = mail.providers.get(MAIL_SMOKE.provider);
    report.provider = MAIL_SMOKE.provider;
    report.preset = preset ? { autoSavesSent: preset.autoSavesSent, sentFolder: preset.sentFolder || null } : null;

    var account = { provider: MAIL_SMOKE.provider, address: MAIL_SMOKE.address };
    if (MAIL_SMOKE.password) {
        account.password = MAIL_SMOKE.password;
    } else {
        account.accessToken = MAIL_SMOKE.accessToken;
    }
    client = step('connect', function () { return mail.connect(account); });

    var folders = step('folders', function () { return client.folders(); });
    report.folders = paths(folders);
    var special = specialUseFolder(folders, 'sent');
    var presetFolder = preset && preset.sentFolder && folderExists(folders, preset.sentFolder) ? preset.sentFolder : null;
    sentPath = special || presetFolder;
    report.sentFolder = { used: sentPath, specialUse: special, preset: presetFolder };

    report.noSave = probe('nosave', { saveToSent: false });
    report.defaultSave = probe('default', undefined);

    if (sentPath) {
        var serverAutoSaves = report.noSave.copies >= 1;
        report.verdict = {
            serverAutoSaves: serverAutoSaves,
            defaultCopies: report.defaultSave.copies,
            presetMatches: !!preset && preset.autoSavesSent === serverAutoSaves && report.defaultSave.copies === 1,
        };
    } else {
        report.verdict = { serverAutoSaves: null, note: 'no sent folder found; copies not counted' };
    }
    report.ok = true;
} catch (e) {
    report.error = redact(e);
    report.errorCode = e && e.code ? String(e.code) : null;
    report.failedAfter = report.steps.length;
} finally {
    try {
        if (client) {
            var removed = 0;
            var targets = sentPath ? [sentPath, 'INBOX'] : ['INBOX'];
            for (var i = 0; i < subjects.length; i++) {
                for (var j = 0; j < targets.length; j++) {
                    var found = copiesOf(subjects[i], targets[j]);
                    if (found.length > 0) {
                        removed += client.delete(found, { expunge: true }).length;
                    }
                }
            }
            report.cleanup = { removed: removed };
        }
    } catch (e) {
        report.cleanupError = redact(e);
    }
    try {
        if (client) {
            client.close();
            report.closed = client.isClosed === true;
        }
    } catch (e) {
        report.closeError = redact(e);
    }
}
var json = JSON.stringify(report);
runtime.putProperty('mail.smoke.report', json);
files.write(MAIL_SMOKE.reportPath, json);
