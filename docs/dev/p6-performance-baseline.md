# P6 performance baseline

Roadmap P6 "性能基线", measured on 2026-09-20 against plugin build 41 plus the changes of this
commit. No regression thresholds: the figures are recorded so that a later change can be compared
against them. Everything runs against a seeded local server, so no real account and no secret is
involved; the one-hour standby uses a QQ account saved on the plugin's settings page as an alias.

## Setup

- **Server**: `PerfMailServer` (mail-core test source set), a GreenMail 2.1.13 started out of
  process by the runner (JDK 21, `-Xmx2g`, class path from `:mail-core:writeTestClasspath`):
  INBOX seeded with 10,000 messages of about 460 bytes (`Perf message n`, every hundredth subject
  `Perf marker 标记 n`, `X-Perf-Index` header, from `sender<n mod 97>@example.com`; 4.9 s to seed)
  and a folder `Perf` holding one message with a 50 MiB random attachment (`perf-random.bin`,
  CRC32 `870b4284`, declared base64 size 71,744,832 bytes). The big message lives in its own folder
  because GreenMail answers every `FETCH` and `SEARCH` in O(messages of the selected folder)
  (`ImapSessionFolder.getMsn` walks `getMessageUids`): a command on the 10,001-message INBOX costs
  1 to 1.5 s regardless of what it fetches, on the one-message folder a few milliseconds. The
  per-command latencies below are therefore GreenMail's, not a provider's (the P6 provider matrix
  saw 0.17 to 0.68 s for a page of 10 on QQ / 163 / 126 / Sina); the server-independent figures are
  the round trips and the bytes per round trip.
- **Clients**: `PerformanceBaselineProbe` (mail-core, JVM, `MailSession` on the loopback, skipped
  without `build/p6/perf.properties`) and `PerformanceDeviceTest` (instrumentation, the same rows
  through the same `MailSession` inside the plugin process; the server is reached through
  `adb reverse tcp:3143` / `tcp:3025`, so the device figures include the USB adb transport).
  Timeouts: connect 20 s, read 600 s. Memory: the JVM probe samples the heap every 10 ms and reads
  the pool peaks; the device test samples `Debug.getPss()` every 100 ms and the Java / native heap
  every 10 ms.
- **Rows**: `session.test`, folder status, `fetch(limit 50)` cold and warm (3 runs), a page 5,000
  deep (`before` uid), `unseen: true`, server searches (subject `marker`, from
  `sender7@example.com`, since 30 days; GreenMail needs a parseable address for `FROM`, a fragment
  answers `BAD`), client searches (`fallback: 'always'`: subject `标记` over the newest 2,000
  candidates, body of the second newest message with `limit 1`, body of the message 50 deep), `get`
  of a small message, the attachment envelope, the 50 MiB attachment download with CRC check, the
  raw download of that message, sending a 10 MiB random attachment and downloading the delivered
  copy back (the IMAP `BODYSTRUCTURE` size of a base64 part is the encoded size, 14,349,088 bytes for
  10,485,760, so the bytes are verified by CRC instead), cleanup.

## Rows

- JVM (loopback): JVM Java HotSpot(TM) 64-Bit Server VM 21.0.1, max heap 512 MiB, 12 cpus, server 127.0.0.1:3143/3025
- Redmi 22120RN86C, API 33 (adb reverse): API 33 (Xiaomi 22120RN86C), max heap 256 MiB, pss 56410 KiB, server 127.0.0.1:3143/3025
- Sony G8441, API 28 (adb reverse): API 28 (Sony G8441), max heap 192 MiB, pss 52676 KiB, server 127.0.0.1:3143/3025

