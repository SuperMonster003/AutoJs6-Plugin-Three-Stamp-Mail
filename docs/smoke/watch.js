// New-mail watch smoke for the host script API (mail roadmap P5 device matrix).
//
// Run by the host device test `MailScriptSmokeDeviceTest#realProviderScript` through
// `.python/run_watch_matrix.py <PROFILE> <serial> --sender <PROFILE> --scenario <name>`, which
// prepends `var WATCH_SCENARIO = { name, expected, totalMs, mode }` to this file, pushes the
// copy and runs it through `.python/run_host_script_smoke.py`; while the script watches, the
// driver sends messages to the watched account from the sender account (PC-side SMTP), applies
// the scenario's disturbance from the PC (Wi-Fi off, Wi-Fi to cellular, forced Doze, plugin
// kill) and reads the progress file the script keeps under its work directory. The test
// prepends `var MAIL_SMOKE = { provider, address, password | accessToken, workDir, reportPath }`;
// the report never carries the secret. With `MAIL_SMOKE.alias` instead (the driver's `--alias`,
// an account saved on the plugin's settings page) the script connects by alias and can be
// launched through the host's run intent, outside the instrumentation, for the Doze case.
//
// The script connects, opens `client.watch('INBOX')`, records every event with the device
// clock (`atEpoch`, so the driver can compute the arrival latency from its send time) and
// writes the progress file on each event and every few seconds. Watch events are delivered on
// the script thread, so the script must not block it: the top level returns after the setup
// (the watch keeps the script alive), a timer bounds the wait by `totalMs`, and the run ends
// after `expected` messages of the sender or at that deadline with `watch.stop()`, the
// `close` event, `client.close()` and the report.

var scenario = typeof WATCH_SCENARIO === 'object' && WATCH_SCENARIO ? WATCH_SCENARIO : {};
var expected = scenario.expected || 2;
var totalMs = scenario.totalMs || 300000;
var mode = scenario.mode || 'auto';
var report = { ok: false, scenario: scenario.name || 'baseline', steps: [], events: [], timings: {}, messages: [] };
var secret = MAIL_SMOKE.password || MAIL_SMOKE.accessToken || '';
var started = Date.now();
var progressPath = files.join(MAIL_SMOKE.workDir, 'watch-progress.json');
var reportCopyPath = files.join(MAIL_SMOKE.workDir, '..', 'watch-' + report.scenario + '-report.json');
var client = null;
var watch = null;
var phase = 'starting';
var finished = false;
var deadlineTimer = null;
var heartbeat = null;

function redact(value) {
    var text = String(value);
    return secret ? text.split(secret).join('****') : text;
}

function step(name, action) {
    var begun = Date.now();
    var value = action();
    report.timings[name] = Date.now() - begun;
    report.steps.push(name);
    return value;
}

function progress(newPhase, extra) {
    if (newPhase) {
        phase = newPhase;
    }
    var doc = { phase: phase, at: Date.now() - started, atEpoch: Date.now(), scenario: report.scenario, events: report.events.length, messages: report.messages.length };
    if (watch) {
        doc.mode = watch.mode;
        doc.generation = watch.generation;
        doc.active = watch.isActive;
    }
    if (extra) {
        for (var key in extra) {
            doc[key] = extra[key];
        }
    }
    files.write(progressPath, JSON.stringify(doc));
}

function record(type, detail) {
    var entry = { type: type, at: Date.now() - started, atEpoch: Date.now() };
    if (detail !== undefined) {
        entry.detail = detail;
    }
    report.events.push(entry);
    console.log('[watch-smoke] ' + JSON.stringify(entry));
    return entry;
}

function writeReport() {
    report.totalMs = Date.now() - started;
    var json = JSON.stringify(report);
    runtime.putProperty('mail.smoke.report', json);
    files.write(MAIL_SMOKE.reportPath, json);
    // the host test removes its work directory afterwards; the driver reads this copy one level up
    files.write(reportCopyPath, json);
    progress('done', { ok: report.ok });
}

