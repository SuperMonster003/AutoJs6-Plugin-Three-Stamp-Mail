// "On mail arrived" task smoke for the plugin's background watches (mail roadmap P8 device matrix).
//
// AutoJs6 starts this script through its "On mail arrived" intent task whenever the Angus Mail
// plugin's background watch reports a new message (the plugin's MAIL_TRIGGER broadcast, mail
// contract version 2). The event document arrives as `engines.myEngine().execArgv.mail`
// (watch id, account alias, address, folder, the message envelope, receivedAt) and the wake-up
// intent as `execArgv.intent`. The script records one JSON line per launch: the device clock
// (`atEpoch`, so `.python/run_trigger_matrix.py` can compute the latency from its send time),
// the watch id, the message uid and subject, the sender domain (never the address), `receivedAt` and the launch delay between the
// plugin's delivery and this script. The line goes to logcat (tag MailTriggerSmoke, the
// driver's source of truth) and is appended to `<host external files>/mail-trigger/report.jsonl`.
// Subjects of the matrix are `AutoJs6 trigger <stamp> #n`; nothing secret reaches this script.

var argv = engines.myEngine().execArgv || {};
var event = argv.mail || null;
var now = Date.now();
var message = event && event.message ? event.message : null;
var line = {
    atEpoch: now,
    hasEvent: !!event,
    type: event ? event.type : null,
    triggerId: event ? event.triggerId : null,
    alias: event ? event.alias : null,
    folder: event ? event.folder : null,
    uid: message ? String(message.uid) : null,
    subject: message ? String(message.subject || '') : null,
    fromDomain: message && message.from && message.from.address ? String(message.from.address).replace(/^[^@]*@/, '@') : null,
    receivedAt: event ? event.receivedAt : null,
    launchDelayMs: event && event.receivedAt ? now - event.receivedAt : null,
    intentAction: argv.intent ? String(argv.intent.getAction()) : null,
    engine: String(engines.myEngine().id),
};
var text = JSON.stringify(line);
android.util.Log.i('MailTriggerSmoke', text);
try {
    var dir = files.join(context.getExternalFilesDir(null).getAbsolutePath(), 'mail-trigger');
    files.ensureDir(dir + '/');
    files.append(files.join(dir, 'report.jsonl'), text + '\n');
} catch (e) {
    android.util.Log.w('MailTriggerSmoke', 'report file: ' + e);
}
console.log('mail trigger: ' + (event ? event.triggerId + ' uid ' + line.uid : 'no event'));
