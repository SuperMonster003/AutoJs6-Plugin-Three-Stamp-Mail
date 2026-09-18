# P0.2 Angus Mail feasibility spike

Evidence for roadmap P0.2 and the D2 decision (Eclipse Angus Mail 2.0.5 as the mail library).
Collected on 2026-09-18 with Gradle 9.5.0, AGP 9.3.2, Kotlin 2.3.20, JDK 21 (Windows 11),
`org.eclipse.angus:jakarta.mail:2.0.5`, `angus-activation:2.0.3`, `jakarta.activation-api:2.1.4`,
GreenMail 2.1.13.

## Decision

D2 holds. Every hard-failure condition of the roadmap decision point was checked and none applies:

| Condition | Result |
| --- | --- |
| Runs on API 24 | Debug and release builds send and list mail on the API 24 x86 emulator (table below) |
| Usable after R8 | `:app:assembleRelease` passes; the five Angus data content handlers, `IMAPStore`, `SMTPTransport`, `META-INF/mailcap` and `META-INF/javamail.default.providers` survive in the APK; `-PandroidTestRelease` instrumentation passes 6/6 |
| Size increase under 4 MiB | Release APK 403,225 bytes in total (download size 372,874 bytes), so the whole plugin is 0.39 MiB; the roadmap expected under 1.5 MiB for the mail stack alone |
| IMAP IDLE usable on Android | Angus negotiates IDLE on the device (`IDLE` capability reported true on API 24 and API 35); push delivery is verified on the JVM against GreenMail, real-provider push is a P5 item |

The fallback of appendix E.2 (`com.sun.mail:android-mail`) is not used.

## JVM (`:mail-core:test`, 17 tests, all passing)

`GreenMailRoundTripTest` (8): SMTP sends a `multipart/mixed` message with a `multipart/alternative`
body and a base64 attachment, IMAP lists it (envelope, flags, UID, size, sent date, not marked
read) and parses the exact MIME tree and attachment bytes; POP3 lists the same message; empty
mailboxes; implicit TLS on 3465 / 3993 / 3995 with `trustAll` against the GreenMail self-signed
certificate; STARTTLS accounts refuse to continue in the clear (GreenMail offers no STARTTLS on its
plain ports, so the assertion is the refusal: SMTP and IMAP report `STARTTLS`, POP3 reports `STLS`);
wrong passwords fail with `AuthenticationFailedException` whose message does not contain the
password; XOAUTH2 authenticates SMTP, IMAP and POP3 with a bearer token and rejects an invalid
token without echoing it; IMAP IDLE delivers a `messagesAdded` event for a message sent during the
IDLE call.

`MailSessionPropertiesTest` (7): provider names (`imaps` / `pop3` / `smtp`), `starttls.required`,
`ssl.checkserveridentity`, `ssl.trust=*` only with `trustAll` and only on encrypted endpoints,
`auth.mechanisms` fixed to `LOGIN PLAIN` or `XOAUTH2`, timeouts, `peek`, and the session never
carrying the secret (`MailSecret` prints as `MailSecret(***)`, `clear()` wipes it).

`MailcapRegistryTest` (2): every handler of `MailcapRegistry.HANDLERS` resolves after explicit
registration, and a composed message survives a write / parse round trip.

Findings that feed later phases:

- GreenMail 2.1.13 advertises and serves IDLE, so the P5 watcher tests can run on the JVM.
- GreenMail validates XOAUTH2 tokens against the user password, which is enough for mechanism
  selection tests; real provider tokens are a P6 matrix item.
- A text attachment sent as `7bit` lost the final line break before the MIME boundary in the
  first run; `SmtpSender` now forces `Content-Transfer-Encoding: base64` for attachments.
- The Kotlin compiler treats `multipart/*` inside a KDoc as a nested comment opener; the wording
  in `MailcapRegistry` avoids it.

## R8 (`:app:assembleRelease`)

The first release build failed on missing classes: `java.awt.Image` and `java.awt.Toolkit`
(reached only from the `image_gif` / `image_jpeg` handlers) and
`javax.security.auth.callback.NameCallback` (reached only from `OAuth2SaslClient`, the SASL path
Android cannot use; the built-in XOAUTH2 path does not need it). `app/proguard-rules.pro` adds
`-dontwarn java.awt.**` and `-dontwarn javax.security.auth.callback.**` next to the existing
desktop-JDK `-dontwarn` entries, keeps `jakarta.mail.**`, `jakarta.activation.**`,
`org.eclipse.angus.mail.**` and `org.eclipse.angus.activation.**`, and the build passes.

