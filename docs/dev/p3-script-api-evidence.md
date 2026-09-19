# P3 script API evidence (P3.1 connection and global object, P3.2 send and receive, follow-up smokes)

Evidence for roadmap P3.1 (the `mail` global, `MailClient`, `MailError`, argument
normalization, the connect smoke script) collected on 2026-09-18, and for P3.2 (the send and
receive methods of `MailClient`, the result decoration, the `docs/smoke` scripts) collected on
2026-09-19, in the host repository `../AutoJs6` (Gradle 9.7.1, AGP 9.3, Kotlin 2.3, JDK 21,
Windows 11) against plugin build 12 installed on the devices below (the last 163 run of P3.2
used a local debug build of the preset fix that became build 15). P3.3 (protocol document
table, host changelog, full host build) will extend this file.

Real accounts come from the git-ignored `mail-test-accounts.properties` and reach the device only
as instrumentation arguments; the runner `.python/run_host_script_smoke.py` masks every secret in
the Gradle output and checks the log afterwards (`report leak check: clean` on every run below).
Logs carry provider ids, address domains and durations only.

## Devices

| Device | API | Network | Used for |
| --- | --- | --- | --- |
| AVD_API_24 (x86 emulator) | 24 | host network | connect smoke against the loopback fake IMAP server, QQ Mail, 163 Mail |
| Redmi 22120RN86C (bek749scrwv4wo8h) | 33 | Wi-Fi | connect smoke against the loopback fake IMAP server, QQ Mail, 163 Mail, Gmail (token expired) |

## P3.1: host code

| File (host `app/src/main/java/org/autojs/autojs/`) | Role |
| --- | --- |
| `runtime/api/mail/MailScriptOptions.kt` | `mail.connect` argument shapes (alias or inline account) in pure Kotlin: `spec` for the plugin, `snapshot` without secrets for `client.account`, `debug` flag, `describe()` for `toString` |
| `runtime/api/mail/MailProviderCatalog.kt` | `mail.providers.list() / get(id) / resolve(address)` over the plugin's provider documents |
| `runtime/api/mail/MailScriptValues.kt` | Gson trees to plain Kotlin values (`Map` / `List` / numbers as `Long` or `Double`) for `RhinoUtils.toJsValue` |
| `runtime/api/mail/MailService.kt` | Per-runtime registry of clients, the default client, the plugin host lease |
| `runtime/api/augment/mail/Mail.kt` | The `mail` global: `connect` / `connectAsync` / `setDefault` / `close` / `default` / `providers` / `accounts` / `MailError`; every client method forwards to the default client |
| `runtime/api/augment/mail/MailClientNativeObject.kt` | The `MailClient` object |
| `runtime/api/augment/mail/MailJsErrors.kt` | The `MailError` constructor (per-scope cache) and the Kotlin-to-JavaScript error mapping |
| `runtime/api/augment/mail/MailPromises.kt` / `MailAsyncDispatcher.kt` | Promise plumbing of the `Async` methods over `ScriptAsyncDispatcher` |
| `runtime/ScriptRuntime.kt` | `Mail(this, mail).augment(target)` and `mail.close()` in the exit hook |
| `core/plugin/mail/MailPluginHost.kt` / `MailBinders.kt` / `MailJson.kt` | Refusal wait (`awaitClosed`, 3 s) for the oneway `closed` status, `Progress.debug` lines |

## P3.2: host code

