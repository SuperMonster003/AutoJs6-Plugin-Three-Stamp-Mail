// XOAUTH2 `tokenProvider` smoke script for the host script API (mail roadmap P3.1 / P3.2).
//
// Run by the host device test `MailScriptSmokeDeviceTest#realProviderScript` through
// `.python/run_host_script_smoke.py GMAIL_A <serial> --script docs/smoke/token-provider.js`.
// The test prepends `var MAIL_SMOKE = { provider, address, accessToken, workDir, reportPath }`.
//
// The account is connected without `accessToken`: the host asks `tokenProvider` for the token at
// `connect`. The provider hands out a bogus token first, so the first IMAP call (`fetch`) fails
// with AUTH_FAILED on the plugin side, the host client asks the provider again, retries once with
// the real token and the call succeeds; `test()` alone would not trigger the refresh because it
// reports endpoint failures in its result instead of throwing. The report counts the provider
// calls and never carries the token.

var report = { ok: false, steps: [], timings: {}, providerCalls: 0 };
var secret = MAIL_SMOKE.accessToken || MAIL_SMOKE.password || '';

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

var client = null;
try {
    check(typeof MAIL_SMOKE.accessToken === 'string' && MAIL_SMOKE.accessToken.length > 0, 'an access token account is required');
    var account = {
        provider: MAIL_SMOKE.provider,
        address: MAIL_SMOKE.address,
        tokenProvider: function () {
            report.providerCalls++;
            // The first token is wrong on purpose; every later request gets the real one.
            return report.providerCalls === 1 ? 'ya29.bogus-token-for-the-smoke-' + Date.now() : MAIL_SMOKE.accessToken;
        },
    };
    client = step('connect', function () { return mail.connect(account); });
    check(report.providerCalls === 1, 'connect asked the provider once');
    check(client.account.auth === 'xoauth2', 'account snapshot says xoauth2');
    check(JSON.stringify(client.account).indexOf('bogus') < 0 && JSON.stringify(client.account).indexOf(secret) < 0, 'account snapshot has no token');

    var listed = step('fetch', function () { return client.fetch({ limit: 1 }); });
    check(report.providerCalls === 2, 'AUTH_FAILED made the host ask the provider again');
    report.fetched = listed.length;

    var result = step('test', function () { return client.test(); });
    check(report.providerCalls === 2, 'no further refresh once the token works');
    check(result.ok === true, 'session.test ok after the refresh: ' + JSON.stringify(result.imap && result.imap.error));
    report.imapMs = result.imap && result.imap.elapsedMs;
    report.smtpOk = !!(result.smtp && result.smtp.ok);

    var again = step('fetch-again', function () { return client.fetch({ limit: 1 }); });
    check(report.providerCalls === 2 && again.length === listed.length, 'the refreshed token is kept for later calls');

    var failure = null;
    try {
        mail.connect({ provider: MAIL_SMOKE.provider, address: MAIL_SMOKE.address, tokenProvider: function () { return 42; } });
    } catch (e) {
        failure = e;
    }
    check(failure !== null && failure.code === 'AUTH_FAILED', 'a provider that returns no string is AUTH_FAILED at connect, got ' + redact(failure));
    report.steps.push('bad-provider');
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
