"""Drives the background-watch device matrix (mail roadmap P8): the plugin's watch service keeps a
saved account under watch while no script runs, a new mail wakes AutoJs6's "On mail arrived" task
and the task runs docs/smoke/trigger.js, which logs one JSON line per launch.

Usage: python .python/run_trigger_matrix.py <PROFILE> <serial> --sender <PROFILE> --scenario <name>
       [--alias qq-smoke] [--watch-id trigger-smoke] [--mode auto|idle|poll] [--minutes 30]
       [--save-account] [--no-install] [--remove]

PROFILE (the watched account) must already be saved on the plugin's settings page under --alias
(`.python/run_settings_real_account.py <PROFILE> <serial> --alias <alias>`; --save-account runs it first).
--sender is a password account (QQ_B, NETEASE_B, ...) whose SMTP the PC uses. Credentials come from the
git-ignored mail-test-accounts.properties and never reach the device through this driver: the device only
sees the alias, the watch id and the subject filter. Nothing secret is printed; the logcat excerpt is saved
with both accounts' secrets masked and scanned ("leak check").

Scenarios (the disturbance happens from the PC through adb while the watch service runs and the host may be dead):

  baseline     send #1, wait, send #2, wait: arrival-to-script latency with the host alive after #1
  screen-off   send #1; screen off + `dumpsys deviceidle force-idle`; send #2 while idle and wait up to --minutes
               (default 30) for the script; then `unforce` + wake and, if #2 is still missing, wait for it
  net-off      send #1; Wi-Fi and mobile data off for --net-off-s (default 300) with #2 sent during the outage;
               network back; the arrival of #2 measures the catch-up after the reconnect
  kill-host    send #1; HOME + `am kill` of the host (run-as kill -9 as the fallback, never force-stop);
               send #2: the plugin's broadcast has to start the host process cold
  kill-plugin  send #1; `run-as <plugin> kill -9`: the sticky service restarts and the watch reconnects; send #2
  reboot       the watch is configured with the boot switch on; `adb reboot`; after boot the boot receiver has to
               start the service without any UI; send #1 and #2. A phone with a pattern / PIN lock keeps its
               credential-encrypted storage locked after the reboot (no BOOT_COMPLETED, no data directory) until
               the user unlocks it: the driver then stops at "waiting for the boot receiver"; once the maintainer
               has unlocked the phone, `--scenario reboot --after-reboot` resumes: no install, no configuration,
               no reboot, just the boot receiver's line, the service and the two mails

Each run installs the host debug + androidTest APKs (built beforehand in D:/idea-projects/AutoJs6 with
`:app:assembleAppDebug :app:assembleAppDebugAndroidTest`) and the plugin debug + androidTest APKs unless
--no-install; the host task and the watch are (re)configured through `am instrument` (the tests keep their
results on the device because they are not Gradle connected runs). --remove deletes the task, the watch, the
boot switch and both test packages. The summary goes to build/p8/trigger-<scenario>-<serial>.json, the masked
logcat excerpt to build/p8/trigger-<scenario>-<serial>.log, the plugin's batterystats section to
build/p8/trigger-<scenario>-<serial>-batterystats.txt.
"""
import argparse
import io
import json
import os
import re
import subprocess
import sys
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from run_host_script_smoke import read_accounts  # noqa: E402
from run_watch_matrix import Sender, adb, device_epoch_ms, shell  # noqa: E402

PLUGIN = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HOST_REPO = os.path.join(os.path.dirname(PLUGIN), "AutoJs6")
HOST_PACKAGE = "org.autojs.autojs6"
HOST_TEST_PACKAGE = HOST_PACKAGE + ".test"
HOST_TEST_CLASS = "org.autojs.autojs.core.plugin.mail.MailTriggerTaskDeviceTest"
PLUGIN_PACKAGE = "io.github.supermonster003.autojs6.plugin.angus.mail"
PLUGIN_TEST_PACKAGE = PLUGIN_PACKAGE + ".test"
PLUGIN_TEST_CLASS = PLUGIN_PACKAGE + ".RealAccountWatchDeviceTest"
RUNNER = "androidx.test.runner.AndroidJUnitRunner"
WATCH_SERVICE = f"{PLUGIN_PACKAGE}/.trigger.MailWatchService"
DEVICE_SCRIPT = "/sdcard/Download/autojs6-mail-trigger.js"
HOST_APK_DIR = os.path.join(HOST_REPO, "app", "build", "outputs", "apk", "app", "debug")
HOST_TEST_APK = os.path.join(HOST_REPO, "app", "build", "outputs", "apk", "androidTest", "app", "debug", "app-app-debug-androidTest.apk")
PLUGIN_APK_DIR = os.path.join(PLUGIN, "app", "build", "outputs", "apk", "debug")
PLUGIN_TEST_APK = os.path.join(PLUGIN, "app", "build", "outputs", "apk", "androidTest", "debug", "app-debug-androidTest.apk")
SCENARIOS = ["baseline", "screen-off", "net-off", "kill-host", "kill-plugin", "reboot"]
LOG_TAGS = ["MailWatchKeeper:I", "MailWatchService:I", "MailTriggerSender:I", "MailBootReceiver:I", "MailTriggerReceiver:D", "MailTriggerSmoke:I",
            "RealAccountWatch:I", "MailTriggerTask:I", "TestRunner:I", "AndroidRuntime:E", "ActivityManager:I"]