| File (host `app/src/main/java/org/autojs/autojs/`) | Role |
| --- | --- |
| `runtime/api/mail/MailScriptArguments.kt` | Pure-Kotlin argument normalization of every client method (Gson trees in, operation `args` out): UIDs as number / UIDL string / message object with its `folder`, unknown keys refused by name, `date` as millis or ISO-8601, attachments as path or `{path, fileName?, mimeType?, contentId?, inline?}` mapped to `descriptorIndex` entries |
| `core/plugin/mail/MailDownloads.kt` | One streamed download end to end: `MailAttachmentSink` pipe with the request, write end closed on submit, read end drained while the call is in flight |
| `runtime/api/augment/mail/MailJsResults.kt` | Result decoration on the script thread: `date` / `receivedDate` as `Date`, addresses print as `Name <address>`, attachments and body parts carry `uid` / `folder` and bound `download` / `downloadAsync`, messages carry bound `load` / `loadAsync`, search results carry a non-enumerable `fallback` |
| `runtime/api/augment/mail/MailPromises.kt` | `OnScriptThread` results (decoration needs a Rhino context) and `launchWith` (the dispatcher reaches the work for `onProgress`) |
| `runtime/api/augment/mail/MailClientNativeObject.kt` | `send`, `folders`, `folder(path)` (`status` / `create` / `delete` / `rename`), `folderStatus`, `createFolder`, `deleteFolder`, `renameFolder`, `fetch`, `search`, `get` (alias `fetchBody`), `download`, `raw`, `setFlags` with `markRead` / `markUnread` / `flag` / `unflag`, `move`, `copy`, `delete`, `expunge`, `append`, each with an `Async` form; `defineOp` shares the arity guard, the sync bridge (`runBlocking` in the runtime's coroutine context) and the async bridge |
| `runtime/api/augment/mail/Mail.kt` | `FORWARDED` table: every client method mirrored on `mail` as a `BaseFunction` property (throws or rejects `NO_DEFAULT_ACCOUNT`) |
| `app/src/androidTest/.../MailScriptSmokeDeviceTest.kt` | `realProviderScript`: runs a script pushed to the device (`mail.smoke.script`) with `mail.smoke.provider` / `address` / `password` or `accessToken`, injects `MAIL_SMOKE` after a leading `"ui";`, reads the report file the script writes, logs the report after the leak check |

Sync methods block the script thread inside `scriptRuntime.coroutineContext`, so stopping the
script cancels the plugin call; `Async` methods return Promises settled on the script thread.
Downloads of `attachments.download` and `messages.raw` go to the script's working directory by
default (or a directory or file path given as `target`), with `MailFileNames.unique` naming unless
`overwrite: true`. The `size` of an attachment is the encoded part size the server reports
(BODYSTRUCTURE), so it only budgets the call timeout and the progress total; the sink never
enforces it against the decoded bytes.

## JVM tests (host `:app:testAppDebugUnitTest`)

| Test class | Cases | Covers |
| --- | --- | --- |
| `runtime/api/mail/MailScriptOptionsTest` | 7 | alias and inline shapes, secret split, XOAUTH2 implied by `accessToken` / `tokenProvider`, refusals |
| `runtime/api/mail/MailProviderCatalogTest` | 4 | list / get / resolve over provider documents |
| `runtime/api/mail/MailScriptValuesTest` | 4 | Gson to plain values |
| `runtime/api/mail/MailScriptArgumentsTest` | 8 | fetch / search paging and unknown keys, get with UIDs and message objects, raw and download sink descriptions, flags / transfers / deletions, folder paths, send with attachments and dates, append, the shared UID rules |
| `runtime/api/augment/mail/MailJsErrorsTest` | 4 | bare Rhino: `MailError` constructor cache, error mapping, `WrappedIllegalArgumentException` to `INVALID_ARGUMENT` |
| `runtime/api/augment/mail/MailJsResultsTest` | 5 | bare Rhino: Dates, address `toString`, attachment `uid` / `folder`, non-enumerable bound methods surviving `JSON.stringify`, bound calls reaching the binding with their receiver, list and search wrapping, arity errors as `MailError` |

Run on 2026-09-19: the two packages above, 32 tests, 0 failures.

## Device smoke: `connect-smoke.js` against the loopback fake IMAP server

`MailScriptSmokeDeviceTest.scriptConnectsTestsAndClosesAgainstTheFakeServer` starts the shared
`FakeImapServer` of P1.3 on 127.0.0.1, prepends `var MAIL_SMOKE = {port, closedPort, password}`
to `app/src/androidTest/assets/mail/connect-smoke.js` and runs it through the real script engine
(`ScriptEngineService.execute`); the script leaves a JSON report in the runtime property
`mail.smoke.report`. 75 checks: `mail.providers` / `mail.accounts` / `MailError` shape,
`connect` with an inline account against the fake server, `client.account` without the
password, `test()` and `testAsync()` (`ok`, `imap.ok`, capabilities), `setDefault` and the
forwarders, `connect('no-such-alias')` as `ACCOUNT_NOT_FOUND`, a closed port as `test()` with
`ok: false` and `imap.error.code === 'CONNECT_FAILED'` (openSession has no network, so `connect`
itself succeeds), argument refusals as `INVALID_ARGUMENT`, `close()` / `isClosed` /
`SESSION_CLOSED` after close, and no secret in the report.

| Device | Checks | `connect` | `test()` | `testAsync()` | Script | IMAP sessions (both with LOGIN) |
| --- | --- | --- | --- | --- | --- | --- |
| AVD_API_24 (P3.1 build) | 75 | 14 ms | 85 ms | 21 ms | 0.519 s | 2 |
| Redmi 22120RN86C (P3.1 build) | 75 | 61 ms | 137 ms | 63 ms | 1.480 s | 2 |
| AVD_API_24 (P3.2 build) | 75 | 12 ms | 110 ms | 17 ms | 1.729 s | 2 |

## Device smoke: real provider (`provider-smoke.js`)

`MailScriptSmokeDeviceTest.realProviderConnectAndTest` runs
`app/src/androidTest/assets/mail/provider-smoke.js` (`mail.connect({provider, address,
password})`, `test()`, `close()`) with `mail.smoke.provider` / `mail.smoke.address` /
`mail.smoke.password` instrumentation arguments and is skipped without them.

| Device | Provider | `connect` | `test()` | IMAP | SMTP | Leak check |
| --- | --- | --- | --- | --- | --- | --- |
| Redmi 22120RN86C | qq | 111 ms | 1678 ms | ok | ok | clean |
| AVD_API_24 | qq | 103 ms | 1031 ms | ok | ok | clean |

## Device smoke: send and receive (`docs/smoke/send-receive.js`, `docs/smoke/send-receive-async.js`)

`MailScriptSmokeDeviceTest.realProviderScript` runs a script of this repository pushed to
`/data/local/tmp/autojs6-mail-smoke/` by `.python/run_host_script_smoke.py <PROFILE> <serial>
--script docs/smoke/<name>.js`. Both scripts run the same 15 steps: `connect`, `createFolder`,
`folders` (the new folder is listed), `send` to the account itself with a 2035-byte text
attachment written in the working directory, `search({subject})` polled every 3 s until the
message is delivered, `load` (body carries the marker, one attachment with the sent name),
`download` of that attachment into the working directory (content compared byte for byte),
`raw` (`.eml` carries subject and body), `markRead`, `get` with `peek: true` (`seen` is true),
`move`, `folderStatus`, `fetch` of the target folder, `delete` with `expunge: true`,
`deleteFolder`, `close`. The synchronous script uses the plain methods; the `"ui";` script
chains the `Async` forms on Promises while a 50 ms `setInterval` ticker runs on the UI thread and
reports `uiBlocked` when fewer than half of the expected ticks arrived. On QQ and 163 the search
uses `fallback: 'always'` (see Lessons), on QQ the message is moved to the trash folder and a
vanished created folder is tolerated at `deleteFolder`. Every report ended with `closed: true`
and passed the leak check.

| Device | Provider | Mode | Steps | `send` | `search` (polls, path) | `load` | `download` | `raw` | `move` | `delete` | UI ticks | Sent copy |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AVD_API_24 | qq | sync | 15/15 | 1010 ms | 38.3 s (2, client) | 409 ms | 569 ms | 486 ms | 1121 ms (trash) | 1093 ms | - | server |
| AVD_API_24 | qq | async (`ui`) | 15/15 | 1155 ms | 28.9 s (2, client) | 412 ms | 498 ms (1 progress call) | 496 ms | 1185 ms (trash) | 1062 ms | 718 of 738, not blocked | server |
| AVD_API_24 | 163 | sync | 15/15 | 1163 ms | 266 ms (1, client) | 259 ms | 244 ms | 249 ms | 1732 ms (created folder) | 1611 ms | - | appended (preset before the fix) |
| AVD_API_24 | 163 | async (`ui`) | 15/15 | 1091 ms | 285 ms (1, client) | 261 ms | 261 ms (1 progress call) | 257 ms | 1770 ms (created folder) | 1566 ms | 143 of 152, not blocked | appended (preset before the fix) |
| AVD_API_24 | 163 | sync, preset fix | 15/15 | 742 ms | 9.9 s (4, client) | 263 ms | 266 ms | 258 ms | 1976 ms (created folder) | 1575 ms | - | server |
| Redmi 22120RN86C | qq | sync | 15/15 | 1539 ms | 34.9 s (2, client) | 701 ms | 732 ms | 667 ms | 1771 ms (trash) | 1617 ms | - | server |
| Redmi 22120RN86C | qq | async (`ui`) | 15/15 | 1454 ms | 30.7 s (2, client) | 699 ms | 738 ms (1 progress call) | 684 ms | 1789 ms (trash) | 1493 ms | 814 of 840, not blocked | server |
| Redmi 22120RN86C | 163 | sync | 15/15 | 1372 ms | 351 ms (1, client) | 317 ms | 306 ms | 286 ms | 1926 ms (created folder) | 1617 ms | - | appended (preset before the fix) |
| Redmi 22120RN86C | 163 | async (`ui`) | 15/15 | 1898 ms | 491 ms (1, client) | 437 ms | 420 ms (1 progress call) | 394 ms | 2387 ms (created folder) | 2002 ms | 215 of 227, not blocked | appended (preset before the fix) |
| Redmi 22120RN86C | gmail | sync (expired token) | 1/15 | - | - | - | - | - | - | - | - | `AUTH_FAILED` at `connect` (340 ms) until the maintainer refreshed `GMAIL_ACCESS_TOKEN_A` |
| Redmi 22120RN86C | gmail | sync | 15/15 | 4698 ms | 2.8 s (1, server) | 3634 ms | 3248 ms | 3470 ms | 8274 ms (created folder) | 6239 ms | - | server |
| Redmi 22120RN86C | gmail | async (`ui`) | 15/15 | 4605 ms | 3.1 s (1, server) | 3027 ms | 3377 ms (1 progress call) | 3101 ms | 5817 ms (created folder) | 5808 ms | 975 of 1004, not blocked | server |

Other numbers: `connect` 71 to 672 ms, `createFolder` 506 to 4677 ms (Gmail slowest), `folders`
84 to 1068 ms, `markRead` 241 to 3004 ms, `folderStatus` 197 to 4508 ms, `deleteFolder` 143 to
2547 ms; the raw `.eml` was 3.8 to 5.1 KB; the downloaded attachment landed as
`smoke-<ts> (1).txt` because the source file of the same name still sat in the working
directory (`MailFileNames.unique`). Gmail's server-side SEARCH found the just-delivered message
on the first poll (`fallback: server`). A first Gmail run on a Sony XQ-DQ72 (API 33) was
skipped by the test because that device had no mail plugin installed; the plugin is installed
there now.

## Follow-up smoke scripts (2026-09-19)

Three more scripts under `docs/smoke` close items that were open since P2 and P3.1; all run
through the same `--script` path of the runner.

`token-provider.js` (Gmail, Redmi 22120RN86C): `mail.connect({provider, address, tokenProvider})`
without `accessToken`; the provider returns a bogus token first and the real one afterwards.
`connect` asks the provider once (164 ms), the first `fetch` fails with `AUTH_FAILED` on the
plugin side, the host client asks the provider again and retries once, and the call succeeds
(8329 ms in total); `test()` (2491 ms, IMAP 556 ms, SMTP ok) and a second `fetch` (2903 ms) reuse
the refreshed token without asking again (2 provider calls in total); a provider returning a
number is `AUTH_FAILED` at `connect`. `test()` alone cannot trigger the refresh because it reports
endpoint failures in its result instead of throwing.

`pop3.js` (`receive: 'pop3'`): `test` (POP3 endpoint ok with capabilities), `folders` (`INBOX`
only), `fetch` 3 envelopes (UIDL string uids, newest first), `get` of the newest, `search` by its
subject (`fallback: client`, `limit: 1`), `raw`, `download` when it has an attachment, and
`markRead` / `createFolder` / `move` as `UNSUPPORTED_OPERATION`, `fetch` of another folder as
`FOLDER_NOT_FOUND`; nothing is deleted.

| Device | Provider | Steps | `test` (POP3 probe) | `folders` | `fetch` 3 | `get` | `search` | `raw` | `download` | INBOX |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Redmi 22120RN86C | 163 (`pop.163.com`) | 8/8 | 1333 ms (561 ms) | 298 ms | 1254 ms | 1037 ms | 2660 ms | 1085 ms | - (no attachment) | 17 messages |
| Redmi 22120RN86C | qq (`pop.qq.com`) | 9/9 | 1517 ms (687 ms) | 301 ms | 2461 ms | 2058 ms | 1494 ms | 2109 ms | 1836 ms | 285 messages |
| Redmi 22120RN86C | gmail (`pop.gmail.com`) | 2/8 | 4163 ms (2226 ms, XOAUTH2 login ok) | `IO_FAILED` | - | - | - | - | - | POP access disabled for the account |

The first QQ run timed out in `search` (30 s): with `limit: 5` the client filter scans up to
200 messages (`TOP` costs about 0.17 s each on QQ, 285 messages in the box) because only one
message matches; `limit: 1` stops at the first hit. The Gmail failure came from the server:
after a successful XOAUTH2 login it answers `STAT` with `[SYS/PERM] Your account is not enabled
for POP access` (plugin debug trace, `.python/run_real_account.py GMAIL_A <serial> --receive
pop3 --debug`); the mapper now reports it as `UNSUPPORTED_OPERATION` naming the cause (plugin
build 18). Verifying Gmail POP3 needs "Enable POP for all mail" in the Gmail settings of the
test account.

`hold-session.js` (QQ, AVD_API_24) is the plugin side of a host death across processes: the
script keeps an IMAP session busy (one envelope fetch every 10 s) while `adb shell am force-stop
org.autojs.autojs6` kills the host from the PC. Before the kill the plugin process (pid 20171,
uid 10253) held one ESTABLISHED connection to `imap.qq.com:993`; 2 s after the kill that
connection was in TIME_WAIT (closed by the plugin side, `/proc/net/tcp6`), the plugin process
was still alive with the same pid 30 s later and no other connection appeared, so the death
recipient of `MailSessionBinder` closed the mail session (`shutdown("host-died")`) without the
host asking. The Gradle run reports the instrumentation as killed, which is the expected
outcome of this probe.

## Second follow-up (2026-09-19, after the maintainer's input)

The maintainer enabled POP for the Gmail test account, added a Sina Mail and a 126 Mail
account to `mail-test-accounts.properties` (profiles `SINA_A` and `NETEASE126_A`, both runners
know them now) and pointed at the official release build of the host
(`app/app/release/autojs6-v6.8.0-*.apk`). The plugin builds of this section carry the preset
changes that became build 20.

### Gmail POP3 (`docs/smoke/pop3.js`, Redmi 22120RN86C)

The first run after enabling POP stopped at 6 of 8 steps: `search` did not find the message
`get` had just retrieved. Gmail serves POP3 in its own way: a session sees one batch of the
oldest mail not downloaded yet (`STAT` said 271, then 270; the highest-numbered message of the
batch dated 2018), and a message fetched with `RETR` is not served again in a later session.
Every plugin operation is one POP3 session (`Pop3Mailbox.withInbox` opens and closes the
folder), so the uid was gone. The script now searches before the first `RETR` and takes `raw`
from the second message when the first answers `MESSAGE_NOT_FOUND` (`onceOnly: true` in the
report). Second run: 8/8, `connect` 712 ms, `test` 5267 ms (probe 1950 ms, XOAUTH2 login),
`folders` 2065 ms, `fetch` 8450 ms, `search` 6384 ms (1 hit, client), `get` 6360 ms, `raw`
12746 ms (8.6 KB, second message), `unsupported` 148 ms; no `download` because the retrieved
message is not there to ask again. 163 and QQ keep serving a message after `RETR`, and so do
126 (`pop.126.com`, 8/8: `test` 1347 ms with a 604 ms probe, `fetch` 1474 ms, `search` 1044 ms,
`get` 1518 ms, `raw` 1914 ms of the same message, 5 messages, no attachment) and Sina
(`pop.sina.com`, 9/9: `test` 2356 ms with a 1021 ms probe, `fetch` 2105 ms, `search` 1524 ms,
`get` 1689 ms, `raw` 1727 ms, `download` 2001 ms, 3 messages).

### `autoSavesSent` of 126 Mail and Sina Mail (`docs/smoke/sent-copy.js`)

`sent-copy.js` sends two messages to the account itself, the first with `saveToSent: false`,
counts the copies of each subject in the sent folder (client filter, up to 90 s, then a 20 s
grace for a late duplicate) and deletes the probe messages from the sent folder and INBOX.

| Device | Provider | Sent folder | `saveToSent: false` | Default | Verdict |
| --- | --- | --- | --- | --- | --- |
| Redmi 22120RN86C | 126 (`imap.126.com`) | `已发送` (special-use `sent`, also the preset) | `sentCopy: server`, 1 copy after 21 s | `sentCopy: server`, 1 copy | the server keeps a copy; preset `autoSavesSent: true` confirmed |
| Redmi 22120RN86C | sina (`imap.sina.com`) | `已发送` (special-use `sent`; the preset had `null`) | `sentCopy: none`, 0 copies after 95 s | `sentCopy: appended`, 1 copy | the server keeps no copy; preset `autoSavesSent: false` confirmed, the preset now names the folder |

The plugin round trip (`.python/run_real_account.py`, Sony XQ-DQ72, API 33) passed for both
accounts: 126 `sentCopy=server` (send 1842 ms), Sina `sentCopy=appended` into `已发送` (send
2877 ms), leak checks clean.

### Sina Mail and the send-receive smokes

The first Sina run of `send-receive.js` stopped at `createFolder` with `SERVER_ERROR: the
server did not create the folder`. A probe with three names (ASCII, hyphenated, Chinese) got
the same answer for each, the folder never appeared in `LIST`, and `folderStatus` /
`deleteFolder` said `FOLDER_NOT_FOUND`: `imap.sina.com` answers NO to every CREATE (folders
exist only through its web UI; the account has 其它邮件 / 商讯信息 / 星标邮件 / 网站通知 /
订阅邮件 beside the special-use ones). The preset notes say so now, and both smokes skip the
folder steps on Sina (`noFolderCreation`: `createFolder` must answer `SERVER_ERROR`, the message
moves to the trash folder, 13 steps instead of 15).

| Device | Provider | Form | Steps | `send` | `search` (polls, fallback) | `load` | `download` | `raw` | `move` (trash) | `delete` | Ticks |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Redmi 22120RN86C | sina | sync | 13/13 | 4125 ms (`appended`) | 5.6 s (2, client) | 1147 ms | 1328 ms | 1095 ms | 2937 ms | 2323 ms | - |
| Redmi 22120RN86C | sina | async (`ui`) | 13/13 | 4612 ms (`appended`) | 1.5 s (1, client) | 1509 ms | 1197 ms (1 progress call) | 1264 ms | 2901 ms | 2797 ms | 419 of 436, not blocked |

### The official signing certificate and the release host

`CallerPolicy` accepts a host whose signer set equals the plugin's own; there is no separate
list of official certificates. The maintainer's release build of the host
(`app/app/release/autojs6-v6.8.0-*.apk`, versionCode 5282, built 2026-09-19 01:42, after the
P3.2 and P3.3 commits) is signed by the certificate with SHA-256
`31a681fcfffb3e428420cae280ded89292b12a3b0f59e19b7a73e32a8ae4c213`. `apksigner verify
--print-certs` shows the same digest on the plugin's release build (`:app:assembleRelease` with
the git-ignored `sign.properties` and `app/sm003.jks`) and on the host and plugin packages pulled
back from the emulator, the Redmi and the Sony: the plugin's `sign.properties` signs the debug
build type as well, so every smoke of P3 already ran with both sides under the official
certificate.

