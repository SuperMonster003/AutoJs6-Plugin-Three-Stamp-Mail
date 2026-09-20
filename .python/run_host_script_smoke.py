"""Runs the host script-API smoke tests against a real account (mail roadmap P3).

Usage: python .python/run_host_script_smoke.py <PROFILE> <serial> [--script docs/smoke/x.js] [--log <name>]

PROFILE is QQ_A / QQ_B / NETEASE_A / NETEASE_B (password accounts) or GMAIL_A (access token).
Credentials come from the git-ignored mail-test-accounts.properties next to this repository's
root and reach the device only as instrumentation arguments of the host tests in
`org.autojs.autojs.runtime.api.augment.mail.MailScriptSmokeDeviceTest` (host repository
`../AutoJs6`): `#realProviderConnectAndTest` without `--script`, `#realProviderScript` with
`--script`, which first pushes the script to /data/local/tmp/autojs6-mail-smoke/ on the device.
Nothing secret is printed; the Gradle log is scanned for the secret afterwards and reported as
"report leak check: clean" or "LEAK".

The host test engine installs the host APK without `-r`: put the matching split APK on the
device with `adb install -r` (or uninstall the host) before running, see
docs/dev/p3-script-api-evidence.md.
"""
import argparse
import io
import os
import subprocess
import sys

PLUGIN = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
HOST = os.path.join(os.path.dirname(PLUGIN), "AutoJs6")
TEST_CLASS = "org.autojs.autojs.runtime.api.augment.mail.MailScriptSmokeDeviceTest"
DEVICE_DIR = "/data/local/tmp/autojs6-mail-smoke"
PROVIDERS = {"qq.com": "qq", "foxmail.com": "qq", "163.com": "163", "126.com": "126", "yeah.net": "163", "sina.com": "sina", "sina.cn": "sina", "gmail.com": "gmail"}
TOKEN_KINDS = {"GMAIL"}


def read_properties(path):
    props = {}
    with io.open(path, encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            props[key.strip()] = value.strip()
    return props


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("profile", help="QQ_A, QQ_B, NETEASE_A, NETEASE_B, NETEASE126_A, SINA_A or GMAIL_A (the part before the last underscore names the property prefix)")
    parser.add_argument("serial", help="adb serial of the device")
    parser.add_argument("--script", default=None, help="repository-relative smoke script (docs/smoke/*.js) to push and run")
    parser.add_argument("--log", default=None, help="log file name under build/p3/")
    parser.add_argument("--alias", default=None, help="alias of an account saved on the plugin's settings page (roadmap P4.7): runs #savedAccountScript with --script and only the alias; the profile's credentials then serve the leak check alone")
    parser.add_argument("--timeout", type=int, default=None, help="milliseconds the host test waits for the script's report (mail.smoke.timeoutMs, default 300000; the P5 watch matrix needs more for Doze)")
    args = parser.parse_args()

    props = read_properties(os.path.join(PLUGIN, "mail-test-accounts.properties"))
    kind, letter = args.profile.rsplit("_", 1)
    address = props[f"{kind}_USER_NAME_{letter}"]
    if kind in TOKEN_KINDS or f"{kind}_ACCESS_TOKEN_{letter}" in props:
        secret = props[f"{kind}_ACCESS_TOKEN_{letter}"]
        secret_argument = "mail.smoke.accessToken"
    else:
        secret = props[f"{kind}_AUTH_CODE_{letter}"]
        secret_argument = "mail.smoke.password"
    domain = address.rsplit("@", 1)[-1].lower()
    provider = PROVIDERS[domain]

    log_dir = os.path.join(PLUGIN, "build", "p3")
    os.makedirs(log_dir, exist_ok=True)
    suffix = ""
    test = TEST_CLASS + "#realProviderConnectAndTest"
    extra = []
    if args.script:
        local = os.path.join(PLUGIN, args.script)
        name = os.path.basename(local)
        remote = f"{DEVICE_DIR}/{name}"
        subprocess.check_call(["adb", "-s", args.serial, "shell", "mkdir", "-p", DEVICE_DIR])
        subprocess.check_call(["adb", "-s", args.serial, "push", local, remote], stdout=subprocess.DEVNULL)
        test = TEST_CLASS + "#realProviderScript"
        extra.append(f"-Pandroid.testInstrumentationRunnerArguments.mail.smoke.script={remote}")
        if args.timeout:
            extra.append(f"-Pandroid.testInstrumentationRunnerArguments.mail.smoke.timeoutMs={args.timeout}")
        suffix = "-" + os.path.splitext(name)[0]
    account = [
        f"-Pandroid.testInstrumentationRunnerArguments.mail.smoke.provider={provider}",
        f"-Pandroid.testInstrumentationRunnerArguments.mail.smoke.address={address}",
        f"-Pandroid.testInstrumentationRunnerArguments.{secret_argument}={secret}",
    ]
    if args.alias:
        assert args.script, "--alias needs --script"
        test = TEST_CLASS + "#savedAccountScript"
        account = [f"-Pandroid.testInstrumentationRunnerArguments.mail.smoke.alias={args.alias}"]
        suffix += "-alias"
    log = os.path.join(log_dir, args.log or f"host-smoke-{args.profile.lower()}{suffix}-{args.serial}.log")
    gradlew = os.path.join(HOST, "gradlew.bat" if os.name == "nt" else "gradlew")
    command = [
        gradlew, ":app:connectedAppDebugAndroidTest",
        f"-Pandroid.testInstrumentationRunnerArguments.class={test}",
        *account,
        *extra,
        "--console=plain",
    ]
    print(f"profile={args.profile} provider={provider} domain={domain} serial={args.serial} test={test.rsplit('#', 1)[1]}" + (f" alias={args.alias}" if args.alias else ""))
    env = dict(os.environ, ANDROID_SERIAL=args.serial)
    if args.alias:
        subprocess.run(["adb", "-s", args.serial, "logcat", "-c"], capture_output=True)
    with io.open(log, "w", encoding="utf-8") as out:
        code = subprocess.call(command, cwd=HOST, env=env, stdout=out, stderr=subprocess.STDOUT)
    text = io.open(log, encoding="utf-8", errors="replace").read()
    print("gradle exit", code)
    print("report leak check:", "LEAK" if secret in text else "clean")
    if args.alias:
        # The credential audit of the alias flow: neither the secret nor the address may reach any log.
        device_log = subprocess.run(["adb", "-s", args.serial, "logcat", "-d"], capture_output=True, text=True, encoding="utf-8", errors="replace").stdout
        print("address in gradle log:", "yes" if address.lower() in text.lower() else "no")
        print("device log leak check:", "LEAK" if secret in device_log else "clean")
        print("address in device log:", "yes" if address.lower() in device_log.lower() else "no")
    for line in text.splitlines():
        if "BUILD " in line or "INSTALL_" in line or line.startswith("e: ") or "FAILED" in line:
            print(line.replace(secret, "****"))
    return code


if __name__ == "__main__":
    sys.exit(main())
