# P6 provider matrix (real accounts)

Roadmap P6 "服务商兼容矩阵", run on 2026-09-19 against plugin build 40 (`abfaa76`) with the accounts
of the git-ignored `mail-test-accounts.properties`; the Gmail and Outlook.com columns were run on
2026-09-20 against build 45 (`2057ad4`) plus the working tree of build 46. The driver is `.python/run_provider_matrix.py`
(JVM, `ProviderMatrixProbe` in `mail-core`, skipped without `build/p6/matrix.properties`); it runs
every row through the same `MailSession` the Binder uses, prints only masked addresses
(`***@domain`) and checks the Gradle output, the XML report and the probe logs for the secrets.
The device confirmation is the P2.3 flow of `run_real_account.py` on the Redmi (API 33).

Accounts: QQ (A and B), 163, 126, yeah.net (163 preset with its own hosts), Sina, Gmail (XOAUTH2
token; expired on 2026-09-19, renewed by the maintainer on 2026-09-20) and three Outlook.com / Hotmail
accounts (app passwords, 2026-09-20; every server refuses them, see the column). iCloud: no account
is available to the project. Yahoo and Aliyun: excluded by the maintainer on 2026-09-19 (no valid
account).

## Rows (one message with a Chinese subject, body, display names and file name, sent to the account itself)

