// OAuth status smoke (mail roadmap P9): what a script sees of a browser sign-in through
// `mail.accounts.list()`: the `oauth` object (provider, whether a new sign-in is needed, when the
// access token expires), never a token. Run by the host device test
// `MailScriptSmokeDeviceTest#savedAccountScript` through
//
//     python .python/run_host_script_smoke.py <PROFILE> <serial> --script docs/smoke/oauth-status.js --alias <alias>
//
// after `.python/run_oauth_device.py` seeded the account. The report carries the alias, booleans,
// durations and the provider id only; it never carries the address.

var report = { ok: false, closed: true };

function check(condition, what) {
    if (!condition) {
        throw new Error('check failed: ' + what);
    }
}

try {
    var alias = MAIL_SMOKE.alias;
    var entries = mail.accounts.list();
    report.savedAccounts = entries.length;
    var entry = null;
    for (var i = 0; i < entries.length; i++) {
        if (entries[i].alias === alias) {
            entry = entries[i];
        }
    }
    check(entry !== null, 'the alias is listed: ' + alias);
    check(mail.accounts.has(alias), 'accounts.has agrees');
    report.auth = entry.auth;
    report.hasAddress = typeof entry.address === 'string' && entry.address.indexOf('@') > 0;
    var keys = Object.keys(entry).sort();
    for (var k = 0; k < keys.length; k++) {
        check(!/token|secret|password/i.test(keys[k]), 'no secret-like key on the entry: ' + keys[k]);
    }
    if (entry.oauth) {
        report.oauth = {
            provider: entry.oauth.provider,
            needsReauth: entry.oauth.needsReauth === true,
            expiresAt: entry.oauth.expiresAt,
            expiresInS: entry.oauth.expiresAt > 0 ? Math.round((entry.oauth.expiresAt - Date.now()) / 1000) : null,
            authorizedAgoS: Math.round((Date.now() - entry.oauth.authorizedAt) / 1000),
            keys: Object.keys(entry.oauth).sort()
        };
        check(report.oauth.keys.join(',') === 'authorizedAt,expiresAt,needsReauth,provider', 'the oauth object carries the four documented fields only');
    } else {
        report.oauth = null;
    }
    report.ok = true;
} catch (e) {
    report.error = String(e);
    if (e && e.code) {
        report.errorCode = String(e.code);
    }
}

var json = JSON.stringify(report);
runtime.putProperty('mail.smoke.report', json);
files.write(MAIL_SMOKE.reportPath, json);
