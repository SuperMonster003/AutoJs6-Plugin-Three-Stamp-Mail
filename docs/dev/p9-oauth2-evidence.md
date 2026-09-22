# P9 OAuth 2.0 evidence (browser sign-in, token storage and renewal, revocation, device runs)

Evidence for roadmap P9 collected on 2026-09-22 in this repository (Gradle 9.5.0, AGP 9.3.2,
Kotlin 2.3.20, JDK 21, Windows 11), plugin build 61 (`3e35ad6`, the P9 feature) plus the device
tests and the driver of build 62, rerun on build 63 (the binder resolving a saved alias through
the store it was given, found by the 1.2.0 gate's full connected suite) for the Microsoft rows of
the emulators, and build 67 (the Google cases of `OAuthDeviceTest`, the driver's `--plugin-only`
and `--signed-in-alias` modes, this document) for the Google rows and the Sony rows, against the
host debug build 5282 (`04c55ac78a`, mail contract version 2; the host is unchanged by P9). The
Microsoft client id of the build is the maintainer's Entra registration with the mobile redirect
URI `io.github.supermonster003.autojs6.plugin.angus.mail://oauth2/microsoft`; the Google client id
is the maintainer's Android client registered on 2026-09-22 (package name + release SHA-1, custom
URI scheme enabled; the Gmail preset's sign-in item is enabled in builds from 67 on). Both come
from the git-ignored `oauth-clients.properties` (see `docs/dev/oauth-client-registration.md`).

Nothing here types a password or a token into a device screen. The real Microsoft record comes
from the tokens `.python/outlook_oauth_login.py` obtained on the PC for the same client
registration (`build/outlook-token.properties`, the `HOTMAIL_A` profile of the git-ignored
`mail-test-accounts.properties`): a refresh token is bound to its client id, so the device's
renewal at `login.microsoftonline.com` with the build's client id is the same renewal every
alias session performs after a browser sign-in on the device. The tokens reach the device only as
base64 instrumentation arguments of an `am instrument` run, the host sees the alias alone, and
every log and report is scanned for the tokens and the address afterwards. No Google refresh
token exists on the PC (an Android client accepts no loopback redirect), so the Google rows are
the plugin-side steps (`--plugin-only`) and the real Google record waits for the maintainer's
sign-in on a device (see "Not done").

## Method

`.python/run_oauth_device.py <profile> <serial> [--skip-host] [--no-install] [--plugin-only] [--signed-in-alias <alias>]`
runs, in order:

1. `OAuthDeviceTest#opensTheProviderPageInTheBrowser` (the plugin's androidTest APK; for the Google
   provider `#opensTheGooglePageInTheBrowser`): the sign-in screen (`OAuthSignInActivity`)
   generates the PKCE verifier and the `state` and opens the authorization URL in a Custom Tab;
   the driver reads the top activity and takes a screenshot meanwhile
   (`build/p9/oauth-browser-<serial>.png`).
2. `OAuthDeviceTest#refusesAForeignStateAndExchangesTheMatchingOneAtTheProvider` (Google:
   `#...AtGoogle`): three implicit `VIEW` intents on the redirect URI, as the browser would send
   them, reach the exported `OAuthRedirectActivity` and are forwarded to the waiting screen: one
   with a foreign `state` (refused, the request stays open), one with the right `state` and a
   made-up code (the screen exchanges it at the token endpoint over HTTPS, the provider refuses
   it, the request is spent), one more with the right `state` (refused: nothing is waiting).
3. `OAuthDeviceTest#anOAuthRecordRoundTripsRefreshesAndMarksARefusal` (a throw-away store
   under `noBackupFilesDir` with its own Keystore key and a scripted token endpoint): an
   `OAUTH2` record yields its access token, the record files hold neither token, a stale token is
   renewed through the refresh token and the `oauth` expiry rewritten, a valid token is not
   renewed again, an `invalid_grant` answer marks the record `needsReauth` and fails with
   `AUTH_FAILED`; `OAuthDeviceTest#aRevokedRecordRefusesEverySessionUntilANewSignIn` (the
   installed plugin's real store, a throw-away alias; Google:
   `#aRevokedGoogleRecordRefusesEverySessionUntilANewSignIn`): `TokenRevoker.revoke` replaces the
   tokens by the revoked marker (`needsReauth` set) and, for a provider with a revocation
   endpoint, posts the refresh token there on its background thread (Google; the made-up token
   is refused, which the `MailOAuth` state line records), every session fails with
   `AUTH_FAILED`, and `storeGrant` (what the sign-in screen does at the end of a re-authorization)
   restores it. `--plugin-only` stops here.
4. `RealAccountOAuthDeviceTest#seedsASignedInAccount`: the PC tokens become the alias's `OAUTH2`
   record exactly as the sign-in screen stores a grant (`AccountFormPolicy.toAccountJson` with
   the `oauth` link); `AccountSecrets.usableTokens` renews the stale access token at the provider;
   the accounts page shows "Microsoft sign-in" without the marker. With `--signed-in-alias` the
   record is the one the maintainer signed in with on the device and nothing is seeded.
5. The host (`.python/run_host_script_smoke.py --alias`, `MailScriptSmokeDeviceTest#savedAccountScript`):
   `docs/smoke/oauth-status.js` reads the alias's `oauth` object from `mail.accounts.list()` and
   checks that no secret-like key exists; `docs/smoke/saved-account.js` runs
   `mail.connect(alias)` -> `test` -> `fetch(3)` -> `close`. The connected run ends with the host
   uninstalled (the AGP test engine), so the reports are read from the host test's log lines.
   `--skip-host` leaves these out (the Sony keeps its P8 host task and watch).
6. `RealAccountOAuthDeviceTest#revokesTheSignIn`, then the two host scripts again (the status
   script must show `needsReauth`, the session script must fail with `AUTH_FAILED`).
7. `RealAccountOAuthDeviceTest#signsInAgain` (a new grant on the same record), then the session
   script again. A signed-in alias skips this step: a new grant needs the browser again.
8. `RealAccountOAuthDeviceTest#removesTheAccount`.

## Results

### Microsoft (Outlook.com, the `HOTMAIL_A` registration)

#### emulator-5556: AVD_API_37.1_16K (x86_64), Android 16 QPR (API 37)

Plugin versionCode 63, host versionCode 5282, the build's Microsoft client id (the `HOTMAIL_A` registration), preset `outlook` (provider `microsoft`), the full run, alias `outlook-oauth`, the PC's access token already expired at the seed.

| Step | Result | Detail |
| --- | --- | --- |
| sign-in screen opens the provider page in the browser | ok | top activity `com.android.chrome/com.google.android.apps.chrome.IntentDispatcher`; browser opened for provider=microsoft, state length 22 |
| foreign `state` refused, matching `state` exchanged at the token endpoint, late redirect refused | ok | foreign state refused: The browser's answer was not accepted: the redirect carries no matching state; matching state exchanged, provider answered: The sign-in failed: the authorization code was refused; sign in again - invalid_grant: AADSTS7000012: The grant was obtained for a different tenant. Trace ID: 802c22ac-cceb-45ab-a958-3f317dd06600 Correlation ID: 59872d88-acdb-458c-b31c-39688c55c994 Timestamp: 2026-09-21 17:12:04Z; late redirect refused: The browser's answer was not accepted: no sign-in is waiting |
| OAUTH2 record round trip (Keystore), renewal, refusal marked `needsReauth` | ok | - |
| revoked record refuses sessions, re-authorization restores it | ok | - |
| real record seeded from the PC tokens; stale access token renewed at the provider | ok | seeded alias=outlook-oauth provider=microsoft staleAtSeed=true renewed=true expiresIn=3595 s in 5006 ms |
| host `mail.accounts.list()`: `oauth` object, live | ok | `ok` True, `auth` xoauth2, `oauth` = provider microsoft, needsReauth False, expires in 3580 s, keys authorizedAt, expiresAt, needsReauth, provider; 16.1 s |
| host `mail.connect(alias)` -> `test` -> `fetch`, live | ok | `ok` true, test {'ok': True, 'imap': True, 'smtp': True}, fetched 3, closed True; accounts.list 109 ms, accounts.has 9 ms, connect 98 ms, test 7097 ms, fetch 966 ms, close 6 ms; 23.2 s |
| "Revoke sign-in": sessions fail with `AUTH_FAILED`, accounts page says "sign in again" | ok | revoked alias=outlook-oauth provider=microsoft in 2606 ms; sessions now fail with AUTH_FAILED |
| host `mail.accounts.list()`: `needsReauth` after the revocation | ok | `ok` True, `auth` xoauth2, `oauth` = provider microsoft, needsReauth True, expiresAt 0 (no expiry left), keys authorizedAt, expiresAt, needsReauth, provider; 16.2 s |
| host `mail.connect(alias)` refused after the revocation | ok | `ok` false, `AUTH_FAILED`: MailError [AUTH_FAILED]: the account has no refresh token; sign in again; 15.9 s |
| "Sign in again": a new grant on the same record | ok | re-authorized alias=outlook-oauth provider=microsoft expiresIn=3596 s in 3739 ms |
| host `mail.connect(alias)` -> `test` -> `fetch` after the re-authorization | ok | `ok` true, test {'ok': True, 'imap': True, 'smtp': True}, fetched 3, closed True; accounts.list 665 ms, accounts.has 20 ms, connect 89 ms, test 8956 ms, fetch 713 ms, close 10 ms; 25.0 s |
| record removed | ok | - |

Leak check: secret in logcat no, in the Gradle logs no; address in logcat no, in the Gradle logs no.

The plugin's `MailOAuth` state lines of the run (ids, states, counts and durations only):

    provider=microsoft browser opened
    provider=microsoft browser opened
    provider=microsoft redirect rejected: the redirect carries no matching state
    provider=microsoft sign-in revoked locally
    provider=microsoft provider-side revocation not done
    provider=microsoft refresh failed: AUTH_FAILED (reauthorize)
    provider=microsoft sign-in stored, expires in 7199 s
    provider=microsoft refresh ok, expires in 3598 s
    provider=microsoft sign-in revoked locally
    provider=microsoft provider-side revocation not done
    provider=microsoft refresh failed: AUTH_FAILED (reauthorize)
    provider=microsoft sign-in stored, expires in -20971 s
    provider=microsoft refresh ok, expires in 3598 s

#### emulator-5554: AVD_API_24 (x86), Android 7.0 (API 24)

Plugin versionCode 63, host versionCode 5282, the build's Microsoft client id (the `HOTMAIL_A` registration), preset `outlook` (provider `microsoft`), the full run, alias `outlook-oauth`, the PC's access token already expired at the seed.

| Step | Result | Detail |
| --- | --- | --- |
| sign-in screen opens the provider page in the browser | ok | top activity `android/com.android.internal.app.ResolverActivity`; browser opened for provider=microsoft, state length 22 |
| foreign `state` refused, matching `state` exchanged at the token endpoint, late redirect refused | ok | foreign state refused: The browser's answer was not accepted: the redirect carries no matching state; matching state exchanged, provider answered: The sign-in failed: the authorization code was refused; sign in again - invalid_grant: AADSTS7000012: The grant was obtained for a different tenant. Trace ID: fd65c702-74e1-4977-a000-684dc0da6b00 Correlation ID: e312dc18-00e7-4f9f-8ffa-a511cdecc6d4 Timestamp: 2026-09-21 17:14:36Z; late redirect refused: The browser's answer was not accepted: no sign-in is waiting |
| OAUTH2 record round trip (Keystore), renewal, refusal marked `needsReauth` | ok | - |
| revoked record refuses sessions, re-authorization restores it | ok | - |
| real record seeded from the PC tokens; stale access token renewed at the provider | ok | seeded alias=outlook-oauth provider=microsoft staleAtSeed=true renewed=true expiresIn=3597 s in 3065 ms |
| host `mail.accounts.list()`: `oauth` object, live | ok | `ok` True, `auth` xoauth2, `oauth` = provider microsoft, needsReauth False, expires in 3575 s, keys authorizedAt, expiresAt, needsReauth, provider; 23.8 s |
| host `mail.connect(alias)` -> `test` -> `fetch`, live | ok | `ok` true, test {'ok': True, 'imap': True, 'smtp': True}, fetched 3, closed True; accounts.list 37 ms, accounts.has 9 ms, connect 37 ms, test 4037 ms, fetch 847 ms, close 5 ms; 29.5 s |
| "Revoke sign-in": sessions fail with `AUTH_FAILED`, accounts page says "sign in again" | ok | revoked alias=outlook-oauth provider=microsoft in 859 ms; sessions now fail with AUTH_FAILED |
| host `mail.accounts.list()`: `needsReauth` after the revocation | ok | `ok` True, `auth` xoauth2, `oauth` = provider microsoft, needsReauth True, expiresAt 0 (no expiry left), keys authorizedAt, expiresAt, needsReauth, provider; 23.8 s |
| host `mail.connect(alias)` refused after the revocation | ok | `ok` false, `AUTH_FAILED`: MailError [AUTH_FAILED]: the account has no refresh token; sign in again; 23.5 s |
| "Sign in again": a new grant on the same record | ok | re-authorized alias=outlook-oauth provider=microsoft expiresIn=3598 s in 1826 ms |
| host `mail.connect(alias)` -> `test` -> `fetch` after the re-authorization | ok | `ok` true, test {'ok': True, 'imap': True, 'smtp': True}, fetched 3, closed True; accounts.list 97 ms, accounts.has 8 ms, connect 48 ms, test 5425 ms, fetch 921 ms, close 4 ms; 30.8 s |
| record removed | ok | - |

Leak check: secret in logcat no, in the Gradle logs no; address in logcat no, in the Gradle logs no.

The plugin's `MailOAuth` state lines of the run (ids, states, counts and durations only):

    provider=microsoft browser opened
    provider=microsoft browser opened
    provider=microsoft redirect rejected: the redirect carries no matching state
    provider=microsoft sign-in revoked locally
    provider=microsoft provider-side revocation not done
    provider=microsoft refresh failed: AUTH_FAILED (reauthorize)
    provider=microsoft sign-in stored, expires in 7199 s
    provider=microsoft refresh ok, expires in 3598 s
    provider=microsoft sign-in revoked locally
    provider=microsoft provider-side revocation not done
    provider=microsoft refresh failed: AUTH_FAILED (reauthorize)
    provider=microsoft sign-in stored, expires in -21145 s
    provider=microsoft refresh ok, expires in 3598 s

#### BH900ASK9E: Sony G8441, Android 9 (API 28)

Plugin versionCode 67, host versionCode 5282, the build's Microsoft client id (the `HOTMAIL_A` registration), preset `outlook` (provider `microsoft`), the plugin-side steps and the real record (`--skip-host`: the phone keeps its P8 host state), alias `outlook-oauth`, the PC's access token already expired at the seed.

| Step | Result | Detail |
| --- | --- | --- |
| sign-in screen opens the provider page in the browser | ok | top activity `android/com.android.internal.app.ResolverActivity`; browser opened for provider=microsoft, state length 22 |
| foreign `state` refused, matching `state` exchanged at the token endpoint, late redirect refused | ok | foreign state refused: The browser's answer was not accepted: the redirect carries no matching state; matching state exchanged, provider answered (microsoft): The sign-in failed: the authorization code was refused; sign in again - invalid_grant: AADSTS7000012: The grant was obtained for a different tenant. Trace ID: 0c1c48c4-dfe3-4fc8-b61c-6e2d3cd99400 Correlation ID: 964e7358-19f1-4b81-a5b6-589752ecceb2 Timestamp: 2026-09-22 04:02:01Z; late redirect refused: The browser's answer was not accepted: no sign-in is waiting |
| OAUTH2 record round trip (Keystore), renewal, refusal marked `needsReauth` | ok | - |
| revoked record refuses sessions, re-authorization restores it | ok | - |
| real record seeded from the PC tokens; stale access token renewed at the provider | ok | seeded alias=outlook-oauth provider=microsoft staleAtSeed=true renewed=true expiresIn=3598 s in 3189 ms |
| "Revoke sign-in": sessions fail with `AUTH_FAILED`, accounts page says "sign in again" | ok | revoked alias=outlook-oauth provider=microsoft in 1141 ms; sessions now fail with AUTH_FAILED |
| "Sign in again": a new grant on the same record | ok | re-authorized alias=outlook-oauth provider=microsoft expiresIn=3598 s in 3069 ms |
| record removed | ok | - |

Leak check: secret in logcat no, in the Gradle logs no; address in logcat no, in the Gradle logs no.

The plugin's `MailOAuth` state lines of the run (ids, states, counts and durations only):

    provider=microsoft browser opened
    provider=microsoft browser opened
    provider=microsoft redirect rejected: the redirect carries no matching state
    provider=microsoft sign-in revoked locally
    provider=microsoft provider-side revocation not done
    provider=microsoft refresh failed: AUTH_FAILED (reauthorize)
    provider=microsoft sign-in stored, expires in 7199 s
    provider=microsoft refresh ok, expires in 3598 s
    provider=microsoft sign-in revoked locally
    provider=microsoft provider-side revocation not done
    provider=microsoft refresh failed: AUTH_FAILED (reauthorize)
    provider=microsoft sign-in stored, expires in -59901 s
    provider=microsoft refresh ok, expires in 3598 s

#### 968e9f18: Xiaomi (HyperOS), Android 15 (API 35)

Plugin versionCode 61, host versionCode 5282, the build's Microsoft client id (the `HOTMAIL_A` registration), preset `outlook` (provider `microsoft`), the plugin-side steps and the real record (`--skip-host`: the phone keeps its P8 host state), alias `outlook-oauth`, the PC's access token already expired at the seed.

| Step | Result | Detail |
| --- | --- | --- |
| sign-in screen opens the provider page in the browser | ok | top activity `com.android.chrome/org.chromium.chrome.browser.customtabs.CustomTabActivity`; browser opened for provider=microsoft, state length 22 |
| foreign `state` refused, matching `state` exchanged at the token endpoint, late redirect refused | ok | foreign state refused: 浏览器返回的结果未被接受: the redirect carries no matching state; matching state exchanged, provider answered: 登录失败: the token endpoint could not be reached - login.microsoftonline.com: UnknownHostException; late redirect refused: 浏览器返回的结果未被接受: no sign-in is waiting |
| OAUTH2 record round trip (Keystore), renewal, refusal marked `needsReauth` | ok | - |
| revoked record refuses sessions, re-authorization restores it | ok | - |
| real record seeded from the PC tokens; stale access token renewed at the provider | FAILED | - |

Leak check: secret in logcat no.

The plugin's `MailOAuth` state lines of the run (ids, states, counts and durations only):

    provider=microsoft browser opened
    provider=microsoft browser opened
    provider=microsoft redirect rejected: the redirect carries no matching state
    provider=microsoft sign-in revoked locally
    provider=microsoft provider-side revocation not done
    provider=microsoft refresh failed: AUTH_FAILED (reauthorize)
    provider=microsoft sign-in stored, expires in 7199 s
    provider=microsoft refresh failed: CONNECT_FAILED (login.microsoftonline.com: UnknownHostException)

### Google (the maintainer's Android client, plugin-side steps)

#### emulator-5556: AVD_API_37.1_16K (x86_64), Android 16 QPR (API 37)

Plugin versionCode 67, host versionCode 5282, the build's Google client id (the maintainer's Android client), preset `gmail` (provider `google`), plugin-side steps only (`--plugin-only`: no real account).

