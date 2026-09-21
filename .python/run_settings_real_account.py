"""Saves a real account through the plugin's settings page on a device (mail roadmap P4.7).

Usage: python .python/run_settings_real_account.py <PROFILE> <serial> [--alias <alias>] [--remove] [--no-build]

PROFILE is QQ_A / QQ_B / NETEASE_A / NETEASE_B / NETEASE126_A / SINA_A (password accounts). The
address and the authorization code come from the git-ignored mail-test-accounts.properties and
reach the device only as instrumentation arguments of
`RealAccountSettingsDeviceTest#savesAnAccountThroughTheEditorForTheHost`, which drives the real
account editor (provider preset, "Test connection", "Save") in the installed plugin's process.
The test runs through `am instrument` directly, so unlike a Gradle connected run the plugin and
the account it saved stay on the device for `.python/run_host_script_smoke.py --alias`.
`--remove` runs `#removesTheSavedAccount` instead (only the alias reaches the device).

Nothing secret is printed: the instrumentation output and a logcat excerpt are written to
build/p4/ with the secret masked and scanned, reported as "leak check: clean" or "LEAK".
"""
import glob
import io
import os
import re
import subprocess
import sys

PLUGIN = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
PACKAGE = "io.github.supermonster003.autojs6.plugin.angus.mail"
TEST_PACKAGE = PACKAGE + ".test"
TEST_CLASS = PACKAGE + ".RealAccountSettingsDeviceTest"
RUNNER = "androidx.test.runner.AndroidJUnitRunner"
PLUGIN_APK = (glob.glob(os.path.join(PLUGIN, "app", "build", "outputs", "apk", "debug", "autojs6-plugin-angus-mail-v*.apk")) or [os.path.join(PLUGIN, "app", "build", "outputs", "apk", "debug", "autojs6-plugin-angus-mail.apk")])[0]
TEST_APK = os.path.join(PLUGIN, "app", "build", "outputs", "apk", "androidTest", "debug", "app-debug-androidTest.apk")
PROVIDERS = {"qq.com": "qq", "foxmail.com": "qq", "163.com": "163", "126.com": "126", "yeah.net": "163", "sina.com": "sina", "sina.cn": "sina"}
SAFE = re.compile(r"^[A-Za-z0-9._\-/+=]+$")


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


def adb(serial, *args, **kwargs):
    return subprocess.run(["adb", "-s", serial, *args], capture_output=True, text=True, encoding="utf-8", errors="replace", **kwargs)


def main():
    argv = sys.argv[1:]
    if len(argv) < 2:
        print(__doc__)
        return 2
    profile, serial = argv[0], argv[1]
    alias = argv[argv.index("--alias") + 1] if "--alias" in argv else "qq-smoke"
    remove = "--remove" in argv
    build = "--no-build" not in argv

    props = read_properties(os.path.join(PLUGIN, "mail-test-accounts.properties"))
    kind, letter = profile.rsplit("_", 1)
    address = props[f"{kind}_USER_NAME_{letter}"]
    secret = props[f"{kind}_AUTH_CODE_{letter}"]
    assert SAFE.match(secret), "secret contains characters unsafe for the command line; aborting"
    domain = address.rsplit("@", 1)[-1].lower()
    provider = PROVIDERS[domain]

    if build:
        gradlew = os.path.join(PLUGIN, "gradlew.bat" if os.name == "nt" else "gradlew")
        code = subprocess.call([gradlew, ":app:assembleDebug", ":app:assembleDebugAndroidTest", "-q", "--console=plain"], cwd=PLUGIN)
        if code != 0:
            print("build failed", code)
            return code
    for apk in (PLUGIN_APK, TEST_APK):
        result = adb(serial, "install", "-r", "-t", apk)
        if "Success" not in result.stdout + result.stderr:
            print("install failed:", os.path.basename(apk), (result.stdout + result.stderr).strip()[-300:])
            return 1

    method = "removesTheSavedAccount" if remove else "savesAnAccountThroughTheEditorForTheHost"
    arguments = ["-e", "class", f"{TEST_CLASS}#{method}", "-e", "mailAlias", alias]
    if not remove:
        arguments += ["-e", "mailAddress", address, "-e", "mailSecret", secret, "-e", "mailProvider", provider]
    adb(serial, "logcat", "-c")
    print(f"profile={profile} provider={provider} domain={domain} serial={serial} alias={alias} test={method}")
    run = adb(serial, "shell", "am", "instrument", "-w", "-r", *arguments, f"{TEST_PACKAGE}/{RUNNER}")
    output = run.stdout + run.stderr
    logcat = adb(serial, "logcat", "-d", "-v", "brief").stdout
    excerpt = "\n".join(line for line in logcat.splitlines() if re.search(r"RealAccountSettings|TestRunner|AndroidRuntime|AngusMail|MailPlugin|angus\.mail", line))
    adb(serial, "uninstall", TEST_PACKAGE)

    log_dir = os.path.join(PLUGIN, "build", "p4")
    os.makedirs(log_dir, exist_ok=True)
    log = os.path.join(log_dir, f"settings-real-{profile.lower()}-{serial}{'-remove' if remove else ''}.log")
    with io.open(log, "w", encoding="utf-8") as out:
        out.write(output.replace(secret, "****") + "\n=== logcat excerpt ===\n" + excerpt.replace(secret, "****") + "\n")
    passed = "OK (1 test)" in output and "FAILURES" not in output and "INSTRUMENTATION_FAILED" not in output
    print("instrumentation:", "passed" if passed else "FAILED", "->", os.path.relpath(log, PLUGIN))
    print("leak check (instrumentation output):", "LEAK" if secret in output else "clean")
    print("leak check (full logcat):", "LEAK" if secret in logcat else "clean")
    print("address in full logcat:", "yes" if address.lower() in logcat.lower() else "no")
    if not passed:
        for line in output.splitlines():
            if line.startswith("INSTRUMENTATION_STATUS: stack=") or "Error" in line or "Exception" in line:
                print("  " + line.replace(secret, "****")[:300])
    return 0 if passed else 1


if __name__ == "__main__":
    sys.exit(main())
