"""Lifecycle matrix on a device: what the plugin and the host leave behind when a script ends in different ways
(mail roadmap P6 lifecycle matrix).

Usage: py .python/run_lifecycle_matrix.py <serial> --alias <alias>
       [--cases exit,exit-open,stop-all,kill-host,kill-plugin,upgrade,disable,uninstall] [--hold-s 60]

Each case runs `docs/smoke/lifecycle.js` by a saved alias through the host's `RunIntentActivity` (no credential
leaves the device): the script connects, opens a watch, keeps the session busy with a fetch every 10 s and reports
what happens. The driver applies the disturbance from the PC, then takes snapshots of the plugin process before the
script, while it holds, and after it ended (5 s and 30 s later):

- established TCP connections of the plugin uid and of the host uid to the mail server (ports 993 / 143),
- the plugin service's bindings (`dumpsys activity services`, `ConnectionRecord` rows) and whether the service and
  the process still exist (`dumpsys activity processes` state),
- the plugin process's open file descriptors, sockets among them, and threads (`run-as` on the debug build),
- the script's report and the `[lifecycle]` events it logged to the host console.

"No leak" for a case means: no connection of either uid to the server afterwards, no binding left, and the plugin
process (when it is the same pid) back to its file descriptor and thread counts from before the script, within a
small tolerance. `uninstall` removes the saved alias with the package, so it runs last; re-save the alias with
`.python/run_settings_real_account.py <PROFILE> <serial> --alias <alias>` afterwards if more runs are needed.
Everything goes to build/p6/lifecycle-<serial>.json; nothing secret is involved.
"""
import argparse
import io
import json
import os
import re
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from run_watch_matrix import HOST_PACKAGE, PLUGIN, PLUGIN_PACKAGE, RUN_INTENT, adb, shell  # noqa: E402

CASES = ["exit", "exit-open", "stop-all", "kill-host", "kill-plugin", "upgrade", "disable", "uninstall"]
# Scripts, progress and reports live under Download: the host reads and writes there with its all-files access
# (which running a user's script needs anyway), and the shell can push, read and delete there on every API level;
# the host's own external files directory is not writable for the shell on API 33 (and a shell-created one is not
# writable for the host).
SCRIPT_DIR = "/sdcard/Download/lifecycle"
WORK_DIR = SCRIPT_DIR
PROGRESS = f"{WORK_DIR}/lifecycle-progress.json"
PLUGIN_APK = os.path.join(PLUGIN, "app", "build", "outputs", "apk", "debug", "autojs6-plugin-angus-mail-v1.0.0.apk")
FD_TOLERANCE = 3
THREAD_TOLERANCE = 2


def uid_of(serial, package):
    match = re.search(r"userId=(\d+)", shell(serial, f"dumpsys package {package}"))
    return int(match.group(1)) if match else None


def pid_of(serial, package):
    text = shell(serial, f"pidof {package}").strip()
    return int(text.split()[0]) if text and text.split()[0].isdigit() else None


def connections(serial, uid):
    rows = []
    for table in ("tcp6", "tcp"):
        for line in shell(serial, f"cat /proc/net/{table}").splitlines()[1:]:
            parts = line.split()
            if len(parts) < 8:
                continue
            if parts[2].rsplit(":", 1)[-1].upper() in ("03E1", "008F") and parts[7] == str(uid):
                rows.append(parts[3])
    return {"established": rows.count("01"), "other": len(rows) - rows.count("01")}


def process_state(serial, package):
    for line in shell(serial, "dumpsys activity processes").splitlines():
        if f":{package}/" in line and "Proc #" in line:
            return line.strip()[:120]
    return None


def service_state(serial, package):
    text = shell(serial, f"dumpsys activity services {package}")
    return {
        "serviceRecords": text.count("* ServiceRecord{"),
        "bindings": len([l for l in text.splitlines() if l.strip().startswith("* ConnectionRecord{")]),
    }