| Step | Result | Detail |
| --- | --- | --- |
| sign-in screen opens the provider page in the browser | ok | top activity `com.android.chrome/org.chromium.chrome.browser.customtabs.CustomTabActivity`; browser opened for provider=google, state length 22 |
| foreign `state` refused, matching `state` exchanged at the token endpoint, late redirect refused | ok | foreign state refused: The browser's answer was not accepted: the redirect carries no matching state; matching state exchanged, provider answered (google): The sign-in failed: the authorization code was refused; sign in again - invalid_grant: Malformed auth code.; late redirect refused: The browser's answer was not accepted: no sign-in is waiting |
| OAUTH2 record round trip (Keystore), renewal, refusal marked `needsReauth` | ok | - |
| revoked Google record: local marker, the token posted to Google's revocation endpoint, sessions refused, re-authorization restores it | ok | - |

Leak check: secret in logcat no, in the Gradle logs no; address in logcat no, in the Gradle logs no.

The plugin's `MailOAuth` state lines of the run (ids, states, counts and durations only):

    provider=google browser opened
    provider=google browser opened
    provider=google redirect rejected: the redirect carries no matching state
    provider=google sign-in revoked locally
    provider=google refresh failed: AUTH_FAILED (reauthorize)
    provider=google sign-in stored, expires in 7199 s
    provider=google provider-side revocation not done