| Row | QQ | 163 | 126 | yeah.net | Sina | Gmail | Outlook.com |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `session.test` (IMAP / SMTP) | ok 820 / 510 ms | ok 688 / 638 ms | ok 617 / 575 ms | ok 379 / 701 ms | ok 541 / 4982 ms | ok 2712 / 1404 ms (XOAUTH2) | IMAP `AUTH_MECHANISM_UNSUPPORTED` (the server advertises `LOGINDISABLED` and only `AUTH=XOAUTH2`; Angus sends no credential), SMTP `535` -> `AUTH_FAILED` after 17.5 s; with `provider: 'outlook'` the preset refuses the app password before connecting (three accounts, same answers) |
| IMAP capabilities | IDLE UIDPLUS MOVE ID NAMESPACE CHILDREN XLIST COMPRESS=DEFLATE | UIDPLUS ID SPECIAL-USE XLIST LITERAL+ AUTH=XOAUTH2 | same as 163 | 163 plus IDLE | UIDPLUS ID | IDLE UIDPLUS MOVE CONDSTORE ID ENABLE NAMESPACE SPECIAL-USE LIST-EXTENDED LIST-STATUS UNSELECT CHILDREN XLIST ESEARCH LITERAL- COMPRESS=DEFLATE UTF8=ACCEPT | AUTH=XOAUTH2 LOGINDISABLED (before login) |
| `folders.list` | 6 in 165 ms, roles from XLIST, `其他文件夹` is a non-selectable parent (account B: 11 folders, `其他文件夹/Archive` = archive) | 6 in 93 ms, roles from SPECIAL-USE, `病毒文件夹` has none | 6 in 93 ms, same roles | 6 in 74 ms, same roles | 10 in 69 ms, roles from the names (no SPECIAL-USE, no XLIST) | 22 in 773 ms, roles from SPECIAL-USE (`[Gmail]/Drafts`, `[Gmail]/Sent Mail`, `[Gmail]/Spam`, `[Gmail]/Starred` = flagged, `[Gmail]/Trash`), `[Gmail]` is a non-selectable parent | - (waits for a token, roadmap P9) |
| Delimiter | `/` | `/` | `/` | `/` | `/` | `/` | - (waits for a token, roadmap P9) |
| `mail.send` | 744 ms, `sentCopy = server` | 331 ms, `sentCopy = server` | 326 ms, `sentCopy = server` | 282 ms, `sentCopy = server` | 1260 ms, `sentCopy = appended` to `已发送` | 2512 ms, `sentCopy = server` | - (waits for a token, roadmap P9) |
| Delivery seen by `messages.list` | 6.4 s (2 polls), page of 10 in 678 ms | 10.9 s (3 polls), page in 222 ms | 5.6 s, page in 201 ms | 5.6 s, page in 165 ms | 6.4 s, page in 574 ms | 1.6 s (1 poll), page of 10 in 1619 ms | - (waits for a token, roadmap P9) |
| Message-ID kept | no (rewritten to `<tencent_...@qq.com>`) | yes | yes | yes | yes | yes | - (waits for a token, roadmap P9) |
| Server search: subject (Chinese) | OK, 0 hits | OK, 0 hits | 1 hit, 296 ms | 1 hit, 269 ms | `BAD` -> client fallback, 1 hit | `BAD Could not parse command` while Angus had enabled `UTF8=ACCEPT` (runs 1 and 2, `SERVER_ERROR` -> client fallback); 1 hit via the server in 2.3 s with the extension left off (build 46, `CHARSET UTF-8` + literal) | - (waits for a token, roadmap P9) |
| Server search: subject (ASCII) / from / body / messageId | 10 / 10 / 10 / 0 hits, 0.75-0.86 s | 0 / 0 / 0 / 0 hits | 4 / 2 / 1 / 1 hits | 4 / 1 / 1 / 1 hits | `BAD` for every text key | 10 / 10 / 3 / 1 hits, 1.9-2.1 s | - (waits for a token, roadmap P9) |
| Server search: since 24 h | 10 hits | 6 hits | 8 hits | 4 hits | 6 hits, 749 ms | 10 hits, 2.0 s | - (waits for a token, roadmap P9) |
| Client search (`fallback: 'always'`): subject / body (Chinese) | 1 hit in 26.8 s / 102.8 s (envelope and body fetches of the newest 200) | 1 hit in 0.53 s / 3.2 s | 1 hit in 0.21 s / 0.97 s | 1 hit in 0.19 s / 1.2 s | 1 hit in 0.75 s / 3.7 s | subject: not needed after build 46 (server hit); body over the full 2000-candidate window: 1.3 s per candidate (two or three round trips each, a single text fetch 0.45 s from this PC), 600 candidates in 13 min, the whole window about 43 min (run 1 was stopped there) | - (waits for a token, roadmap P9) |
| `messages.get` (peek) | 423 ms, 12 headers, `seen` stays false | 259 ms | 254 ms | 223 ms | 977 ms, 18 headers | 2360 ms, 9 headers, `seen` stays false | - (waits for a token, roadmap P9) |
| Display names | From kept; ENVELOPE To name replaced by the recipient's account name (raw `To` header intact) | From `AutoJs6 矩阵` comes back as `AutoJs6_矩阵`; To kept | same as 163 | same as 163 | both kept | both kept | - (waits for a token, roadmap P9) |
| Attachment (`报表 <n>.txt`, 33 bytes) | name, type and bytes correct, 498 ms | correct, 255 ms | correct, 247 ms | correct, 221 ms | correct, 978 ms | correct, 2359 ms | - (waits for a token, roadmap P9) |
| Flags (`flagged` add / remove, `seen`) | ok, 2.5 s for six round trips | ok, 1.5 s | ok, 1.5 s | ok, 1.3 s | ok, 6.6 s | ok, 14.4 s for six round trips | - (waits for a token, roadmap P9) |
| Custom keyword (`AutoJs6Matrix`) | accepted, not stored | accepted, not stored | accepted, not stored | accepted, not stored | accepted, not stored | stored (listed back with the system flags; the only provider of the six) | - (waits for a token, roadmap P9) |
| `folders.create` | `NO` (run 1: created, gone within seconds) | ok, survives | ok | ok | `NO` (web UI only) | ok, survives (listed 3 s later) | - (waits for a token, roadmap P9) |
| `messages.move` | `MOVE` to `Deleted Messages` 1.2 s, COPYUID | COPY + delete (no MOVE) 1.8 s, COPYUID | same, 1.8 s | same, 1.7 s | COPY + delete to `已删除` 2.4 s, COPYUID | `MOVE` 4.0 s, COPYUID (target uid 1) | - (waits for a token, roadmap P9) |
| Watch (`mode: auto`, poll 30 s) | poll (IDLE advertised, `idlePush = false`); second delivery seen after 30.8 / 61.4 / 59.4 s in three runs | poll (no IDLE); 30.4 s | poll; 30.4 s | poll (IDLE advertised, preset polls); first poll after delivery | poll (no IDLE advertised); 32.3 s | IDLE (`idlePush = true`); second delivery seen after 32.0 s (the `message` event 34.0 s after the SMTP submission), Gmail's notification cadence | - (waits for a token, roadmap P9) |
| POP3 (`session.test`, list 5, get newest) | ok 498 ms; UIDL 30 chars; list 1.5 s; get 1.6 s; CAPA lists XOAUTH2 | ok 318 ms; UIDL 22 chars; list 0.59 s; get 0.63 s | ok 364 ms; list 0.62 s; get 0.60 s | `AUTH_FAILED` (`-ERR Unable to log on` for the code IMAP and SMTP accept: POP3 not enabled for this account) | ok 734 ms; UIDL 48 chars; list 0.54 s; get 0.95 s | ok 2405 ms; UIDL 23 chars; list 5 in 7.8 s; get 4.2 s; the message the account had just sent to itself is not in the POP3 view | `-ERR Basic authentication is disabled` (`build/p6/auth_probe.py`) |
| Cleanup (delete + expunge) | INBOX and `Deleted Messages` ok; the server's own copies in `Sent Messages` cannot be addressed by UID (below) | INBOX, `已发送` (2 server copies) and the created folder deleted | same | same | INBOX, `已发送` (appended copies) and `已删除` deleted | INBOX (2), `[Gmail]/Sent Mail` (3 server copies, addressable by UID) and the created folder deleted | - (waits for a token, roadmap P9) |

