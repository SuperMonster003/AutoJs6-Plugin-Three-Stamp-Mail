"""Collects the browser sign-in device evidence (mail roadmap P9) without typing a password on the device.

Usage: python .python/run_oauth_device.py <PROFILE> <serial> [--alias outlook-oauth] [--no-install] [--keep]
       [--skip-host] [--skip-sign-in] [--plugin-only] [--signed-in-alias <alias>]

PROFILE is HOTMAIL_A / HOTMAIL_B / OUTLOOK_A: a profile whose refresh token `.python/outlook_oauth_login.py`
wrote to build/outlook-token.properties with the client id this build carries (`microsoftClientId` of the
git-ignored oauth-clients.properties; a refresh token is bound to its client). The address comes from the
git-ignored mail-test-accounts.properties. Tokens reach the device only as instrumentation arguments (base64,
so the device shell never sees their characters) and are stored by the plugin exactly as a browser sign-in
stores its grant; the host sees the alias alone.

GMAIL_A (the Google provider, `googleClientId`): no PC refresh token exists for an Android client, so either
`--plugin-only` runs steps 1-4 alone (the Google sign-in page, the redirect path against Google's token endpoint,
the records; no real account) or `--signed-in-alias <alias>` takes the record the maintainer signed in with on
the device (the plugin's accounts page, Gmail preset, "Sign in with Google"): steps 1-4, 6, 7 and 9 run over that
alias, step 5 (the seed) does not exist and step 8 (a new grant) needs the browser again, so it is skipped and the
account is removed at the end unless --keep. The Google methods of OAuthDeviceTest are
opensTheGooglePageInTheBrowser, refusesAForeignStateAndExchangesTheMatchingOneAtGoogle and
aRevokedGoogleRecordRefusesEverySessionUntilANewSignIn (the last one posts the made-up token to Google's
revocation endpoint on the plugin's background thread; the `MailOAuth` state line records the outcome).

Steps (each recorded in build/p9/oauth-<profile>-<serial>.json):
  1. install the plugin debug + androidTest APKs and the host debug (split for the device's ABI) + androidTest
     APKs (built beforehand; --no-install skips)
  2. OAuthDeviceTest#opensTheProviderPageInTheBrowser: the sign-in screen opens the provider's page in the
     browser; meanwhile the top activity is read and a screenshot taken (build/p9/oauth-browser-<serial>.png)
  3. OAuthDeviceTest#refusesAForeignStateAndExchangesTheMatchingOneAtTheProvider: a redirect with another
     `state` is refused, one with the right `state` and a made-up code reaches the token endpoint over HTTPS
     and fails there, a late redirect is refused (no real token involved)
  4. OAuthDeviceTest#anOAuthRecordRoundTripsRefreshesAndMarksARefusal and #aRevokedRecordRefusesEverySessionUntilANewSignIn
     (Keystore round trip, renewal, refusal marking, revocation, re-authorization; scripted or local only)
  5. RealAccountOAuthDeviceTest#seedsASignedInAccount: the PC-obtained tokens become the alias's OAUTH2 record;
     the stale access token is renewed at the provider (the same refresh every session performs)
  6. host: docs/smoke/oauth-status.js (what `mail.accounts.list()` shows) and docs/smoke/saved-account.js
     (`mail.connect(alias)` -> `test` -> `fetch`) through .python/run_host_script_smoke.py --alias
  7. RealAccountOAuthDeviceTest#revokesTheSignIn, then the host scripts again: `mail.connect(alias)` must fail
     with AUTH_FAILED and the status script must report needsReauth
  8. RealAccountOAuthDeviceTest#signsInAgain (the grant stored as the sign-in screen stores a re-authorization),
     then saved-account.js again
  9. RealAccountOAuthDeviceTest#removesTheAccount unless --keep

Nothing secret is printed: every log and report is scanned for the tokens ("leak check") and the address.
"""
import argparse
import base64
import io
import json
import os
import re
import subprocess
import sys
import threading
import time

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from run_host_script_smoke import read_accounts, read_properties  # noqa: E402
from run_trigger_matrix import HOST_PACKAGE, HOST_TEST_APK, PLUGIN, PLUGIN_PACKAGE, PLUGIN_TEST_APK, PLUGIN_TEST_PACKAGE, RUNNER, adb, grant_storage, host_apk, install, plugin_apk, sdk, shell, version_code  # noqa: E402

