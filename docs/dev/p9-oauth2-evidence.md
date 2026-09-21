# P9 OAuth 2.0 evidence (browser sign-in, token storage and renewal, revocation, device runs)

Evidence for roadmap P9 collected on 2026-09-22 in this repository (Gradle 9.5.0, AGP 9.3.2,
Kotlin 2.3.20, JDK 21, Windows 11), plugin build 61 (`3e35ad6`, the P9 feature) plus the device
tests and the driver of build 62 (this document), against the host debug build 5282
(`04c55ac78a`, mail contract version 2; the host is unchanged by P9). The Microsoft client id of
the build is the maintainer's Entra registration with the mobile redirect URI
`io.github.supermonster003.autojs6.plugin.angus.mail://oauth2/microsoft` (git-ignored
`oauth-clients.properties`, see `docs/dev/oauth-client-registration.md`); the Google client is
not registered yet, so the Google column is open and the Gmail preset's sign-in item is disabled
in this build.

Nothing here types a password or a token into a device screen. The real record comes from the
tokens `.python/outlook_oauth_login.py` obtained on the PC for the same client registration
(`build/outlook-token.properties`, the `HOTMAIL_A` profile of the git-ignored
`mail-test-accounts.properties`): a refresh token is bound to its client id, so the device's
renewal at `login.microsoftonline.com` with the build's client id is the same renewal every
alias session performs after a browser sign-in on the device. The tokens reach the device only as
base64 instrumentation arguments of an `am instrument` run, the host sees the alias alone, and
every log and report is scanned for the tokens and the address afterwards.

## Method

`.python/run_oauth_device.py <profile> <serial> [--skip-host] [--no-install]` runs, in order:

