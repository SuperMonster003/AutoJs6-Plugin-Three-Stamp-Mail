# P2 mail core evidence (P2.1 sessions, P2.2 sending, P2.3 receiving)

Evidence for roadmap P2.1 (account layer, `session.test`), P2.2 (`mail.send`, `saveToSent`,
`messages.append`) and P2.3 (folders, listing, search, bodies, downloads, flags), collected on 2026-09-18 with Gradle 9.5.0, AGP 9.3.2, Kotlin 2.3.20, JDK 21
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
| Sony XQ-AT72 (QV710AF65F) | 31 | Wi-Fi | Gmail (XOAUTH2 access token from the OAuth Playground, scope `https://mail.google.com/`), P2.3 round trip to itself |
| Redmi 22120RN86C (bek749scrwv4wo8h) | 33 | Wi-Fi | QQ Mail (authorization code), two accounts; 163 Mail to yeah.net (authorization codes), P2.3 |

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

## P2.3: folders, listing, search, bodies, downloads and flags

The same runner, now with `--cleanup`: after the send, the receiving side (the peer account, or
the account itself when it is its own recipient) polls `messages.list` for the message and runs
every P2.3 operation against it. Logs carry folder paths, roles, counts, sizes and durations.

### QQ Mail, account A to account B (Redmi 22120RN86C, API 33)

| Step | Result |
| --- | --- |
| `session.test` | imap 1108 ms, smtp 1003 ms |
| `mail.send` (1 attachment) | 949 ms, `sentCopy = server` |
| `messages.list` (sender, 5) | 1016 ms |
| peer `messages.list` polling | received after 1977 ms, first poll, 2356 bytes, unread, `hasAttachments = true` |
| `folders.list` with counts | 11 folders in 1041 ms; roles from `XLIST`: `Deleted Messages` = trash, `Drafts` = drafts, `INBOX` = inbox, `Junk` = junk, `Sent Messages` = sent, `其他文件夹/Archive` = archive; `其他文件夹` is a non-selectable parent with five children |
| `folders.status INBOX` | 385 messages, 3 unseen, `uidNext` / `uidValidity` present (323 ms) |
| `messages.search` by Message-ID, server | 0 hits in 4 attempts over 16 s (each `SEARCH` about 80 ms) |
| `messages.search` with `fallback: "always"` | 1 hit via the client in 9.6 s (ENVELOPE prefetch of 385 messages, filter 23 ms) |
| `messages.search` by subject, client | 5 hits in about 7 s, ours included |
| `messages.list` with `unseenOnly` | 3 messages, ours included (`SEARCH UNSEEN` 79 ms) |
| `messages.get` (peek) | 680 ms: 49 characters of text, 12 headers, 1 attachment `2:text/plain:40`, still unread |
| `attachments.download` part 2 | 28 bytes, byte-identical to what was sent, 678 ms, 1 progress report |
| `messages.raw` | 2359 bytes in 677 ms, re-parsed subject matches |
| `messages.setFlags` add then remove | about 220 ms each, verified through `messages.get` |
| `messages.delete` with `expunge` | uid gone, `messages.get` answers `MESSAGE_NOT_FOUND` |

Two provider facts came out of this run and shaped the search design:

- QQ replaces the Message-ID of outgoing mail: the id the receiver holds differs from
  `SendResult.messageId`, so the device test searches for the stored id (`messages.get` and the
  listing agree on it).
- QQ answers `OK` to `SEARCH HEADER Message-ID` but returns nothing for a message that arrived
  seconds earlier, so a failed server search is not the only case a script needs the client
  filter. `messages.search` therefore accepts `fallback: "always"`, and the compiled query
  evaluates `messageId` on the client from the ENVELOPE instead of fetching every header (the
  first attempt with a HEADERS prefetch took 35 s for 384 messages; the ENVELOPE path takes 9.6 s).

### 163 Mail to yeah.net (Redmi 22120RN86C, API 33)

Two NetEase accounts: the sender is a 163.com mailbox, the receiver a yeah.net mailbox (the same
servers policy on its own hosts `imap.yeah.net` / `smtp.yeah.net`, the `163` preset otherwise);
runner profiles `NETEASE_A` / `NETEASE_B`.

