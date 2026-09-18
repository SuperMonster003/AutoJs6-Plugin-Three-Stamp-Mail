// Send / receive smoke script for the host script API, synchronous forms (mail roadmap P3.2).
//
// Run by the host device test `MailScriptSmokeDeviceTest#realProviderScript` through
// `.python/run_host_script_smoke.py <PROFILE> <serial> --script docs/smoke/send-receive.js`.
// The test prepends `var MAIL_SMOKE = { provider, address, password | accessToken, workDir,
// reportPath }` from instrumentation arguments; the script writes its JSON report to
// `reportPath` (and runtime property `mail.smoke.report`). The report never carries the secret.
//
// Steps: connect -> createFolder -> folders -> send to self with an attachment -> search the
// subject until delivered -> load the body -> download the attachment -> raw -> markRead ->
// get (peek) -> move to the new folder -> folderStatus -> fetch -> delete (expunge) ->
// deleteFolder -> close.

var report = { ok: false, mode: 'sync', steps: [], timings: {} };
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

function containsFolder(folders, path) {
    for (var i = 0; i < folders.length; i++) {
        if (folders[i].path === path || containsFolder(folders[i].children || [], path)) {
            return true;
        }
    }
    return false;
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

function findBySubject(messages, subject) {
    for (var i = 0; i < messages.length; i++) {
        if (messages[i].subject === subject) {
            return messages[i];
        }
    }
    return null;
}

var stamp = Date.now();
var subject = 'AutoJs6 smoke ' + stamp;
var folderName = 'AutoJs6Smoke' + stamp;
var marker = 'smoke-body-' + stamp;
var attachmentName = 'smoke-' + stamp + '.txt';
var workDir = MAIL_SMOKE.workDir;
var attachmentPath = files.join(workDir, attachmentName);
var attachmentContent = 'AutoJs6 mail smoke attachment ' + stamp + '\n' + new Array(200).join('0123456789') + '\n';
// QQ and 163 answer SEARCH SUBJECT / FROM for just-delivered mail with OK and no hits, so the
// smoke filters on the client there (`fallback: 'always'`); other providers ask the server first.
var searchFallback = MAIL_SMOKE.provider === 'qq' || MAIL_SMOKE.provider === '163' || MAIL_SMOKE.provider === '126' ? 'always' : 'client';
// QQ stops finding a folder created over IMAP about a minute after its creation (FOLDER_NOT_FOUND
// on status / move / delete; cause not determined), so the moved message goes to the trash
// folder there and a vanished folder is tolerated at deleteFolder.
var ephemeralFolders = MAIL_SMOKE.provider === 'qq';
var moveTarget = null;
var deliveryTimeoutMs = 150000;
var client = null;

function waitForDelivery() {
    var deadline = Date.now() + deliveryTimeoutMs;
    var polls = 0;
    var lastFallback = null;
    while (Date.now() < deadline) {
        polls++;
        var page = client.search({ subject: subject }, { folder: 'INBOX', limit: 5, fallback: searchFallback });
        lastFallback = page.fallback;
        var hit = findBySubject(page, subject);
        if (hit) {
            report.search = { polls: polls, fallback: page.fallback };
            return hit;
        }
        sleep(3000);
    }
    throw new Error('message not delivered within ' + deliveryTimeoutMs + ' ms (' + polls + ' polls, fallback ' + lastFallback + ')');
}

try {
    files.createWithDirs(attachmentPath);
    files.write(attachmentPath, attachmentContent);

    var account = { provider: MAIL_SMOKE.provider, address: MAIL_SMOKE.address };
    if (MAIL_SMOKE.password) {
        account.password = MAIL_SMOKE.password;
    } else {
        account.accessToken = MAIL_SMOKE.accessToken;
    }
    client = step('connect', function () { return mail.connect(account); });
    check(client.isConnected === true && client.isClosed === false, 'client open after connect');
    check(JSON.stringify(client.account).indexOf(secret) < 0, 'account snapshot has no secret');

    step('createFolder', function () { client.createFolder(folderName); });
    var folders = step('folders', function () { return client.folders(); });
    check(containsFolder(folders, folderName), 'created folder is listed');
    moveTarget = ephemeralFolders ? specialUseFolder(folders, 'trash') || folderName : folderName;
    report.moveTarget = moveTarget === folderName ? 'created' : 'trash';

    var sent = step('send', function () {
        return client.send({ to: MAIL_SMOKE.address, subject: subject, text: marker, attachments: [attachmentPath] });
    });
    report.send = { accepted: sent.accepted.length, rejected: sent.rejected.length, savedToSent: sent.savedToSent, sentCopy: sent.sentCopy, elapsedMs: sent.elapsedMs };
    check(typeof sent.messageId === 'string' && sent.messageId.length > 0, 'send returned a messageId');
    check(sent.accepted.length === 1 && sent.rejected.length === 0, 'one accepted recipient');

    var envelope = step('search', waitForDelivery);
    check(envelope.date instanceof Date, 'date is a Date');
    check(String(envelope.from).indexOf(MAIL_SMOKE.address) >= 0, 'from is the account');
    check(envelope.bodyLoaded === false, 'search returns envelopes');
    check(envelope.hasAttachments === true, 'envelope reports the attachment');

    var full = step('load', function () { return envelope.load(); });
    check(full.uid === envelope.uid, 'load keeps the uid');
    check(full.bodyLoaded === true, 'body loaded');
    check((full.text || '').indexOf(marker) >= 0, 'body carries the marker');
    check(full.attachments.length === 1 && full.attachments[0].fileName === attachmentName, 'one attachment with the sent name');

    var downloaded = step('download', function () { return full.attachments[0].download(workDir); });
    check(files.exists(downloaded), 'downloaded file exists');
    check(files.read(downloaded) === attachmentContent, 'attachment content round trip');
    report.download = { name: files.getName(downloaded), bytes: attachmentContent.length, size: full.attachments[0].size };
    files.remove(downloaded);

    var rawPath = step('raw', function () { return client.raw(full, workDir); });
    var raw = files.read(rawPath);
    check(raw.indexOf(subject) >= 0 && raw.indexOf(marker) >= 0, 'raw message carries subject and body');
    report.raw = { name: files.getName(rawPath), bytes: raw.length };
    files.remove(rawPath);

    var marked = step('markRead', function () { return client.markRead(envelope); });
    check(marked.length === 1 && String(marked[0]) === String(envelope.uid), 'markRead returns the uid');
    var peeked = step('peek', function () { return client.get(envelope.uid, { peek: true }); });
    check(peeked.seen === true, 'seen after markRead');

    step('move', function () { return client.move(envelope, moveTarget); });
    var status = step('folderStatus', function () { return client.folderStatus(moveTarget); });
    check(status.messages >= 1, 'moved message counted in the target folder');
    var listed = step('fetch', function () { return client.fetch({ folder: moveTarget, limit: 10 }); });
    var moved = findBySubject(listed, subject);
    check(moved !== null, 'moved message listed in the target folder');
    check(moved.folder === moveTarget, 'listed message names its folder');

    var deleted = step('delete', function () { return client.delete(moved, { expunge: true }); });
    check(deleted.length === 1, 'delete returns the uid');
    step('deleteFolder', function () {
        try {
            client.deleteFolder(folderName);
        } catch (e) {
            if (ephemeralFolders && e.code === 'FOLDER_NOT_FOUND') {
                report.folderVanished = true;
                return;
            }
            throw e;
        }
    });
    check(!containsFolder(client.folders(), folderName), 'deleted folder is gone');
    report.ok = true;
} catch (e) {
    report.error = redact(e);
    report.errorCode = e && e.code ? String(e.code) : null;
    report.failedAfter = report.steps.length;
} finally {
    try {
        files.remove(attachmentPath);
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
