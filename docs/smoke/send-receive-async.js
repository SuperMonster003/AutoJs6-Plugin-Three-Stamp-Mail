"ui";

// Send / receive smoke script for the host script API, `Async` forms in a `ui` mode script
// (mail roadmap P3.2). Same steps as send-receive.js, chained on Promises, while a
// `setInterval` ticker runs on the UI thread: the ticker keeps counting only when no mail
// call blocks that thread, and the report says whether it stalled (`uiBlocked`).
//
// Run by the host device test `MailScriptSmokeDeviceTest#realProviderScript` through
// `.python/run_host_script_smoke.py <PROFILE> <serial> --script docs/smoke/send-receive-async.js`.
// The test inserts `var MAIL_SMOKE = { provider, address, password | accessToken, workDir,
// reportPath }` after the "ui" line; the script writes its JSON report to `reportPath` (and
// runtime property `mail.smoke.report`). The report never carries the secret.

var report = { ok: false, mode: 'async', steps: [], timings: {}, ticks: 0 };
var secret = MAIL_SMOKE.password || MAIL_SMOKE.accessToken || '';
var tickMs = 50;
var startedAt = Date.now();
var ticker = setInterval(function () { report.ticks++; }, tickMs);

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
    return action().then(function (value) {
        report.timings[name] = Date.now() - started;
        report.steps.push(name);
        return value;
    });
}