OUT_DIR = os.path.join(PLUGIN, "build", "p8")


def log(text):
    print(time.strftime("%H:%M:%S"), text, flush=True)


MARKERS = {"installsAMailArrivedTask": ("MailTriggerTask", "installed task"), "removesTheMailArrivedTasks": ("MailTriggerTask", "removed "),
           "configuresAWatchForTheHost": ("RealAccountWatch", "configured watch="), "removesTheWatch": ("RealAccountWatch", "removed watch=")}


def marker_count(serial, tag, marker):
    return len([l for l in adb(serial, "logcat", "-d", "-v", "raw", "-s", f"{tag}:I", check=False).splitlines() if marker in l])


def instrument(serial, test_package, test_class, method, arguments):
    """Runs one test method through `am instrument -w -r`; returns (passed, output). The pass is judged by the
    test's own log line as well: on Android 9 the runner's result is sometimes reported as "Process crashed"
    (code 0) at the force-stop of the finished instrumentation although the test ran to its end."""
    command = ["shell", "am", "instrument", "-w", "-r", "-e", "class", f"{test_class}#{method}"]
    for key, value in arguments.items():
        if value is None or value == "":
            continue
        assert re.match(r"^[A-Za-z0-9._/#@,:+=\-]+$", str(value)), f"instrumentation argument {key} has characters unsafe for the shell"
        command += ["-e", key, str(value)]
    command.append(f"{test_package}/{RUNNER}")
    tag, marker = MARKERS[method]
    before = marker_count(serial, tag, marker)
    output = adb(serial, *command, check=False)
    failed_status = "INSTRUMENTATION_STATUS_CODE: -2" in output or "INSTRUMENTATION_STATUS_CODE: -3" in output
    passed = "OK (1 test)" in output and not failed_status and "INSTRUMENTATION_FAILED" not in output and "FAILURES" not in output
    if not passed and not failed_status:
        time.sleep(1.5)  # the log line may still be on its way
        if marker_count(serial, tag, marker) > before:
            log(f"  {method}: the runner reported '{output.strip().splitlines()[-2][:80] if len(output.strip().splitlines()) > 1 else output.strip()[:80]}' but the test's log line is there; taken as passed")
            passed = True
    if not passed:
        log(f"  {method}: runner output tail: {output.strip()[-240:]!r}")
        for line in output.splitlines():
            if line.startswith("INSTRUMENTATION_STATUS: stack=") or "Error" in line or "Exception" in line or "INSTRUMENTATION_STATUS_CODE: -3" in line:
                log("  " + line[:400])
    return passed, output


def device_abi(serial):
    return shell(serial, "getprop ro.product.cpu.abilist").strip().split(",")[0]


def host_apk(serial):
    abi = device_abi(serial)
    for name in os.listdir(HOST_APK_DIR):
        if name.endswith(f"-{abi}.apk"):
            return os.path.join(HOST_APK_DIR, name)
    for name in os.listdir(HOST_APK_DIR):
        if name.endswith("-universal.apk"):
            return os.path.join(HOST_APK_DIR, name)
    raise SystemExit(f"no host debug APK for {abi} under {HOST_APK_DIR}")


def plugin_apk():
    names = sorted(name for name in os.listdir(PLUGIN_APK_DIR) if name.endswith(".apk"))
    if not names:
        raise SystemExit(f"no plugin debug APK under {PLUGIN_APK_DIR}")
    return os.path.join(PLUGIN_APK_DIR, names[-1])


def install(serial, apk):
    result = adb(serial, "install", "-r", "-t", apk, check=False)
    if "Success" not in result:
        raise SystemExit(f"install failed: {os.path.basename(apk)}: {result.strip()[-300:]}")
    log(f"installed {os.path.basename(apk)}")


def version_code(serial, package):
    match = re.search(r"versionCode=(\d+)", shell(serial, f"dumpsys package {package}"))
    return int(match.group(1)) if match else None


def sdk(serial):
    return int(shell(serial, "getprop ro.build.version.sdk").strip() or 0)