def fds(serial, package, pid):
    if pid is None:
        return None
    text = shell(serial, f"run-as {package} sh -c 'ls /proc/{pid}/fd | wc -l; ls -l /proc/{pid}/fd | grep -c socket; ls /proc/{pid}/task | wc -l'")
    numbers = [int(x) for x in text.split() if x.isdigit()]
    return {"fds": numbers[0], "sockets": numbers[1], "threads": numbers[2]} if len(numbers) == 3 else {"error": text.strip()[:120]}


def snapshot(serial, plugin_uid, host_uid):
    pid = pid_of(serial, PLUGIN_PACKAGE)
    snap = {"pluginPid": pid, "hostPid": pid_of(serial, HOST_PACKAGE), "pluginConnections": connections(serial, plugin_uid),
            "hostConnections": connections(serial, host_uid), "service": service_state(serial, PLUGIN_PACKAGE),
            "process": process_state(serial, PLUGIN_PACKAGE)}
    snap.update(fds(serial, PLUGIN_PACKAGE, pid) or {})
    return snap


def read_json(serial, path):
    text = shell(serial, f"cat {path} 2>/dev/null").strip()
    try:
        return json.loads(text) if text.startswith("{") else None
    except ValueError:
        return None


def wait_for(serial, predicate, timeout_s, what):
    deadline = time.time() + timeout_s
    last = None
    while time.time() < deadline:
        doc = read_json(serial, PROGRESS)
        if doc is not None:
            last = doc
            if predicate(doc):
                return doc
        time.sleep(1.5)
    raise RuntimeError(f"{what} did not happen within {timeout_s} s; last progress {last}")


def push_script(serial, name, body):
    local = os.path.join(PLUGIN, "build", "p6", f"lifecycle-{name}.js")
    with io.open(local, "w", encoding="utf-8", newline="\n") as f:
        f.write(body)
    shell(serial, f"mkdir -p {SCRIPT_DIR}")
    adb(serial, "push", local, f"{SCRIPT_DIR}/{name}.js")
    return f"{SCRIPT_DIR}/{name}.js"


def launch(serial, device_script):
    shell(serial, f"am start -n {RUN_INTENT} -d file://{device_script}")