`apkanalyzer` on the release APK: `classes.dex` 608,456 bytes plus `classes2.dex` 4,392 bytes
(desugared library); `org.eclipse.angus.mail.handlers.{text_plain, text_html, text_xml,
multipart_mixed, message_rfc822, image_gif, image_jpeg}`, `IMAPStore`, `IMAPSSLStore`,
`SMTPTransport`, `SMTPSSLTransport` and `jakarta.activation.MailcapCommandMap` are defined;
`META-INF/mailcap` (769 bytes), `META-INF/javamail.default.providers`, `javamail.providers`,
`javamail.charset.map` and the address maps are packaged. `MailcapRegistry.ensureRegistered()`
registers the handlers in code as well, so the plugin does not depend on the class loader exposing
`META-INF/mailcap`.

Sizes: release APK 403,225 bytes (download 372,874), debug APK 2,597,081 bytes. The 16 KB page
alignment check is not applicable (no native libraries; `verifyReleaseNativePageAlignment` passes
with `expectNoNativeLibraries`).

Release builds under test (`-PandroidTestRelease`) shrink the instrumentation APK against the
application mapping. Two test-only rule files make that work and are never part of the shipped
build: `app/proguard-test-rules.pro` (test APK, `-dontwarn com.google.errorprone.annotations.**`)
and `app/proguard-android-test-release.pro` (application, keeps the plugin and mail core surface
and `kotlin.**`, because the test APK does not bundle its own Kotlin runtime and otherwise fails
with `ClassNotFoundException: kotlin.jvm.internal.Lambda`).

## Devices

No real provider account was available (`mail-test-accounts.properties` not supplied), so the
device round trips ran against GreenMail standalone 2.1.13 on the development machine (JDK 21,
`-Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0`, plain ports 3025 / 3143). The emulator
reaches it through `10.0.2.2`; the physical device through `adb reverse`. Accounts enter the test
through instrumentation arguments (`MailCoreDeviceTest` KDoc); nothing is stored in the repository.

| Device | API | Build | Contract tests | Handlers resolve | MIME round trip | SMTP send | IMAP list (5) | IDLE advertised |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| AVD `emulator-5554` (x86) | 24 | debug | 3/3 | yes | 1005 bytes | 745 ms | 1 message, 164 ms | yes |
| AVD `emulator-5554` (x86) | 24 | release (`-PandroidTestRelease`) | 3/3 | yes | 1005 bytes | 117 ms | 3 messages, 404 ms | yes |
| Xiaomi 23046RP50C | 35 | debug | 3/3 | yes | 1005 bytes | 696 ms | 2 messages, 285 ms | yes |

The contract tests are `AngusMailPluginContractTest` (Wake Activity, INFO service `getInfo()` round
trip with an explicit empty `supportedAbis`, `org.autojs.plugin.MAIL` service with the `IMailPlugin`
descriptor). Timings include TLS-free socket setup on a loopback path and are only a sanity bound;
first-connection latency and idle PSS against a real provider are recorded when an account exists.

## Still open

- P0.2 item 2 (real provider: QQ or 163 authorization code, Gmail App Password) stays open until
  the maintainer supplies `mail-test-accounts.properties`; rerun
  `MailCoreDeviceTest.realAccountSendsAndListsWhenProvided` with the `mail*` arguments and add the
  row above.
- XOAUTH2 against Gmail / Outlook.com with a real token: P6 compatibility matrix.
- Positive STARTTLS sessions (ports 587 / 143 / 110): real providers only, P6.
- Idle PSS and battery: P6 performance baseline.

## Reproduce

```
./gradlew :mail-core:test
./gradlew :app:assembleRelease
java -Dgreenmail.setup.test.all -Dgreenmail.hostname=0.0.0.0 \
  -Dgreenmail.users=alice:alice-secret@localhost -jar greenmail-standalone-2.1.13.jar
adb reverse tcp:3025 tcp:3025 && adb reverse tcp:3143 tcp:3143     # physical device only
./gradlew :app:connectedDebugAndroidTest [-PandroidTestRelease :app:connectedReleaseAndroidTest] \
  -Pandroid.testInstrumentationRunnerArguments.mailAddress=alice@localhost \
  -Pandroid.testInstrumentationRunnerArguments.mailUsername=alice \
  -Pandroid.testInstrumentationRunnerArguments.mailSecret=alice-secret \
  -Pandroid.testInstrumentationRunnerArguments.mailImapHost=127.0.0.1 \
  -Pandroid.testInstrumentationRunnerArguments.mailImapPort=3143 \
  -Pandroid.testInstrumentationRunnerArguments.mailSmtpHost=127.0.0.1 \
  -Pandroid.testInstrumentationRunnerArguments.mailSmtpPort=3025 \
  -Pandroid.testInstrumentationRunnerArguments.mailTls=none
```

Use `10.0.2.2` instead of `127.0.0.1` on an emulator.