#### emulator-5554: AVD_API_24 (x86), Android 7.0 (API 24)

Plugin versionCode 67, host versionCode 5282, the build's Google client id (the maintainer's Android client), preset `gmail` (provider `google`), plugin-side steps only (`--plugin-only`: no real account).

| Step | Result | Detail |
| --- | --- | --- |
| sign-in screen opens the provider page in the browser | ok | top activity `android/com.android.internal.app.ResolverActivity`; browser opened for provider=google, state length 22 |
| foreign `state` refused, matching `state` exchanged at the token endpoint, late redirect refused | ok | foreign state refused: The browser's answer was not accepted: the redirect carries no matching state; matching state exchanged, provider answered (google): The sign-in failed: the authorization code was refused; sign in again - invalid_grant: Malformed auth code.; late redirect refused: The browser's answer was not accepted: no sign-in is waiting |
| OAUTH2 record round trip (Keystore), renewal, refusal marked `needsReauth` | ok | - |
| revoked Google record: local marker, the token posted to Google's revocation endpoint, sessions refused, re-authorization restores it | ok | - |

Leak check: secret in logcat no, in the Gradle logs no; address in logcat no, in the Gradle logs no.

The plugin's `MailOAuth` state lines of the run (ids, states, counts and durations only):

    provider=google browser opened
    provider=google browser opened
    provider=google redirect rejected: the redirect carries no matching state
    provider=google sign-in revoked locally
    provider=google refresh failed: AUTH_FAILED (reauthorize)
    provider=google sign-in stored, expires in 7199 s
    provider=google provider-side revocation not done