| Step | Result |
| --- | --- |
| `session.test` | imap 628 ms (`UIDPLUS`, `ID`, `SPECIAL-USE`, `XLIST`, `LITERAL+`, `AUTH=XOAUTH2`), smtp 675 ms |
| `ID` handshake | 60 ms on every connection: connection 1 right after the login, connection 2 before the `APPEND` while the sent folder held the first one |
| `mail.send` (1 attachment) | 1671 ms, `sentCopy = appended` into `已发送` (854 ms; the preset's `autoSavesSent = false` holds) |
| `messages.list` (sender, 5) | 408 ms |
| peer `messages.list` polling | received after 914 ms, first poll, 1824 bytes, unread, `hasAttachments = true` |
| `folders.list` with counts | 6 folders in 480 ms; roles: `INBOX` = inbox, `垃圾邮件` = junk, `已删除` = trash, `已发送` = sent, `草稿箱` = drafts, `病毒文件夹` without a role |
| `folders.status INBOX` | 7 messages, 4 unseen, `uidValidity` 1, no `uidNext` (163 omits UIDNEXT) (244 ms) |
| `messages.search` by Message-ID, server | 1 hit on the first attempt, 411 ms (the receiver keeps the sender's Message-ID) |
| `messages.search` by subject, client | 4 hits, ours included |
| `messages.list` with `unseenOnly` | 4 messages, ours included (`SEARCH UNSEEN` 58 ms) |
| `messages.get` (peek) | 456 ms: 49 characters of text, 13 headers, 1 attachment `2:text/plain:42`, still unread |
| `attachments.download` part 2 | 28 bytes, byte-identical, 397 ms, 1 progress report |
| `messages.raw` | 1824 bytes in 359 ms, re-parsed subject matches |
| `messages.setFlags` add then remove | verified through `messages.get` |
| `messages.delete` with `expunge` | `UID EXPUNGE` answered `BAD Parse command error`; the folder was expunged as a whole (1862 ms), uid gone |

Two NetEase facts changed the mail core during this run:

- `Unsafe Login` on every unidentified connection. The first attempt sent `ID` once through
  `IMAPStore.id` right after `connect` and still failed: Angus Mail opens a second connection when
  a store command (`LIST`, `STATUS`, `hasCapability`) runs while a folder holds the first one, and
  drops the surplus connection once the folder closes; that second connection (tag prefix `B`)
  got `NO SELECT Unsafe Login`. `IdentifyingImapStore` now overrides `newIMAPProtocol` and sends
  `ID` after every successful authentication, whichever mechanism Angus picked, so every pooled
  connection is identified. `EXAMINE` works once the connection is identified (an intermediate
  build selected folders read-write instead; that workaround is gone).
- `UID EXPUNGE` is refused with `BAD` although `UIDPLUS` is advertised. `ImapMailbox` remembers
  the refusal per store and expunges the whole folder from then on (RFC 3501 `EXPUNGE`, every
  `\Deleted` message of the folder), the path servers without `UIDPLUS` take anyway.

### Gmail to itself (Sony XQ-AT72, API 31)

The Gmail account sends to its own address with an OAuth Playground access token (XOAUTH2), so
the receiving side is the same session.

| Step | Result |
| --- | --- |
| `session.test` | imap 2427 ms (`IDLE`, `UIDPLUS`, `MOVE`, `CONDSTORE`, `ID`, `SPECIAL-USE`, `LIST-STATUS`, `ESEARCH`, `UTF8=ACCEPT`, ...), smtp 2320 ms |
| `mail.send` (1 attachment) | 2548 ms, `sentCopy = server` (`[Gmail]/Sent Mail` listed afterwards with 231 messages, 1 unseen) |
| `messages.list` (5) | 2178 ms |
| own inbox polling | received after 2154 ms, first poll, uid 18471, 1125 bytes, unread, `hasAttachments = true` |
| `folders.list` with counts | 22 folders in 11.8 s (one `STATUS` per selectable folder, INBOX holds 13790 messages); roles from the RFC 6154 `LIST` attributes: `INBOX` = inbox, `[Gmail]/Drafts` = drafts, `[Gmail]/Sent Mail` = sent, `[Gmail]/Spam` = junk, `[Gmail]/Starred` = flagged, `[Gmail]/Trash` = trash; `[Gmail]` is a non-selectable parent, `Sync Issues` has three children |
| `folders.status INBOX` | 13790 messages, 1 unseen, `uidNext` 18472, `uidValidity` 3 (1955 ms) |
| `messages.search` by Message-ID, server | 1 hit on the first attempt, 4186 ms (Gmail keeps the Message-ID and indexes at once) |
| `messages.search` by subject, client | 4 hits, ours included |
| `messages.list` with `unseenOnly` | 1 message, ours |
| `messages.get` (peek) | 3280 ms: 49 characters of text, 9 headers, 1 attachment `2:text/plain:40`, still unread |
| `attachments.download` part 2 | 28 bytes, byte-identical, 3273 ms (the transfer itself 441 ms), 1 progress report |
| `messages.raw` | 1125 bytes in 3171 ms (the transfer itself 577 ms), re-parsed subject matches |
| `messages.setFlags` add then remove | verified through `messages.get` |
| `messages.delete` with `expunge` | `UID EXPUNGE` in 2915 ms, uid gone |

Gmail figures are dominated by the round trip to the server from this network (about 2 s per
connection, 1 to 3 s per folder open); the operations themselves behave like the IMAP baseline.

### GreenMail (JVM)

`ImapOperationsGreenMailTest` seeds 120 messages through `APPEND` and checks the cursor paging in
both directions (`before` / `after`, deleted cursor UIDs included), `unseenOnly` paging, server
and client searches (GreenMail matches `FROM` / `TO` on whole addresses only), `messages.get` with
`peek` true / false and `includeRaw`, an over-budget text body landing in `bodyParts` and staying
downloadable, a 10 MiB attachment streamed with 20 progress reports and compared byte for byte,
the raw source re-parsed, a sink that fails after 200 KB (`IO_FAILED`, the IMAP connection is
kept: one connect for the whole test), flags in the three modes, `MOVE` and the
`COPY` + `\Deleted` + `UID EXPUNGE` path, copy, delete with and without expunge and expunge
counts.

`IdentifyingImapStoreTest` scripts a minimal IMAP server on the loopback interface (CAPABILITY,
LOGIN, ID, LIST, SELECT / EXAMINE, UID FETCH, UID EXPUNGE answered with `BAD`, EXPUNGE, LOGOUT)
and checks that every pooled connection sends `ID` once, after `LOGIN` and before any folder is
opened, with the account's payload; that an empty `clientId` sends nothing (the scripted server
then refuses the folder with `Unsafe Login`, as NetEase does); that a server without the `ID`
capability is left alone; and that `messages.delete` with `expunge` falls back to a plain
`EXPUNGE` after the `BAD` and records `uidExpungeRefused`.

## P2.4: the POP3 degraded path

The runner's `--receive pop3` makes the receiving side read over POP3: `QQ_A` sends over SMTP
(IMAP for its own listing) and `QQ_B` receives through `pop.qq.com:995`. Its POP3 view holds
1305 messages, which made the cost of the protocol visible.

### QQ Mail, account A to account B over POP3 (Redmi 22120RN86C, API 33)

| Step | Result |
| --- | --- |
| peer `session` connect | `pop.qq.com:995/ssl` in 700 ms |
| peer `messages.list` polling (limit 10) | received after 14.5 s, second poll; each poll is one `UIDL` for all 1305 messages (about 200 ms) plus `TOP` + `LIST` per listed message (10 messages in about 3 s); the uid is a 30-character opaque string, `seen = false`, `hasAttachments = true` from the Content-Type |
| `folders.list` with counts | `INBOX` alone, 1305 messages, `unseen = null`, 1.0 s (`STAT`) |
| `folders.status INBOX` | `UNSUPPORTED_OPERATION` before any command |
| `messages.search` by Message-ID, `limit: 1` | 1 hit in 1.8 s: the scan stopped after the first candidate (`client filter scanned 1 matched 1`) |
| `messages.search` by subject, `limit: 5` | 5 hits in 1.2 s, ours included, 5 candidates scanned |
| `messages.search` by Message-ID, `limit: 5` (earlier run) | 1 hit in 39.6 s: one hit cannot fill the limit, so all 200 candidates of the window were scanned at about 190 ms each |
| `messages.search` with `text`, `messages.list` with `unseenOnly` | `UNSUPPORTED_OPERATION` before any command |
| `messages.get` | 1.8 s (`RETR`): 49 characters of text, 12 headers, 1 attachment `2:text/plain:40` |
| `attachments.download` part 2 | 28 bytes, byte-identical to what was sent, 1.5 s (a second `RETR`) |
| `messages.raw` | 2377 bytes in 2.0 s, re-parsed subject matches |
| `messages.setFlags`, `move`, `expunge`, `folders.status` | `UNSUPPORTED_OPERATION` |
| `messages.delete` | `DELE` then `QUIT` commits (`close INBOX commit` 385 ms); the next `UIDL` counts 1304 and `messages.get` answers `MESSAGE_NOT_FOUND` |

The first version of the POP3 search reused the IMAP window (`MAX_CLIENT_FILTER`, 2000) and
prefetched the envelopes of the whole window before filtering. Angus Mail implements the POP3
`ENVELOPE` prefetch as one `TOP n 0` plus one `LIST n` per message, so the search over 1305
messages took 428 s (and 387 s on the second search of the same run). The search now walks the
window newest first, loads the headers of one candidate at a time (`TOP` only, the `LIST` runs for
the matched page) and stops as soon as `limit` messages match, with the window cut to
`MAX_POP3_CLIENT_FILTER` (200). A miss is therefore bounded by about 40 s against QQ; a search
for something recent costs a few round trips. QQ rewrites the outgoing Message-ID, and the POP3
view shows the rewritten id too, so the device test searches for the id the listing reported.

### GreenMail (JVM)

`Pop3OperationsGreenMailTest` seeds the mailbox through IMAP `APPEND` (GreenMail serves one
store over both protocols) and checks, over POP3: `folders.list` answering `INBOX` alone with the
`STAT` count and no unseen count, the nine IMAP-only operations and any folder other than `INBOX`
refused before a connection (the connect count stays at one), UIDL cursor paging in both
directions over headers only (`hasAttachments` from `multipart/mixed`, uids serialized as
strings), `messages.get` with `includeRaw`, an attachment download compared byte for byte, the
raw source re-parsed, missing uids and parts, client-side header searches (`subject`, `from` with
`not`, `header`, `messageId`, `sentSince`, `before`), the scan count in the trace (`limit: 1`
scans one candidate, a miss scans all seven), `DELE` committed when the folder closes with the
IMAP view of the same mailbox agreeing, and `session.test` / `mail.send` for a POP3 account
(no sent copy without IMAP).

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
| `messages.raw` with one pipe write end (P2.3) | reaches the mail core; the write end is closed before `onResult` and the read end sees EOF with nothing written |
| `attachments.download` with two write ends | `INVALID_ARGUMENT` "single descriptor (2 supplied)", both closed |
| `messages.raw` without a descriptor | `INVALID_ARGUMENT` "(0 supplied)" |
| `attachments.download` without `partId` | `INVALID_ARGUMENT` "'partId' is required", the write end is still released |
| `messages.get` with a file descriptor | `INVALID_ARGUMENT` "does not take descriptors" |
| `folders.status` without `folder` | `INVALID_ARGUMENT` "'folder' is required" (argument errors of receive ops travel through the Binder) |

Both `connectedDebugAndroidTest` and `connectedReleaseAndroidTest -PandroidTestRelease` (R8)
passed 6 tests with the real-account test skipped at the time; since P2.5 the suites hold 10
tests (see below) and the session envelope cases run on an in-process binder.

## P2.5: Binder routing, limits and caller checks

### Caller guard (API 24 emulator and Redmi 22120RN86C, API 33)

Both devices have the AutoJs6 host installed. The instrumentation runs under the plugin's own
UID, so the installed `org.autojs.plugin.MAIL` service refuses it as a session caller:

| Call on the installed service | Outcome |
| --- | --- |
| `getInfo`, `getCapabilities`, `listProviders` | answered (metadata is open to holders of the plugin permission) |
| `openSession` | `SecurityException` "Caller is not the installed same-signer AutoJs6 host: uid <plugin uid> is not the installed AutoJs6 host", nothing through the session callback |
| `listSavedAccounts` | the same `SecurityException` |

`CallerPolicy` is the pure decision (caller UID equals the installed host UID, the host package
runs under that UID, host `versionCode` at least `REQUIRED_HOST_VERSION`, SHA-256 signer sets of
host and plugin equal and non-empty), `HostCallerGuard` gathers the facts through
`Binder.getCallingUid` and the package manager; the rule and the exception text match the MCP
Server plugin's `HostCallerVerifier`. Every session method additionally checks that the caller is
the UID that opened the session.

### Queue, cancel, close and envelope ceilings (`MailSessionBinderTest`)

The test opens a session on an in-process `MailPluginBinder` with a trusting guard against a
loopback server that accepts and never writes, so the first `folders.list` blocks in the IMAP
greeting read with a 30 s read timeout:

| Step | Outcome (API 24 emulator debug / release, Redmi API 33) |
| --- | --- |
| `cancel("c1")` while c1 blocks | `CANCELLED` (not retryable) from the session thread in about 0.5 s including the wait for the block; `getStatus` shows `lastError` `CANCELLED`, `queued` 0, no `active`, `connected` empty |
| `cancel` of a finished, an unknown and a null id | ignored; the next call (`folders.status` without `folder`) still answers `INVALID_ARGUMENT`, no second answer for c1 |
| 32 calls submitted behind the blocked one | `getStatus` reports `queued` 32 and `active` "c1"; none of them is answered while c1 blocks |
| the 33rd queued call (`messages.raw` with a pipe write end) | `LIMIT_EXCEEDED` "MAX_QUEUED_CALLS" (not retryable) from the responder thread, the write end closed before the answer |
| `cancel("q5")` on a queued call | `CANCELLED` at once, `queued` 31 |
| `close()` | 31 queued calls and the blocked c1 answer `SESSION_CLOSED` exactly once each, then `onStatus(closed, "closed")`; a later call answers `SESSION_CLOSED`, no further status |
| request envelope of 512 KiB + args | `LIMIT_EXCEEDED` "request envelope of N bytes exceeds 524288 bytes" before any parsing, no connection attempted, the descriptor copy closed |

Whole suites: `connectedDebugAndroidTest` and `connectedReleaseAndroidTest -PandroidTestRelease`
(R8) on the API 24 emulator 10 tests each, the Redmi 10 tests, one skip each (the real-account
test without arguments). The cancel path also runs on the JVM (`MailSessionAbortTest`): an
operation blocked on the greeting answers `CANCELLED` 144 ms after `MailSession.abort()`, a
cancelled `session.test` does not probe SMTP after IMAP was cut, and an abort that lands before
the socket exists still cancels the connect (`SocketRegistry` closes sockets registered between
`abort` and `resume`).

Why sockets: Angus asks `mail.<protocol>.socketFactory` for an unconnected plain socket and does
the connect, the timeouts and the TLS layering itself (`SocketFetcher`), for implicit SSL and
STARTTLS alike, so closing that socket from another thread breaks a connect, a handshake, a read
or a transfer at once; `Folder.close(false)` would only cover an open folder. The factory ships
with `socketFactory.fallback=false`, because Angus otherwise retries a failed factory connect on
an untracked plain socket. `MailSessionGreenMailTest.implicitTlsEndpointsWorkWithTrustAll` covers
SSL through the tracked socket on the JVM; the QQ regression below covers it on a device.

### QQ regression (Redmi 22120RN86C, API 33)

`python .python/run_real_account.py QQ_A bek749scrwv4wo8h --peer QQ_B --cleanup --debug` after the
binder rewrite, so every QQ connection now goes through the tracked plain socket with Angus'
own SSL layer on top (`imaps` 993, `smtps` 465): `session.test` IMAP 1211 ms and SMTP 903 ms,
`imap connect` 1103 ms / 754 ms and `smtp connect` 752 ms in the trace, `mail.send` with one
attachment 966 ms (`sentCopy = server`); on the peer `folders.list` 11 folders in 1041 ms,
`folders.status` INBOX 385 messages, the Message-ID search fell back to the client after four
server attempts (QQ's index lag, `fallback: always`, 1 hit over 385 envelopes), `messages.get`
619 ms, `attachments.download` 639 ms, `messages.raw` 656 ms, flag add / remove, delete with
expunge 1240 ms; 1 test, 77 s, `report leak check: clean`. The runner's AVD pass of the same
build was 10/10 with one skip.

## JVM

`:mail-core:test`: 146 tests. New in P2.2: `OutgoingMessageParserTest` (9), `MessageComposerTest`
(7), `SmtpSendGreenMailTest` (9). `mail.mime.allowutf8` was dropped from the session properties
so display names and subjects are always RFC 2047 encoded on the wire (decision D34). New in
P2.3: `HtmlToTextTest` (9), `SearchQueryCompilerTest` (11), `MessageArgsTest` (6), `TransferTest`
(5), `MessageMapperFixturesTest` (10, over the ten `.eml` fixtures generated by
`build/make_fixtures.py`), `ImapOperationsGreenMailTest` (10) and `IdentifyingImapStoreTest` (4).
New in P2.4: `Pop3OperationsGreenMailTest` (6) and a POP3 case in `MessageArgsTest` (now 7).
New in P2.5 (now 156 tests): `SocketRegistryTest` (3), `MailSessionAbortTest` (3), an interrupt case
in `TransferTest` (6), a forbidden-retry and an interrupted-thread case in `ConnectionGuardTest`
(10) and the socket-factory case in `MailSessionPropertiesTest` (8).

`:app:testDebugUnitTest`: 31 tests; `RequestRouterTest` (8) snapshots the op table (all 19
contract ops handled since P2.3, `PENDING_OPS` empty), checks that descriptors belong to the
transfer ops only and that the descriptor rules run after routing and before the handler, that
argument errors of every op surface before a descriptor is opened or a connection is made, and
that a POP3 account gets the degraded subset (IMAP-only ops, other folders, `unseenOnly` and body
searches refused) without connecting, and (P2.5) that the protocol table marks nine ops IMAP-only
so a POP3 account is refused before its arguments are parsed. New in P2.5: `LimitsTest` (4:
UTF-8 counting, envelopes at and one byte over the ceiling, queue depth, error message clamping)
and `CallerPolicyTest` (4: the passing host and every refusal reason).

## Reproducing

```
python .python/run_real_account.py QQ_A <serial> --peer QQ_B --append Drafts --debug
python .python/run_real_account.py GMAIL_A <serial> --append "[Gmail]/Drafts" --debug
python .python/run_real_account.py QQ_A <serial> --save-sent true    # shows sentCopy = server
python .python/run_real_account.py QQ_A <serial> --peer QQ_B --cleanup --debug   # P2.3 round trip
python .python/run_real_account.py NETEASE_A <serial> --peer NETEASE_B --cleanup --debug
python .python/run_real_account.py GMAIL_A <serial> --cleanup --debug
python .python/run_real_account.py QQ_A <serial> --peer QQ_B --receive pop3 --cleanup --debug   # P2.4, peer reads over POP3
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedDebugAndroidTest                      # P2.5 binder suites
ANDROID_SERIAL=emulator-5554 ./gradlew :app:connectedReleaseAndroidTest -PandroidTestRelease
```

Profiles read `QQ_USER_NAME_A` / `QQ_AUTH_CODE_A`, `QQ_USER_NAME_B` / `QQ_AUTH_CODE_B`,
`NETEASE_USER_NAME_A` / `NETEASE_AUTH_CODE_A`, `NETEASE_USER_NAME_B` / `NETEASE_AUTH_CODE_B` and
`GMAIL_USER_NAME_A` / `GMAIL_ACCESS_TOKEN_A` from `mail-test-accounts.properties`; NetEase
profiles pick the `163` or `126` preset from the address domain and point yeah.net addresses at
`imap.yeah.net` / `smtp.yeah.net`; the Gmail token from the OAuth Playground is valid for about
one hour.
