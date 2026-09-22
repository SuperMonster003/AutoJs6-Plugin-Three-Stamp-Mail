# OAuth 2.0 client registration (the maintainer's part of the browser sign-in, roadmap P9)

The browser sign-in of the plugin (Angus Mail 1.2.0, `OAuthSignInActivity`, decisions D44 / D45
in `ROADMAP.md`) is an OAuth 2.0 authorization-code flow with PKCE run by a **public client**:
no client secret exists anywhere, only client ids, and the redirect comes back to the app through
a custom URI scheme. The client ids are the maintainer's registrations with Google and Microsoft;
they never enter the repository. This document is the registration procedure for both providers
and how a registration reaches a build.

## What a build reads

`oauth-clients.properties` next to `settings.gradle.kts` (git-ignored, see `.gitignore` and
`AGENTS.md` section 6):

```properties
# OAuth 2.0 public client ids of this build (git-ignored; see docs/dev/oauth-client-registration.md)
googleClientId=123456789012-abcdefghijklmnopqrstuvwxyz012345.apps.googleusercontent.com
microsoftClientId=00000000-0000-0000-0000-000000000000
microsoftTenant=consumers
```

`app/build.gradle.kts` turns them into the string resources `oauth_google_client_id`,
`oauth_microsoft_client_id` and `oauth_microsoft_tenant` (`resValue`, read by `OAuthClients.of`)
and the manifest placeholder `oauthGoogleScheme` (`com.googleusercontent.apps.<client id without
the .apps.googleusercontent.com suffix>`, the scheme of the Google redirect filter of
`OAuthRedirectActivity`). A missing file or key leaves the provider "not configured": the
authentication dialog then shows the sign-in item disabled with the reason, the redirect filter
keeps a placeholder scheme, and the token and app-password paths keep working. Nothing else
changes between a build with and without the file, so the file may be added to a machine at any
time; a rebuild (`:app:assembleDebug` / `:app:assembleRelease`) picks it up. `aapt2 dump xmltree
--file AndroidManifest.xml <apk>` shows the two redirect filters of the built APK, and the JVM
test `OAuthClientsTest` plus the device test `OAuthDeviceTest` check the derived redirect URIs.

The redirect URIs the providers must know (nothing else is app-specific on their side):

| Provider | Redirect URI | Where it is registered |
| --- | --- | --- |
| Microsoft | `io.github.supermonster003.autojs6.plugin.angus.mail://oauth2/microsoft` | Entra app registration, platform "Mobile and desktop applications" |
| Google | `com.googleusercontent.apps.<client id prefix>:/oauth2redirect` | implied by the Android client type (no field to type; the "Enable custom URI scheme" setting must be on) |

The signing certificate of the APKs matters for Google only. On the maintainer's machine both
build types are signed with the release key (`sign.properties` + `app/sm003.jks`; the debug build
type takes the same `signingConfig`, see `app/build.gradle.kts`), so one Android client covers
the debug and the release APKs:

| Keystore | Alias | SHA-1 | SHA-256 |
| --- | --- | --- | --- |
| `app/sm003.jks` (release; also the debug build here) | `003` | `3C:E4:58:EE:42:22:C5:ED:1E:B4:84:4E:FA:79:79:9B:AD:53:EE:FD` | `31:A6:81:FC:FF:FB:3E:42:84:20:CA:E2:80:DE:D8:92:92:B1:2A:3B:0F:59:E1:9B:7A:73:E3:2A:8A:E4:C2:13` |
| `~/.android/debug.keystore` (only a machine without `sign.properties`, e.g. CI) | `androiddebugkey` | machine-specific, `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android` | - |

```
"E:\.java\jdk-21.0.1\bin\keytool" -list -v -keystore app\sm003.jks -alias 003
```

prints them (the store password is in `sign.properties`); `apksigner verify --print-certs
<apk>` prints the same digests of a built APK. The APKs are distributed as GitHub release assets
signed directly with this key (no Play App Signing), so there is no second "upload" certificate
to register.

## Microsoft (done by the maintainer, 2026-09-21; kept here for the next registration)

