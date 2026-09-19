"""Performance baseline against a seeded GreenMail (mail roadmap P6): JVM rows and device rows.

Usage: py .python/run_performance_baseline.py [--devices SERIAL[=HOST],...] [--no-jvm] [--messages 10000]
       [--attachment-mib 50] [--send-mib 10] [--imap-port 3143] [--smtp-port 3025] [--pop3-port 3110]
       [--server-heap 2g] [--keep-server]

1. `:mail-core:writeTestClasspath` and `PerfMailServer` (a `main` in the mail-core test sources) start a plain
   GreenMail on the given ports in its own JVM, seeded with `--messages` small messages and one message with a
   random attachment of `--attachment-mib` MiB; the runner waits for its READY line.
2. `--no-jvm` not given: `PerformanceBaselineProbe` runs through `:mail-core:test` with build/p6/perf.properties
   and its rows land in build/p6/perf-jvm.log.
3. Every `--devices` entry runs `PerformanceDeviceTest` through `:app:connectedDebugAndroidTest` (plugin
   uninstalled first, `adb reverse` for the two ports unless a HOST such as 10.0.2.2 is given for an emulator);
   its rows come from `logcat -s PerfDevice` into build/p6/perf-device-<serial>.log.
4. The server is stopped through its stop file; `--keep-server` leaves it running (stop it with
   `build/p6/perf-server-stop`).

No account or secret is involved: the server's only user is perf / perf-secret on localhost.
"""
import argparse
import io
import os
import subprocess
import sys
import threading
import time

PLUGIN = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PLUGIN_PACKAGE = "io.github.supermonster003.autojs6.plugin.angus.mail"
OUT = os.path.join(PLUGIN, "build", "p6")
JAVA = os.environ.get("PERF_JAVA") or os.path.join(os.environ.get("JAVA_HOME", "E:/.java/jdk-21.0.1"), "bin", "java.exe")
SERVER_MAIN = PLUGIN_PACKAGE + ".core.perf.PerfMailServerKt"
DEVICE_TEST = PLUGIN_PACKAGE + ".PerformanceDeviceTest"


def gradle(*args, timeout=3600):
    gradlew = os.path.join(PLUGIN, "gradlew.bat")
    cmd = ["cmd", "/c", gradlew, *args, "--console=plain", "-q"]
    result = subprocess.run(cmd, cwd=PLUGIN, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=timeout)
    return result.returncode, result.stdout + result.stderr


def adb(serial, *args, check=True):
    result = subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace")
    if check and result.returncode != 0:
        raise RuntimeError(f"adb {' '.join(args)} failed: {result.stderr.strip()}")
    return result.stdout


def start_server(args):
    code, output = gradle(":mail-core:writeTestClasspath")
    if code != 0:
        raise SystemExit(f"writeTestClasspath failed:\n{output[-3000:]}")
    classpath = io.open(os.path.join(PLUGIN, "mail-core", "build", "test-classpath.txt"), encoding="utf-8").read().strip()
    ready_file = os.path.join(OUT, "perf-server-ready")
    stop_file = os.path.join(OUT, "perf-server-stop")
    for f in (ready_file, stop_file):
        if os.path.exists(f):
            os.remove(f)
    argfile = os.path.join(OUT, "perf-server-args.txt")
    with io.open(argfile, "w", encoding="utf-8", newline="\n") as f:
        f.write(f"-Xmx{args.server_heap}\n-Dfile.encoding=UTF-8\n-cp\n{classpath}\n{SERVER_MAIN}\n")
        f.write(f"imapPort={args.imap_port}\nsmtpPort={args.smtp_port}\npop3Port={args.pop3_port}\nbind=0.0.0.0\n")
        f.write(f"messages={args.messages}\nattachmentMiB={args.attachment_mib}\nstopFile={stop_file}\nreadyFile={ready_file}\n")
    log = io.open(os.path.join(OUT, "perf-server.log"), "w", encoding="utf-8")
    process = subprocess.Popen([JAVA, "@" + argfile], cwd=PLUGIN, stdin=subprocess.PIPE, stdout=log, stderr=subprocess.STDOUT)
    started = time.time()
    while not os.path.exists(ready_file):
        if process.poll() is not None:
            log.close()
            raise SystemExit("the server ended before READY:\n" + io.open(os.path.join(OUT, "perf-server.log"), encoding="utf-8", errors="replace").read()[-3000:])
        if time.time() - started > 900:
            process.kill()
            raise SystemExit("the server did not become ready within 15 minutes")
        time.sleep(1)
    ready = io.open(ready_file, encoding="utf-8").read().strip()
    print(f"server {ready} (after {int(time.time() - started)} s)", flush=True)
    return process, stop_file, ready


def stop_server(process, stop_file):
    with io.open(stop_file, "w", encoding="utf-8") as f:
        f.write("stop\n")
    try:
        process.wait(timeout=60)
    except subprocess.TimeoutExpired:
        process.kill()
    if os.path.exists(stop_file):
        os.remove(stop_file)