def run_case(serial, case, alias, hold_s, plugin_uid, host_uid, source):
    report_path = f"{WORK_DIR}/lifecycle-{case}-report.json"
    shell(serial, f"rm -f {PROGRESS} {report_path}")
    adb(serial, "logcat", "-c", check=False)
    prelude = "var LIFECYCLE = " + json.dumps({"case": case, "holdMs": hold_s * 1000, "workDir": WORK_DIR, "reportPath": report_path}) + ";\n"
    prelude += "var MAIL_SMOKE = " + json.dumps({"alias": alias}) + ";\n"
    device_script = push_script(serial, case, prelude + source)
    result = {"case": case, "before": snapshot(serial, plugin_uid, host_uid)}
    print(f"\n## {case}\nbefore: {summarize(result['before'])}", flush=True)
    started = time.time()
    launch(serial, device_script)
    try:
        doc = wait_for(serial, lambda d: d.get("phase") in ("holding", "done"), 120, "the watch")
        result["holdingAfterMs"] = doc.get("at")
        time.sleep(12)  # one keepalive fetch, then the disturbance
        result["during"] = snapshot(serial, plugin_uid, host_uid)
        print(f"during: {summarize(result['during'])} (generation {doc.get('generation')}, mode {doc.get('mode')})", flush=True)
        disturbed_at = time.time()
        if case == "stop-all":
            launch(serial, push_script(serial, "stop-all", "engines.stopAll();\n"))
        elif case == "kill-host":
            shell(serial, f"am force-stop {HOST_PACKAGE}")
        elif case == "kill-plugin":
            shell(serial, f"am force-stop {PLUGIN_PACKAGE}")
        elif case == "upgrade":
            result["install"] = adb(serial, "install", "-r", "-t", PLUGIN_APK, check=False).strip()[-80:]
        elif case == "disable":
            result["disable"] = shell(serial, f"pm disable-user --user 0 {PLUGIN_PACKAGE}").strip()[:80]
        elif case == "uninstall":
            result["uninstall"] = adb(serial, "uninstall", PLUGIN_PACKAGE, check=False).strip()[:80]
        if case != "exit" and case != "exit-open":
            print(f"disturbance applied: {case}", flush=True)
        if case == "kill-host":
            time.sleep(5)
            result["afterKill5s"] = snapshot(serial, plugin_uid, host_uid)
            print(f"5 s after the host kill: {summarize(result['afterKill5s'])}", flush=True)
        elif case == "stop-all":
            time.sleep(10)  # the engine is stopped at once; the script cannot write a report
            result["finishReason"] = "engine-stopped"
            result["afterStop10s"] = snapshot(serial, plugin_uid, host_uid)
            print(f"10 s after stop-all: {summarize(result['afterStop10s'])}", flush=True)
        else:
            try:
                done = wait_for(serial, lambda d: d.get("phase") == "done", hold_s + 90, "the script end")
                result["scriptEndedAfterMs"] = done.get("at")
                result["finishReason"] = done.get("why")
                print(f"script ended after {done.get('at')} ms ({done.get('why')}), generation {done.get('generation')}, watch active={done.get('active')} reason={done.get('reason')}, client closed={done.get('closed')}", flush=True)
            except RuntimeError as e:
                result["scriptEndError"] = str(e)[:300]
                print(f"script end: {e}", flush=True)
        if case == "disable":
            time.sleep(3)
            result["enable"] = shell(serial, f"pm enable {PLUGIN_PACKAGE}").strip()[:80]
        elif case == "uninstall":
            time.sleep(3)
            result["reinstall"] = adb(serial, "install", "-r", "-t", PLUGIN_APK, check=False).strip()[-80:]
        time.sleep(5)
        result["after5s"] = snapshot(serial, plugin_uid, host_uid)
        print(f"after 5 s: {summarize(result['after5s'])}", flush=True)
        time.sleep(25)
        result["after30s"] = snapshot(serial, plugin_uid, host_uid)
        print(f"after 30 s: {summarize(result['after30s'])}", flush=True)
        result["disturbanceToEndMs"] = int((time.time() - disturbed_at) * 1000)
        report = read_json(serial, report_path)
        if report:
            result["report"] = {k: report.get(k) for k in ("ok", "finishReason", "initialGeneration", "finalWatch", "clientClosed", "steps", "errors", "totalMs")}
            result["report"]["events"] = [e.get("type") + ("" if e.get("detail") is None else ":" + json.dumps(e.get("detail"), ensure_ascii=False)[:80]) for e in report.get("events", [])]
            result["report"]["fetches"] = [("ok" if f.get("ok") else "FAIL " + str(f.get("code"))) + f" {f.get('ms')} ms" for f in report.get("fetches", [])]
        result["console"] = [line.split("[lifecycle] ", 1)[1].strip()[:200] for line in adb(serial, "logcat", "-d", "-v", "raw", "-s", "GlobalConsole:D", check=False).splitlines() if "[lifecycle] " in line][-30:]
        result["verdict"] = verdict(case, result)
        print(f"verdict: {result['verdict']}", flush=True)
    finally:
        if case == "kill-host":
            pass
        result["caseMs"] = int((time.time() - started) * 1000)
    return result


def summarize(snap):
    return (f"pid {snap.get('pluginPid')} conn plugin={snap['pluginConnections']['established']} host={snap['hostConnections']['established']} "
            f"bindings={snap['service']['bindings']} services={snap['service']['serviceRecords']} fds={snap.get('fds')} sockets={snap.get('sockets')} threads={snap.get('threads')} "
            f"state={snap.get('process')}")