Portal: <https://entra.microsoft.com> (or <https://portal.azure.com>) > Microsoft Entra ID > App
registrations. A personal Microsoft account without a directory is refused ("The ability to
create applications outside of a directory has been deprecated"): the account needs a tenant,
which the Microsoft 365 Developer Program or an Azure sign-up provides; the app registered inside
that tenant still serves personal accounts when the audience below admits them.

1. **New registration**: any name (shown on the consent page, e.g. "AutoJs6 Angus Mail");
   supported account types "Personal Microsoft accounts only" (the plugin's default tenant
   `consumers`) or "Accounts in any organizational directory and personal Microsoft accounts"
   (then `microsoftTenant=common`, which also admits work and school accounts); no redirect URI
   on this page yet.
2. **Authentication** > Add a platform > **Mobile and desktop applications** > custom redirect
   URI `io.github.supermonster003.autojs6.plugin.angus.mail://oauth2/microsoft`. The same
   registration may keep `http://localhost` for `.python/outlook_oauth_login.py` (the PC login of
   the P6 matrix): the two redirects coexist. Further down the page, **Allow public client
   flows** = Yes (the mobile redirect implies it; the setting matters for the refresh grant
   without a secret).
3. **API permissions** > Add a permission > APIs my organization uses > "Office 365 Exchange
   Online" > Delegated: `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All`, `SMTP.Send`; plus
   Microsoft Graph > Delegated: `offline_access`, `openid`, `email`. No admin consent is needed
   for personal accounts; the user consents at the first sign-in. These are the scopes
   `OAuthProviders.microsoft()` asks for (`https://outlook.office.com/...` resource form).
4. **Overview** > "Application (client) ID" = `microsoftClientId`. No certificate or secret is
   created.

Checks: an authorization URL of the form
`https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize?client_id=<id>&response_type=code&redirect_uri=io.github.supermonster003.autojs6.plugin.angus.mail%3A%2F%2Foauth2%2Fmicrosoft&scope=openid&code_challenge=x&code_challenge_method=S256`
opened in a PC browser must show the login page (an `AADSTS50011` page means the redirect URI is
not registered; `AADSTS7000218` at the token endpoint means public client flows are off).
Refresh tokens are bound to the client id they were issued to: a token obtained on the PC with
one registration does not refresh under another (`AADSTS7000012` / `invalid_grant`), which is
also what the device evidence sees when a made-up code is exchanged.

## Google (the Android client registered by the maintainer on 2026-09-22; the restricted-scope verification is open)

Google keeps the registration under the **Google Auth Platform** of a Google Cloud project
(<https://console.cloud.google.com/auth/overview>; the older "APIs & Services > Credentials" page
lists the same clients). The Gmail preset needs the scope `https://mail.google.com/`, which is
the only scope Gmail's IMAP / SMTP XOAUTH2 accept and which Google classes as a **restricted**
scope; this decides the audience questions below.

1. **Project**: create or pick a Cloud project (any name; the project id is not visible to
   users). No billing account is needed for OAuth clients. Enable the **Gmail API** in the API
   Library (Google checks the requested scopes against enabled APIs when verifying).
2. **Google Auth Platform > Get started / Branding**: app name (shown on the consent screen,
   e.g. "AutoJs6 Angus Mail"), user support email, developer contact email; logo and links are
   optional until verification. Authorized domains are only needed when a home page or privacy
   policy URL is entered (both are required for verification, not for testing).
3. **Audience**: user type **External** (a personal Google account cannot create an Internal
   app; Internal needs a Google Workspace organization). Publishing status starts as
   **Testing**: add the maintainer's Gmail accounts under **Test users** (up to 100). In Testing,
   only test users can sign in, everyone sees a "Google hasn't verified this app" notice, and
   **refresh tokens expire after 7 days** (Google's documented rule for External + Testing with
   scopes beyond `openid` / `email` / `profile`): the plugin then marks the account "sign in
   again" weekly and the accounts page offers the re-authorization. Moving to **In production**
   removes the 7-day rule but, for a restricted scope, requires the verification in step 7 first;
   an unverified production app is limited to the unverified-app screen and a user cap.
4. **Data Access**: Add or remove scopes > add `https://mail.google.com/` (listed under Gmail
   API, restricted), `openid` and `.../auth/userinfo.email` (`email`; the id token that prefills
   the address). Save. The scopes must match what the app requests
   (`OAuthProviders.GOOGLE.scopes` + `identityScopes`).
5. **Clients > Create client**: application type **Android**; name (internal); package name
   `io.github.supermonster003.autojs6.plugin.angus.mail`; SHA-1 certificate fingerprint
   `3C:E4:58:EE:42:22:C5:ED:1E:B4:84:4E:FA:79:79:9B:AD:53:EE:FD` (the release key above, which
   also signs the debug builds on the maintainer's machine). Expand **Advanced Settings** and turn
   on **Enable custom URI scheme**: since 2023 Google disables custom-scheme redirects for new
   Android clients by default and its native-app page says they are "no longer supported"
   for the recommended path, but the setting stays available for an Android client and is what
   the reversed-client-id redirect needs; without it the authorization page answers
   `Error 400: invalid_request` / "Custom URI scheme is not enabled for your Android client".
   Google's recommended alternative (the Google Identity Services `AuthorizationClient` of Play
   services) is not used because it needs Google Play services on the device and returns
   tokens through Play services rather than to the app's own redirect; the plugin keeps the
   RFC 8252 flow that works on any device with a browser. Create; the client id looks like
   `123456789012-abcdefghijklmnopqrstuvwxyz012345.apps.googleusercontent.com` and is
   `googleClientId`. Google says a new client or a changed setting may take a few minutes to a
   few hours to take effect.
6. **A second Android client** is needed only for APKs signed with another key (a CI or another
   machine's `debug.keystore`): same package name, that key's SHA-1, its own client id in that
   machine's `oauth-clients.properties`. The package name + SHA-1 pair must be unique across all
   Google projects, so the pair cannot be registered twice.
7. **Verification (only for In production with the restricted Gmail scope)**: Verification
   Center > brand verification first (home page under an authorized domain, the privacy policy
   on the same domain and linked from the consent screen, the app name and logo), then the
   restricted-scope submission: a justification of why `https://mail.google.com/` is needed (IMAP
   and SMTP access is what the plugin does), an unlisted YouTube video of the sign-in and the
   scopes' use in English, and a **security assessment (CASA)** by a Google-empanelled assessor,
   repeated at least every 12 months. Google's pages give no fee; assessors charge for it and the
   process takes weeks. Until this is done the project stays in Testing with test users, which is
   enough for the maintainer's own accounts and for the device evidence.

Checks done on build 67 (the first build with `googleClientId`, 2026-09-22): `aapt2 dump
xmltree` shows the Google filter's scheme as `com.googleusercontent.apps.<prefix>` next to the
Microsoft filter; `OAuthDeviceTest`'s Google cases passed on AVD_API_37, AVD_API_24 and the Sony
G8441 (`.python/run_oauth_device.py GMAIL_A <serial> --plugin-only`): the sign-in screen opens
`accounts.google.com/o/oauth2/v2/auth` with the client id, the reversed-client-id redirect, the
`https://mail.google.com/ openid email` scopes, `access_type=offline` and `prompt=consent` in a
Custom Tab; a redirect on the reversed-client-id scheme reaches the plugin's redirect activity
through the manifest's scheme-only filter; a made-up code is exchanged at
`oauth2.googleapis.com/token` and refused there (`invalid_grant: Malformed auth code.`, so the
client id and the redirect URI are accepted by the token endpoint); and a revoked Google record
posts its token to `oauth2.googleapis.com/revoke` (the made-up token was refused, as expected).
A Google refresh token cannot be obtained on the PC for the device the way
`.python/outlook_oauth_login.py` does for Microsoft (an Android client accepts no loopback
redirect, and a token issued to a Desktop client would not refresh under the Android client id),
so the real Gmail record starts with a sign-in in the plugin on a device by the maintainer. Done
on 2026-09-22 on the Sony G8441: the accounts page, the Gmail preset, "Sign in with Google
(browser)", the Google consent of a test user (the project is in Testing), saved under the alias
`gmail-oauth`; the consent page and the grant were thereby exercised for real. Then
`py -X utf8 .python/run_oauth_device.py GMAIL_A BH900ASK9E --signed-in-alias gmail-oauth --skip-host`
(the maintainer's run of 12:55) revoked the sign-in with the real refresh token posted to
`oauth2.googleapis.com/revoke`, which accepted it, and removed the record; "sign in again" needs
the browser and is reported as skipped. What that run did not cover is a live session over the
record (the in-plugin session step of build 68 came after it, and the host steps were skipped
to keep the phone's P8 state): the next sign-in on a device followed by the same command covers
it. In Testing the refresh token expires after 7 days, which the plugin shows as "sign in again".

## References

- Google, "OAuth 2.0 for iOS & Desktop Apps" (custom URI scheme note, redirect URI table):
  <https://developers.google.com/identity/protocols/oauth2/native-app>
- Google Developers Blog, 2023-10-02, "Improving user safety in OAuth flows through new OAuth
  Custom URI scheme restrictions" (the Advanced Settings switch):
  <https://developers.googleblog.com/improving-user-safety-in-oauth-flows-through-new-oauth-custom-uri-scheme-restrictions/>
- Google, "Manage OAuth Clients" (Android client fields, keytool):
  <https://support.google.com/cloud/answer/15549257>
- Google, "Using OAuth 2.0 to Access Google APIs", refresh token expiration (7 days in Testing,
  100 tokens per client per account): <https://developers.google.com/identity/protocols/oauth2#expiration>
- Google, restricted scope verification: <https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification>
- Microsoft, "Quickstart: Register an application", mobile redirect URIs and public client flows:
  <https://learn.microsoft.com/entra/identity-platform/quickstart-register-app>
- Microsoft, "Authenticate an IMAP, POP or SMTP connection using OAuth" (the Exchange Online
  delegated permissions): <https://learn.microsoft.com/exchange/client-developer/legacy-protocols/how-to-authenticate-an-imap-pop-smtp-application-by-using-oauth>