| Row | JVM (loopback) | Redmi 22120RN86C, API 33 (adb reverse) | Sony G8441, API 28 (adb reverse) |
| --- | --- | --- | --- |
| `test` | 96 ms: imap 127.0.0.1:3143 ok 69 ms caps=[IDLE, UIDPLUS, MOVE, SORT, LITERAL+, AUTH=XOAUTH2]; smtp 127.0.0.1:3025 ok 18 ms caps=[SMTPUTF8, AUTH] | 342 ms: imap 127.0.0.1:3143 ok 218 ms caps=[IDLE, UIDPLUS, MOVE, SORT, LITERAL+, AUTH=XOAUTH2]; smtp 127.0.0.1:3025 ok 117 ms caps=[SMTPUTF8, AUTH] | 648 ms: imap 127.0.0.1:3143 ok 452 ms caps=[IDLE, UIDPLUS, MOVE, SORT, LITERAL+, AUTH=XOAUTH2]; smtp 127.0.0.1:3025 ok 189 ms caps=[SMTPUTF8, AUTH] |
| `status` | 18 ms: INBOX messages=10000 unseen=10000 uidNext=10001 | 184 ms: INBOX messages=10000 unseen=10000 uidNext=10002 | 215 ms: INBOX messages=10000 unseen=10000 uidNext=10001 |
| `list-first-page` | 1543 ms: cold: 50 messages in 1537 ms, newest uid=10000 subject=Perf marker 标记 10000 | 1727 ms: cold: 50 messages in 1724 ms, newest uid=10000 | 2186 ms: cold: 50 messages in 2185 ms, newest uid=10000 |
| `list-first-page-warm` | 4515 ms: 3 runs: 1508 / 1492 / 1513 ms, median 1508 ms | 5032 ms: 3 runs: 1675 / 1672 / 1684 ms, median 1675 ms | 5115 ms: 3 runs: 1772 / 1678 / 1663 ms, median 1678 ms |
| `list-deep-page` | 3038 ms: page below uid 5000 (about 5000 deep): 50 messages in 3035 ms, uids 4999..4950 | 3079 ms: page below uid 5000 (about 5000 deep): 50 messages in 3079 ms | 3208 ms: page below uid 5000 (about 5000 deep): 50 messages in 3208 ms |
| `list-unseen-only` | 3221 ms: server SEARCH UNSEEN over 10000 then a page of 50 in 3219 ms | 3259 ms: server SEARCH UNSEEN over 10000 then a page of 50 in 3258 ms | 3655 ms: server SEARCH UNSEEN over 10000 then a page of 50 in 3654 ms |
| `search-server-subject` | 2130 ms: subject 'marker': 50 hits via server in 2127 ms | 1769 ms: subject 'marker': 50 hits via server in 1768 ms | 2084 ms: subject 'marker': 50 hits via server in 2084 ms |
| `search-server-from` | 1547 ms: from 'sender7@example.com': 50 hits via server in 1546 ms | 1655 ms: from 'sender7@example.com': 50 hits via server in 1655 ms | 1695 ms: from 'sender7@example.com': 50 hits via server in 1694 ms |
| `search-server-since` | 3015 ms: since 30 days: 50 hits via server in 3015 ms | 3202 ms: since 30 days: 50 hits via server in 3201 ms | 3398 ms: since 30 days: 50 hits via server in 3397 ms |
| `search-client-subject` | 3194 ms: subject '标记' over the newest 2000 candidates: 20 hits via client in 3195 ms | 3379 ms: subject '标记' over the newest 2000 candidates: 20 hits via client in 3378 ms | 3601 ms: subject '标记' over the newest 2000 candidates: 20 hits via client in 3600 ms |
| `search-client-body-newest` | 9049 ms: body 'perf body 9999' (second newest message), limit 1: 1 hits via client in 9045 ms (the scan stops at the first match) | 7781 ms: body 'perf body 9999' (second newest message), limit 1: 1 hits via client in 7780 ms (the scan stops at the first match) | 7709 ms: body 'perf body 9999' (second newest message), limit 1: 1 hits via client in 7709 ms (the scan stops at the first match) |
| `search-client-body-50-deep` | 158910 ms: body 'perf body 9950' (50 messages deep), limit 1: 1 hits via client in 158909 ms (about 50 candidates, two fetches each) | 187115 ms: body 'perf body 9950' (50 messages deep), limit 1: 1 hits via client in 187114 ms (about 50 candidates, two fetches each) | 154109 ms: body 'perf body 9950' (50 messages deep), limit 1: 1 hits via client in 154108 ms (about 50 candidates, two fetches each) |
| `get-small` | 4339 ms: uid 10000 in 4337 ms: textLength=459 headers=9 seen=false | 5999 ms: uid 10000 in 5998 ms: textLength=459 headers=9 | 4465 ms: uid 10000 in 4464 ms: textLength=459 headers=9 |
| `get-attachment-envelope` | 149 ms: uid 1 in 138 ms: part 2 perf-random.bin application/octet-stream declared size=71744832, expected 52428800 bytes crc32=870b4284 | 341 ms: uid 1 in 199 ms: part 2 perf-random.bin declared size=71744832, expected 52428800 bytes | 560 ms: uid 1 in 382 ms: part 2 perf-random.bin declared size=71744832, expected 52428800 bytes |
| `download-attachment` | 5063 ms: 52428800 bytes in 4979 ms = 10.0 MiB/s, 50 progress reports, crc ok=true; heap used before 6 MiB, peak 10 MiB (+3 MiB), pool peak 9 MiB | 14719 ms: 52428800 bytes in 14665 ms = 3.4 MiB/s, 50 progress reports, crc ok=true; pss before 60 MiB peak 61 MiB (+1 MiB), java heap before 9 MiB peak 10 MiB (+1 MiB), native heap before 9 MiB peak 9 MiB | 14089 ms: 52428800 bytes in 13980 ms = 3.6 MiB/s, 50 progress reports, crc ok=true; pss before 49 MiB peak 51 MiB (+1 MiB), java heap before 3 MiB peak 5 MiB (+1 MiB), native heap before 4 MiB peak 5 MiB |
| `download-raw` | 3260 ms: 71745455 bytes (71745455 on disk) in 3184 ms = 21.5 MiB/s; heap peak 8 MiB (+2 MiB) | 8437 ms: 71745455 bytes in 8369 ms = 8.2 MiB/s; pss before 58 MiB peak 59 MiB (+1 MiB), java heap before 3 MiB peak 5 MiB (+1 MiB), native heap before 9 MiB peak 9 MiB | 7730 ms: 71745455 bytes in 7638 ms = 9.0 MiB/s; pss before 51 MiB peak 52 MiB (+1 MiB), java heap before 5 MiB peak 6 MiB (+1 MiB), native heap before 4 MiB peak 5 MiB |
| `send-attachment` | 30538 ms: 10485760 bytes in 590 ms = 16.9 MiB/s (send elapsed 574 ms, accepted=1); delivered uid=10001 size=14349779 declared part size=14349088, downloaded back 10485760 bytes in 24032 ms crc ok=true; heap peak 19 MiB (+2 MiB) | 42667 ms: 10485760 bytes in 861 ms = 11.6 MiB/s (send elapsed 842 ms); delivered uid=10002 size=14349774 declared part size=14349088, downloaded back 10485760 bytes in 33246 ms crc ok=true; pss before 60 MiB peak 61 MiB (+0 MiB), java heap before 6 MiB peak 7 MiB (+1 MiB), native heap before 9 MiB peak 9 MiB | 33824 ms: 10485760 bytes in 1499 ms = 6.7 MiB/s (send elapsed 1378 ms); delivered uid=10001 size=14349773 declared part size=14349088, downloaded back 10485760 bytes in 25702 ms crc ok=true; pss before 49 MiB peak 51 MiB (+1 MiB), java heap before 2 MiB peak 4 MiB (+1 MiB), native heap before 4 MiB peak 5 MiB |
| `cleanup` | 4447 ms: deleted 1 sent copies | 6204 ms: deleted 1 sent copies | 4710 ms: deleted 1 sent copies |