## Provider notes (backfilled into appendix C and `providers.json`)

- QQ: text SEARCH with a non-ASCII argument answers `OK` with no hits, never an error, so the
  default `fallback: 'client'` (which reacts to a rejection) leaves Chinese searches empty; scripts
  use `fallback: 'always'`. Message-IDs of outgoing mail are rewritten, so a search by `messageId`
  finds nothing (the P2.3 device flow saw the same and fell back to the client). The ENVELOPE `To`
  display name is replaced by the recipient's own account name while the raw header keeps the
  sent name. `CREATE` answered `NO` in three of four runs; in the run where it succeeded the folder
  was gone from `LIST` within a second (`STATUS` answers `NO Folder not exist!`), so the move
  target on QQ is the trash role. The copies the server files in `Sent Messages` for SMTP-sent mail
  are listed by sequence number with UIDs, but `UID FETCH` on them answers `OK Mails not exist!`
  and `STORE` answers `NO System busy!` for at least an hour after sending; `EXISTS` on that
  folder overstates the count by two. `MOVE` works. New mail becomes visible to a fresh `EXAMINE`
  15-40 s after delivery (P5), hence the 30-61 s watch latency with a 30 s poll.
- 163: text SEARCH (`SUBJECT`, `FROM`, `BODY`, `HEADER Message-ID`) answers `OK` with no hits for
  recent mail while `SINCE` works, on the same connection and right after a raw `UID SEARCH
  SUBJECT` confirmed it; 126 and yeah.net, on the same server software, answered every text search
  correctly in this run. Spaces in the sender display name come back as underscores on all three
  NetEase hosts. Server copies of sent mail appear in `已发送` within seconds here (P2.3 saw
  minutes) and delete normally. yeah.net advertises IDLE where 163 and 126 do not; the preset
  keeps `idlePush = false`, so watches poll on all three. yeah.net POP3 refuses the authorization
  code that IMAP and SMTP accept (NetEase enables POP3 separately in the web settings).
- Sina: `CAPABILITY` is `IMAP4rev1 ID UIDPLUS`. SEARCH accepts only `ALL`, `SINCE` and flag keys;
  `SUBJECT`, `FROM`, `BODY`, `TEXT`, `HEADER` and `CHARSET UTF-8 ...` all answer `BAD Missing or
  invalid or unimplemented argument`, so every text search runs on the client through the default
  fallback (the trace records "search rejected by the server, filtering on the client"). Roles come
  from the folder names (`已发送`, `已删除`, `垃圾邮件`, `草稿夹`). `CREATE` answers `NO`. SMTP
  greets slowly (5 s). POP3 UIDLs are 48 characters.
