"""One-hour watch standby on a device: plugin PSS, CPU time and battery (mail roadmap P6 performance baseline).

Usage: py .python/run_idle_standby.py <serial> --alias <alias> [--minutes 60] [--mode idle|auto|poll]
       [--sample-minutes 5] [--keep-doze] [--screen-on]

The account is a saved alias (`.python/run_settings_real_account.py <PROFILE> <serial> --alias <alias>`), so no
credential leaves the device. The script is `docs/smoke/watch.js` with a `standby` scenario (no message is
expected; the watch just stays open until the deadline), pushed to the device and started through the host's
`RunIntentActivity` like a user's script. While it runs, the driver:

- starts the host's own foreground service first (`run-as` + `am start-foreground-service`, the drawer switch
  "foreground service" of AutoJs6) unless `--no-host-foreground`: a script running in a background host without it
  is a cached empty process, and so is the plugin bound only by it; Android 9 killed both after 31 minutes
  (`am_kill ... empty #25`) in the first run;
- fakes an unplugged battery (`dumpsys battery unplug`) and resets `batterystats` so the hour is attributed;
- disables Doze (`dumpsys deviceidle disable`) unless `--keep-doze`, so the figure is the IDLE standby itself
  (the Doze behaviour is the P5 matrix), and turns the screen off unless `--screen-on`;
- samples every `--sample-minutes`: plugin and host PSS (`dumpsys meminfo`), their `oom_score_adj`, plugin CPU ticks
  (`/proc/<pid>/stat`), the plugin's TCP connections to port 993 / 143 (`/proc/net/tcp6`), and the script's progress
  document;
- at the end reads the script report, the watch events the script logged to the host console, the `am_kill` /
  `am_proc_died` records of both processes, `dumpsys batterystats` (the "Estimated power use" rows and the per-uid
  blocks of the plugin and the host) and restores the battery state, Doze, the screen and the foreground service.

Everything is written to build/p6/standby-<serial>.json plus the raw dumps next to it; nothing secret is involved.
"""
import argparse
import io
import json
import os
import re
import statistics
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from run_watch_matrix import HOST_PACKAGE, PLUGIN, PLUGIN_PACKAGE, PROGRESS, REPORT, RUN_INTENT, WORK_DIR, adb, read_progress, shell, wait_progress  # noqa: E402

TICK_HZ = 100  # CLK_TCK on every Android kernel the plugin supports
HOST_FOREGROUND_SERVICE = "org.autojs.autojs.external.foreground.AppForegroundService"


def uid_of(serial, package):
    match = re.search(r"userId=(\d+)", shell(serial, f"dumpsys package {package}"))
    return int(match.group(1)) if match else None


def oom_adj(serial, pid):
    text = shell(serial, f"cat /proc/{pid}/oom_score_adj").strip() if pid else ""
    return int(text) if text.lstrip("-").isdigit() else None


def pid_of(serial, package):
    text = shell(serial, f"pidof {package}").strip()
    return int(text.split()[0]) if text and text.split()[0].isdigit() else None


def pss_kb(serial, package):
    text = shell(serial, f"dumpsys meminfo {package}")
    match = re.search(r"TOTAL PSS:\s+(\d+)", text) or re.search(r"^\s*TOTAL\s+(\d+)", text, re.M)
    return int(match.group(1)) if match else None


def cpu_ticks(serial, pid):
    if pid is None:
        return None
    text = shell(serial, f"cat /proc/{pid}/stat").strip()
    if ")" not in text:
        return None
    fields = text.rsplit(")", 1)[1].split()
    return int(fields[11]) + int(fields[12])  # utime + stime after the comm field


def mail_connections(serial, uid):
    """Established TCP rows of the uid to remote port 993 (03E1) or 143 (008F)."""
    if uid is None:
        return None
    rows = []
    for table in ("tcp6", "tcp"):
        for line in shell(serial, f"cat /proc/net/{table}").splitlines()[1:]:
            parts = line.split()
            if len(parts) < 8:
                continue
            remote_port = parts[2].rsplit(":", 1)[-1].upper()
            if remote_port in ("03E1", "008F") and parts[7] == str(uid):
                rows.append(parts[3])  # state: 01 established, 06 time-wait, ...
    return {"established": rows.count("01"), "other": len(rows) - rows.count("01")}


def power_rows(text, uids):
    """The `Uid u0aNNN:` rows of the "Estimated power use" section for the given uids."""
    section = text.split("Estimated power use (mAh):", 1)[-1] if "Estimated power use (mAh):" in text else ""
    rows = {}
    for uid in uids:
        if uid is None:
            continue
        tag = "u0a%d" % (uid - 10000) if uid >= 10000 else str(uid)
        match = re.search(r"^\s*Uid %s:.*$" % re.escape(tag), section, re.M)
        rows[uid] = match.group(0).strip() if match else None
    capacity = re.search(r"Capacity: (\d+), Computed drain: ([\d.]+), actual drain: ([\d.-]+)", section)
    return rows, capacity.group(0) if capacity else None