#### BH900ASK9E: Sony G8441, Android 9 (API 28)

Plugin versionCode 67, host versionCode 5282, the build's Google client id (the maintainer's Android client), preset `gmail` (provider `google`), plugin-side steps only (`--plugin-only`: no real account).

| Step | Result | Detail |
| --- | --- | --- |
| sign-in screen opens the provider page in the browser | ok | top activity `android/com.android.internal.app.ResolverActivity`; browser opened for provider=google, state length 22 |
| foreign `state` refused, matching `state` exchanged at the token endpoint, late redirect refused | ok | foreign state refused: The browser's answer was not accepted: the redirect carries no matching state; matching state exchanged, provider answered (google): The sign-in failed: the authorization code was refused; sign in again - invalid_grant: Malformed auth code.; late redirect refused: The browser's answer was not accepted: no sign-in is waiting |
| OAUTH2 record round trip (Keystore), renewal, refusal marked `needsReauth` | ok | - |
| revoked Google record: local marker, the token posted to Google's revocation endpoint, sessions refused, re-authorization restores it | ok | - |

Leak check: secret in logcat no, in the Gradle logs no; address in logcat no, in the Gradle logs no.

The plugin's `MailOAuth` state lines of the run (ids, states, counts and durations only):

    provider=google browser opened
    provider=google browser opened
    provider=google redirect rejected: the redirect carries no matching state
    provider=google sign-in revoked locally
    provider=google refresh failed: AUTH_FAILED (reauthorize)
    provider=google sign-in stored, expires in 7199 s
    provider=google provider-side revocation not done