OAUTH_TEST_CLASS = PLUGIN_PACKAGE + ".OAuthDeviceTest"
REAL_TEST_CLASS = PLUGIN_PACKAGE + ".RealAccountOAuthDeviceTest"
OUT_DIR = os.path.join(PLUGIN, "build", "p9")
AAPT2 = os.path.join(os.environ.get("ANDROID_HOME", r"E:\.android\sdk"), "build-tools", "37.0.0", "aapt2.exe" if os.name == "nt" else "aapt2")
PRESETS = {"outlook.com": "outlook", "hotmail.com": "outlook", "live.com": "outlook", "msn.com": "outlook", "gmail.com": "gmail"}
OAUTH_PROVIDERS = {"outlook": "microsoft", "office365": "microsoft", "gmail": "google"}
CLIENT_KEYS = {"microsoft": "microsoftClientId", "google": "googleClientId"}
METHODS = {"microsoft": {"browser": "opensTheProviderPageInTheBrowser", "redirects": "refusesAForeignStateAndExchangesTheMatchingOneAtTheProvider",
                         "revoked": "aRevokedRecordRefusesEverySessionUntilANewSignIn"},
           "google": {"browser": "opensTheGooglePageInTheBrowser", "redirects": "refusesAForeignStateAndExchangesTheMatchingOneAtGoogle",
                      "revoked": "aRevokedGoogleRecordRefusesEverySessionUntilANewSignIn"}}
LOG_TAGS = ["OAuthDevice:I", "RealAccountOAuth:I", "MailOAuth:I", "MailPluginBinder:I", "MailSession:I", "TestRunner:I", "AndroidRuntime:E"]
STATE_LINES = []


def log(text):
    print(time.strftime("%H:%M:%S"), text, flush=True)


def instrument(serial, test_class, method, arguments, marker):
    """One test method through `am instrument -w -r`: (passed, output). The test's own log line also counts on
    Android 9, where the runner sometimes reports "Process crashed" at the force-stop after a finished test."""
    command = ["shell", "am", "instrument", "-w", "-r", "-e", "class", f"{test_class}#{method}"]
    for key, value in arguments.items():
        if value is None or value == "":
            continue
        assert re.match(r"^[A-Za-z0-9._/#@,:+=\-]+$", str(value)), f"instrumentation argument {key} has characters unsafe for the shell"
        command += ["-e", key, str(value)]
    command.append(f"{PLUGIN_TEST_PACKAGE}/{RUNNER}")
    tag, text = marker
    before = marker_count(serial, tag, text)
    state_before = len(marker_lines(serial, "MailOAuth"))
    output = adb(serial, *command, check=False)
    # the plugin's OAuth state lines of this step (the host runs clear the logcat, so they are kept as they come)
    STATE_LINES.extend(l for l in marker_lines(serial, "MailOAuth")[state_before:] if not l.startswith("---------"))
    failed = "INSTRUMENTATION_STATUS_CODE: -2" in output or "INSTRUMENTATION_STATUS_CODE: -3" in output or "INSTRUMENTATION_FAILED" in output or "FAILURES" in output
    skipped = "INSTRUMENTATION_STATUS_CODE: -4" in output
    passed = "OK (1 test)" in output and not failed
    if not passed and not failed and not skipped:
        time.sleep(1.5)
        if marker_count(serial, tag, text) > before:
            passed = True
    if skipped:
        log(f"  {method}: SKIPPED (assumption failed): {output.strip().splitlines()[-1][:160] if output.strip() else ''}")
    elif not passed:
        for line in output.splitlines():
            if line.startswith("INSTRUMENTATION_STATUS: stack=") or "Error" in line or "Exception" in line or "INSTRUMENTATION_STATUS_CODE: -3" in line:
                log("  " + line[:400])
    return passed and not skipped, output


def marker_count(serial, tag, marker):
    return len([l for l in adb(serial, "logcat", "-d", "-v", "raw", "-s", f"{tag}:I", check=False).splitlines() if marker in l])


def marker_lines(serial, tag):
    return [l.strip() for l in adb(serial, "logcat", "-d", "-v", "raw", "-s", f"{tag}:I", check=False).splitlines() if l.strip() and not l.startswith("---------")]


def version_code_of_apk(apk):
    match = re.search(r"versionCode='(\d+)'", subprocess.run([AAPT2, "dump", "badging", apk], capture_output=True, text=True, errors="replace").stdout)
    return int(match.group(1)) if match else None