def uid_section(text, uid):
    """The per-uid block of `dumpsys batterystats` (network bytes, cpu time, foreground / cached time, services)."""
    if uid is None:
        return None
    tag = "u0a%d" % (uid - 10000) if uid >= 10000 else str(uid)
    match = re.search(r"^  %s:\n((?:    .*\n)+)" % re.escape(tag), text, re.M)
    return [line.strip() for line in match.group(1).splitlines() if line.strip()] if match else None


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--alias", required=True)
    parser.add_argument("--minutes", type=float, default=60.0)
    parser.add_argument("--mode", choices=["auto", "idle", "poll"], default="idle")
    parser.add_argument("--sample-minutes", type=float, default=5.0)
    parser.add_argument("--keep-doze", action="store_true")
    parser.add_argument("--screen-on", action="store_true")
    parser.add_argument("--no-host-foreground", action="store_true")
    args = parser.parse_args()

    out_dir = os.path.join(PLUGIN, "build", "p6")
    os.makedirs(out_dir, exist_ok=True)
    total_ms = int(args.minutes * 60_000)
    scenario = "standby"
    script = os.path.join(out_dir, f"watch-{scenario}.js")
    header = "var WATCH_SCENARIO = " + json.dumps({"name": scenario, "expected": 1_000_000, "totalMs": total_ms, "mode": args.mode}) + ";\n"
    prelude = "var MAIL_SMOKE = " + json.dumps({"alias": args.alias, "workDir": WORK_DIR, "reportPath": f"{WORK_DIR}/watch-{scenario}-report.json"}) + ";\n"
    with io.open(os.path.join(PLUGIN, "docs", "smoke", "watch.js"), encoding="utf-8") as source, io.open(script, "w", encoding="utf-8", newline="\n") as target:
        target.write(prelude + header + source.read())
    device_script = f"/sdcard/Download/watch-{scenario}.js"

    plugin_uid, host_uid = uid_of(args.serial, PLUGIN_PACKAGE), uid_of(args.serial, HOST_PACKAGE)
    api = shell(args.serial, "getprop ro.build.version.sdk").strip()
    model = shell(args.serial, "getprop ro.product.model").strip()
    summary = {"serial": args.serial, "model": model, "api": api, "alias": args.alias, "mode": args.mode, "minutes": args.minutes,
               "pluginUid": plugin_uid, "hostUid": host_uid, "samples": [], "dozeDisabled": not args.keep_doze, "screenOff": not args.screen_on,
               "hostForegroundService": not args.no_host_foreground}
    print(f"{model} API {api}: standby {args.minutes:g} min, mode {args.mode}, plugin uid {plugin_uid}, host uid {host_uid}", flush=True)

    shell(args.serial, f"rm -f {PROGRESS} {REPORT % scenario}")
    adb(args.serial, "logcat", "-c", check=False)
    adb(args.serial, "logcat", "-G", "16M", check=False)
    adb(args.serial, "push", script, device_script)
    restore = []
    try:
        if not args.no_host_foreground:
            shell(args.serial, f"run-as {HOST_PACKAGE} am start-foreground-service --user 0 -n {HOST_PACKAGE}/{HOST_FOREGROUND_SERVICE}")
            restore.append(lambda: shell(args.serial, f"run-as {HOST_PACKAGE} am stopservice --user 0 -n {HOST_PACKAGE}/{HOST_FOREGROUND_SERVICE}"))
            time.sleep(2)
            summary["hostForegroundStarted"] = "isForeground=true" in shell(args.serial, f"dumpsys activity services {HOST_PACKAGE}")
            print(f"host foreground service running: {summary['hostForegroundStarted']}", flush=True)
        shell(args.serial, "dumpsys battery unplug")
        restore.append(lambda: shell(args.serial, "dumpsys battery reset"))
        if not args.keep_doze:
            shell(args.serial, "dumpsys deviceidle disable")
            restore.append(lambda: shell(args.serial, "dumpsys deviceidle enable"))
        shell(args.serial, "dumpsys batterystats --reset")
        shell(args.serial, f"am start -n {RUN_INTENT} -d file://{device_script}")
        started = time.time()
        doc = wait_progress(args.serial, lambda d: d.get("phase") in ("watching", "message"), 240, "the watch")
        summary["watchReadyAfterMs"] = doc.get("at")
        summary["initialMode"] = doc.get("mode")
        print(f"watch ready after {doc.get('at')} ms, mode {doc.get('mode')}, generation {doc.get('generation')}", flush=True)
        if not args.screen_on:
            shell(args.serial, "input keyevent KEYCODE_SLEEP")
            restore.append(lambda: shell(args.serial, "input keyevent KEYCODE_WAKEUP"))
        pid = pid_of(args.serial, PLUGIN_PACKAGE)
        first_ticks = cpu_ticks(args.serial, pid)
        deadline = started + args.minutes * 60
        next_sample = time.time()
        while True:
            now = time.time()
            if now >= next_sample:
                pid_now = pid_of(args.serial, PLUGIN_PACKAGE)
                sample = {
                    "minute": round((now - started) / 60, 1),
                    "pluginPssKb": pss_kb(args.serial, PLUGIN_PACKAGE),
                    "hostPssKb": pss_kb(args.serial, HOST_PACKAGE),
                    "pluginPid": pid_now,
                    "pluginOomAdj": oom_adj(args.serial, pid_now),
                    "hostOomAdj": oom_adj(args.serial, pid_of(args.serial, HOST_PACKAGE)),
                    "pluginCpuTicks": cpu_ticks(args.serial, pid_now),
                    "connections": mail_connections(args.serial, plugin_uid),
                    "progress": {k: (read_progress(args.serial) or {}).get(k) for k in ("phase", "mode", "generation", "events", "active", "at")},
                }
                summary["samples"].append(sample)
                print(f"[{sample['minute']:5.1f} min] plugin PSS {sample['pluginPssKb']} KiB (adj {sample['pluginOomAdj']}), host PSS {sample['hostPssKb']} KiB (adj {sample['hostOomAdj']}), cpu ticks {sample['pluginCpuTicks']}, "
                      f"connections {sample['connections']}, progress {sample['progress']}", flush=True)
                next_sample = now + args.sample_minutes * 60
            if now >= deadline:
                break
            time.sleep(min(5.0, max(0.5, next_sample - now, deadline - now)))
        last_ticks = summary["samples"][-1]["pluginCpuTicks"] if summary["samples"] else None
        if first_ticks is not None and last_ticks is not None and summary["samples"][-1]["pluginPid"] == pid:
            summary["pluginCpuSeconds"] = (last_ticks - first_ticks) / TICK_HZ
        try:
            done = wait_progress(args.serial, lambda d: d.get("phase") == "done", 240, "the script end")
            summary["finalProgress"] = done
        except RuntimeError as e:
            summary["finalProgressError"] = str(e)
        text = shell(args.serial, f"cat {REPORT % scenario} 2>/dev/null").strip()
        report = json.loads(text) if text.startswith("{") else None
        if report:
            summary["report"] = {k: report.get(k) for k in ("ok", "initialMode", "finalMode", "finalState", "events", "timings", "totalMs", "error")}
        console = [line.split("[watch-smoke] ", 1)[1].strip() for line in adb(args.serial, "logcat", "-d", "-v", "time", "-s", "GlobalConsole:D", check=False).splitlines()
                   if "[watch-smoke] " in line and '"type":"progress"' not in line]
        summary["consoleEvents"] = console[-40:]
        kills = [line.strip() for line in adb(args.serial, "logcat", "-d", "-b", "events", "-v", "time", check=False).splitlines()
                 if ("am_kill" in line or "am_proc_died" in line) and (PLUGIN_PACKAGE in line or HOST_PACKAGE in line)]
        summary["killRecords"] = kills[-20:]
        for line in console[-10:]:
            print("console:", line[:300], flush=True)
        for line in kills[-10:]:
            print("kill:", line[:200], flush=True)
        stats = shell(args.serial, "dumpsys batterystats")
        with io.open(os.path.join(out_dir, f"standby-batterystats-{args.serial}.txt"), "w", encoding="utf-8") as f:
            f.write(stats)
        rows, capacity = power_rows(stats, [plugin_uid, host_uid])
        summary["power"] = {"pluginRow": rows.get(plugin_uid), "hostRow": rows.get(host_uid), "capacity": capacity,
                            "pluginUidStats": uid_section(stats, plugin_uid), "hostUidStats": uid_section(stats, host_uid)}
        print(f"power: plugin {rows.get(plugin_uid)!r}; host {rows.get(host_uid)!r}; {capacity}", flush=True)
        with io.open(os.path.join(out_dir, f"standby-meminfo-{args.serial}.txt"), "w", encoding="utf-8") as f:
            f.write(shell(args.serial, f"dumpsys meminfo {PLUGIN_PACKAGE}"))
    finally:
        for step in reversed(restore):
            try:
                step()
            except Exception as e:  # noqa: BLE001
                print(f"restore failed: {e}", flush=True)
    pss = [s["pluginPssKb"] for s in summary["samples"] if s.get("pluginPssKb")]
    if pss:
        summary["pluginPssKbStats"] = {"first": pss[0], "last": pss[-1], "min": min(pss), "median": int(statistics.median(pss)), "max": max(pss)}
    with io.open(os.path.join(out_dir, f"standby-{args.serial}.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=1)
    print("summary:", json.dumps({k: v for k, v in summary.items() if k != "samples"}, ensure_ascii=False), flush=True)
    return 0


if __name__ == "__main__":
    sys.exit(main())