def grant_storage(serial):
    """The host reads the pushed script from /sdcard/Download: the all-files appop on Android 11+, the legacy permission before."""
    if sdk(serial) >= 30:
        shell(serial, f"appops set {HOST_PACKAGE} MANAGE_EXTERNAL_STORAGE allow")
    else:
        shell(serial, f"pm grant {HOST_PACKAGE} android.permission.READ_EXTERNAL_STORAGE")
        shell(serial, f"pm grant {HOST_PACKAGE} android.permission.WRITE_EXTERNAL_STORAGE")
    if sdk(serial) >= 33:
        shell(serial, f"pm grant {PLUGIN_PACKAGE} android.permission.POST_NOTIFICATIONS")


def pid(serial, package):
    text = shell(serial, f"pidof {package}").strip()
    return int(text.split()[0]) if text and text.split()[0].isdigit() else None


def ensure_root(serial):
    """An emulator's adbd runs as root when asked (a reboot drops it): root may start the non-exported service
    directly, which matters on API 31+ where `run-as` (the plugin's own uid, in the background) is refused by the
    background foreground-service restriction. A production device keeps its shell user; nothing changes there."""
    if shell(serial, "id").strip().startswith("uid=0("):
        return True
    if not serial.startswith("emulator-"):
        return False
    subprocess.run(["adb", "-s", serial, "root"], capture_output=True, text=True, timeout=30)
    subprocess.run(["adb", "-s", serial, "wait-for-device"], timeout=60, check=False)
    deadline = time.time() + 20
    while time.time() < deadline:
        if shell(serial, "id").strip().startswith("uid=0("):
            return True
        time.sleep(1)
    return False


def start_watch_service(serial):
    """Starts the (non-exported) watch service the way the Watches page would. The shell user may start it on
    recent Android versions; Android 9 denies the shell access to a non-exported service (only logcat says so),
    then the start goes through `run-as` under the plugin's own uid (debug build)."""
    ensure_root(serial)
    result = shell(serial, f"am start-foreground-service -n {WATCH_SERVICE}").strip()
    try:
        wait_for(lambda: service_running(serial) or None, 8, "the service start", interval_s=1.0)
    except TimeoutError:
        result = shell(serial, f"run-as {PLUGIN_PACKAGE} am start-foreground-service --user 0 -n {WATCH_SERVICE}").strip() + " (via run-as)"
        try:
            wait_for(lambda: service_running(serial) or None, 8, "the service start", interval_s=1.0)
        except TimeoutError:
            result += " [service not in the foreground afterwards]"
            # the crash buffer, before anything rotates it away (a service that crashes at start lands here)
            crash = adb(serial, "logcat", "-d", "-b", "crash", "-v", "threadtime", check=False)
            tail = [l.rstrip() for l in crash.splitlines() if PLUGIN_PACKAGE in l or "AndroidRuntime" in l or "FATAL" in l][-40:]
            if tail:
                result += "\n    crash buffer:\n    " + "\n    ".join(tail)
    return result


def service_running(serial):
    text = shell(serial, f"dumpsys activity services {PLUGIN_PACKAGE}")
    return "MailWatchService" in text and "isForeground=true" in text


def logcat_lines(serial):
    return [line.rstrip() for line in adb(serial, "logcat", "-d", "-v", "threadtime", "-s", *LOG_TAGS, check=False).splitlines()]


def watch_state(serial, watch_id):
    """The newest keeper state line of the watch: (state, mode) or (None, None)."""
    state = mode = None
    for line in logcat_lines(serial):
        match = re.search(rf"MailWatchKeeper: watch={re.escape(watch_id)} state=(\S+) mode=(\S+)", line)
        if match:
            state, mode = match.group(1), match.group(2)
    return state, mode


def wait_for(predicate, timeout_s, what, interval_s=2.0):
    deadline = time.time() + timeout_s
    last = None
    while time.time() < deadline:
        last = predicate()
        if last:
            return last
        time.sleep(interval_s)
    raise TimeoutError(f"{what} did not happen within {int(timeout_s)} s")


def smoke_lines(serial):
    """The trigger script's JSON lines (newest run only: the driver clears logcat at the start)."""
    docs = []
    for line in adb(serial, "logcat", "-d", "-v", "raw", "-s", "MailTriggerSmoke:I", check=False).splitlines():
        line = line.strip()
        if line.startswith("{"):
            try:
                docs.append(json.loads(line))
            except ValueError:
                pass
    return docs


