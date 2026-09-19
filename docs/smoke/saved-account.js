// Saved-account smoke (mail roadmap P4.7): the account was added on the plugin's settings page
// ("Test connection", then "Save") and the script knows nothing but its alias. Run by the host
// device test `MailScriptSmokeDeviceTest#savedAccountScript` through
//
//     python .python/run_host_script_smoke.py <PROFILE> <serial> --script docs/smoke/saved-account.js --alias <alias>
//
// after `.python/run_settings_real_account.py <PROFILE> <serial> --alias <alias>` saved the account.
// No credential reaches the host or this script; the report holds aliases, counts and timings only.

var report = { ok: false, steps: [] };

function step(name, action) {
    var started = Date.now();
    var value = action();
    report.steps.push({ name: name, ms: Date.now() - started });
    return value;
}

function check(condition, what) {
    if (!condition) {
        throw new Error('check failed: ' + what);
    }
}

var client = null;
try {
    var alias = MAIL_SMOKE.alias;
    var saved = step('accounts.list', function () { return mail.accounts.list(); });
    report.savedAccounts = saved.length;
    check(step('accounts.has', function () { return mail.accounts.has(alias); }), 'the alias is saved: ' + alias);
    client = step('connect', function () { return mail.connect(alias); });
    check(client.alias === alias || client.alias === undefined, 'the client carries the alias when it exposes one');
    var test = step('test', function () { return client.test(); });
    // Only the verdict per endpoint: the full result carries the account document (address, user).
    report.test = { ok: test.ok === true };
    ['imap', 'pop3', 'smtp'].forEach(function (protocol) {
        if (test[protocol]) {
            report.test[protocol] = test[protocol].ok === true;
        }
    });
    check(report.test.ok, 'test() reports every endpoint reachable');
    var messages = step('fetch', function () { return client.fetch({ limit: 3 }); });
    report.fetched = messages.length;
    for (var i = 0; i < messages.length; i++) {
        check(typeof messages[i].subject === 'string', 'message ' + i + ' has a subject');
    }
    step('close', function () { client.close(); });
    report.closed = client.isClosed === true;
    report.ok = true;
} catch (e) {
    report.error = String(e);
    if (e && e.code) {
        report.errorCode = String(e.code);
    }
    if (client !== null) {
        try { client.close(); } catch (ignored) { /* already closed or never opened */ }
        report.closed = client.isClosed === true;
    }
}

var json = JSON.stringify(report);
runtime.putProperty('mail.smoke.report', json);
files.write(MAIL_SMOKE.reportPath, json);