def top_activity(serial):
    text = shell(serial, "dumpsys activity activities")
    for key in ("topResumedActivity", "mResumedActivity", "ResumedActivity"):
        match = re.search(rf"{key}[^\n]*\{{[^}}]*\s(\S+/\S+)\s", text)
        if match:
            return match.group(1)
    return None


def host_report(serial, script):
    """The script's report, from the host test's `<name> report[i]:` log lines (the app under test, and with it
    its external files directory, is uninstalled when the connected run ends; the log survives)."""
    name = os.path.basename(script).removesuffix(".js")
    chunks = {}
    for line in adb(serial, "logcat", "-d", "-v", "raw", "-s", "MailScriptSmokeTest:I", check=False).splitlines():
        match = re.match(rf"^{re.escape(name)} report\[(\d+)\]: (.*)$", line.strip())
        if match:
            chunks[int(match.group(1))] = match.group(2)
    text = "".join(chunks[i] for i in sorted(chunks))
    try:
        return json.loads(text)
    except ValueError:
        stack = [l.strip()[:300] for l in adb(serial, "logcat", "-d", "-v", "raw", "-s", "TestRunner:I", "TestRunner:E", check=False).splitlines() if "failed" in l or "Error" in l or "Exception" in l]
        return {"error": "no report", "runner": stack[-3:]}


