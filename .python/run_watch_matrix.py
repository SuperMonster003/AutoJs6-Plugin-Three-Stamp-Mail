"""Drives the new-mail watch device matrix (mail roadmap P5).

Usage: python .python/run_watch_matrix.py <PROFILE> <serial> --sender <PROFILE> --scenario <name> [--mode auto|idle|poll]

PROFILE (the watched account) and --sender (the account whose SMTP the PC uses to send) are
QQ_A / QQ_B / NETEASE_A / NETEASE_B / NETEASE126_A / SINA_A (password accounts) or GMAIL_A,
OUTLOOK_A, HOTMAIL_B (access token, watched only; the Outlook.com tokens come from
build/outlook-token.properties written by .python/outlook_oauth_login.py). Credentials come from the
git-ignored mail-test-accounts.properties; the watched account reaches the device only as instrumentation
arguments (see run_host_script_smoke.py), the sender's stay on the PC. Nothing secret is printed.

Scenarios (the disturbance happens from the PC through adb while docs/smoke/watch.js watches):

  baseline          send #1, wait, send #2, wait: plain arrival latency
  wifi-off          send #1; `svc wifi disable`, 30 s, `svc wifi enable`; send #2 (recovery)
  net-off           the same with Wi-Fi and mobile data both off (the emulator has no Wi-Fi)
  wifi-to-cellular  send #1; `svc data enable` + `svc wifi disable` (stays on cellular); send #2; Wi-Fi back at the end
  doze              send #1; screen off + `dumpsys deviceidle force-idle`; send #2 while idle; after --doze-minutes
                    (default 15) without arrival the device is woken (`unforce`) and the arrival after the wake is recorded
  doze-background   like doze, but the host is sent to the background first (HOME key); needs --alias, because
                    the script then runs through the host's run intent instead of the instrumentation (which keeps
                    the host process in the foreground and therefore keeps its network during Doze)
  kill-plugin       send #1; `am force-stop <plugin>`; send #2 after the host re-watched (progress shows generation 2)

--alias <alias>: connect by an account saved on the plugin's settings page (`.python/run_settings_real_account.py
<PROFILE> <serial> --alias <alias>`); the script is pushed to the device and started through the host's
`RunIntentActivity` (the host must be installed and allowed to read external storage), no credential reaches the
host and the report is read from the device.

The script's progress file (`watch-progress.json` under the host's mail-smoke work directory)
is read through adb (the script also leaves a copy of its report one level up, because the host
test removes the work directory); the summary goes to build/p5/watch-<scenario>-<serial>.json,
the Gradle log of the host test to build/p3/watch-<scenario>-<serial>.log.
"""
import argparse
import io
import json
import os
import smtplib
import subprocess
import sys
import threading
import time
from email.message import EmailMessage

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from run_host_script_smoke import PROVIDERS, TOKEN_KINDS, read_accounts  # noqa: E402

PLUGIN = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HOST_PACKAGE = "org.autojs.autojs6"
PLUGIN_PACKAGE = "io.github.supermonster003.autojs6.plugin.angus.mail"
PROGRESS = f"/sdcard/Android/data/{HOST_PACKAGE}/files/mail-smoke/watch-progress.json"
REPORT = f"/sdcard/Android/data/{HOST_PACKAGE}/files/watch-%s-report.json"  # the script's copy outside the work directory the host test removes
SMTP = {"qq": ("smtp.qq.com", 465), "163": ("smtp.163.com", 465), "126": ("smtp.126.com", 465), "sina": ("smtp.sina.com", 465)}
SCENARIOS = ["baseline", "wifi-off", "net-off", "wifi-to-cellular", "doze", "doze-background", "kill-plugin"]
WORK_DIR = f"/sdcard/Android/data/{HOST_PACKAGE}/files/mail-smoke"
RUN_INTENT = f"{HOST_PACKAGE}/org.autojs.autojs.external.open.RunIntentActivity"


def adb(serial, *args, check=True):
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if check and result.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout


def shell(serial, command):
    return adb(serial, "shell", command, check=False)


def device_epoch_ms(serial):
    """The device clock in ms; toybox on API 24 has no %N, so seconds are the fallback."""
    text = shell(serial, "echo $EPOCHREALTIME; date +%s%3N; date +%s").strip().splitlines()
    for line in text:
        line = line.strip()
        if line and line[0].isdigit():
            if "." in line:
                return int(float(line) * 1000)
            if len(line) == 13 and line.isdigit():
                return int(line)
            if len(line) == 10 and line.isdigit():
                return int(line) * 1000
    raise RuntimeError(f"cannot read the device clock: {text}")


