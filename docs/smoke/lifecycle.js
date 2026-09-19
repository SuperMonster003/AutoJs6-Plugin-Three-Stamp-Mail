// Lifecycle probe for the host script API (mail roadmap P6 lifecycle matrix).
//
// Run by `.python/run_lifecycle_matrix.py <serial> --alias <alias> --cases ...`, which prepends
// `var LIFECYCLE = { case, holdMs, workDir, reportPath }` and `var MAIL_SMOKE = { alias }`, pushes the
// copy into the host's files directory and starts it through the host's `RunIntentActivity`, like a
// user's script; no credential is involved (the alias is an account saved on the plugin's settings page).
//
// The script connects by alias, opens a watch on INBOX, keeps the session busy with one envelope
// fetch every 10 s and writes a progress document on every step and event. What ends it depends on
// the case (the driver applies the disturbance from the PC while the script holds):
//
//   exit        after holdMs the script stops the watch, closes the client and ends normally
//   exit-open   after holdMs the script calls exit() with the watch and the client still open
//   stop-all    the driver runs a second script that calls engines.stopAll() (the user's "stop all")
//   kill-host   the driver force-stops the host process
//   kill-plugin the driver force-stops the plugin process; the watch reconnects (generation 2)
//   disable     the driver disables the plugin package, then enables it again
//   uninstall   the driver uninstalls the plugin package, then installs it again
//   upgrade     the driver installs the plugin APK over the running one
//
// For kill-plugin / upgrade the keepalive fetches fail while the plugin is away and succeed again
// afterwards; for disable / uninstall they keep failing and the script ends by itself after the
// first PLUGIN_UNAVAILABLE that lasts longer than 20 s. Every outcome goes into the report.

var config = typeof LIFECYCLE === 'object' && LIFECYCLE ? LIFECYCLE : {};
var holdMs = config.holdMs || 60000;
var report = { ok: false, case: config.case || 'exit', steps: [], events: [], fetches: [], errors: [] };
var started = Date.now();
var progressPath = files.join(config.workDir, 'lifecycle-progress.json');
var client = null;
var watch = null;
var phase = 'starting';
var finished = false;
var keepalive = null;
var deadline = null;
var firstFailureAt = null;

function step(name, action) {
    var begun = Date.now();
    var value = action();
    report.steps.push({ name: name, ms: Date.now() - begun });
    return value;
}

function progress(newPhase, extra) {
    if (newPhase) {
        phase = newPhase;
    }
    var doc = { phase: phase, at: Date.now() - started, atEpoch: Date.now(), case: report.case, events: report.events.length, fetches: report.fetches.length, errors: report.errors.length, pid: engines.myEngine().id };
    if (watch) {
        doc.mode = watch.mode;
        doc.generation = watch.generation;
        doc.active = watch.isActive;
        doc.reason = watch.reason;
    }
    if (client) {
        doc.closed = client.isClosed === true;
    }
    if (extra) {
        for (var key in extra) {
            doc[key] = extra[key];
        }
    }
    files.write(progressPath, JSON.stringify(doc));
}

function record(type, detail) {
    var entry = { type: type, at: Date.now() - started };
    if (detail !== undefined) {
        entry.detail = detail;
    }
    report.events.push(entry);
    console.log('[lifecycle] ' + JSON.stringify(entry));
    return entry;
}

function writeReport(why) {
    report.finishReason = why;
    report.totalMs = Date.now() - started;
    if (watch) {
        report.finalWatch = { active: watch.isActive, mode: watch.mode, generation: watch.generation, reason: watch.reason };
    }
    if (client) {
        report.clientClosed = client.isClosed === true;
    }
    report.ok = report.errors.length === 0 || report.case === 'disable' || report.case === 'uninstall' || report.case === 'kill-plugin' || report.case === 'upgrade';
    var json = JSON.stringify(report);
    files.write(config.reportPath, json);
    progress('done', { ok: report.ok, why: why });
}

function finish(why) {
    if (finished) {
        return;
    }
    finished = true;
    if (keepalive !== null) {
        clearInterval(keepalive);
    }
    if (deadline !== null) {
        clearTimeout(deadline);
    }
    if (report.case === 'exit-open') {
        // leave the watch and the client open on purpose: the host has to clean up on exit
        progress('exiting', { why: why });
        writeReport(why);
        exit();
        return;
    }
    try {
        if (watch && watch.isActive) {
            step('stop', function () { watch.stop(); });
        }
    } catch (e) {
        report.errors.push({ where: 'stop', code: e && e.code ? String(e.code) : null, message: String(e) });
    }
    setTimeout(function () {
        try {
            if (client && !client.isClosed) {
                step('close', function () { client.close(); });
            }
        } catch (e) {
            report.errors.push({ where: 'close', code: e && e.code ? String(e.code) : null, message: String(e) });
        }
        writeReport(why);
    }, 1000);
}

files.ensureDir(progressPath);
try {
    progress('starting');
    client = step('connect', function () { return mail.connect(MAIL_SMOKE.alias); });
    watch = step('watch', function () { return client.watch('INBOX', { fetchBody: false }); });
    report.initialGeneration = watch.generation;
    watch.on('message', function (message) { record('message', { uid: String(message.uid) }); progress('message'); });
    watch.on('mode', function (value) { record('mode', value); progress('mode'); });
    watch.on('resync', function (reason) { record('resync', reason); progress('resync'); });
    watch.on('error', function (err) { record('error', { code: err && err.code ? String(err.code) : null, message: String(err && err.message ? err.message : err) }); progress('error'); });
    watch.on('close', function (reason) {
        record('close', reason);
        progress('close');
        if (!finished && (report.case === 'disable' || report.case === 'uninstall')) {
            finish('closed');
        }
    });
    keepalive = setInterval(function () {
        var begun = Date.now();
        try {
            var page = client.fetch({ limit: 1 });
            report.fetches.push({ at: begun - started, ms: Date.now() - begun, ok: true, count: page.length });
            firstFailureAt = null;
            progress('holding');
        } catch (e) {
            var code = e && e.code ? String(e.code) : null;
            report.fetches.push({ at: begun - started, ms: Date.now() - begun, ok: false, code: code });
            report.errors.push({ where: 'fetch', code: code, message: String(e) });
            if (firstFailureAt === null) {
                firstFailureAt = begun;
            }
            progress('failing', { code: code });
            if (begun - firstFailureAt > 20000 && (report.case === 'disable' || report.case === 'uninstall')) {
                finish('plugin-gone');
            }
        }
    }, 10000);
    deadline = setTimeout(function () { finish('hold-elapsed'); }, holdMs);
    progress('holding', { generation: watch.generation, mode: watch.mode });
} catch (e) {
    report.errors.push({ where: 'setup', code: e && e.code ? String(e.code) : null, message: String(e) });
    finished = true;
    if (keepalive !== null) {
        clearInterval(keepalive);
    }
    try {
        if (client) {
            client.close();
        }
    } catch (ignored) {
    }
    writeReport('setup-failed');
}