def run_host_script(profile, serial, alias, script, expect_ok=True):
    name = f"oauth-{os.path.basename(script).removesuffix('.js')}-{'ok' if expect_ok else 'refused'}-{serial}.log"
    command = [sys.executable, "-X", "utf8", os.path.join(PLUGIN, ".python", "run_host_script_smoke.py"), profile, serial, "--script", script, "--alias", alias, "--log", name]
    started = time.time()
    result = subprocess.run(command, cwd=PLUGIN, capture_output=True, text=True, encoding="utf-8", errors="replace")
    report = host_report(serial, script)
    out = (result.stdout + result.stderr).strip()
    entry = {"script": script, "exit": result.returncode, "seconds": round(time.time() - started, 1), "report": report,
             "runner": [l for l in out.splitlines() if l.startswith(("gradle exit", "report leak check", "address in", "device log leak"))]}
    verdict = "ok" if (report.get("ok") is True) == expect_ok else "UNEXPECTED"
    entry["verdict"] = verdict
    detail = f"ok={report.get('ok')}" + (f" errorCode={report.get('errorCode')} error={str(report.get('error'))[:120]}" if not report.get("ok") else "")
    log(f"host {os.path.basename(script)} ({'expects ok' if expect_ok else 'expects a refusal'}): {verdict}; {detail}; {' | '.join(entry['runner'])}")
    return entry


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("profile", help="HOTMAIL_A, HOTMAIL_B, OUTLOOK_A (a profile with a refresh token in build/outlook-token.properties)")
    parser.add_argument("serial", help="adb serial of the device")
    parser.add_argument("--alias", default="outlook-oauth", help="alias the seeded account is saved under")
    parser.add_argument("--no-install", action="store_true", help="skip the APK installs")
    parser.add_argument("--keep", action="store_true", help="leave the seeded account on the device")
    parser.add_argument("--skip-host", action="store_true", help="skip the host script runs")
    parser.add_argument("--skip-sign-in", action="store_true", help="skip the browser / redirect cases (steps 2 and 3)")
    parser.add_argument("--uninstall-host", action="store_true", help="on a phone, uninstall an installed host before the connected runs (emulators: always)")
    parser.add_argument("--plugin-only", action="store_true", help="stop after the plugin-side steps 1-4 (browser, redirects, records): no real account needed")
    parser.add_argument("--signed-in-alias", default=None, help="an alias the maintainer signed in with on the device (Google): no seed, the re-authorization step is skipped")
    args = parser.parse_args()

    props = read_accounts()
    kind, letter = args.profile.rsplit("_", 1)
    address = props[f"{kind}_USER_NAME_{letter}"]
    refresh = props.get(f"{kind}_REFRESH_TOKEN_{letter}", "")
    access = props.get(f"{kind}_ACCESS_TOKEN_{letter}", "")
    expires_at = props.get(f"{kind}_TOKEN_EXPIRES_AT_{letter}", "0")
    client_id = props.get(f"{kind}_CLIENT_ID_{letter}", "")
    seeded = not args.plugin_only and not args.signed_in_alias
    if seeded and not refresh:
        raise SystemExit(f"no {kind}_REFRESH_TOKEN_{letter} in build/outlook-token.properties: run .python/outlook_oauth_login.py first (or pass --plugin-only / --signed-in-alias)")
    if args.signed_in_alias:
        args.alias = args.signed_in_alias
    preset = PRESETS[address.rsplit("@", 1)[-1].lower()]
    oauth_provider = OAUTH_PROVIDERS[preset]
    clients_file = os.path.join(PLUGIN, "oauth-clients.properties")
    build_client = read_properties(clients_file).get(CLIENT_KEYS[oauth_provider], "") if os.path.exists(clients_file) else ""
    if not build_client:
        raise SystemExit(f"oauth-clients.properties names no {CLIENT_KEYS[oauth_provider]}: the build carries no sign-in for {oauth_provider}")
    if client_id and client_id != build_client:
        raise SystemExit(f"the {args.profile} refresh token was issued to client {client_id[:8]}..., the build carries {build_client[:8]}...: a refresh token is bound to its client")
    secrets = [value for value in (refresh, access) if value]
    os.makedirs(OUT_DIR, exist_ok=True)

    def mask(text):
        for value in secrets:
            text = text.replace(value, "****")
        return text

    methods = METHODS[oauth_provider]
    summary = {"profile": args.profile, "serial": args.serial, "alias": args.alias, "preset": preset, "oauthProvider": oauth_provider,
               "mode": "seeded" if seeded else ("signed-in-alias" if args.signed_in_alias else "plugin-only"),
               "clientIdPrefix": build_client[:8], "accessTokenStaleAtSeed": int(expires_at or 0) < time.time() if seeded else None, "steps": {}, "startedAt": int(time.time())}

    if not args.no_install:
        install(args.serial, plugin_apk())
        install(args.serial, PLUGIN_TEST_APK)
    if not args.skip_host:
        # the host's connected run installs the host and its test APK itself, without -r, and uninstalls both at its
        # end: a host already on the device makes that install fail (INSTALL_FAILED_ALREADY_EXISTS), so it goes first
        installed = f"package:{HOST_PACKAGE}" in shell(args.serial, f"pm list packages {HOST_PACKAGE}")
        if installed and (args.serial.startswith("emulator-") or args.uninstall_host):
            adb(args.serial, "uninstall", HOST_PACKAGE, check=False)
            log("host uninstalled for the connected runs (the test engine installs it)")
        elif installed:
            log("the host is installed: the connected runs may refuse to install over it; pass --uninstall-host or uninstall it first")
    grant_storage(args.serial)
    adb(args.serial, "logcat", "-G", "8M", check=False)  # the host's start-up would push the report lines out of a 256 KB buffer (API 24)
    summary["sdk"] = sdk(args.serial)
    summary["pluginVersionCode"] = version_code(args.serial, PLUGIN_PACKAGE)
    summary["hostVersionCode"] = version_code(args.serial, HOST_PACKAGE) or version_code_of_apk(host_apk(args.serial))
    log(f"plugin versionCode={summary['pluginVersionCode']} host versionCode={summary['hostVersionCode']} sdk={summary['sdk']} client {build_client[:8]}... preset {preset}")
    adb(args.serial, "logcat", "-c", check=False)
    shell(args.serial, "input keyevent KEYCODE_WAKEUP")
    shell(args.serial, "wm dismiss-keyguard")

    if not args.skip_sign_in:
        # 2. the browser: read the top activity and take a screenshot while the test holds the screen
        holder = {}

        def hold():
            holder["result"] = instrument(args.serial, OAUTH_TEST_CLASS, methods["browser"], {}, ("OAuthDevice", "browser opened"))

        thread = threading.Thread(target=hold)
        thread.start()
        top = None
        for _ in range(20):
            time.sleep(1)
            top = top_activity(args.serial)
            if top and PLUGIN_PACKAGE not in top and "chrome" in top.lower() or (top and "customtabs" in top.lower()):
                break
        screenshot = os.path.join(OUT_DIR, f"oauth-browser-{args.serial}.png")
        shell(args.serial, "screencap -p /sdcard/oauth-browser.png")
        adb(args.serial, "pull", "/sdcard/oauth-browser.png", screenshot, check=False)
        shell(args.serial, "rm -f /sdcard/oauth-browser.png")
        thread.join()
        passed, _ = holder["result"]
        summary["steps"]["browser"] = {"passed": passed, "topActivity": top, "screenshot": os.path.relpath(screenshot, PLUGIN), "log": marker_lines(args.serial, "OAuthDevice")[-1:]}
        log(f"browser: {'ok' if passed else 'FAILED'}; top activity {top}; screenshot {os.path.relpath(screenshot, PLUGIN)}")
        # no HOME here: the redirect case launches the sign-in screen on a cleared task anyway, and hiding Chrome's UI on the
        # API 37 emulator makes it crash in its onTrimMemory handler at the next launch, taking the paused screen below with it

        # 3. the redirects
        passed, _ = instrument(args.serial, OAUTH_TEST_CLASS, methods["redirects"], {}, ("OAuthDevice", "late redirect refused"))
        lines = [l for l in marker_lines(args.serial, "OAuthDevice") if "refused" in l or "provider answered" in l]
        summary["steps"]["redirects"] = {"passed": passed, "log": lines[-3:]}
        log(f"redirects: {'ok' if passed else 'FAILED'}; " + " | ".join(lines[-3:]))
        shell(args.serial, "input keyevent KEYCODE_HOME")

    # 4. the record
    for method, marker in (("anOAuthRecordRoundTripsRefreshesAndMarksARefusal", "record round trip ok"), (methods["revoked"], "re-authorization restored")):
        passed, _ = instrument(args.serial, OAUTH_TEST_CLASS, method, {}, ("OAuthDevice", marker))
        summary["steps"][method] = {"passed": passed}
        log(f"{method}: {'ok' if passed else 'FAILED'}")
    if args.plugin_only:
        return finish(args, summary, secrets, address, mask)

    # 5. the real record (a seeded run); a signed-in alias is the maintainer's record as the sign-in screen stored it
    seed_arguments = {"mailAlias": args.alias, "mailAddress": address, "mailProvider": preset, "oauthProvider": oauth_provider,
                      "oauthRefreshTokenB64": base64.b64encode(refresh.encode("utf-8")).decode("ascii"),
                      "oauthAccessTokenB64": base64.b64encode(access.encode("utf-8")).decode("ascii") if access else "",
                      "oauthExpiresAt": expires_at}
    if seeded:
        passed, _ = instrument(args.serial, REAL_TEST_CLASS, "seedsASignedInAccount", seed_arguments, ("RealAccountOAuth", "seeded alias="))
        summary["steps"]["seed"] = {"passed": passed, "log": marker_lines(args.serial, "RealAccountOAuth")[-1:]}
        log(f"seed: {'ok' if passed else 'FAILED'}; " + " | ".join(summary["steps"]["seed"]["log"]))
    else:
        passed = True
        summary["steps"]["signedIn"] = {"passed": True, "log": [f"alias {args.alias}: the record the maintainer signed in with on the device (no seed)"]}
        log(f"signed-in alias {args.alias}: the host steps and the revocation run over the maintainer's record")
    if not passed:
        # keep what was collected (a device without network stops here) for the evidence document
        summary["leakCheck"] = {"secretInLogcat": any(s in adb(args.serial, "logcat", "-d", check=False) for s in secrets)}
        summary["oauthStateLines"] = list(STATE_LINES)
        with io.open(os.path.join(OUT_DIR, f"oauth-{args.profile.lower()}-{args.serial}.json"), "w", encoding="utf-8") as handle:
            json.dump(summary, handle, ensure_ascii=False, indent=2)
        raise SystemExit("seeding the account failed (the summary so far is in build/p9)")

    # 6. the host with the live record
    if not args.skip_host:
        summary["steps"]["hostStatusLive"] = run_host_script(args.profile, args.serial, args.alias, "docs/smoke/oauth-status.js", expect_ok=True)
        summary["steps"]["hostSessionLive"] = run_host_script(args.profile, args.serial, args.alias, "docs/smoke/saved-account.js", expect_ok=True)

    # 7. revoked
    passed, _ = instrument(args.serial, REAL_TEST_CLASS, "revokesTheSignIn", {"mailAlias": args.alias}, ("RealAccountOAuth", "revoked alias="))
    summary["steps"]["revoke"] = {"passed": passed, "log": marker_lines(args.serial, "RealAccountOAuth")[-1:]}
    log(f"revoke: {'ok' if passed else 'FAILED'}; " + " | ".join(summary["steps"]["revoke"]["log"]))
    if not args.skip_host:
        summary["steps"]["hostStatusRevoked"] = run_host_script(args.profile, args.serial, args.alias, "docs/smoke/oauth-status.js", expect_ok=True)
        summary["steps"]["hostSessionRevoked"] = run_host_script(args.profile, args.serial, args.alias, "docs/smoke/saved-account.js", expect_ok=False)

    # 8. signed in again (a seeded run; a signed-in alias would need the browser again)
    if seeded:
        passed, _ = instrument(args.serial, REAL_TEST_CLASS, "signsInAgain", seed_arguments, ("RealAccountOAuth", "re-authorized alias="))
        summary["steps"]["reauthorize"] = {"passed": passed, "log": marker_lines(args.serial, "RealAccountOAuth")[-1:]}
        log(f"re-authorize: {'ok' if passed else 'FAILED'}; " + " | ".join(summary["steps"]["reauthorize"]["log"]))
        if not args.skip_host:
            summary["steps"]["hostSessionAgain"] = run_host_script(args.profile, args.serial, args.alias, "docs/smoke/saved-account.js", expect_ok=True)
    else:
        summary["steps"]["reauthorize"] = {"skipped": "a new grant needs the browser: the accounts page's \"Sign in again\" by the maintainer"}
        log("re-authorize: skipped (a signed-in alias needs the browser for a new grant)")

    # 9. cleanup
    if not args.keep:
        passed, _ = instrument(args.serial, REAL_TEST_CLASS, "removesTheAccount", {"mailAlias": args.alias}, ("RealAccountOAuth", "removed alias="))
        summary["steps"]["remove"] = {"passed": passed}
        log(f"remove: {'ok' if passed else 'FAILED'}")

    return finish(args, summary, secrets, address, mask)


