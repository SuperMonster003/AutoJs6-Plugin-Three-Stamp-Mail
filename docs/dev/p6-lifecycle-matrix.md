# P6 lifecycle matrix (what a session and a watch leave behind)

Roadmap P6 "生命周期矩阵", run on 2026-09-20 against plugin build 41 plus the changes of this commit
on the Redmi 22120RN86C (API 33, HyperOS) with the host 6.8.0 / 5282 and a QQ account saved on the
plugin's settings page as alias `lifecycle`. The driver is `.python/run_lifecycle_matrix.py`, the
script `docs/smoke/lifecycle.js`; no credential leaves the device (the script connects by alias
through the host's `RunIntentActivity`, like a user's script).

## Method

The script connects (`mail.connect(alias)`), opens `client.watch('INBOX', { fetchBody: false })`,
keeps the session busy with `client.fetch({ limit: 1 })` every 10 s, logs every watch event to the
host console as `[lifecycle] {...}` and writes a progress document on every step. The driver takes a
snapshot of the plugin process before the script, while it holds (12 s after the watch is up), and
5 s and 30 s after it ended; between the second and the third snapshot it applies the disturbance
from the PC. A snapshot is:

- the established TCP connections of the plugin uid and of the host uid to the mail server (remote
  port 993 / 143 in `/proc/net/tcp6`);