function conclude() {
    try {
        if (watch) {
            report.stopped = watch.isActive === false && watch.reason === 'stopped';
            report.finalState = { active: watch.isActive, mode: watch.mode, generation: watch.generation, reason: watch.reason };
        }
        if (client) {
            step('close', function () { client.close(); });
            report.closed = client.isClosed === true;
        }
        if (report.timedOut) {
            report.error = 'only ' + report.messages.length + ' of ' + expected + ' messages arrived within ' + totalMs + ' ms';
        } else if (!report.stopped) {
            report.error = 'the watch did not end with reason stopped: ' + (watch ? watch.reason : '?');
        }
        report.ok = !report.error;
    } catch (e) {
        report.error = redact(e);
        report.errorCode = e && e.code ? String(e.code) : null;
    }
    writeReport();
}

function finish(why) {
    if (finished) {
        return;
    }
    finished = true;
    if (deadlineTimer !== null) {
        clearTimeout(deadlineTimer);
    }
    if (heartbeat !== null) {
        clearInterval(heartbeat);
    }
    report.finishReason = why;
    report.timedOut = report.messages.length < expected;
    report.finalMode = watch.mode;
    report.finalGeneration = watch.generation;
    try {
        step('stop', function () { watch.stop(); });
    } catch (e) {
        report.error = redact(e);
    }
    // the close event is dispatched on the script thread after this callback returns
    setTimeout(conclude, 1500);
}

var account = MAIL_SMOKE.alias || { provider: MAIL_SMOKE.provider, address: MAIL_SMOKE.address };
if (!MAIL_SMOKE.alias) {
    if (MAIL_SMOKE.password) {
        account.password = MAIL_SMOKE.password;
    } else {
        account.accessToken = MAIL_SMOKE.accessToken;
    }
}
files.ensureDir(progressPath);

try {
    progress('starting');
    client = step('connect', function () { return mail.connect(account); });
    watch = step('watch', function () {
        var options = { fetchBody: false };
        if (mode !== 'auto') {
            options.mode = mode;
        }
        return client.watch('INBOX', options);
    });
    if (!(watch.isActive === true && watch.folder === 'INBOX')) {
        throw new Error('check failed: watch is active on INBOX');
    }
    report.initialMode = watch.mode;
    report.generation = watch.generation;
    watch.on('message', function (message, source) {
        var subject = String(message.subject || '');
        var marker = subject.indexOf('AutoJs6 watch ');
        var entry = record('message', { subject: marker === 0 ? subject.substring(subject.lastIndexOf('#')) : '(other)', uid: String(message.uid), folder: message.folder, same: source === watch, bodyLoaded: message.bodyLoaded === true });
        if (marker === 0) {
            report.messages.push({ subject: subject, atEpoch: entry.atEpoch, uid: String(message.uid) });
        }
        progress('message', { last: subject });
        if (report.messages.length >= expected) {
            setTimeout(function () { finish('expected'); }, 0);
        }
    });
    watch.on('mode', function (value) { record('mode', value); progress('mode'); });
    watch.on('resync', function (reason) { record('resync', reason); progress('resync'); });
    watch.on('error', function (err) { record('error', { code: err && err.code ? String(err.code) : null, message: redact(err && err.message ? err.message : err), retryable: !!(err && err.retryable) }); progress('error'); });
    watch.on('close', function (reason) {
        record('close', reason);
        progress('close');
        if (!finished) {
            // the plugin side ended the watch on its own
            report.closedEarly = reason;
            finish('closed');
        }
    });
    heartbeat = setInterval(function () { progress(null); }, 5000);
    deadlineTimer = setTimeout(function () { finish('timeout'); }, totalMs);
    progress('watching');
} catch (e) {
    report.error = redact(e);
    report.errorCode = e && e.code ? String(e.code) : null;
    report.failedAfter = report.steps.length;
    finished = true;
    if (heartbeat !== null) {
        clearInterval(heartbeat);
    }
    try {
        if (client) {
            client.close();
            report.closed = client.isClosed === true;
        }
    } catch (e2) {
        report.closeError = redact(e2);
    }
    writeReport();
}
