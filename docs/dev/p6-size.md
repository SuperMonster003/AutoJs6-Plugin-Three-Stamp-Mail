# P6 size (release APK and the R8 scope of Angus Mail)

Roadmap P6 "体积", measured on 2026-09-20 with `:app:assembleRelease` (AGP 9.3.2, R8 8.13.19,
`proguard-android-optimize.txt` + `app/proguard-rules.pro`, resource shrinking on). "Before" is the
rule set of P0.2 (every class of `jakarta.mail`, `jakarta.activation`, `org.eclipse.angus.mail` and
`org.eclipse.angus.activation` kept with its name); "after" is the narrowed set of this commit. Sizes
from `apkanalyzer apk file-size` / `download-size`, DEX figures from `apkanalyzer dex packages`, the
surviving classes from `mapping.txt`.

## Figures

| Measure | Before (P0.2 rules) | After (narrowed rules) | Change |
| --- | --- | --- | --- |
| Universal release APK on disk | 2,254,035 bytes | 2,153,957 bytes | -100,078 bytes (-4.4 %) |
| Estimated download size | 1,507,931 bytes | 1,407,295 bytes | -100,636 bytes (-6.7 %) |
| `classes.dex` + `classes2.dex` | 1,793,508 + 193,376 bytes | 1,532,192 + 193,376 bytes | -261,316 bytes |
| DEX methods defined (all packages) | 12,150 | 9,831 | -2,319 |
| DEX bytes (all packages) | 1,544,116 | 1,369,403 | -174,713 |
| Classes in the DEX | 1,914 | 1,727 | -187 |
| Classes of the four namespaces surviving | 410 (all, by name) | 248 (20 by name, 228 renamed) | -162 |
| `resources.arsc` | 765,744 bytes | 765,744 bytes | 0 (AppCompat; not this item) |

Angus Mail before, by package (methods / bytes, all kept by name): `imap` 785 / 112,667,
`imap.protocol` 260 / 41,982, `util` 629 / 74,686, `util.logging` 326 / 40,471, `pop3` 201 / 25,065,
`smtp` 176 / 27,940, `iap` 140 / 12,828, `auth` 42 / 8,513, `handlers` 40 / 3,957, `nativeimage`
12 / 864, `activation` 82 / 9,259; Jakarta Mail 1,217 / 114,537 and Jakarta Activation 262 / 26,484.
After, only the 20 classes loaded by name keep their package (108 methods / 31,927 bytes in
`org.eclipse.angus.mail`, 5 / 573 in `org.eclipse.angus.activation`); the rest of the surviving code
is renamed into the single-letter packages and counted there.

Surviving classes after, by namespace: `jakarta.mail` 33, `jakarta.mail.internet` 29,
`jakarta.mail.search` 20, `jakarta.mail.util` 4, `jakarta.mail.event` 2, `jakarta.activation` 21,
`angus.mail.util` 29, `angus.mail.imap.protocol` 25, `angus.mail.imap` 25, `angus.mail.smtp` 16,
`angus.mail.iap` 14, `angus.mail.pop3` 12, `angus.mail.handlers` 7, `angus.mail.auth` 4,
`angus.activation` 7. Dropped entirely: `angus.mail.util.logging` (the `MailHandler` for
`java.util.logging`, 11 classes), `angus.mail.nativeimage` (GraalVM feature), the `image_gif` /
`image_jpeg` handlers (need `java.awt`), `IMAPSaslAuthenticator` and `OAuth2SaslClient` (SASL is
never enabled; the built-in XOAUTH2 command path stays), `SortTerm`, most of `jakarta.mail.event`.
Still present because the code reaches them: `SMTPSaslAuthenticator` and `auth.Ntlm` (referenced
by `SMTPTransport` / `IMAPStore` directly), `MailSSLSocketFactory`, `POP3Folder`.

## The rules and their basis

Everything the mail core calls directly (stores, folders, messages, MIME classes, search terms,
exceptions) is reached by R8 through the code and needs no rule; R8 keeps what is reachable and
renames it. Rules are needed only for the classes the two libraries instantiate **by name**, and each
rule in `app/proguard-rules.pro` names the resource that carries the name:

| Rule (class, kept member) | Loaded by | Named in |
| --- | --- | --- |
| `IMAPStore`, `IMAPSSLStore`, `POP3Store`, `POP3SSLStore`, `SMTPTransport`, `SMTPSSLTransport`; `<init>(Session, URLName)` | `jakarta.mail.Session.getService`: `Class.forName(className)` + `getConstructor(Session.class, URLName.class)` | `META-INF/javamail.default.providers` (`class=` values) |
| `IMAPProvider`, `IMAPSSLProvider`, `POP3Provider`, `POP3SSLProvider`, `SMTPProvider`, `SMTPSSLProvider`; `<init>()` | `ServiceLoader.load(Provider.class)` in `Session` | `META-INF/services/jakarta.mail.Provider` |
| `org.eclipse.angus.mail.util.MailStreamProvider`; `<init>()` | `jakarta.mail.util.FactoryFinder` (ServiceLoader, then the default class name) | `META-INF/services/jakarta.mail.util.StreamProvider` |
| `org.eclipse.angus.activation.MailcapRegistryProviderImpl`, `MimeTypeRegistryProviderImpl`; `<init>()` | `jakarta.activation.FactoryFinder` (ServiceLoader, then the default class name) | `META-INF/services/jakarta.activation.spi.*RegistryProvider` |
| `org.eclipse.angus.mail.handlers.text_plain`, `text_html`, `text_xml`, `multipart_mixed`, `message_rfc822`; `<init>()` | `jakarta.activation.MailcapCommandMap`: `Class.forName(name).newInstance()` | `META-INF/mailcap` and `MailcapRegistry.HANDLERS` (mail core, P0.2) |

