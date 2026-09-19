// POP3 degraded-path smoke script for the host script API (mail roadmap P2.4 over P3.2).
//
// Run by the host device test `MailScriptSmokeDeviceTest#realProviderScript` through
// `.python/run_host_script_smoke.py <PROFILE> <serial> --script docs/smoke/pop3.js`.
// The test prepends `var MAIL_SMOKE = { provider, address, password | accessToken, workDir,
// reportPath }`; the account must have POP3 enabled at the provider.
//
// Steps: connect with `receive: 'pop3'` -> test -> folders (INBOX only) -> fetch 3 envelopes
// (string UIDs, highest message number first) -> search by the subject of the first one (client
// filter over TOP) -> get it (RETR, body) -> raw and download -> setFlags and createFolder answer
// UNSUPPORTED_OPERATION -> close. Nothing is deleted.
//
// Gmail serves POP3 differently: each session sees one batch of the oldest mail that has not been
// downloaded yet (about 270 messages), and a message fetched with RETR is not served again in a
// later session. Every plugin operation is one POP3 session, so after `get` the same uid is gone;
// the script records that (`onceOnly`) and takes `raw` from the second message instead.

var report = { ok: false, steps: [], timings: {} };
var secret = MAIL_SMOKE.password || MAIL_SMOKE.accessToken || '';

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

function expectCode(code, action) {
    try {
        action();
    } catch (e) {
        check(e.code === code, 'expected ' + code + ', got ' + redact(e));
        return;
    }
    throw new Error('expected ' + code + ' but the call succeeded');
}

var client = null;
try {
    var account = { provider: MAIL_SMOKE.provider, address: MAIL_SMOKE.address, receive: 'pop3' };
    if (MAIL_SMOKE.password) {
        account.password = MAIL_SMOKE.password;
    } else {
        account.accessToken = MAIL_SMOKE.accessToken;
    }
    client = step('connect', function () { return mail.connect(account); });
    check(client.account.receive === 'pop3', 'account snapshot keeps receive');

    var probe = step('test', function () { return client.test(); });
    check(probe.ok === true, 'session.test ok: ' + JSON.stringify(probe.pop3 && probe.pop3.error));
    check(!!(probe.pop3 && probe.pop3.ok), 'pop3 endpoint ok');
    report.pop3 = { host: probe.pop3.host, elapsedMs: probe.pop3.elapsedMs, capabilities: probe.pop3.capabilities };

    var folders = step('folders', function () { return client.folders({ status: true }); });
    check(folders.length === 1 && folders[0].path === 'INBOX', 'POP3 lists INBOX only');
    report.inbox = { messages: folders[0].messages };

    var listed = step('fetch', function () { return client.fetch({ limit: 3 }); });
    check(listed.length >= 1, 'at least one message in INBOX');
    check(listed.every(function (m) { return typeof m.uid === 'string' && m.uid.length > 0; }), 'POP3 uids are UIDL strings');
    check(listed.every(function (m) { return m.bodyLoaded === false && m.folder === 'INBOX'; }), 'envelopes only');
    var newest = listed[0];
    report.newest = { uidLength: newest.uid.length, subjectLength: (newest.subject || '').length, date: newest.date instanceof Date ? newest.date.toISOString() : null };

    if (newest.subject) {
        // Before any RETR (Gmail stops serving a message once it was retrieved). limit 1: the client
        // filter stops at the first hit; QQ answers TOP in about 0.17 s per message.
        var found = step('search', function () { return client.search({ subject: newest.subject }, { limit: 1 }); });
        check(found.fallback === 'client', 'POP3 search filters on the client');
        check(found.some(function (m) { return m.uid === newest.uid; }), 'search finds the first message by subject');
        report.search = { hits: found.length, fallback: found.fallback };
    }

    var full = step('get', function () { return newest.load(); });
    check(full.uid === newest.uid && full.bodyLoaded === true, 'body loaded for the first message');
    report.body = { text: (full.text || '').length, html: (full.html || '').length, attachments: full.attachments.length };

    var rawSource = newest;
    var rawPath = step('raw', function () {
        try {
            return client.raw(newest, MAIL_SMOKE.workDir);
        } catch (e) {
            if (e.code !== 'MESSAGE_NOT_FOUND' || listed.length < 2) {
                throw e;
            }
            // The retrieved message is not served again (Gmail); take the next one.
            report.onceOnly = true;
            rawSource = listed[1];
            return client.raw(rawSource, MAIL_SMOKE.workDir);
        }
    });
    check(files.exists(rawPath), 'raw file exists');
    report.raw = { bytes: files.read(rawPath).length, sameMessage: rawSource === newest };
    files.remove(rawPath);

    if (full.attachments.length > 0 && !report.onceOnly) {
        var downloaded = step('download', function () { return full.attachments[0].download(MAIL_SMOKE.workDir); });
        check(files.exists(downloaded), 'attachment downloaded');
        files.remove(downloaded);
    }

    step('unsupported', function () {
        expectCode('UNSUPPORTED_OPERATION', function () { client.markRead(rawSource); });
        expectCode('UNSUPPORTED_OPERATION', function () { client.createFolder('AutoJs6Pop3Smoke'); });
        expectCode('UNSUPPORTED_OPERATION', function () { client.move(rawSource, 'Archive'); });
        expectCode('FOLDER_NOT_FOUND', function () { client.fetch({ folder: 'Archive', limit: 1 }); });
    });
    report.ok = true;
} catch (e) {
    report.error = redact(e);
    report.errorCode = e && e.code ? String(e.code) : null;
    report.failedAfter = report.steps.length;
} finally {
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