## What the rows say

- **Listing and searching** cost 2 to 3 GreenMail commands each (`SELECT`, `SEARCH`, one `FETCH` of
  the envelopes for the page): a page of 50 in one `FETCH` batch, 2,000 envelopes for the client
  subject search in one batch too (3.2 s including the 1 s `SEARCH`). The devices add 0.1 to 0.6 s
  per row to the JVM figure (the Sony's cold first page 2.2 s), so the client side is not where
  the time goes.
- **Client body search** fetches `BODYSTRUCTURE` and the text parts of every candidate (Jakarta
  `BodyTerm.match`), two `FETCH` round trips per message: 7.6 to 9 s for a hit in the newest
  message, 154 to 187 s for a hit 50 messages deep. Before this commit the scan did not stop at
  the first hit and walked all 2,000 candidates (the first JVM run was stopped after 40 minutes on
  that row); `ImapMailbox.clientSearch` now stops as soon as `limit` matches are found and records
  how many candidates it looked at.
- **50 MiB download**: with the previous `mail.imap.fetchsize` of 64 KiB the base64 body needs
  about 1,100 partial `FETCH` round trips, each of them a full O(n) command on GreenMail (the second
  JVM run was stopped on that row; on a provider with 50 ms round trips the ceiling is about 1.3
  MiB/s regardless of bandwidth). With 1 MiB (this commit, D40) the download is 69 round trips: 5.0 s
  on the JVM loopback (10.0 MiB/s, heap +3 MiB, pool peak 9 MiB), 14.7 s on the Redmi over adb
  (3.4 MiB/s, PSS +1 MiB, Java heap +1 MiB, native heap flat), 14.1 s on the Sony (3.6 MiB/s, PSS
  +1 MiB). The memory a download holds is one
  chunk plus the decoder buffers, about 2 MiB, on every path (`IMAPMessage.writeTo` streams
  `BODY[]` in `fetchsize` chunks).
- **Raw download** of the same message is 3.2 / 8.4 / 7.7 s (21.5 / 8.2 / 9.0 MiB/s on the JVM /
  Redmi / Sony): the base64 bytes go to the sink as they are, no decoding.
- **Sending 10 MiB** takes 0.59 s on the JVM (16.9 MiB/s), 0.86 s on the Redmi (11.6 MiB/s) and
  1.5 s on the Sony (6.7 MiB/s) with the heap +2 MiB / PSS +0 to +1 MiB: `FileAttachmentSource` streams the file into the SMTP `DATA`
  stream. Downloading the copy back from INBOX costs 14 round trips of the 10,001-message folder
  (24 / 33 / 26 s), the GreenMail cost again.
- **Connect**: `session.test` 92 to 96 ms on the JVM (IMAP 67, SMTP 18), 306 to 342 ms on the
  Redmi and 648 ms on the Sony over adb (IMAP 452, SMTP 189).

## IDLE standby, one hour (Sony G8441, API 28, QQ, `mode: 'idle'`)

Driver `.python/run_idle_standby.py BH900ASK9E --alias perfidle --minutes 60 --mode idle`: the
script is `docs/smoke/watch.js` with a `standby` scenario (no message expected; the watch stays
open until the deadline), started through the host's `RunIntentActivity` like a user's script;
battery faked unplugged, `batterystats` reset, Doze disabled (the Doze behaviour is the P5
matrix), screen off, samples every 5 minutes.

Run 1 (without the host's own foreground service): a script started from the run intent runs in a
background host, which Android 9 treats as a cached empty process, and so is the plugin bound only
by it; both were killed after 31 minutes (`am_kill ... 904 empty #25` for the plugin, `906` for the
host), the watch reported `PLUGIN_UNAVAILABLE "mail plugin process died"` and died with the host.
Until then the plugin's PSS was 31.4 to 32.8 MiB and one IMAP connection was open. A script that
has to keep a watch alive for long needs the host's foreground service (the drawer switch of
AutoJs6; the P4 settings page already links the battery optimisation guide).

Run 2 (host foreground service on, both processes at `oom_score_adj` 200 for the whole hour):

| Measure | Value |
| --- | --- |
| Watch ready (connect 702 ms, `watch` 175 ms) | 913 ms after the script start, mode `idle` |
| Plugin PSS | 37.9 MiB at the start, 36.5 MiB at 5 and 10 min, 31.8 to 33.0 MiB from 15 min on (median 32.2 MiB, last 32.2 MiB) |
| Host PSS | 56.9 MiB at the start, 58.1 to 59.5 MiB afterwards |
| Plugin CPU over the hour | 3.45 s from `/proc/<pid>/stat` (batterystats: 4.49 s user + 0.34 s kernel) |
| Host CPU over the hour | 23.6 s user + 2.7 s kernel (script engine, console, foreground service) |
| Plugin network | 30.4 KB received, 9.2 KB sent, 114 / 111 packets; Wi-Fi radio time 24 ms receive, 13 ms transmit |
| Plugin battery (`Estimated power use`) | 0.00264 mAh (Wi-Fi), with smearing 0.00521 mAh |
| Host battery | 0.0000784 mAh (wake), with smearing 0.000155 mAh |
| Device | computed drain 7.98 mAh of 2700 mAh in the hour (everything on the device; Wi-Fi sleep time 100 %) |
| Connections | exactly one established IMAP connection at every sample |
| Events | `error(CONNECT_FAILED "the connection to the server was lost", retryable)` at 15.1 min and at 40.1 min, the watcher reconnected in the same generation each time (the next sample shows one connection again); `close('stopped')` at 60.0 min; 0 messages |

QQ closes an idling connection after about 15 minutes (the first drop) and again about a minute
after the 24-minute IDLE renew (the second); each drop costs one `error` event and a reconnect,
nothing else. The QQ preset does not push (`idlePush = false`, D38), so scripts only see this
with an explicit `mode: 'idle'`; the poll mode of the P5 matrix (30 to 60 s) never showed it.

The Sony is shared with other sessions (the EPUB reader plugin's tests ran on it during the hour);
the samples only look at the two uids. The event log buffer is not cleared by the driver, so the
kill records of run 1 appear in the run 2 summary as well.

## Changes made because of the baseline

- `mail.imap.fetchsize` 64 KiB -> 1 MiB (`MailSessionProperties`, D40), see the download row.
- `ImapMailbox.clientSearch` stops at `limit` matches (was: every candidate, up to
  `MAX_CLIENT_FILTER` = 2,000), see the client body search rows.
- The probes verify a sent attachment by downloading it back and comparing the CRC, not by the
  declared part size.

## Reproduce

```text
py .python/run_performance_baseline.py --devices bek749scrwv4wo8h,BH900ASK9E
py .python/run_performance_baseline.py --no-jvm --devices emulator-5556=10.0.2.2
py .python/run_settings_real_account.py QQ_A BH900ASK9E --alias perfidle
py .python/run_idle_standby.py BH900ASK9E --alias perfidle --minutes 60 --mode idle
```

The runner starts and stops the server (`--keep-server` leaves it running, `--messages` /
`--attachment-mib` / `--send-mib` change the fixtures), writes `build/p6/perf-jvm.log` and
`build/p6/perf-device-<serial>.log` (one row per line: `op | outcome | ms | note`) and
`build/p6/perf-summary.txt`; the standby writes `build/p6/standby-<serial>.json` plus the
`batterystats` and `meminfo` dumps.