function delay(ms) {
    return new Promise(function (resolve) { setTimeout(resolve, ms); });
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
var subject = 'AutoJs6 smoke async ' + stamp;
var folderName = 'AutoJs6SmokeAsync' + stamp;
var marker = 'smoke-body-async-' + stamp;
var attachmentName = 'smoke-async-' + stamp + '.txt';
var workDir = MAIL_SMOKE.workDir;
var attachmentPath = files.join(workDir, attachmentName);
var attachmentContent = 'AutoJs6 mail smoke attachment (async) ' + stamp + '\n' + new Array(200).join('9876543210') + '\n';
// QQ and 163 answer SEARCH SUBJECT / FROM for just-delivered mail with OK and no hits, so the
// smoke filters on the client there (`fallback: 'always'`); other providers ask the server first.
var searchFallback = MAIL_SMOKE.provider === 'qq' || MAIL_SMOKE.provider === '163' || MAIL_SMOKE.provider === '126' ? 'always' : 'client';
// QQ stops finding a folder created over IMAP about a minute after its creation (FOLDER_NOT_FOUND
// on status / move / delete; cause not determined), so the moved message goes to the trash
// folder there and a vanished folder is tolerated at deleteFolder.
var ephemeralFolders = MAIL_SMOKE.provider === 'qq';
// Sina Mail answers NO to every IMAP CREATE (folders exist only through its web UI), so the smoke
// skips the folder steps there and moves into the trash folder.
var noFolderCreation = MAIL_SMOKE.provider === 'sina';
var moveTarget = null;
var deliveryTimeoutMs = 150000;
var deliveryDeadline = 0;
var polls = 0;
var client = null;
var envelope = null;
var full = null;
var moved = null;

function waitForDelivery() {
    polls++;
    return client.searchAsync({ subject: subject }, { folder: 'INBOX', limit: 5, fallback: searchFallback }).then(function (page) {
        var hit = findBySubject(page, subject);
        if (hit) {
            report.search = { polls: polls, fallback: page.fallback };
            return hit;
        }
        if (Date.now() >= deliveryDeadline) {
            throw new Error('message not delivered within ' + deliveryTimeoutMs + ' ms (' + polls + ' polls, fallback ' + page.fallback + ')');
        }
        return delay(3000).then(waitForDelivery);
    });
}

function finish() {
    clearInterval(ticker);
    report.elapsedMs = Date.now() - startedAt;
    report.expectedTicks = Math.floor(report.elapsedMs / tickMs);
    // A blocked UI thread would have stalled the ticker for whole network calls (seconds each).
    report.uiBlocked = report.ticks < report.expectedTicks * 0.5;
    try {
        files.remove(attachmentPath);
        if (client) {
            client.close();
            report.closed = client.isClosed === true;
        }
    } catch (e) {
        report.closeError = redact(e);
    }
    var json = JSON.stringify(report);
    runtime.putProperty('mail.smoke.report', json);
    files.write(MAIL_SMOKE.reportPath, json);
    setTimeout(exit, 500);
}

files.createWithDirs(attachmentPath);
files.write(attachmentPath, attachmentContent);

var account = { provider: MAIL_SMOKE.provider, address: MAIL_SMOKE.address };
if (MAIL_SMOKE.password) {
    account.password = MAIL_SMOKE.password;
} else {
    account.accessToken = MAIL_SMOKE.accessToken;
}

step('connect', function () { return mail.connectAsync(account); })
    .then(function (connected) {
        client = connected;
        check(client.isConnected === true && client.isClosed === false, 'client open after connect');
        check(JSON.stringify(client.account).indexOf(secret) < 0, 'account snapshot has no secret');
        if (noFolderCreation) {
            return client.createFolderAsync(folderName).then(function () {
                throw new Error('expected SERVER_ERROR but createFolder succeeded');
            }, function (e) {
                check(e.code === 'SERVER_ERROR', 'expected SERVER_ERROR, got ' + redact(e));
                report.folderCreation = 'refused';
            });
        }
        return step('createFolder', function () { return client.createFolderAsync(folderName); });
    })
    .then(function () { return step('folders', function () { return client.foldersAsync(); }); })
    .then(function (folders) {
        check(noFolderCreation || containsFolder(folders, folderName), 'created folder is listed');
        moveTarget = ephemeralFolders || noFolderCreation ? specialUseFolder(folders, 'trash') || folderName : folderName;
        report.moveTarget = moveTarget === folderName ? 'created' : 'trash';
        return step('send', function () {
            return client.sendAsync({ to: MAIL_SMOKE.address, subject: subject, text: marker, attachments: [attachmentPath] });
        });
    })
    .then(function (sent) {
        report.send = { accepted: sent.accepted.length, rejected: sent.rejected.length, savedToSent: sent.savedToSent, sentCopy: sent.sentCopy, elapsedMs: sent.elapsedMs };
        check(typeof sent.messageId === 'string' && sent.messageId.length > 0, 'send returned a messageId');
        check(sent.accepted.length === 1 && sent.rejected.length === 0, 'one accepted recipient');
        deliveryDeadline = Date.now() + deliveryTimeoutMs;
        return step('search', waitForDelivery);
    })
    .then(function (found) {
        envelope = found;
        check(envelope.date instanceof Date, 'date is a Date');
        check(String(envelope.from).indexOf(MAIL_SMOKE.address) >= 0, 'from is the account');
        check(envelope.bodyLoaded === false, 'search returns envelopes');
        return step('load', function () { return envelope.loadAsync(); });
    })
    .then(function (loaded) {
        full = loaded;
        check(full.uid === envelope.uid, 'load keeps the uid');
        check(full.bodyLoaded === true, 'body loaded');
        check((full.text || '').indexOf(marker) >= 0, 'body carries the marker');
        check(full.attachments.length === 1 && full.attachments[0].fileName === attachmentName, 'one attachment with the sent name');
        var progress = [];
        report.progress = progress;
        return step('download', function () {
            return full.attachments[0].downloadAsync(workDir, {
                onProgress: function (transferred, total) { progress.push([transferred, total]); },
            });
        });
    })
    .then(function (downloaded) {
        check(files.exists(downloaded), 'downloaded file exists');
        check(files.read(downloaded) === attachmentContent, 'attachment content round trip');
        report.download = { name: files.getName(downloaded), bytes: attachmentContent.length, size: full.attachments[0].size, progressCalls: report.progress.length };
        delete report.progress;
        files.remove(downloaded);
        return step('raw', function () { return client.rawAsync(full, workDir); });
    })
    .then(function (rawPath) {
        var raw = files.read(rawPath);
        check(raw.indexOf(subject) >= 0 && raw.indexOf(marker) >= 0, 'raw message carries subject and body');
        report.raw = { name: files.getName(rawPath), bytes: raw.length };
        files.remove(rawPath);
        return step('markRead', function () { return client.markReadAsync(envelope); });
    })
    .then(function (marked) {
        check(marked.length === 1 && String(marked[0]) === String(envelope.uid), 'markRead returns the uid');
        return step('peek', function () { return client.getAsync(envelope.uid, { peek: true }); });
    })
    .then(function (peeked) {
        check(peeked.seen === true, 'seen after markRead');
        return step('move', function () { return client.moveAsync(envelope, moveTarget); });
    })
    .then(function () { return step('folderStatus', function () { return client.folderStatusAsync(moveTarget); }); })
    .then(function (status) {
        check(status.messages >= 1, 'moved message counted in the target folder');
        return step('fetch', function () { return client.fetchAsync({ folder: moveTarget, limit: 10 }); });
    })
    .then(function (listed) {
        moved = findBySubject(listed, subject);
        check(moved !== null, 'moved message listed in the target folder');
        check(moved.folder === moveTarget, 'listed message names its folder');
        return step('delete', function () { return client.deleteAsync(moved, { expunge: true }); });
    })
    .then(function (deleted) {
        check(deleted.length === 1, 'delete returns the uid');
        if (noFolderCreation) {
            return true;
        }
        return step('deleteFolder', function () {
            return client.deleteFolderAsync(folderName).catch(function (e) {
                if (ephemeralFolders && e.code === 'FOLDER_NOT_FOUND') {
                    report.folderVanished = true;
                    return true;
                }
                throw e;
            });
        });
    })
    .then(function () { return client.foldersAsync(); })
    .then(function (folders) {
        check(noFolderCreation || !containsFolder(folders, folderName), 'deleted folder is gone');
        report.ok = true;
    })
    .catch(function (e) {
        report.error = redact(e);
        report.errorCode = e && e.code ? String(e.code) : null;
        report.failedAfter = report.steps.length;
    })
    .then(finish);