- `dumpsys activity services <plugin>`: the `ServiceRecord` of `AngusMailPluginService` and its
  `ConnectionRecord` rows (the host's bindings);
- the plugin process state line of `dumpsys activity processes`;
- the plugin process's open file descriptors, the sockets among them and its threads
  (`run-as <plugin> ls /proc/<pid>/fd | wc -l` and friends; debug build).

"Clean" means: afterwards no connection of either uid to the server, no binding left, and, when the
plugin process is the same one, its file descriptors back to the count before the script (tolerance
3) and its threads within 2 of that count. The uninstall case runs last because the saved alias goes
with the package.

## Results (hold 60 s, `mode: poll` by the QQ preset, D38)

| Case | Disturbance (from the PC, 12 s into the hold) | Script end | Plugin after 30 s | Verdict |
| --- | --- | --- | --- | --- |
| `exit` | none; the script stops the watch and closes the client after the hold | `hold-elapsed` at 61.2 s; `stop` 14 ms, `close` 24 ms, `close('stopped')` event | same pid, 0 connections, 0 bindings, 0 service records, fds 87 = before, threads 17 (before 16) | clean |
| `exit-open` | none; the script calls `exit()` with the watch and the client open | `hold-elapsed` at 60.2 s; no event reaches the script (the host cancels the watch silently and closes the default client on exit) | same pid, 0 / 0 / 0, fds 87 = before, threads 18 (before 17) | clean |
| `stop-all` | a second script runs `engines.stopAll()` | the engine is stopped at once (no report); 10 s later 0 connections and one binding still held by the exit path, gone at 15 s | same pid, 0 / 0 / 0, fds 85 (before 87), threads 20 (before 18) | clean |
| `kill-host` | `am force-stop org.autojs.autojs6` | the host is gone (no report) | 5 s after the kill already 0 connections, 0 bindings and no service record (the plugin's `linkToDeath` handler shut the sessions down and the service stopped itself); same pid stays cached, fds 87, threads 22 (before 20) | clean |
| `kill-plugin` | `am force-stop <plugin>` | `error(PLUGIN_UNAVAILABLE "mail plugin process died")` 2.7 s after the kill, `resync('rewatched')` 1.1 s later with generation 2, the keepalive fetch of that moment took 1.4 s and succeeded, `hold-elapsed` at 61.4 s | new pid, 0 / 0 / 0, fds 87, threads 17 | clean |
| `upgrade` | `adb install -r -t` of the same debug APK over the running plugin | same sequence as `kill-plugin`: `error(PLUGIN_UNAVAILABLE)` 2.7 s after the install, `resync('rewatched')` 1.3 s later, generation 2, fetches keep succeeding, `hold-elapsed` at 61.2 s | new pid, 0 / 0 / 0, fds 87, threads 17 | clean |
| `disable` | `pm disable-user --user 0 <plugin>` (then `pm enable` after the script ended) | `error(PLUGIN_UNAVAILABLE "mail plugin process died")` 2.3 s after the disable, then the rewatch fails with the non-retryable `PLUGIN_UNAVAILABLE "mail plugin is unavailable: Missing required plugin ..."` and the watch ends with `close('error')` 17 ms later; the script's close handler closed the client (2 ms) and ended at 15.3 s | no process, 0 / 0 / 0 | clean |
| `uninstall` | `adb uninstall <plugin>` (then reinstall) | same as `disable`: `error`, `error`, `close('error')` at 15.9 s, client closed, script ended at 17.0 s | no process, 0 / 0 / 0 | clean |

Common figures: while a script holds, the plugin process has 2 established connections to QQ (one
for the session, one for the watch), 2 bindings from the host (`MailSessionBinder` and
`MailWatchBinder` leases), 4 more file descriptors and 5 to 9 more threads than before; the host
process never connects to the server itself (0 rows in every snapshot). Connecting by alias took
59 to 150 ms with a warm plugin process and 1.2 s after a reinstall (cold start); `watch` 27 to
69 ms; the first keepalive fetch 1.1 to 1.5 s, the following ones 0.43 to 0.66 s.

## The thread that stayed behind

The thread count of the plugin process grew by one or two with every case that reused the same
process (16, 17, 18, 20, 22 across `exit`, `exit-open`, `stop-all`, `kill-host`): within the
tolerance of a single case, not over a day of scripts. Listing the threads by name after three
`exit` runs in a row showed the growth: one `pool-N-thread-1` per run (N = 2, 4, 6) besides the
Binder pool the platform grows on demand (`binder:<pid>_3`, `_4`; it never shrinks and is not the
plugin's). `pool-N-thread-1` is the default thread factory's name, used by nothing in the plugin:
it is the `ScheduledThreadPoolExecutor` Angus Mail creates for each `WriteTimeoutSocket` (one per
connection when `mail.<protocol>.writetimeout` is set, which the mail core always does) and shuts
down in the wrapper's `close()`. A cancel or a watch stop closes the plain socket underneath the
TLS layer first (`SocketRegistry.abort`, D35), and the TLS socket on Android (Conscrypt) then does
not close the wrapper below it, so the timer's thread stayed for the life of the process, one per
aborted TLS connection. The session's own connection is closed through the wrapper on
`client.close()` and was never affected; on the JVM the JSSE socket closes the wrapper regardless,
so the leak is Android-only. Angus 2.0.5 accepts a shared timer through
`mail.<protocol>.executor.writetimeout` and then never shuts it down itself: the mail core now
hands every session one daemon `ScheduledThreadPoolExecutor` (`WriteTimeouts`; its core thread
ends 30 s after the last timeout task), `WriteTimeoutExecutorTest` asserts over GreenMail's
implicit SSL that no pool of Angus's own appears and that this timer is the only one (the test
fails without the property), and the same three `exit` runs on the Redmi with the fix end with 16,
17 and 17 threads and no `pool-` thread at all (the Binder pool 4 -> 5 is the only change).

## Driver notes

- On API 33 the shell can neither write into the host's external files directory nor can the host
  write into a directory the shell created there, so scripts, progress and reports live under
  `/sdcard/Download/lifecycle/` (the host reads and writes there with its all-files access, which
  running a user's script needs anyway).
- The host on the Redmi had been reinstalled by another session on 2026-09-19 and had lost that
  access (`MANAGE_EXTERNAL_STORAGE` denied): the `RunIntentActivity` then finishes without a word.
  `appops set org.autojs.autojs6 MANAGE_EXTERNAL_STORAGE allow` restores it.
- `engines.stopAll()` from a second script stops the probe without a report; the driver snapshots
  10 s after the stop instead of waiting for one.
- The device is shared with other sessions' plugin tests; the snapshots only look at the two uids.

## Reproduce

```text
py .python/run_settings_real_account.py QQ_A <serial> --alias lifecycle
py .python/run_lifecycle_matrix.py <serial> --alias lifecycle --hold-s 60
py .python/run_lifecycle_matrix.py <serial> --alias lifecycle --cases exit,kill-plugin --hold-s 30
```

Output: `build/p6/lifecycle-<serial>.json` (every snapshot, the script reports and the console
events) and the markdown table on stdout.