Cross-process on the emulator (AVD_API_24, release host plus release plugin, no Gradle): a
script started through `RunIntentActivity` (`am start -n
org.autojs.autojs6/org.autojs.autojs.external.open.RunIntentActivity -d file:///sdcard/mail-guard.js`)
connected an inline account against a closed loopback port. `connect` succeeded (the plugin
accepted the caller), `test()` answered `CONNECT_FAILED`, `close()` closed; the report went to
logcat through `android.util.Log`. The negative control, the same release host with a plugin
signed by the default debug key (digest `2e64822e...`), was refused before the plugin's guard
could run: `PLUGIN_UNAVAILABLE: mail plugin is unavailable: Plugin "..." is not authorized in
Plugin Center` (the host checks plugin signatures on its side). R8 on both sides did not get
in the way. The emulator holds the debug host and plugin again.


## Lessons

- The host's Rhino 2.0 snapshot derives `TopLevelScope` from `ScopeObject`, a
  `SlotMapOwner<VarScope>` that is not a `ScriptableObject`; a per-scope cache (the `MailError`
  constructor) must be keyed through `SlotMapOwner.associateValue`, otherwise every call
  re-evaluates the constructor and `instanceof` fails.
- `IMailSessionCallback` is `oneway`: the `closed` status of a refused `openSession` may land
  after the null return, so the host used to answer `PLUGIN_UNAVAILABLE` ("mail plugin refused
  the account") instead of the plugin's code. `MailSessionCallbackBinder.awaitClosed` (3 s)
  closes the gap.
- A `BaseFunction` body returning a `LinkedHashMap` hands the script a `NativeJavaObject`
  (`JSON.stringify` gives `undefined`); sync results go through `RhinoUtils.toJsValue` like the
  async dispatcher already does.
- The AGP test engine installs the host APK without `-r`. On the Redmi, `adb install -r` of the
  matching arm64 split made the engine skip the install by digest as in P1.3; on the API 24
  emulator the same recipe still failed with `INSTALL_FAILED_ALREADY_EXISTS`, and
  `adb uninstall org.autojs.autojs6` reported `DELETE_FAILED_INTERNAL_ERROR` while removing the
  package anyway, after which the engine installed fresh.
- (P3.2) `Augmentable` resolves `selfAssignmentFunctions` by reflection to same-named methods,
  so a generated forwarder table cannot use it; the `mail` forwarders are `BaseFunction`
  properties (`selfAssignmentProperties`) built from one `FORWARDED` list.
- (P3.2) `ArgumentGuards` messages need the application context, so the bound methods of
  decorated results (which the bare-Rhino JVM tests exercise) check their arity themselves and
  throw `MailError` (`INVALID_ARGUMENT`) through `MailJsErrors.jsException`.
- (P3.2) A KDoc line containing `` `docs/smoke/*.js` `` opens a nested block comment for the
  Kotlin compiler ("Unclosed comment"); write "the `docs/smoke` scripts" instead.
- (P3.2) 163 Mail keeps a server copy of every message sent through SMTP (a message sent with
  `saveToSent: false` appeared in `已发送` a few minutes later), so the preset `autoSavesSent:
  false` doubled every sent message; the preset is `true` for 163 and 126 since plugin build 15.
  Its IMAP `SEARCH` by `SUBJECT` or `FROM` answers OK with no hits for just-delivered mail
  (`SINCE` works), so the smoke scripts search with `fallback: 'always'` on 163 as on QQ.
- (P3.2) On QQ a folder created over IMAP is usable for a few seconds (`STATUS`, `COPY` into it)
  and then answers `FOLDER_NOT_FOUND` to `STATUS` / `COPY` / `MOVE` / `DELETE` (observed 60 s
  and 65 s after creation on both devices; a nested folder under `其他文件夹` cannot be created
  at all, and a folder holding messages refuses `DELETE`). The cause was not determined; the
  smoke scripts move the message to the trash folder on QQ and tolerate the vanished folder.
- (P3.2) A `"ui";` script runs in an Activity whose engine the execution does not expose, so the
  device test reads the report from a file the script writes (`MAIL_SMOKE.reportPath`) instead
  of a runtime property; the `MAIL_SMOKE` prelude is inserted after the directive line.
- (follow-up) A plugin-repository instrumentation run (`.python/run_real_account.py`) uninstalls
  the plugin from the device when it ends (AGP default), so a host smoke started afterwards on
  the same device is skipped with "the mail plugin is not installed"; reinstall the plugin APK
  first.
- (follow-up) POP3 `search` on a big QQ box needs a small `limit`: the client filter scans until
  `limit` messages matched or 200 were seen, at about 0.17 s per `TOP`.
- (follow-up 2) Gmail POP3 hands out one batch of old mail per session and never serves a
  message again once `RETR` fetched it; a POP3 script must search or `TOP` before it loads a
  body, and `raw` / `download` of the same message afterwards answer `MESSAGE_NOT_FOUND`.
- (follow-up 2) Sina Mail refuses IMAP CREATE for any name; a script that needs its own folder
  has to work in the trash folder or an existing one.
- (follow-up 2) Under `MSYS_NO_PATHCONV=1` (needed so `adb pull /data/app/...` keeps its
  path) local files must be given as `D:/...`, not `/d/...`; `/data/app` of the API 24 emulator
  is readable only after `adb root`; `pm grant` against a package that failed to install prints
  the whole `pm` usage, so check the install line first.
- (follow-up 2) A host Gradle run can die in `processAppDebugResources` with "Couldn't delete
  R.jar" while another process holds the file; the smoke never starts, so rerun it.

## Reproducing

```
cd ../AutoJs6
./gradlew :app:testAppDebugUnitTest --tests "org.autojs.autojs.runtime.api.mail.*" --tests "org.autojs.autojs.runtime.api.augment.mail.*" --tests "org.autojs.autojs.core.plugin.mail.*"
ANDROID_SERIAL=<serial> ./gradlew :app:connectedAppDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=org.autojs.autojs.runtime.api.augment.mail.MailScriptSmokeDeviceTest"
cd ../AutoJs6-Plugin-Angus-Mail
python .python/run_host_script_smoke.py QQ_A <serial>
python .python/run_host_script_smoke.py QQ_A <serial> --script docs/smoke/send-receive.js
python .python/run_host_script_smoke.py NETEASE_A <serial> --script docs/smoke/send-receive-async.js
python .python/run_host_script_smoke.py GMAIL_A <serial> --script docs/smoke/send-receive.js
python .python/run_host_script_smoke.py GMAIL_A <serial> --script docs/smoke/token-provider.js
python .python/run_host_script_smoke.py NETEASE_A <serial> --script docs/smoke/pop3.js
python .python/run_host_script_smoke.py QQ_A <serial> --script docs/smoke/hold-session.js   # then: adb -s <serial> shell am force-stop org.autojs.autojs6
python .python/run_host_script_smoke.py GMAIL_A <serial> --script docs/smoke/pop3.js           # POP enabled in the Gmail settings, fresh token
python .python/run_host_script_smoke.py NETEASE126_A <serial> --script docs/smoke/sent-copy.js
python .python/run_host_script_smoke.py SINA_A <serial> --script docs/smoke/sent-copy.js
python .python/run_host_script_smoke.py SINA_A <serial> --script docs/smoke/send-receive.js
python .python/run_real_account.py SINA_A <serial>
./gradlew :app:assembleRelease   # with sign.properties; then apksigner verify --print-certs on this APK and the host release APK
```

The host must already be installed on the device with the current debug build (`adb install -r`
of the matching split on the Redmi, uninstall first on the API 24 emulator). Reports are logged
as `MailScriptSmokeTest` lines (`adb logcat -s MailScriptSmokeTest`) after the leak check; the
runner's Gradle log lands under `build/p3/`.
