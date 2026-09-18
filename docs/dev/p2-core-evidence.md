# P2 mail core evidence (P2.1 sessions, P2.2 sending)

Evidence for roadmap P2.1 (account layer, `session.test`) and P2.2 (`mail.send`, `saveToSent`,
`messages.append`), collected on 2026-09-18 with Gradle 9.5.0, AGP 9.3.2, Kotlin 2.3.20, JDK 21
(Windows 11), Eclipse Angus Mail 2.0.5, GreenMail 2.1.13. It also closes roadmap P0.2 item 2 (the
real-provider round trip), which `docs/dev/p0-spike-evidence.md` had left open.

Real accounts come from the git-ignored `mail-test-accounts.properties` and reach the device only
as instrumentation arguments; the runner `.python/run_real_account.py` masks every secret in the
Gradle output, the logcat excerpt and the XML report check (`report leak check: clean` on every
run below). Logs carry provider ids, host names, folder names, capabilities, sizes and durations
only. Addresses are omitted here on purpose.

## Devices

| Device | API | Network | Used for |
| --- | --- | --- | --- |
| AVD_API_24 (x86 emulator) | 24 | host network | contract and descriptor tests, debug and release (R8) |
| Sony XQ-AT72 (QV710AF65F) | 31 | Wi-Fi | Gmail (XOAUTH2 access token from the OAuth Playground, scope `https://mail.google.com/`) |
| Redmi 22120RN86C (bek749scrwv4wo8h) | 33 | Wi-Fi | QQ Mail (authorization code), two accounts |

Devices with Wi-Fi off (only a VPN `tun0` interface) fail with `CONNECT_FAILED` within a few
seconds; the Xiaomi 23046RP50C of the P0 runs was in that state and was not used.

## P2.1: `session.test` against real providers

Account JSON built exactly as the Binder builds it (`provider` preset, `timeout.connect` 20 s,
`timeout.read` 60 s), one `MailSession`, `session.test()`:

| Provider | Device | IMAP | SMTP | Capabilities reported |
| --- | --- | --- | --- | --- |
| Gmail (XOAUTH2) | Sony API 31 | `imap.gmail.com:993/ssl` ok, 3113 ms | `smtp.gmail.com:465/ssl` ok, 3550 ms | IMAP: IDLE, UIDPLUS, MOVE, CONDSTORE, ID, ENABLE, NAMESPACE, SPECIAL-USE, LIST-EXTENDED, LIST-STATUS, UNSELECT, CHILDREN, XLIST, ESEARCH, LITERAL-, COMPRESS=DEFLATE, UTF8=ACCEPT; SMTP: SIZE, 8BITMIME, SMTPUTF8, PIPELINING, AUTH, CHUNKING, ENHANCEDSTATUSCODES |
| QQ Mail (authorization code) | Redmi API 33 | `imap.qq.com:993/ssl` ok, 971 ms | `smtp.qq.com:465/ssl` ok, 775 ms | IMAP: IDLE, UIDPLUS, MOVE, ID, NAMESPACE, CHILDREN, XLIST, COMPRESS=DEFLATE; SMTP: SIZE, 8BITMIME, SMTPUTF8, PIPELINING, AUTH |

The same runs sent one plain message through `SmtpSender` (Gmail 3512 ms, QQ 908 ms) and listed
the five newest inbox messages (Gmail 2560 ms, QQ 756 ms), which is the P0.2 item 2 round trip.

## P2.2: `mail.send`, `saveToSent`, `messages.append`

`MailSession.send` with one text attachment (`FileAttachmentSource`), then `MailSession.append`
of an unsent draft with `\Draft \Seen`, then the inbox listing; the QQ run sends from account A to
account B and polls B's inbox every 5 s (`listInbox(10)`, matched by subject):