- All five: custom IMAP keywords are accepted (`OK`) but not stored; the flag document lists only
  the system flags afterwards.
- Gmail (2026-09-20, token renewed): folders live under `[Gmail]` with roles from SPECIAL-USE and
  `[Gmail]` itself is a non-selectable parent; the server keeps the sent copy (`sentCopy = server`)
  and, unlike QQ, the copies in `[Gmail]/Sent Mail` can be addressed and deleted by UID. The
  Message-ID and both display names survive. The server advertises `UTF8=ACCEPT`; Angus Mail
  enables it on every server that offers it and then sends non-ASCII search text the RFC 6855 way
  (no `CHARSET`, UTF-8 quoted string or literal), which Gmail answers with `BAD Could not parse
  command`, while `SEARCH CHARSET UTF-8 SUBJECT {n}` with the same bytes finds the message; the
  mail core no longer enables the extension (below). Every other server search key hits. Custom
  IMAP keywords are stored (the only provider of the six). `CREATE` works and the folder
  survives; `MOVE` is advertised and used. IDLE pushes: from the PC (`build/p6/gmail_idle_probe.py`,
  `imaplib` with XOAUTH2) the untagged `EXISTS` for a message sent from the QQ B account came
  30.0 s after the SMTP submission and for a self-sent message 60.1 s after it, so the watch
  latency on Gmail is Gmail's own notification cadence (about 30 s), not a poll interval. The
  POP3 view (`pop.gmail.com`) does not include the message the account had just sent to itself.
  A client-side body search over the whole 2000-candidate window costs about 1.3 s per candidate
  from this network (two or three round trips each), about 43 min for the full window when fewer
  than `limit` messages match: the first run was stopped there, and the client filter now reports
  its progress to the trace every 25 candidates. The P5 device matrix on the Redmi (baseline,
  plugin killed, Wi-Fi off) was repeated on the IDLE path the same day
  (`docs/dev/p5-watch-evidence.md`).
- Outlook.com / Hotmail (three personal accounts, app passwords, 2026-09-20): with
  `provider: 'outlook'` the preset (`auth: [xoauth2]`) refuses the password before any connection
  (`provider 'outlook' accepts only xoauth2 authentication`). With the preset bypassed
  (`--no-preset`: the same hosts spelled out, no `provider`) the servers answer for themselves:
  IMAP advertises `AUTH=XOAUTH2 LOGINDISABLED` and answers `NO Basic authentication is disabled.`
  to `LOGIN` and `AUTHENTICATE PLAIN`, POP3 answers `-ERR Basic authentication is disabled`, SMTP
  587 STARTTLS answers `535` after 17.5 s. Angus, finding no mechanism it may use, stops before
  sending a credential (`No login methods supported!`); that used to map to `SERVER_ERROR` and
  now maps to `AUTH_MECHANISM_UNSUPPORTED` (below). The preset's XOAUTH2-only rule therefore
  stands and the operation rows wait for a token (roadmap P9).

## Defect found and fixed: POP3 XOAUTH2 refusals reported as `IO_FAILED`

Gmail's POP3 server answers a refused `AUTH XOAUTH2` the way the XOAUTH2 specification asks: a SASL
continuation `+ <base64 JSON>` (`{"status":"400","schemes":"Bearer","scope":"https://mail.google.com/"}`),
then `-ERR [AUTH] Invalid credentials.` once the client sends the empty line, and `-ERR bad command`
for anything else. Angus Mail 2.0.5's POP3 `OAuth2Authenticator` turns the continuation into an
`EOFException` that `Protocol.Authenticator.authenticate` catches and ignores, and because the
continuation line starts with `+` the store considers the login successful. The first real command
(`STAT` from the INBOX open) then fails and the plugin reported `IO_FAILED I/O failed (Open failed)`,
a retryable code, for what is an `AUTH_FAILED`.

`MailSessionFactory.connectStore` now verifies a POP3 XOAUTH2 login with one INBOX open (`STAT`)
right after the connect; a failure other than Gmail's "not enabled for POP" refusal closes the
store and raises `AuthenticationFailedException`, which maps to `AUTH_FAILED` (not retryable, no
token in the message). `Pop3OAuthScriptedTest` (2 cases) scripts the Gmail dialogue on a loopback
POP3 server: the refused token maps to `AUTH_FAILED` in `session.test` and in `messages.list`, the
report and the error text never contain the token, and an accepted token lists the mailbox. The
extra `STAT` costs one round trip per POP3 XOAUTH2 connect (password logins are unchanged).