def verdict(case, result):
    after = result.get("after30s") or result.get("after5s") or {}
    before = result.get("before") or {}
    problems = []
    if after.get("pluginConnections", {}).get("established", 0) != 0:
        problems.append("plugin still connected to the server")
    if after.get("hostConnections", {}).get("established", 0) != 0:
        problems.append("host connected to the server directly")
    if after.get("service", {}).get("bindings", 0) != 0:
        problems.append("service still bound")
    same_pid = after.get("pluginPid") is not None and after.get("pluginPid") == before.get("pluginPid")
    if same_pid and isinstance(after.get("fds"), int) and isinstance(before.get("fds"), int):
        if after["fds"] > before["fds"] + FD_TOLERANCE:
            problems.append(f"fds grew {before['fds']} -> {after['fds']}")
        if after.get("threads", 0) > before.get("threads", 0) + THREAD_TOLERANCE:
            problems.append(f"threads grew {before.get('threads')} -> {after.get('threads')}")
    elif after.get("pluginPid") is None:
        problems.append("(plugin process gone: fd comparison not applicable)") if case in ("exit", "exit-open", "stop-all") else None
    return "clean" if not [p for p in problems if not p.startswith("(")] else "LEAK? " + "; ".join(problems)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("serial")
    parser.add_argument("--alias", required=True)
    parser.add_argument("--cases", default=",".join(CASES))
    parser.add_argument("--hold-s", type=int, default=60)
    args = parser.parse_args()
    cases = [c.strip() for c in args.cases.split(",") if c.strip()]
    unknown = [c for c in cases if c not in CASES]
    if unknown:
        raise SystemExit(f"unknown cases {unknown}; known: {CASES}")
    if "uninstall" in cases and cases[-1] != "uninstall":
        raise SystemExit("uninstall removes the saved alias: put it last")
    with io.open(os.path.join(PLUGIN, "docs", "smoke", "lifecycle.js"), encoding="utf-8") as f:
        source = f.read()
    plugin_uid, host_uid = uid_of(args.serial, PLUGIN_PACKAGE), uid_of(args.serial, HOST_PACKAGE)
    api = shell(args.serial, "getprop ro.build.version.sdk").strip()
    model = shell(args.serial, "getprop ro.product.model").strip()
    print(f"{model} API {api}: plugin uid {plugin_uid}, host uid {host_uid}, cases {cases}, hold {args.hold_s} s", flush=True)
    results = {"serial": args.serial, "model": model, "api": api, "alias": args.alias, "holdS": args.hold_s, "cases": []}
    for case in cases:
        results["cases"].append(run_case(args.serial, case, args.alias, args.hold_s, plugin_uid, host_uid, source))
        if case == "kill-host":
            time.sleep(3)
    out = os.path.join(PLUGIN, "build", "p6", f"lifecycle-{args.serial}.json")
    with io.open(out, "w", encoding="utf-8") as f:
        json.dump(results, f, ensure_ascii=False, indent=1)
    print("\n| case | during | after 30 s | script | verdict |\n| --- | --- | --- | --- | --- |")
    for r in results["cases"]:
        d, a = r.get("during", {}), r.get("after30s", {})
        rep = r.get("report") or {}
        print(f"| {r['case']} | conn {d.get('pluginConnections', {}).get('established')} bind {d.get('service', {}).get('bindings')} fds {d.get('fds')} thr {d.get('threads')} "
              f"| conn {a.get('pluginConnections', {}).get('established')} bind {a.get('service', {}).get('bindings')} fds {a.get('fds')} thr {a.get('threads')} pid {'same' if a.get('pluginPid') == r['before'].get('pluginPid') else a.get('pluginPid')} "
              f"| {r.get('finishReason') or r.get('scriptEndError', '')[:40]} gen {(rep.get('finalWatch') or {}).get('generation')} errors {len(rep.get('errors') or [])} | {r.get('verdict')} |")
    return 0


if __name__ == "__main__":
    sys.exit(main())