def wait_for_boot(serial, timeout_s=300):
    subprocess.run(["adb", "-s", serial, "wait-for-device"], timeout=timeout_s, check=False)
    wait_for(lambda: shell(serial, "getprop sys.boot_completed").strip() == "1", timeout_s, "boot")
    time.sleep(5)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("profile", help="the watched account (QQ_A, NETEASE_A, ...)")
    parser.add_argument("serial", help="adb serial of the device")
    parser.add_argument("--sender", required=True, help="the account the PC sends from (a password account)")
    parser.add_argument("--scenario", choices=SCENARIOS, default="baseline")
    parser.add_argument("--alias", default="qq-smoke", help="alias the watched account is saved under on the plugin's settings page")
    parser.add_argument("--watch-id", default="trigger-smoke", help="watch id (trigger id) to configure")
    parser.add_argument("--mode", choices=["auto", "idle", "poll"], default="auto", help="watch mode")
    parser.add_argument("--interval-s", type=int, default=60, help="poll interval for the poll mode / fallback")
    parser.add_argument("--minutes", type=float, default=30.0, help="screen-off: how long the device stays in forced idle waiting for #2")
    parser.add_argument("--net-off-s", type=int, default=300, help="net-off: how long the network stays off")
    parser.add_argument("--wait-s", type=int, default=240, help="how long to wait for each arrival before giving up")
    parser.add_argument("--save-account", action="store_true", help="save the watched account under --alias first (run_settings_real_account.py)")
    parser.add_argument("--no-install", action="store_true", help="skip the APK installs")
    parser.add_argument("--after-reboot", action="store_true", help="reboot only: the device was rebooted by an earlier run and unlocked since; resume with the boot receiver and the mails")
    parser.add_argument("--remove", action="store_true", help="remove the task, the watch, the boot switch and both test packages, then exit")
    args = parser.parse_args()

    props = read_accounts()
    kind, letter = args.profile.rsplit("_", 1)
    watched = props[f"{kind}_USER_NAME_{letter}"]
    watched_secret = props.get(f"{kind}_AUTH_CODE_{letter}") or props.get(f"{kind}_ACCESS_TOKEN_{letter}") or ""
    sender = Sender(props, args.sender)
    secrets = [value for value in (watched_secret, sender.secret) if value]
    addresses = [watched.lower(), sender.address.lower()]
    os.makedirs(OUT_DIR, exist_ok=True)

    def mask(text):
        for value in secrets:
            text = text.replace(value, "****")
        return text

    if args.remove:
        log(f"removing the task and the watch on {args.serial}")
        ok_host, _ = instrument(args.serial, HOST_TEST_PACKAGE, HOST_TEST_CLASS, "removesTheMailArrivedTasks", {})
        ok_plugin, _ = instrument(args.serial, PLUGIN_TEST_PACKAGE, PLUGIN_TEST_CLASS, "removesTheWatch", {"watchId": args.watch_id})
        start_watch_service(args.serial)  # no watch left: the service syncs to zero and stops itself
        shell(args.serial, "dumpsys deviceidle unforce")
        shell(args.serial, "svc wifi enable")
        shell(args.serial, "svc data enable")
        shell(args.serial, f"rm -f {DEVICE_SCRIPT}")
        for package in (HOST_TEST_PACKAGE, PLUGIN_TEST_PACKAGE):
            adb(args.serial, "uninstall", package, check=False)
        log(f"removed: host task {'ok' if ok_host else 'FAILED'}, watch {'ok' if ok_plugin else 'FAILED'}, service running={service_running(args.serial)}")
        return 0 if ok_host and ok_plugin else 1

    resumed = None
    if args.after_reboot:
        assert args.scenario == "reboot", "--after-reboot goes with --scenario reboot"
        earlier = os.path.join(OUT_DIR, f"trigger-reboot-{args.serial}.json")
        with io.open(earlier, encoding="utf-8") as handle:
            resumed = json.load(handle)
        assert resumed.get("rebootedAt"), f"{earlier} does not come from a run that rebooted the device"
        log(f"resuming the reboot scenario of {time.strftime('%H:%M:%S', time.localtime(resumed['rebootedAt']))} (subject filter {resumed['subjectToken']})")
    if args.after_reboot:
        pass
    elif not args.no_install:
        install(args.serial, plugin_apk())
        install(args.serial, PLUGIN_TEST_APK)
        install(args.serial, host_apk(args.serial))
        install(args.serial, HOST_TEST_APK)
    else:
        # the settings runner uninstalls the plugin's test package after saving an account; both test packages must be present
        installed = shell(args.serial, "pm list packages")
        for package, apk in ((PLUGIN_TEST_PACKAGE, PLUGIN_TEST_APK), (HOST_TEST_PACKAGE, HOST_TEST_APK)):
            if f"package:{package}" not in installed:
                install(args.serial, apk)
    grant_storage(args.serial)
    log(f"host versionCode={version_code(args.serial, HOST_PACKAGE)} plugin versionCode={version_code(args.serial, PLUGIN_PACKAGE)} sdk={sdk(args.serial)}")

    if args.save_account and not args.after_reboot:
        code = subprocess.call([sys.executable, os.path.join(PLUGIN, ".python", "run_settings_real_account.py"), args.profile, args.serial, "--alias", args.alias, "--no-build"], cwd=PLUGIN)
        if code != 0:
            raise SystemExit(f"saving the account failed with exit code {code}")
        install(args.serial, PLUGIN_TEST_APK)  # the settings runner uninstalls the test package afterwards

    stamp = int(time.time())
    subject_token = resumed["subjectToken"] if resumed else f"trigger-{stamp}"
    summary = {"scenario": args.scenario, "serial": args.serial, "sdk": sdk(args.serial), "watched": watched.rsplit("@", 1)[-1], "sender": sender.provider,
               "alias": args.alias, "watchId": args.watch_id, "mode": args.mode, "hostVersionCode": version_code(args.serial, HOST_PACKAGE),
               "pluginVersionCode": version_code(args.serial, PLUGIN_PACKAGE), "sends": [], "startedAt": stamp, "subjectToken": subject_token}
    if resumed:
        summary["rebootedAt"] = resumed["rebootedAt"]
        summary["stoppedBeforeReboot"] = resumed.get("stoppedBeforeReboot")
        summary["resumedAfterUnlock"] = True

    if resumed:
        # the script, the task and the watch survived the reboot; the logcat holds the receiver's line since the unlock
        boot_line = wait_for(lambda: [l for l in logcat_lines(args.serial) if "MailBootReceiver" in l], 180, "the boot receiver (unlock the device first)")
        summary["bootReceiver"] = boot_line[-1].split("MailBootReceiver: ", 1)[-1]
        summary["uptimeAtResumeS"] = float((shell(args.serial, "cat /proc/uptime").split() or ["0"])[0])
        log(f"boot receiver: {summary['bootReceiver']} (device up for {summary['uptimeAtResumeS']:.0f} s)")
        return finish_run(args, summary, sender, watched, subject_token, secrets, addresses, mask, resumed=True)

    # the script the host task launches
    adb(args.serial, "push", os.path.join(PLUGIN, "docs", "smoke", "trigger.js"), DEVICE_SCRIPT)
    shell(args.serial, f"rm -f /sdcard/Android/data/{HOST_PACKAGE}/files/mail-trigger/report.jsonl")
    adb(args.serial, "logcat", "-c", check=False)
    shell(args.serial, "dumpsys batterystats --reset")

    # the host must not be in the stopped state (a fresh install is): launch it once, then leave it in the background
    shell(args.serial, f"monkey -p {HOST_PACKAGE} -c android.intent.category.LAUNCHER 1")
    time.sleep(4)
    shell(args.serial, "input keyevent KEYCODE_HOME")
    ok, _ = instrument(args.serial, HOST_TEST_PACKAGE, HOST_TEST_CLASS, "installsAMailArrivedTask",
                       {"mail.trigger.script": DEVICE_SCRIPT, "mail.trigger.watch": args.watch_id})
    if not ok:
        raise SystemExit("installing the host task failed (is the host debug build with the mail trigger receiver installed?)")
    log("host task installed")

    # the watch, then the service (the instrumentation's end kills the plugin process)
    ok, output = instrument(args.serial, PLUGIN_TEST_PACKAGE, PLUGIN_TEST_CLASS, "configuresAWatchForTheHost",
                            {"mailAlias": args.alias, "watchId": args.watch_id, "watchMode": args.mode, "watchIntervalMs": args.interval_s * 1000,
                             "watchSubject": subject_token, "watchBoot": "true" if args.scenario == "reboot" else "false"})
    if not ok:
        if "must be saved first" in output:
            log(f"the account is not saved under alias '{args.alias}': run .python/run_settings_real_account.py {args.profile} {args.serial} --alias {args.alias} (or pass --save-account)")
        raise SystemExit("configuring the watch failed")
    log(f"watch {args.watch_id} configured (subject filter {subject_token}, mode {args.mode})")

    restore = []
    try:
        if args.scenario == "reboot":
            # a package the instrumentation force-stopped is in the stopped state and gets no BOOT_COMPLETED;
            # starting the service (as the Watches page does) clears it, and the running watch is what a user reboots with
            started = start_watch_service(args.serial)
            log(f"service start before the reboot: {started or 'ok'}")
            try:
                wait_for(lambda: (lambda s: s if s[0] == "connected" else None)(watch_state(args.serial, args.watch_id)), 120, "the connected state before the reboot")
            except TimeoutError as e:
                log(str(e))
            time.sleep(12)  # the package manager writes the component state a few seconds after the change
            summary["stoppedBeforeReboot"] = "stopped=true" in shell(args.serial, f"dumpsys package {PLUGIN_PACKAGE} | grep -m1 ' stopped='")
            log(f"rebooting the device (plugin stopped state {summary['stoppedBeforeReboot']})")
            summary["rebootedAt"] = int(time.time())
            with io.open(os.path.join(OUT_DIR, f"trigger-reboot-{args.serial}.json"), "w", encoding="utf-8") as handle:
                json.dump(summary, handle, ensure_ascii=False, indent=2)  # a locked phone needs `--after-reboot` later
            adb(args.serial, "reboot", check=False)
            time.sleep(15)
            wait_for_boot(args.serial)
            booted_at = time.time()
            log("device booted; waiting for the boot receiver and the service")
            try:
                boot_line = wait_for(lambda: [l for l in logcat_lines(args.serial) if "MailBootReceiver" in l], 180, "the boot receiver")
            except TimeoutError:
                locked = "deviceLocked=1" in shell(args.serial, "dumpsys trust")
                if locked:
                    log("the boot receiver did not run: the device is locked (credential-encrypted storage); unlock it, then rerun with --after-reboot --no-install")
                    return 2
                raise
            summary["bootReceiver"] = boot_line[-1].split("MailBootReceiver: ", 1)[-1]
            log(summary["bootReceiver"])
        else:
            started = start_watch_service(args.serial)
            log(f"service start: {started or 'ok'}")
        return finish_run(args, summary, sender, watched, subject_token, secrets, addresses, mask, resumed=False, restore=restore, booted_at=locals().get("booted_at"))
    finally:
        pass