## Observations

- **The redirect path is closed to anything but the request the screen issued.** A redirect
  with another `state` is refused ("the redirect carries no matching state") and the request
  stays open; the matching one is exchanged once and the request is spent (`pendingState`
  null), so a replay of the same redirect is refused too. The exchange of the made-up code
  reached `login.microsoftonline.com` over HTTPS and came back as `invalid_grant`
  (`AADSTS7000012`; the emulators' answer says the code was not issued for this client, the
  Sony's that "the grant was obtained for a different tenant", both the provider's wording for a
  code it never issued), which the screen shows as "the authorization code was refused; sign in
  again" without echoing the code. On the offline phone the same step ended in `CONNECT_FAILED`
  ("the token endpoint could not be reached", the exception class name only), the other outcome
  the screen handles. The Google redirect (the reversed-client-id scheme, a URI without an
  authority that the manifest's scheme-only filter matches) took the same three answers on all
  three devices; its made-up code reached `oauth2.googleapis.com/token` and came back as
  `invalid_grant: Malformed auth code.`.
- **The renewal on the device is the real one.** The PC's access token had expired hours before
  the runs; the seed step's `usableTokens` posted the refresh grant with the build's client id
  and got a token with 3596 s left on the emulators (3598 s on the Sony, in 3.2 s), and the
  record's `oauth.expiresAt` followed. The same code path serves scripts, the connection test and
  the background watches (`AccountSecrets.withUsableSecret`).
- **Revocation is local first.** `TokenRevoker` writes the revoked marker before it talks to
  anyone (Microsoft personal accounts have no revocation endpoint: the accounts-page dialog
  points at the account's privacy page); from that moment `mail.connect(alias)` on the host
  fails with `AUTH_FAILED` ("the account has no refresh token; sign in again"),
  `mail.accounts.list()` reports `needsReauth: true` with `expiresAt: 0` (build 62: the first
  runs still showed the old expiry next to `needsReauth`, so the revoker now clears it in the
  `oauth` link as well), and the accounts page shows "sign in again". A new grant on the record
  (the "Sign in again" action) puts everything back. For a Google record the revoker also posts
  the refresh token to `oauth2.googleapis.com/revoke` on its background thread: the made-up
  token of the device test was refused there ("provider-side revocation not done" in the state
  lines; a real token gets "accepted"), and the local half stood regardless.
- **What the host sees.** `mail.accounts.list()` carries `auth: "xoauth2"` and the `oauth`
  object with exactly `provider`, `authorizedAt`, `expiresAt` and `needsReauth`; no key of the
  entry matches token / secret / password, and the token never left the plugin process.
- **Browsers.** The API 37 emulator's Chrome showed its first-run screen on the first attempt
  (Custom Tabs open the browser app; the screenshot is that screen); with the first run
  skipped (`/data/local/tmp/chrome-command-line`, a userdebug image) the tab shows
  `login.microsoftonline.com` or `accounts.google.com` (the Google URL carries
  `access_type=offline&prompt=consent`, so a refresh token comes with the first grant). The API
  24 emulator has Chrome and the WebView shell, and the Sony two browsers, so the Custom Tab
  intent lands on the system's chooser (`ResolverActivity`) first there, which is where the
  screenshot was taken; the test does not pick one, it only needs the request to be waiting. The
  Xiaomi's Chrome opened the tab directly. The Xiaomi had no network during the run
  (`Active default network: none`, its Wi-Fi off), so its tab stayed blank and the seed step
  stopped at the renewal (`CONNECT_FAILED`); its plugin-side steps (browser, redirects, records)
  passed and its summary is the partial one the driver keeps on a failed seed.
- **Chrome's crash on the API 37 emulator.** Twice in the Google runs Chrome (browser and
  renderer processes) died with `SIGILL` inside `libmonochrome` under `Application.onTrimMemory`
  (Chrome's own check trap, the tombstones of 11:54 and 11:58): its UI had been hidden by the
  driver's HOME press after the browser step and the next Custom Tab launch ran the pending trim
  callback. The platform finishes a paused activity underneath a crashing one, so the sign-in
  screen of the redirect case died with the tab and the case timed out. Without the HOME press
  the crash came 4 s after the launch, when the screen had already stopped, and the case
  passed; the driver no longer presses HOME there, and the test ends a case whose screen was
  finished underneath the browser as a skipped assumption naming the cause rather than as a
  timeout. The Microsoft runs and the two other devices never hit it.
- **Secrets.** No run printed a token or the address to logcat or to the Gradle logs; the
  `MailOAuth` state lines carry the provider, the outcome and seconds only (audited by
  `SecretAuditTest`).

## Not done

- **Google, the real record**: no refresh token can be obtained on the PC for the Android client
  (no loopback redirect), so the maintainer signs in once on a device (the accounts page, Gmail
  preset, "Sign in with Google (browser)", the Testing-status consent of the project's test
  user) under an alias, then `py -X utf8 .python/run_oauth_device.py GMAIL_A <serial>
  --signed-in-alias <alias>` runs the host status and session scripts live, the revocation (this
  time a real token reaches `oauth2.googleapis.com/revoke`), the host scripts after it and the
  removal; "sign in again" needs the browser again and is reported as skipped. In Testing the
  refresh token expires after 7 days, which the plugin reports as "sign in again".
- **A real browser sign-in end to end on a device** (typing the maintainer's password into the
  Custom Tab): not automated on purpose; the code path after the redirect is the one exercised
  by step 2 with a made-up code, and the grant storage is the one exercised by the seed.
- **The host steps on the Sony**: `--skip-host` kept the phone's P8 host task and watch; the
  host rows are the emulators'.
- The roadmap's "revoked record cannot be decrypted": D44 chose a revoked marker instead (the
  record still decrypts, to an access token the provider never issued and no refresh token), so
  that the accounts page can show the state and "Sign in again" can reuse the record.