Not kept, on purpose:

- `org.eclipse.angus.mail.util.logging.**` (`MailHandler`, `LogManagerProperties` ...): a
  `java.util.logging` handler that mails log records; nothing in the plugin references it.
- `org.eclipse.angus.mail.nativeimage.**` and `org.eclipse.angus.activation.nativeimage.**`: GraalVM
  native-image features.
- `image_gif` / `image_jpeg` handlers: not in the shipped `META-INF/mailcap` either (they need
  `java.awt.Toolkit`); the P0.2 `-dontwarn java.awt.**` stays for the references inside the bundle.
- SASL (`IMAPSaslAuthenticator`, `OAuth2SaslClient`, `SMTPSaslAuthenticator` where unreachable):
  `mail.*.sasl.enable` is never set; XOAUTH2 uses the built-in `AUTHENTICATE XOAUTH2` /
  `AUTH XOAUTH2` commands (P2, P6 provider matrix).
- `gimap` (Gmail IMAP extensions) and `dsn` (delivery status notifications): the
  `org.eclipse.angus:jakarta.mail` bundle does not ship them (its `javamail.default.providers`
  lists imap / imaps / smtp / smtps / pop3 / pop3s only), so there is nothing to trim; if the Gmail
  label API of a later roadmap item needs `gimap`, its provider classes join the first table.

R8 keeps the `META-INF` resources and rewrites the three `META-INF/services/<interface>` file names
to the renamed interfaces (`jakarta.mail.Provider -> d20`, `jakarta.mail.util.StreamProvider -> s80`,
`jakarta.activation.spi.MailcapRegistryProvider -> hv` in this build; `MimeTypeRegistryProvider` was
unreachable and its file went with it), while their contents keep naming the implementation classes
the rules preserve. `META-INF/javamail.default.providers` and `META-INF/mailcap` are plain resources
and survive unchanged. `MailcapRegistry.ensureRegistered()` (mail core) registers the five handlers
in code as well, so the plugin does not depend on the class loader exposing `META-INF/mailcap`.

## Verification

- `:app:assembleRelease` passes with no R8 warning about the two namespaces (the `-dontwarn` list of
  P0.2 is unchanged).
- `mapping.txt` spot checks: the 20 rule classes map to themselves; `MailHandler`, the GraalVM
  features, the image handlers, `IMAPSaslAuthenticator`, `OAuth2SaslClient`, `SortTerm` and
  `TransportEvent` are absent.
- Release build under test: `./gradlew.bat -PandroidTestRelease :app:connectedReleaseAndroidTest`
  runs the instrumentation suite against the shrunk build (the test-only rules of
  `app/proguard-android-test-release.pro` keep the plugin surface and now also the five Jakarta
  classes the test code links against, `MimeMessage`, `MimeBodyPart`, `MimeMultipart`, `Flags`,
  `CommandMap`, and the AndroidX classes the test APK shares with the application: the first run
  crashed in `AndroidJUnitRunner.onCreate` with `NoSuchMethodError: androidx.tracing.Trace.beginSection`
  because the shrunk application had stripped it. The by-name loading of providers, handlers, stream
  provider and registries is verified through the mail core alone). Result on the Redmi 22120RN86C
  (API 33), 2026-09-20: 27 tests, 0 failures, 4 skipped (the real-account and performance tests that
  need instrumentation arguments), 1 min 18 s including the build.

## Reproduce

```text
./gradlew.bat :app:assembleRelease
E:/.android/sdk/cmdline-tools/latest/bin/apkanalyzer.bat apk file-size app/build/outputs/apk/release/autojs6-plugin-angus-mail-v1.0.0.apk
E:/.android/sdk/cmdline-tools/latest/bin/apkanalyzer.bat apk download-size app/build/outputs/apk/release/autojs6-plugin-angus-mail-v1.0.0.apk
E:/.android/sdk/cmdline-tools/latest/bin/apkanalyzer.bat dex packages --defined-only app/build/outputs/apk/release/autojs6-plugin-angus-mail-v1.0.0.apk
grep -cE "^(org\.eclipse\.angus|jakarta\.)[^ ]+ -> " app/build/outputs/mapping/release/mapping.txt
./gradlew.bat -PandroidTestRelease :app:connectedReleaseAndroidTest
```