| Step | QQ Mail, Redmi API 33 | Gmail, Sony API 31 |
| --- | --- | --- |
| `session.test` | imap 927 ms, smtp 901 ms | imap 2800 ms, smtp 2119 ms |
| `mail.send` (1 recipient, 1 attachment) | 1102 ms, `sentCopy = server` | 2345 ms, `sentCopy = server` |
| `messages.append` | `Drafts`, UID 2, 2019 ms | `[Gmail]/Drafts`, UID 327, 5613 ms |
| inbox listing (5) | 876 ms | 2460 ms |
| peer inbox | received after 1535 ms, first poll, 2335 bytes, unread | not run (single account) |

Redacted `debug` traces of the runs (roadmap D28) show only command summaries, for example
`smtp send recipients=1 attachments=1 ok 836ms` and `imap append Drafts ok 1620ms`.

### QQ refuses a second sent copy

The first P2.2 run forced `saveToSent = true` on QQ. The preset names `Sent Messages` as the sent
folder, the folder exists, `APPEND` was issued with `\Seen`, and the server answered:

```
A5 NO Mail has saved by smtp!
```

QQ files the copy of every message that leaves through its SMTP and rejects a duplicate by
Message-ID. This confirms `autoSavesSent = true` for QQ and led to roadmap decision D33: an
explicit `saveToSent: true` never appends a copy on a provider whose preset says the server files
it (Gmail would show a duplicate instead of refusing). `SendResult.sentCopy` reports the outcome:
`server` (the provider keeps it), `appended` (the plugin appended it to `sentFolder`), `failed`
(`saveError` set, the message itself was sent) or `none` (no copy wanted, send-only account, or
no sent folder found; the plugin never creates one). After the change the same run reports
`sentCopy = server` without an error on both providers.

## Descriptor ownership through `IMailSession.call` (API 24 emulator)

`AngusMailPluginContractTest` binds the real service in-process, so the descriptors reach
`MailSessionBinder` as the objects the test created and their state is observable afterwards:

| Call | Outcome |
| --- | --- |
| `session.test` with one descriptor | `INVALID_ARGUMENT` "does not take descriptors", copy closed before `onResult` |
| `messages.append` referencing `descriptorIndex` 1 with one descriptor | `INVALID_ARGUMENT` "has no descriptor (1 supplied)" |
| `mail.send` with a pipe | `INVALID_ARGUMENT` "descriptor 0 is not a seekable file" (`ESPIPE`) |
| `mail.send` with 65 descriptors | `LIMIT_EXCEEDED`, not retryable, all 65 copies closed |
| well-formed `messages.append` with one file | reaches the mail core (`CONNECT_FAILED` on the unmapped loopback port, or the appended folder with GreenMail mapped) |

Both `connectedDebugAndroidTest` and `connectedReleaseAndroidTest -PandroidTestRelease` (R8)
pass 6 tests with the real-account test skipped.

## JVM

`:mail-core:test`: 85 tests. New in P2.2: `OutgoingMessageParserTest` (9), `MessageComposerTest`
(7), `SmtpSendGreenMailTest` (9). `mail.mime.allowutf8` was dropped from the session properties
so display names and subjects are always RFC 2047 encoded on the wire (decision D34).

`:app:testDebugUnitTest`: 21 tests; `RequestRouterTest` (6) snapshots the op table with
`mail.send` and `messages.append`, checks that descriptors belong to the transfer ops only, and
that argument errors of both ops surface before any connection is made.

## Reproducing

```
python .python/run_real_account.py QQ_A <serial> --peer QQ_B --append Drafts --debug
python .python/run_real_account.py GMAIL_A <serial> --append "[Gmail]/Drafts" --debug
python .python/run_real_account.py QQ_A <serial> --save-sent true    # shows sentCopy = server
```

Profiles read `QQ_USER_NAME_A` / `QQ_AUTH_CODE_A`, `QQ_USER_NAME_B` / `QQ_AUTH_CODE_B` and
`GMAIL_USER_NAME_A` / `GMAIL_ACCESS_TOKEN_A` from `mail-test-accounts.properties`; the Gmail
token from the OAuth Playground is valid for about one hour.
