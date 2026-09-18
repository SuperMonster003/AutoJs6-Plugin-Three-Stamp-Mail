// Host-death probe for the plugin side (mail roadmap P1.3 / P2.5 open item).
//
// Run by the host device test `MailScriptSmokeDeviceTest#realProviderScript` through
// `.python/run_host_script_smoke.py <PROFILE> <serial> --script docs/smoke/hold-session.js`
// and, while the script sleeps with its IMAP connection open, kill the host from the PC:
//
//     adb -s <serial> shell am force-stop org.autojs.autojs6
//
// The plugin process survives the host; its session binder gets the death notification and
// closes the mail session, which shows up as the plugin's TCP connections to the IMAP server
// (port 993) disappearing from /proc/net/tcp6 within a few seconds. The Gradle run itself ends
// in failure because the instrumentation process was killed; that is expected.
//
// The script keeps the connection busy with one envelope fetch per 10 s so an idle teardown of
// the plugin does not close the connection first.

var report = { ok: false, steps: [] };
var secret = MAIL_SMOKE.password || MAIL_SMOKE.accessToken || '';
var account = { provider: MAIL_SMOKE.provider, address: MAIL_SMOKE.address };
if (MAIL_SMOKE.password) {
    account.password = MAIL_SMOKE.password;
} else {
    account.accessToken = MAIL_SMOKE.accessToken;
}
var client = mail.connect(account);
report.steps.push('connect');
client.test();
report.steps.push('test');
for (var i = 0; i < 9; i++) {
    client.fetch({ limit: 1 });
    report.steps.push('fetch-' + i);
    sleep(10000);
}
client.close();
report.closed = client.isClosed === true;
report.ok = true;
var json = JSON.stringify(report).split(secret).join('****');
runtime.putProperty('mail.smoke.report', json);
files.write(MAIL_SMOKE.reportPath, json);