def read_progress(serial):
    text = shell(serial, f"cat {PROGRESS} 2>/dev/null").strip()
    if not text.startswith("{"):
        return None
    try:
        return json.loads(text)
    except ValueError:
        return None


def read_report(serial, scenario):
    """The script's report: from the copy the script leaves in the host's files directory, else from the
    chunks the host test logs (the connected run uninstalls the host afterwards, taking its files along)."""
    text = shell(serial, f"cat {REPORT % scenario} 2>/dev/null").strip()
    if not text.startswith("{"):
        prefix = f"watch-{scenario} report["
        chunks = {}
        for line in adb(serial, "logcat", "-d", "-v", "raw", "-s", "MailScriptSmokeTest:I", check=False).splitlines():
            line = line.strip()
            if line.startswith(f"watch-{scenario}: "):
                chunks = {}  # a new run of the test: keep only its chunks
            elif line.startswith(prefix):
                index, _, chunk = line[len(prefix):].partition("]: ")
                chunks[int(index)] = chunk
        text = "".join(chunks[i] for i in sorted(chunks))
    try:
        return json.loads(text) if text.startswith("{") else None
    except ValueError:
        return None


def wait_progress(serial, predicate, timeout_s, what, process=None):
    deadline = time.time() + timeout_s
    last = None
    while time.time() < deadline:
        if process is not None and process.poll() is not None and not (read_progress(serial) or {}).get("phase") == "done":
            raise RuntimeError(f"the host test ended with exit code {process.returncode} before {what}")
        doc = read_progress(serial)
        if doc is not None:
            last = doc
            if predicate(doc):
                return doc
        time.sleep(1.5)
    raise RuntimeError(f"{what} did not happen within {timeout_s} s; last progress {last}")