def finish_run(args, summary, sender, watched, subject_token, secrets, addresses, mask, resumed, restore=None, booted_at=None):
    restore = restore if restore is not None else []
    try:
        try:
            state, mode = wait_for(lambda: (lambda s: s if s[0] == "connected" else None)(watch_state(args.serial, args.watch_id)), 180, "the connected state")
        except TimeoutError as e:
            state, mode = watch_state(args.serial, args.watch_id)
            log(f"{e}; last state {state} mode {mode}")
            summary["connectError"] = str(e)
        summary["initialState"], summary["initialMode"] = state, mode
        summary["serviceForeground"] = service_running(args.serial)
        if args.scenario == "reboot" and booted_at is not None:
            summary["connectedAfterBootMs"] = int((time.time() - booted_at) * 1000)
        log(f"watch state {state} mode {mode}, service foreground={summary['serviceForeground']}")

        def send_and_wait(index, wait_s=args.wait_s):
            subject = f"AutoJs6 {subject_token} #{index}"
            offset = device_epoch_ms(args.serial) - int(time.time() * 1000)
            sent_pc = int(time.time() * 1000)
            smtp_ms = sender.send(watched, subject)
            entry = {"index": index, "subject": subject, "smtpMs": smtp_ms, "scriptMs": None, "pluginMs": None, "launches": 0}
            summary["sends"].append(entry)
            log(f"sent #{index} in {smtp_ms} ms")
            try:
                docs = wait_for(lambda: [d for d in smoke_lines(args.serial) if d.get("subject") == subject] or None, wait_s, f"the script for #{index}")
                first = docs[0]
                entry["scriptMs"] = first["atEpoch"] - (sent_pc + offset)
                entry["pluginMs"] = (first.get("receivedAt") or first["atEpoch"]) - (sent_pc + offset)
                entry["launchDelayMs"] = first.get("launchDelayMs")
                entry["uid"] = first.get("uid")
                entry["triggerId"] = first.get("triggerId")
                entry["intentAction"] = first.get("intentAction")
                time.sleep(6)  # a duplicate launch would follow within the throttle window
                entry["launches"] = len([d for d in smoke_lines(args.serial) if d.get("subject") == subject])
                entry["mode"] = watch_state(args.serial, args.watch_id)[1]
                log(f"#{index}: plugin saw it after {entry['pluginMs']} ms, script ran after {entry['scriptMs']} ms (launch delay {entry['launchDelayMs']} ms, launches {entry['launches']}, mode {entry['mode']})")
            except TimeoutError as e:
                entry["error"] = str(e)
                state, mode = watch_state(args.serial, args.watch_id)
                entry["stateAfter"] = state
                log(f"#{index}: {e}; watch state {state} mode {mode}; broadcasts {len([l for l in logcat_lines(args.serial) if 'broadcast sent' in l])}, receiver lines {len([l for l in logcat_lines(args.serial) if 'MailTriggerReceiver' in l])}")
            return entry

        send_and_wait(1)

        if resumed:
            send_and_wait(2)
        elif args.scenario == "screen-off":
            shell(args.serial, "input keyevent KEYCODE_SLEEP")
            time.sleep(3)
            forced = shell(args.serial, "dumpsys deviceidle force-idle").strip()
            summary["forceIdle"] = forced
            restore.append(lambda: shell(args.serial, "dumpsys deviceidle unforce"))
            restore.append(lambda: shell(args.serial, "input keyevent KEYCODE_WAKEUP"))
            time.sleep(5)
            summary["deviceIdle"] = shell(args.serial, "dumpsys deviceidle get deep").strip()
            summary["procStates"] = {name: " ".join(shell(args.serial, f"dumpsys activity p {package} | grep -m1 -i procstate").split())
                                     for name, package in (("host", HOST_PACKAGE), ("plugin", PLUGIN_PACKAGE))}
            log(f"screen off, force-idle: {forced}; idle {summary['deviceIdle']}; proc states {summary['procStates']}")
            idle_wait_s = int(args.minutes * 60)
            # keep the device idle for the whole window before sending, then send while still idle
            settle_s = min(idle_wait_s // 2, 600)
            log(f"holding idle for {settle_s} s before #2")
            time.sleep(settle_s)
            summary["stateBeforeIdleSend"] = watch_state(args.serial, args.watch_id)[0]
            summary["deviceIdleBeforeSend"] = shell(args.serial, "dumpsys deviceidle get deep").strip()
            entry = send_and_wait(2, wait_s=idle_wait_s - settle_s)
            if entry.get("scriptMs") is None:
                shell(args.serial, "dumpsys deviceidle unforce")
                shell(args.serial, "input keyevent KEYCODE_WAKEUP")
                woke_at = int(time.time() * 1000)
                log("no script while idle: device woken")
                try:
                    offset = device_epoch_ms(args.serial) - int(time.time() * 1000)
                    docs = wait_for(lambda: [d for d in smoke_lines(args.serial) if d.get("subject") == entry["subject"]] or None, args.wait_s, "the script after the wake")
                    entry["scriptAfterWakeMs"] = docs[0]["atEpoch"] - (woke_at + offset)
                    log(f"#2 script ran {entry['scriptAfterWakeMs']} ms after the wake")
                except TimeoutError as e:
                    entry["errorAfterWake"] = str(e)
                    log(f"#2 after the wake: {e}")
        elif args.scenario == "net-off":
            shell(args.serial, "svc wifi disable")
            shell(args.serial, "svc data disable")
            restore.append(lambda: shell(args.serial, "svc wifi enable"))
            restore.append(lambda: shell(args.serial, "svc data enable"))
            log(f"network off for {args.net_off_s} s")
            time.sleep(min(30, args.net_off_s // 2))
            summary["stateDuringOutage"] = watch_state(args.serial, args.watch_id)[0]
            subject = f"AutoJs6 {subject_token} #2"
            sent_pc = int(time.time() * 1000)
            offset = device_epoch_ms(args.serial) - int(time.time() * 1000)
            smtp_ms = sender.send(watched, subject)
            entry = {"index": 2, "subject": subject, "smtpMs": smtp_ms, "scriptMs": None, "pluginMs": None, "launches": 0, "sentDuringOutage": True}
            summary["sends"].append(entry)
            log(f"sent #2 during the outage in {smtp_ms} ms; watch state {summary['stateDuringOutage']}")
            time.sleep(max(0, args.net_off_s - min(30, args.net_off_s // 2)))
            shell(args.serial, "svc wifi enable")
            shell(args.serial, "svc data enable")
            network_back = int(time.time() * 1000)
            log("network on")
            try:
                docs = wait_for(lambda: [d for d in smoke_lines(args.serial) if d.get("subject") == subject] or None, args.wait_s, "the script for #2")
                entry["scriptMs"] = docs[0]["atEpoch"] - (sent_pc + offset)
                entry["pluginMs"] = (docs[0].get("receivedAt") or docs[0]["atEpoch"]) - (sent_pc + offset)
                entry["afterNetworkBackMs"] = docs[0]["atEpoch"] - (network_back + offset)
                entry["uid"] = docs[0].get("uid")
                time.sleep(6)
                entry["launches"] = len([d for d in smoke_lines(args.serial) if d.get("subject") == subject])
                entry["mode"] = watch_state(args.serial, args.watch_id)[1]
                log(f"#2 script ran {entry['afterNetworkBackMs']} ms after the network came back (launches {entry['launches']})")
            except TimeoutError as e:
                entry["error"] = str(e)
                log(f"#2: {e}; watch state {watch_state(args.serial, args.watch_id)}")
            send_and_wait(3)
        elif args.scenario == "kill-host":
            shell(args.serial, "input keyevent KEYCODE_HOME")
            time.sleep(2)
            before = pid(args.serial, HOST_PACKAGE)
            shell(args.serial, f"am kill {HOST_PACKAGE}")
            time.sleep(2)
            if pid(args.serial, HOST_PACKAGE) is not None:
                shell(args.serial, f"run-as {HOST_PACKAGE} kill -9 {pid(args.serial, HOST_PACKAGE)}")
                time.sleep(2)
            summary["hostPidBeforeKill"] = before
            summary["hostPidAfterKill"] = pid(args.serial, HOST_PACKAGE)
            log(f"host killed: pid {before} -> {summary['hostPidAfterKill']}")
            entry = send_and_wait(2)
            entry["hostPidAfter"] = pid(args.serial, HOST_PACKAGE)
            log(f"host pid after #2: {entry['hostPidAfter']}")
        elif args.scenario == "kill-plugin":
            before = pid(args.serial, PLUGIN_PACKAGE)
            shell(args.serial, f"run-as {PLUGIN_PACKAGE} kill -9 {before}")
            killed_at = time.time()
            log(f"plugin killed (pid {before})")
            try:
                wait_for(lambda: pid(args.serial, PLUGIN_PACKAGE) not in (None, before), 120, "the process restart")
                summary["pluginRestartMs"] = int((time.time() - killed_at) * 1000)
                # the restart's connected line follows the kill: count the connected lines
                def reconnected():
                    lines = [l for l in logcat_lines(args.serial) if f"watch={args.watch_id} state=connected" in l]
                    return lines if len(lines) >= 2 else None
                wait_for(reconnected, 180, "the reconnect after the restart")
                summary["pluginReconnectMs"] = int((time.time() - killed_at) * 1000)
                log(f"plugin restarted after {summary['pluginRestartMs']} ms, reconnected after {summary['pluginReconnectMs']} ms")
            except TimeoutError as e:
                summary["restartError"] = str(e)
                log(str(e))
            summary["serviceForegroundAfterKill"] = service_running(args.serial)
            send_and_wait(2)
        else:
            send_and_wait(2)
    finally:
        for action in reversed(restore):
            try:
                action()
            except Exception as e:  # noqa: BLE001
                log(f"restore failed: {e}")

    # evidence: states, dedup, batterystats, leaks
    lines = logcat_lines(args.serial)
    summary["finalState"], summary["finalMode"] = watch_state(args.serial, args.watch_id)
    summary["serviceForegroundAtEnd"] = service_running(args.serial)
    summary["broadcasts"] = len([l for l in lines if "MailTriggerSender" in l and "broadcast sent=true" in l])
    summary["receiverEvents"] = len([l for l in lines if "MailTriggerReceiver: mail trigger: watch=" in l])
    summary["scriptLaunches"] = len(smoke_lines(args.serial))
    summary["stateLines"] = [l.split("MailWatchKeeper: ", 1)[-1] for l in lines if "MailWatchKeeper: watch=" in l and " state=" in l]
    summary["pluginThreads"] = int(shell(args.serial, f"run-as {PLUGIN_PACKAGE} ls /proc/{pid(args.serial, PLUGIN_PACKAGE)}/task 2>/dev/null | wc -l").strip() or 0) if pid(args.serial, PLUGIN_PACKAGE) else None
    battery = shell(args.serial, f"dumpsys batterystats {PLUGIN_PACKAGE}")
    battery_path = os.path.join(OUT_DIR, f"trigger-{args.scenario}-{args.serial}-batterystats.txt")
    with io.open(battery_path, "w", encoding="utf-8") as handle:
        handle.write(mask(battery))
    interesting = [l.strip() for l in battery.splitlines() if re.search(r"Wake lock|Foreground services|Foreground for|CPU|Wifi|Mobile network|Total cpu time|Job ", l)]
    summary["batterystats"] = interesting[:40]
    logcat_full = adb(args.serial, "logcat", "-d", check=False)
    summary["leakCheck"] = {"secretInLogcat": any(s in logcat_full for s in secrets), "addressInLogcat": any(a in logcat_full.lower() for a in addresses)}
    log_path = os.path.join(OUT_DIR, f"trigger-{args.scenario}-{args.serial}.log")
    with io.open(log_path, "w", encoding="utf-8") as handle:
        handle.write(mask("\n".join(lines)) + "\n")
    out = os.path.join(OUT_DIR, f"trigger-{args.scenario}-{args.serial}.json")
    with io.open(out, "w", encoding="utf-8") as handle:
        json.dump(summary, handle, ensure_ascii=False, indent=2)
    arrivals = [e for e in summary["sends"] if e.get("scriptMs") is not None]
    ok = len(arrivals) == len(summary["sends"]) and all(e.get("launches") == 1 for e in arrivals)
    log(f"result: {'ok' if ok else 'INCOMPLETE'}; sends {len(summary['sends'])}, scripts {len(arrivals)}, broadcasts {summary['broadcasts']}, receiver events {summary['receiverEvents']}, "
        f"launches {summary['scriptLaunches']}, final state {summary['finalState']} ({summary['finalMode']}), leak check {'LEAK' if summary['leakCheck']['secretInLogcat'] else 'clean'}, "
        f"address in logcat {'yes' if summary['leakCheck']['addressInLogcat'] else 'no'}")
    log(f"summary: {os.path.relpath(out, PLUGIN)}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