1. `OAuthDeviceTest#opensTheProviderPageInTheBrowser` (the plugin's androidTest APK): the
   sign-in screen (`OAuthSignInActivity`, provider Microsoft) generates the PKCE verifier and
   the `state` and opens the authorization URL in a Custom Tab; the driver reads the top
   activity and takes a screenshot meanwhile (`build/p9/oauth-browser-<serial>.png`).
2. `OAuthDeviceTest#refusesAForeignStateAndExchangesTheMatchingOneAtTheProvider`: three
   implicit `VIEW` intents on the redirect URI, as the browser would send them, reach the
   exported `OAuthRedirectActivity` and are forwarded to the waiting screen: one with a foreign
   `state` (refused, the request stays open), one with the right `state` and a made-up code
   (the screen exchanges it at the token endpoint over HTTPS, the provider refuses it, the
   request is spent), one more with the right `state` (refused: nothing is waiting).
3. `OAuthDeviceTest#anOAuthRecordRoundTripsRefreshesAndMarksARefusal` (a throw-away store
   under `noBackupFilesDir` with its own Keystore key and a scripted token endpoint): an
   `OAUTH2` record yields its access token, the record files hold neither token, a stale token is
   renewed through the refresh token and the `oauth` expiry rewritten, a valid token is not
   renewed again, an `invalid_grant` answer marks the record `needsReauth` and fails with
   `AUTH_FAILED`; `OAuthDeviceTest#aRevokedRecordRefusesEverySessionUntilANewSignIn` (the
   installed plugin's real store, a throw-away alias): `TokenRevoker.revoke` replaces the tokens
   by the revoked marker (`needsReauth` set), every session fails with `AUTH_FAILED`, and
   `storeGrant` (what the sign-in screen does at the end of a re-authorization) restores it.
4. `RealAccountOAuthDeviceTest#seedsASignedInAccount`: the PC tokens become the alias's `OAUTH2`
   record exactly as the sign-in screen stores a grant (`AccountFormPolicy.toAccountJson` with
   the `oauth` link); `AccountSecrets.usableTokens` renews the stale access token at the provider;
   the accounts page shows "Microsoft sign-in" without the marker.
5. The host (`.python/run_host_script_smoke.py --alias`, `MailScriptSmokeDeviceTest#savedAccountScript`):
   `docs/smoke/oauth-status.js` reads the alias's `oauth` object from `mail.accounts.list()` and
   checks that no secret-like key exists; `docs/smoke/saved-account.js` runs
   `mail.connect(alias)` -> `test` -> `fetch(3)` -> `close`. The connected run ends with the host
   uninstalled (the AGP test engine), so the reports are read from the host test's log lines.
6. `RealAccountOAuthDeviceTest#revokesTheSignIn`, then the two host scripts again (the status
   script must show `needsReauth`, the session script must fail with `AUTH_FAILED`).
7. `RealAccountOAuthDeviceTest#signsInAgain` (a new grant on the same record), then the session
   script again.
8. `RealAccountOAuthDeviceTest#removesTheAccount`.

## Results

### emulator-5556: AVD_API_37.1_16K (x86_64), Android 16 QPR (API 37)

Plugin versionCode 62, host versionCode 5282, the build's Microsoft client id (the `HOTMAIL_A` registration), preset `outlook` (provider `microsoft`), alias `outlook-oauth`, the PC's access token already expired at the seed.

| Step | Result | Detail |
| --- | --- | --- |
| sign-in screen opens the provider page in the browser | ok | top activity `com.android.chrome/org.chromium.chrome.browser.customtabs.CustomTabActivity`; browser opened for provider=microsoft, state length 22 |
| foreign `state` refused, matching `state` exchanged at the token endpoint, late redirect refused | ok | foreign state refused: The browser's answer was not accepted: the redirect carries no matching state; matching state exchanged, provider answered: The sign-in failed: the authorization code was refused; sign in again - invalid_grant: AADSTS7000012: The grant was obtained for a different tenant. Trace ID: dcbc4e30-b8eb-4154-9d29-f6d3d0b26d00 Correlation ID: b4467c34-9595-4d31-acfb-bce3424c3756 Timestamp: 2026-09-21 16:52:33Z; late redirect refused: The browser's answer was not accepted: no sign-in is waiting |
| OAUTH2 record round trip (Keystore), renewal, refusal marked `needsReauth` | ok | - |
| revoked record refuses sessions, re-authorization restores it | ok | - |
| real record seeded from the PC tokens; stale access token renewed at the provider | ok | seeded alias=outlook-oauth provider=microsoft staleAtSeed=true renewed=true expiresIn=3596 s in 3821 ms |
| host `mail.accounts.list()`: `oauth` object, live | ok | `ok` True, `auth` xoauth2, `oauth` = provider microsoft, needsReauth False, expires in 3582 s, keys authorizedAt, expiresAt, needsReauth, provider; 16.0 s |
| host `mail.connect(alias)` -> `test` -> `fetch`, live | ok | `ok` true, test {'ok': True, 'imap': True, 'smtp': True}, fetched 3, closed True; accounts.list 96 ms, accounts.has 8 ms, connect 118 ms, test 8467 ms, fetch 940 ms, close 5 ms; 24.5 s |
| "Revoke sign-in": sessions fail with `AUTH_FAILED`, accounts page says "sign in again" | ok | revoked alias=outlook-oauth provider=microsoft in 2333 ms; sessions now fail with AUTH_FAILED |
| host `mail.accounts.list()`: `needsReauth` after the revocation | ok | `ok` True, `auth` xoauth2, `oauth` = provider microsoft, needsReauth True, expiresAt 0 (no expiry left), keys authorizedAt, expiresAt, needsReauth, provider; 42.6 s |
| host `mail.connect(alias)` refused after the revocation | ok | `ok` false, `AUTH_FAILED`: MailError [AUTH_FAILED]: the account has no refresh token; sign in again; 31.3 s |
| "Sign in again": a new grant on the same record | ok | re-authorized alias=outlook-oauth provider=microsoft expiresIn=3596 s in 4669 ms |
| host `mail.connect(alias)` -> `test` -> `fetch` after the re-authorization | ok | `ok` true, test {'ok': True, 'imap': True, 'smtp': True}, fetched 3, closed True; accounts.list 607 ms, accounts.has 17 ms, connect 122 ms, test 7844 ms, fetch 1103 ms, close 8 ms; 25.5 s |
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
    provider=microsoft sign-in stored, expires in -19843 s
    provider=microsoft refresh ok, expires in 3598 s

### emulator-5554: AVD_API_24 (x86), Android 7.0 (API 24)

Plugin versionCode 62, host versionCode 5282, the build's Microsoft client id (the `HOTMAIL_A` registration), preset `outlook` (provider `microsoft`), alias `outlook-oauth`, the PC's access token already expired at the seed.

| Step | Result | Detail |
| --- | --- | --- |
| sign-in screen opens the provider page in the browser | ok | top activity `android/com.android.internal.app.ResolverActivity`; browser opened for provider=microsoft, state length 22 |
| foreign `state` refused, matching `state` exchanged at the token endpoint, late redirect refused | ok | foreign state refused: The browser's answer was not accepted: the redirect carries no matching state; matching state exchanged, provider answered: The sign-in failed: the authorization code was refused; sign in again - invalid_grant: AADSTS7000012: The grant was obtained for a different tenant. Trace ID: 11bcceb7-ce0e-4a2c-af71-abf7cecb6c00 Correlation ID: 3ca92a3e-af14-4c5e-9c0a-d330ac6cea98 Timestamp: 2026-09-21 16:55:49Z; late redirect refused: The browser's answer was not accepted: no sign-in is waiting |
| OAUTH2 record round trip (Keystore), renewal, refusal marked `needsReauth` | ok | - |
| revoked record refuses sessions, re-authorization restores it | ok | - |
| real record seeded from the PC tokens; stale access token renewed at the provider | ok | seeded alias=outlook-oauth provider=microsoft staleAtSeed=true renewed=true expiresIn=3598 s in 3377 ms |
| host `mail.accounts.list()`: `oauth` object, live | ok | `ok` True, `auth` xoauth2, `oauth` = provider microsoft, needsReauth False, expires in 3575 s, keys authorizedAt, expiresAt, needsReauth, provider; 24.5 s |
| host `mail.connect(alias)` -> `test` -> `fetch`, live | ok | `ok` true, test {'ok': True, 'imap': True, 'smtp': True}, fetched 3, closed True; accounts.list 42 ms, accounts.has 9 ms, connect 36 ms, test 4495 ms, fetch 1068 ms, close 5 ms; 29.8 s |
| "Revoke sign-in": sessions fail with `AUTH_FAILED`, accounts page says "sign in again" | ok | revoked alias=outlook-oauth provider=microsoft in 891 ms; sessions now fail with AUTH_FAILED |
| host `mail.accounts.list()`: `needsReauth` after the revocation | ok | `ok` True, `auth` xoauth2, `oauth` = provider microsoft, needsReauth True, expiresAt 0 (no expiry left), keys authorizedAt, expiresAt, needsReauth, provider; 24.4 s |
| host `mail.connect(alias)` refused after the revocation | ok | `ok` false, `AUTH_FAILED`: MailError [AUTH_FAILED]: the account has no refresh token; sign in again; 24.1 s |
| "Sign in again": a new grant on the same record | ok | re-authorized alias=outlook-oauth provider=microsoft expiresIn=3598 s in 1877 ms |
| host `mail.connect(alias)` -> `test` -> `fetch` after the re-authorization | ok | `ok` true, test {'ok': True, 'imap': True, 'smtp': True}, fetched 3, closed True; accounts.list 99 ms, accounts.has 8 ms, connect 45 ms, test 3801 ms, fetch 771 ms, close 3 ms; 28.6 s |
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
    provider=microsoft sign-in stored, expires in -20021 s
    provider=microsoft refresh ok, expires in 3598 s

### 968e9f18: Xiaomi (HyperOS), Android 15 (API 35)

Plugin versionCode 61, host versionCode 5282, the build's Microsoft client id (the `HOTMAIL_A` registration), preset `outlook` (provider `microsoft`), alias `outlook-oauth`, the PC's access token already expired at the seed.

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

## Observations

- **The redirect path is closed to anything but the request the screen issued.** A redirect
  with another `state` is refused ("the redirect carries no matching state") and the request
  stays open; the matching one is exchanged once and the request is spent (`pendingState`
  null), so a replay of the same redirect is refused too. The exchange of the made-up code
  reached `login.microsoftonline.com` over HTTPS and came back as `invalid_grant`
  (`AADSTS7000012`, the code was not issued for this client), which the screen shows as "the
  authorization code was refused; sign in again" without echoing the code. On the offline phone
  the same step ended in `CONNECT_FAILED` ("the token endpoint could not be reached", the
  exception class name only), the other outcome the screen handles.
- **The renewal on the device is the real one.** The PC's access token had expired hours before
  the runs; the seed step's `usableTokens` posted the refresh grant with the build's client id
  and got a token with 3596 s left, and the record's `oauth.expiresAt` followed. The same code
  path serves scripts, the connection test and the background watches
  (`AccountSecrets.withUsableSecret`).
- **Revocation is local first.** `TokenRevoker` writes the revoked marker before it talks to
  anyone (Microsoft personal accounts have no revocation endpoint: the accounts-page dialog
  points at the account's privacy page); from that moment `mail.connect(alias)` on the host
  fails with `AUTH_FAILED` ("the account has no refresh token; sign in again"),
  `mail.accounts.list()` reports `needsReauth: true` with `expiresAt: 0` (build 62: the first
  runs still showed the old expiry next to `needsReauth`, so the revoker now clears it in the
  `oauth` link as well), and the accounts page shows "sign in again". A new grant on the record
  (the "Sign in again" action) puts everything back.
- **What the host sees.** `mail.accounts.list()` carries `auth: "xoauth2"` and the `oauth`
  object with exactly `provider`, `authorizedAt`, `expiresAt` and `needsReauth`; no key of the
  entry matches token / secret / password, and the token never left the plugin process.
- **Browsers.** The API 37 emulator's Chrome showed its first-run screen on the first attempt
  (Custom Tabs open the browser app; the screenshot is that screen); with the first run
  skipped (`/data/local/tmp/chrome-command-line`, a userdebug image) the tab shows
  `login.microsoftonline.com`. The API 24 emulator has Chrome and the WebView shell, so the
  Custom Tab intent lands on the system's chooser (`ResolverActivity`) first, which is where the
  screenshot was taken; the test does not pick one, it only needs the request to be waiting.
  The phone's Chrome opened the tab directly. The phone had no network during the run
  (`Active default network: none`, its Wi-Fi off), so its tab stayed blank and the seed step
  stopped at the renewal (`CONNECT_FAILED`); its plugin-side steps (browser, redirects, records)
  passed and its summary is the partial one the driver keeps on a failed seed.
- **Secrets.** No run printed a token or the address to logcat or to the Gradle logs; the
  `MailOAuth` state lines carry the provider, the outcome and seconds only (audited by
  `SecretAuditTest`).

## Not done

- **Google**: no Android client is registered (`docs/dev/oauth-client-registration.md` has the
  procedure); the Gmail column needs the client id in `oauth-clients.properties` and a real
  sign-in on a device by the maintainer (a Google refresh token cannot be obtained on the PC for
  the Android client), then `.python/run_oauth_device.py` cannot seed it but the two device
  tests and the host scripts apply unchanged to the alias the sign-in screen saved.
- **A real browser sign-in end to end on a device** (typing the maintainer's Microsoft password
  into the Custom Tab): not automated on purpose; the code path after the redirect is the one
  exercised by step 2 with a made-up code, and the grant storage is the one exercised by the seed.
- **Sony G8441**: locked since the P8 reboot scenario; not run here.
- The roadmap's "revoked record cannot be decrypted": D44 chose a revoked marker instead (the
  record still decrypts, to an access token the provider never issued and no refresh token), so
  that the accounts page can show the state and "Sign in again" can reuse the record.