class Sender:
    def __init__(self, props, profile):
        kind, letter = profile.rsplit("_", 1)
        if kind in TOKEN_KINDS or f"{kind}_ACCESS_TOKEN_{letter}" in props:
            raise SystemExit("the sender must be a password account")
        self.address = props[f"{kind}_USER_NAME_{letter}"]
        self.secret = props[f"{kind}_AUTH_CODE_{letter}"]
        self.provider = PROVIDERS[self.address.rsplit("@", 1)[-1].lower()]
        self.host, self.port = SMTP[self.provider]

    def send(self, to, subject):
        message = EmailMessage()
        message["From"] = self.address
        message["To"] = to
        message["Subject"] = subject
        message.set_content("AutoJs6 watch matrix " + subject)
        started = time.time()
        with smtplib.SMTP_SSL(self.host, self.port, timeout=60) as smtp:
            smtp.login(self.address, self.secret)
            smtp.send_message(message)
        return int((time.time() - started) * 1000)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("profile", help="the watched account (QQ_A, ...)")
    parser.add_argument("serial", help="adb serial of the device")
    parser.add_argument("--sender", required=True, help="the account the PC sends from (a password account)")
    parser.add_argument("--scenario", choices=SCENARIOS, default="baseline")
    parser.add_argument("--mode", choices=["auto", "idle", "poll"], default="auto", help="watch mode option")
    parser.add_argument("--doze-minutes", type=float, default=15.0, help="how long the device stays in forced idle before it is woken")
    parser.add_argument("--wait-s", type=int, default=180, help="how long to wait for each arrival before giving up")
    parser.add_argument("--alias", default=None, help="run by saved-account alias through the host's run intent instead of the instrumentation")
    args = parser.parse_args()

    props = read_accounts()
    kind, letter = args.profile.rsplit("_", 1)
    watched = props[f"{kind}_USER_NAME_{letter}"]
    sender = Sender(props, args.sender)
    doze_s = args.doze_minutes * 60
    total_ms = int((args.wait_s * 2 + (doze_s if args.scenario in ("doze", "doze-background") else 0) + 240) * 1000)
    script_dir = os.path.join(PLUGIN, "build", "p5", "smoke")
    os.makedirs(script_dir, exist_ok=True)
    script = os.path.join(script_dir, f"watch-{args.scenario}.js")
    header = "var WATCH_SCENARIO = " + json.dumps({"name": args.scenario, "expected": 2, "totalMs": total_ms, "mode": args.mode}) + ";\n"
    with io.open(os.path.join(PLUGIN, "docs", "smoke", "watch.js"), encoding="utf-8") as source, io.open(script, "w", encoding="utf-8", newline="\n") as target:
        target.write(header + source.read())

    if args.scenario == "doze-background" and not args.alias:
        raise SystemExit("doze-background needs --alias (the instrumentation keeps the host in the foreground)")
    shell(args.serial, f"rm -f {PROGRESS} {REPORT % args.scenario}")
    adb(args.serial, "logcat", "-c", check=False)
    print(f"scenario={args.scenario} watched={args.profile} ({watched.rsplit('@', 1)[-1]}) sender={args.sender} ({sender.provider}) serial={args.serial} mode={args.mode} totalMs={total_ms}" + (f" alias={args.alias}" if args.alias else ""))
    runner_output = []
    if args.alias:
        # by alias through the host's run intent: a prelude without any credential, launched like a user's script
        device_script = f"/sdcard/Download/watch-{args.scenario}.js"
        with io.open(script, encoding="utf-8") as source:
            body = source.read()
        prelude = "var MAIL_SMOKE = " + json.dumps({"alias": args.alias, "workDir": WORK_DIR, "reportPath": f"{WORK_DIR}/watch-{args.scenario}-report.json"}) + ";\n"
        with io.open(script, "w", encoding="utf-8", newline="\n") as target:
            target.write(prelude + body)
        adb(args.serial, "push", script, device_script)
        # no -W: the run intent activity finishes at once and `am start -W` would wait for it forever
        shell(args.serial, f"am start -n {RUN_INTENT} -d file://{device_script}")
        process = None
    else:
        log_name = f"watch-{args.scenario}-{args.serial}.log"
        runner = [sys.executable, os.path.join(PLUGIN, ".python", "run_host_script_smoke.py"), args.profile, args.serial,
                  "--script", os.path.relpath(script, PLUGIN), "--log", log_name, "--timeout", str(total_ms + 90_000)]
        process = subprocess.Popen(runner, cwd=PLUGIN, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace")
        threading.Thread(target=lambda: runner_output.extend(process.stdout), daemon=True).start()

    summary = {"scenario": args.scenario, "serial": args.serial, "watched": watched.rsplit("@", 1)[-1], "sender": sender.provider, "mode": args.mode, "sends": []}
    stamp = int(time.time())
    restore = []
    try:
        doc = wait_progress(args.serial, lambda d: d.get("phase") in ("watching", "message"), 240, "the watch", process)
        summary["initialMode"] = doc.get("mode")
        summary["watchReadyAfterMs"] = doc.get("at")

        def send_and_wait(index, wait_s=args.wait_s):
            subject = f"AutoJs6 watch {stamp} #{index}"
            offset = device_epoch_ms(args.serial) - int(time.time() * 1000)
            sent_pc = int(time.time() * 1000)
            smtp_ms = sender.send(watched, subject)
            entry = {"index": index, "smtpMs": smtp_ms, "arrivalMs": None}
            summary["sends"].append(entry)
            print(f"sent #{index} in {smtp_ms} ms", flush=True)
            try:
                arrived = wait_progress(args.serial, lambda d: d.get("messages", 0) >= index, wait_s, f"arrival of #{index}", process)
                entry["arrivalMs"] = arrived["atEpoch"] - (sent_pc + offset)
                entry["mode"] = arrived.get("mode")
                entry["generation"] = arrived.get("generation")
                print(f"#{index} arrived after {entry['arrivalMs']} ms (mode {entry['mode']}, generation {entry['generation']})", flush=True)
            except RuntimeError as e:
                entry["error"] = str(e)
                print(f"#{index}: {e}", flush=True)
            return entry

        send_and_wait(1)

        if args.scenario in ("wifi-off", "net-off"):
            shell(args.serial, "svc wifi disable")
            if args.scenario == "net-off":
                shell(args.serial, "svc data disable")
                restore.append(lambda: shell(args.serial, "svc data enable"))
            print("network off", flush=True)
            time.sleep(30)
            shell(args.serial, "svc wifi enable")
            if args.scenario == "net-off":
                shell(args.serial, "svc data enable")
            print("network on", flush=True)
            time.sleep(8)
            send_and_wait(2)
        elif args.scenario == "wifi-to-cellular":
            shell(args.serial, "svc data enable")
            shell(args.serial, "svc wifi disable")
            restore.append(lambda: shell(args.serial, "svc wifi enable"))
            print("wifi off, cellular data on", flush=True)
            time.sleep(15)
            send_and_wait(2)
        elif args.scenario == "kill-plugin":
            before = read_progress(args.serial) or {}
            shell(args.serial, f"am force-stop {PLUGIN_PACKAGE}")
            killed_at = time.time()
            print("plugin killed", flush=True)
            rewatched = wait_progress(args.serial, lambda d: (d.get("generation") or 0) > (before.get("generation") or 1), 120, "the re-watch")
            summary["rewatchAfterMs"] = int((time.time() - killed_at) * 1000)
            summary["rewatchGeneration"] = rewatched.get("generation")
            print(f"re-watched with generation {rewatched.get('generation')} about {summary['rewatchAfterMs']} ms after the kill", flush=True)
            send_and_wait(2)
        elif args.scenario in ("doze", "doze-background"):
            if args.scenario == "doze-background":
                shell(args.serial, "input keyevent KEYCODE_HOME")
                time.sleep(2)
            shell(args.serial, "input keyevent KEYCODE_SLEEP")
            time.sleep(3)
            forced = shell(args.serial, "dumpsys deviceidle force-idle").strip()
            summary["forceIdle"] = forced
            restore.append(lambda: shell(args.serial, "dumpsys deviceidle unforce"))
            restore.append(lambda: shell(args.serial, "input keyevent KEYCODE_WAKEUP"))
            print(f"screen off, force-idle: {forced}", flush=True)
            time.sleep(5)
            summary["procStates"] = {name: " ".join(shell(args.serial, f"dumpsys activity p {package} | grep -m1 -i procstate").split()) for name, package in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))}
            summary["deviceIdle"] = shell(args.serial, "dumpsys deviceidle get deep").strip()
            print(f"idle state {summary['deviceIdle']}, proc states {summary['procStates']}", flush=True)
            entry = send_and_wait(2, wait_s=int(doze_s))
            if entry.get("arrivalMs") is None:
                shell(args.serial, "dumpsys deviceidle unforce")
                shell(args.serial, "input keyevent KEYCODE_WAKEUP")
                woke_at = int(time.time() * 1000)
                print("no arrival while idle: device woken", flush=True)
                try:
                    offset = device_epoch_ms(args.serial) - int(time.time() * 1000)
                    arrived = wait_progress(args.serial, lambda d: d.get("messages", 0) >= 2, args.wait_s, "arrival after the wake")
                    entry["arrivalAfterWakeMs"] = arrived["atEpoch"] - (woke_at + offset)
                    entry["mode"] = arrived.get("mode")
                    entry["generation"] = arrived.get("generation")
                    print(f"#2 arrived {entry['arrivalAfterWakeMs']} ms after the wake", flush=True)
                except RuntimeError as e:
                    entry["errorAfterWake"] = str(e)
                    print(f"#2 after the wake: {e}", flush=True)
        else:
            send_and_wait(2)
    finally:
        for action in reversed(restore):
            try:
                action()
            except Exception as e:  # noqa: BLE001
                print(f"restore failed: {e}")

    if process is not None:
        process.wait()
        text = "".join(runner_output)
        report = read_report(args.serial, args.scenario)
        summary["runnerExit"] = process.returncode
    else:
        text = ""
        wait_progress(args.serial, lambda d: d.get("phase") == "done", 60, "the script's report")
        report_text = shell(args.serial, f"cat {WORK_DIR}/watch-{args.scenario}-report.json 2>/dev/null").strip()
        report = json.loads(report_text) if report_text.startswith("{") else None
        summary["runnerExit"] = None
    summary["report"] = report
    out = os.path.join(PLUGIN, "build", "p5", f"watch-{args.scenario}-{args.serial}.json")
    with io.open(out, "w", encoding="utf-8") as handle:
        json.dump(summary, handle, ensure_ascii=False, indent=2)
    for line in text.splitlines():
        if line.startswith(("profile=", "gradle exit", "report leak check")) or "BUILD " in line or "FAILED" in line:
            print(line)
    if report is not None:
        print("report ok:", report.get("ok"), "events:", [(e["type"], e.get("detail")) for e in report.get("events", [])])
        print("initial mode:", report.get("initialMode"), "final mode:", report.get("finalMode"), "generation:", report.get("finalGeneration"), "error:", report.get("error"))
    print("summary:", out)
    return 0 if report is not None and report.get("ok") else 1


if __name__ == "__main__":
    sys.exit(main())
