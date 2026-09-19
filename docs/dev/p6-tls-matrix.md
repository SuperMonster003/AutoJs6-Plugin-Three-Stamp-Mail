# P6 TLS matrix (SSL, STARTTLS, plain text, certificates, API 24 TLS 1.2)

Roadmap P6 "TLS 矩阵", run on 2026-09-19 against plugin build 38 (`a93a6f1`). The enforcing
tests are `TlsMatrixTest` (5 cases, `mail-core`, GreenMail plus a STARTTLS front) and
`TlsDeviceTest` (2 cases, app `androidTest`, run on API 24 / 28 / 33); `ExceptionMapperTest`
pins the two POP3 mappings the matrix corrected.

## Servers

- GreenMail 2.1.13 (`ServerSetupTest.ALL`): plain 3143 / 3110 / 3025 and implicit SSL 3993 /
  3995 / 3465 with its self-signed certificate (`greenmail.p12`, CN "GreenMail selfsigned Test
  Certificate", no subject alternative name).
- `StartTlsProxy` (test helper): GreenMail has no STARTTLS, so a proxy in front of each plain
  port advertises the upgrade (`STARTTLS` in the IMAP capabilities, `STLS` in the POP3 `CAPA`
  list, `250-STARTTLS` in the SMTP `EHLO` reply), answers the upgrade command itself, completes
  a real TLS handshake on the client connection with GreenMail's certificate and relays the rest
  in the clear to GreenMail. It records the protocol version of every handshake.
- `ScriptedImapServer` over an `SSLServerSocket` (device tests): the key pair and the
  self-signed certificate are generated in the Android Keystore at run time (EC P-256,
  `DIGEST_NONE` allowed because Conscrypt signs the handshake through `NONEwithECDSA`), so no key
  material lives in the repository. The server is limited to one protocol version per run.

## JVM rows (`TlsMatrixTest`, JDK 21)

Each row is a `session.test` report for IMAP, POP3 and SMTP (`EndpointReport.error.code`).

| Mode | Ports | Certificate trust | IMAP | POP3 | SMTP | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| SSL | 3993 / 3995 / 3465 | `trustAll` | ok | ok | ok | implicit TLS |
| SSL | 3993 / 3995 / 3465 | JVM default trust (self-signed unknown) | `TLS_FAILED` | `TLS_FAILED` | `TLS_FAILED` | fails closed, not retryable |
| SSL | 3993 / 3995 / 3465 | certificate trusted, name mismatch (`127.0.0.1` vs a certificate naming no host) | `TLS_FAILED` | `TLS_FAILED` | `TLS_FAILED` | Angus 2.0.5 checks the identity through `SSLParameters.setEndpointIdentificationAlgorithm`, so the mismatch is an `SSLHandshakeException` ("TLS handshake failed") |
| STARTTLS | proxy in front of 3143 / 3110 / 3025 | `trustAll` | ok | ok | ok | every connection negotiated TLSv1.3; a message sent over SMTP STARTTLS is listed over IMAP STARTTLS and POP3 STARTTLS (at least two upgraded connections per protocol) |
| STARTTLS | proxy | JVM default trust | `TLS_FAILED` | `TLS_FAILED` | `TLS_FAILED` | the upgrade is attempted, the handshake refused |
| STARTTLS | plain 3143 / 3110 / 3025 (no upgrade offered) | `trustAll` | `TLS_FAILED` | `TLS_FAILED` | `TLS_FAILED` | "the server does not offer the required STARTTLS upgrade"; nothing reaches the server in the clear |
| none | plain | - | ok | ok | ok | GreenMail only; `tls: none` is refused for real providers by the presets |
| SSL | plain ports | `trustAll` | `TLS_FAILED` | `TLS_FAILED` | `TLS_FAILED` | a ClientHello meets a plain-text greeting |
| none | SSL ports (2 s timeouts) | - | `TIMEOUT` | `TIMEOUT` | `TIMEOUT` | the server waits for a ClientHello, the client for a greeting; retryable |

Two POP3 rows failed on the first run with `AUTH_FAILED`: Angus's `POP3Store.protocolConnect`
wraps every `EOFException` of the connect phase in `AuthenticationFailedException(message)`,
so "STLS required but not supported" and a timed-out greeting ("Read timed out") reached the
mapper as authentication failures before any credential was sent. `ExceptionMapper` now maps an
`AuthenticationFailedException` that mentions STARTTLS / STLS to `TLS_FAILED` and one that
mentions a timeout to `TIMEOUT` (retryable); real credential refusals keep `AUTH_FAILED`.

## Device rows (`TlsDeviceTest`)

`theDefaultClientEnablesTls12` reads the platform's default `SSLSocket`;
`theMailCoreConnectsToATls12OnlyServerAndTls13TellsTheApiLevel` runs `session.test` of the mail
core (`tls: ssl`, `trustAll`) against the loopback server limited to one version.

| Device | API | Default enabled protocols | TLS 1.2-only server | TLS 1.3-only server |
| --- | --- | --- | --- | --- |
| AVD_API_24 (x86) | 24 | TLSv1, TLSv1.1, TLSv1.2 (SSLv3 supported but disabled) | ok, negotiated TLSv1.2 (249 ms) | the platform cannot host TLS 1.3 |
| Sony G8441 | 28 | TLSv1, TLSv1.1, TLSv1.2 | ok, negotiated TLSv1.2 (537 ms) | the platform cannot host TLS 1.3 |
| Redmi 22120RN86C | 33 | TLSv1, TLSv1.1, TLSv1.2, TLSv1.3 | ok, negotiated TLSv1.2 (357 ms) | ok, negotiated TLSv1.3 |

Conclusion: TLS 1.2 is enabled by default on API 24 and used by the mail core without any
`ssl.protocols` setting; TLS 1.3 comes with API 29 and is picked automatically where the server
offers it. The plugin sets no protocol list, so the platform's defaults (and its deprecations of
TLS 1.0 / 1.1 from API 29 on) apply. Real providers all negotiate TLS 1.2 or 1.3 (roadmap P6
provider matrix).

## Reproducing

```
./gradlew.bat :mail-core:test --tests "*TlsMatrixTest" --tests "*ExceptionMapperTest"
adb -s <serial> uninstall io.github.supermonster003.autojs6.plugin.angus.mail
ANDROID_SERIAL=<serial> ./gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=io.github.supermonster003.autojs6.plugin.angus.mail.TlsDeviceTest
adb -s <serial> logcat -d -s TlsDeviceTest ScriptedServer
```

The API 24 run used `emulator -avd AVD_API_24 -no-window -no-audio -no-boot-anim -gpu
swiftshader_indirect`, shut down afterwards.