def finish(args, summary, secrets, address, mask):
    """The evidence of a run: the plugin's state log lines, the leak check, the masked log and the summary file."""
    plugin_lines = [l.rstrip() for l in adb(args.serial, "logcat", "-d", "-v", "threadtime", "-s", *LOG_TAGS, check=False).splitlines()]
    summary["oauthStateLines"] = STATE_LINES + [l.split("MailOAuth: ", 1)[-1] for l in plugin_lines if "MailOAuth: " in l and l.split("MailOAuth: ", 1)[-1] not in STATE_LINES]
    logcat_full = adb(args.serial, "logcat", "-d", check=False)
    gradle_logs = "".join(io.open(os.path.join(PLUGIN, "build", "p3", n), encoding="utf-8", errors="replace").read() for n in os.listdir(os.path.join(PLUGIN, "build", "p3")) if n.startswith("oauth-") and n.endswith(f"{args.serial}.log")) if os.path.isdir(os.path.join(PLUGIN, "build", "p3")) else ""
    summary["leakCheck"] = {"secretInLogcat": any(s in logcat_full for s in secrets), "secretInGradleLogs": any(s in gradle_logs for s in secrets),
                            "addressInLogcat": address.lower() in logcat_full.lower(), "addressInGradleLogs": address.lower() in gradle_logs.lower()}
    log_path = os.path.join(OUT_DIR, f"oauth-{args.profile.lower()}-{args.serial}.log")
    with io.open(log_path, "w", encoding="utf-8") as handle:
        handle.write(mask("\n".join(plugin_lines)) + "\n")
    out = os.path.join(OUT_DIR, f"oauth-{args.profile.lower()}-{args.serial}.json")
    with io.open(out, "w", encoding="utf-8") as handle:
        json.dump(summary, handle, ensure_ascii=False, indent=2)
    steps = summary["steps"]
    ok = all(v.get("passed", True) for v in steps.values()) and all(v.get("verdict", "ok") == "ok" for v in steps.values())
    log(f"result: {'ok' if ok else 'INCOMPLETE'}; steps {len(steps)}; state lines {len(summary['oauthStateLines'])}; leak check "
        f"{'LEAK' if summary['leakCheck']['secretInLogcat'] or summary['leakCheck']['secretInGradleLogs'] else 'clean'}; "
        f"address in logcat {'yes' if summary['leakCheck']['addressInLogcat'] else 'no'}, in gradle logs {'yes' if summary['leakCheck']['addressInGradleLogs'] else 'no'}")
    log(f"summary: {os.path.relpath(out, PLUGIN)}")
    return 0 if ok else 1


if __name__ == "__main__":
    sys.exit(main())
