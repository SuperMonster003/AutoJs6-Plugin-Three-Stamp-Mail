"""Runs the host script-API smoke test against a real account (mail roadmap P3).

Usage: python .python/run_host_script_smoke.py <PROFILE> <serial> [--log <name>]

PROFILE is QQ_A / QQ_B / NETEASE_A / NETEASE_B (password accounts). Credentials come from the
git-ignored mail-test-accounts.properties next to this repository's root and reach the device
only as instrumentation arguments of the host test
`org.autojs.autojs.runtime.api.augment.mail.MailScriptSmokeDeviceTest#realProviderConnectAndTest`
(host repository `../AutoJs6`). Nothing secret is printed; the Gradle log is scanned for the
secret afterwards and reported as "report leak check: clean" or "LEAK".

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
TEST = "org.autojs.autojs.runtime.api.augment.mail.MailScriptSmokeDeviceTest#realProviderConnectAndTest"
PROVIDERS = {"qq.com": "qq", "foxmail.com": "qq", "163.com": "163", "126.com": "126", "yeah.net": "163"}


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
    parser.add_argument("profile", help="QQ_A, QQ_B, NETEASE_A or NETEASE_B")
    parser.add_argument("serial", help="adb serial of the device")
    parser.add_argument("--log", default=None, help="log file name under build/p3/")
    args = parser.parse_args()

    props = read_properties(os.path.join(PLUGIN, "mail-test-accounts.properties"))
    kind, letter = args.profile.rsplit("_", 1)
    address = props[f"{kind}_USER_NAME_{letter}"]
    secret = props[f"{kind}_AUTH_CODE_{letter}"]
    domain = address.rsplit("@", 1)[-1].lower()
    provider = PROVIDERS[domain]

    log_dir = os.path.join(PLUGIN, "build", "p3")
    os.makedirs(log_dir, exist_ok=True)
    log = os.path.join(log_dir, args.log or f"host-smoke-{args.profile.lower()}-{args.serial}.log")
    gradlew = os.path.join(HOST, "gradlew.bat" if os.name == "nt" else "gradlew")
    command = [
        gradlew, ":app:connectedAppDebugAndroidTest",
        f"-Pandroid.testInstrumentationRunnerArguments.class={TEST}",
        f"-Pandroid.testInstrumentationRunnerArguments.mail.smoke.provider={provider}",
        f"-Pandroid.testInstrumentationRunnerArguments.mail.smoke.address={address}",
        f"-Pandroid.testInstrumentationRunnerArguments.mail.smoke.password={secret}",
        "--console=plain",
    ]
    print(f"profile={args.profile} provider={provider} domain={domain} serial={args.serial}")
    env = dict(os.environ, ANDROID_SERIAL=args.serial)
    with io.open(log, "w", encoding="utf-8") as out:
        code = subprocess.call(command, cwd=HOST, env=env, stdout=out, stderr=subprocess.STDOUT)
    text = io.open(log, encoding="utf-8", errors="replace").read()
    print("gradle exit", code)
    print("report leak check:", "LEAK" if secret in text else "clean")
    for line in text.splitlines():
        if "BUILD " in line or "INSTALL_" in line or line.startswith("e: "):
            print(line.replace(secret, "****"))
    return code


if __name__ == "__main__":
    sys.exit(main())