def run_jvm(args):
    props = os.path.join(OUT, "perf.properties")
    with io.open(props, "w", encoding="utf-8") as f:
        f.write(f"host=127.0.0.1\nimapPort={args.imap_port}\nsmtpPort={args.smtp_port}\nsendMiB={args.send_mib}\nwarmRuns=3\n")
    try:
        code, output = gradle(":mail-core:test", "--tests", "*PerformanceBaselineProbe")
    finally:
        os.remove(props)
    log = os.path.join(OUT, "perf-jvm.log")
    text = io.open(log, encoding="utf-8").read() if os.path.exists(log) else ""
    print(f"--- JVM rows (gradle exit {code}) ---\n{text}", flush=True)
    if code != 0:
        print(output[-3000:], flush=True)
    return code == 0 and text and all(" | ok | " in line for line in text.splitlines() if " | " in line)


def run_device(args, entry):
    serial, _, host = entry.partition("=")
    host = host or "127.0.0.1"
    reverse = not host.startswith("10.0.2.")
    if reverse:
        adb(serial, "reverse", f"tcp:{args.imap_port}", f"tcp:{args.imap_port}")
        adb(serial, "reverse", f"tcp:{args.smtp_port}", f"tcp:{args.smtp_port}")
    adb(serial, "uninstall", PLUGIN_PACKAGE, check=False)
    adb(serial, "logcat", "-c", check=False)
    env_args = [
        f"-Pandroid.testInstrumentationRunnerArguments.class={DEVICE_TEST}",
        f"-Pandroid.testInstrumentationRunnerArguments.perfHost={host}",
        f"-Pandroid.testInstrumentationRunnerArguments.perfImapPort={args.imap_port}",
        f"-Pandroid.testInstrumentationRunnerArguments.perfSmtpPort={args.smtp_port}",
        f"-Pandroid.testInstrumentationRunnerArguments.perfSendMiB={args.send_mib}",
    ]
    env = dict(os.environ, ANDROID_SERIAL=serial)
    gradlew = os.path.join(PLUGIN, "gradlew.bat")
    result = subprocess.run(["cmd", "/c", gradlew, ":app:connectedDebugAndroidTest", "--console=plain", "-q", *env_args],
                            cwd=PLUGIN, env=env, capture_output=True, text=True, encoding="utf-8", errors="replace", timeout=3600)
    rows = [line.split("PerfDevice:", 1)[1].strip() if "PerfDevice:" in line else line.strip()
            for line in adb(serial, "logcat", "-d", "-v", "brief", "-s", "PerfDevice:I", check=False).splitlines() if "PerfDevice" in line]
    with io.open(os.path.join(OUT, f"perf-device-{serial}.log"), "w", encoding="utf-8") as f:
        f.write("\n".join(rows) + "\n")
    print(f"--- device {serial} rows (gradle exit {result.returncode}) ---\n" + "\n".join(rows), flush=True)
    if result.returncode != 0:
        print((result.stdout + result.stderr)[-3000:], flush=True)
    if reverse:
        adb(serial, "reverse", "--remove", f"tcp:{args.imap_port}", check=False)
        adb(serial, "reverse", "--remove", f"tcp:{args.smtp_port}", check=False)
    return result.returncode == 0 and rows and all(" | ok | " in r for r in rows if " | " in r)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--devices", default="", help="comma-separated adb serials, each optionally =HOST (10.0.2.2 for an emulator)")
    parser.add_argument("--no-jvm", action="store_true")
    parser.add_argument("--messages", type=int, default=10_000)
    parser.add_argument("--attachment-mib", type=int, default=50)
    parser.add_argument("--send-mib", type=int, default=10)
    parser.add_argument("--imap-port", type=int, default=3143)
    parser.add_argument("--smtp-port", type=int, default=3025)
    parser.add_argument("--pop3-port", type=int, default=3110)
    parser.add_argument("--server-heap", default="2g")
    parser.add_argument("--keep-server", action="store_true")
    args = parser.parse_args()
    os.makedirs(OUT, exist_ok=True)
    process, stop_file, ready = start_server(args)
    outcomes = {}
    try:
        if not args.no_jvm:
            outcomes["jvm"] = run_jvm(args)
        for entry in [d for d in args.devices.split(",") if d.strip()]:
            outcomes[entry.strip()] = run_device(args, entry.strip())
    finally:
        if args.keep_server:
            print(f"server kept running; create {stop_file} to end it", flush=True)
        else:
            stop_server(process, stop_file)
    with io.open(os.path.join(OUT, "perf-summary.txt"), "w", encoding="utf-8") as f:
        f.write(ready + "\n" + "\n".join(f"{k}: {'ok' if v else 'PROBLEM'}" for k, v in outcomes.items()) + "\n")
    print("outcomes:", outcomes, flush=True)
    return 0 if all(outcomes.values()) else 1


if __name__ == "__main__":
    sys.exit(main())