## Defects found and fixed on the Gmail and Outlook.com columns (build 46)

- Gmail `UTF8=ACCEPT` (`IdentifyingImapProtocol.enable`): the extension is never enabled, so
  searches always carry `CHARSET UTF-8` (a literal for non-ASCII text) and mailbox names always
  use modified UTF-7, the path every other server takes. `Utf8SearchTest` runs a Chinese subject
  search against the scripted server advertising `ENABLE UTF8=ACCEPT` and asserts that nothing is
  enabled and the server parses `CHARSET UTF-8 SUBJECT 矩阵 ALL`. Decision D41.
- Outlook.com `LOGINDISABLED` (`ExceptionMapper`): Angus's `No login methods supported!` (a
  `ProtocolException` raised before any credential is sent when the server disables `LOGIN` and
  advertises no mechanism the credential kind allows) maps to `AUTH_MECHANISM_UNSUPPORTED`, not
  retryable, with a detail that names `xoauth2`, instead of the retry-looking `SERVER_ERROR`.
  `LoginDisabledTest` scripts the Outlook.com capability line and checks that `session.test`
  reports the code and that no `LOGIN` / `AUTHENTICATE` reaches the server.
- Client filter progress (`ImapMailbox`): every 25 candidates the client-side filter records
  `client filter progress <n> of <window>, <hits> matched, <ms>` to the debug trace, so a slow
  `fallback: 'always'` body search is visible while it runs.
- Test environment: the scripted IMAP server of these tests (`FakeImapServer`) now advertises
  `LITERAL+` like GreenMail and every preset provider. On the development machine ESET's email
  client protection recognises the `* OK` greeting on a loopback connection and holds the bytes
  a client sends after a synchronizing literal (`{n}` then `+`), so a test built on the
  synchronizing form never completes there; with `{n+}` the literal goes through.

## Device confirmation (Redmi 22120RN86C, API 33, QQ A to QQ B)

`py .python/run_real_account.py QQ_A bek749scrwv4wo8h --peer QQ_B --cleanup`: `session.test` 1660 ms,
send 919 ms, peer delivery after 1.8 s, `folders.list` 11 folders with counts in 841 ms, search by
Message-ID 0 hits on the server (rewritten) and 1 hit through `fallback: 'always'` in 33 s, subject
search 5 hits, `unseenOnly` contains the message, `messages.get` 559 ms, download 28 bytes in 559 ms,
raw 2266 bytes in 557 ms, flags add / remove, delete with expunge; the plugin was removed from the
phone by the connected run.

## Reproducing

```
py -X utf8 .python/run_provider_matrix.py QQ_A,NETEASE_A,NETEASE126_A,NETEASE_B,SINA_A,GMAIL_A --idle-seconds 150
py -X utf8 .python/run_provider_matrix.py QQ_A --ops diag --diag 'UID SEARCH SUBJECT "matrix";STATUS "INBOX" (MESSAGES UIDNEXT)'
py -X utf8 .python/run_provider_matrix.py GMAIL_A --idle-seconds 90 --ops test,folders,send,list,search-server,body,attachment,flags,move,watch,pop3,cleanup
py -X utf8 .python/run_provider_matrix.py OUTLOOK_A,HOTMAIL_A,HOTMAIL_B --ops test --no-pop3 --no-preset
./gradlew.bat :mail-core:test --tests "*Utf8SearchTest" --tests "*LoginDisabledTest"
./gradlew.bat :mail-core:test --tests "*Pop3OAuthScriptedTest"
adb -s <serial> uninstall io.github.supermonster003.autojs6.plugin.angus.mail
py .python/run_real_account.py QQ_A <serial> --peer QQ_B --cleanup
```

Results per profile are written to `build/p6/matrix-<profile>.log` (masked rows plus the redacted
protocol trace) and `build/p6/matrix-summary.txt`; both are git-ignored.
