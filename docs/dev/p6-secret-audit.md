# P6 secret audit (credentials never reach logs, JSON, `toString` or exception messages)

Audit of roadmap P6 "秘密审计", run on 2026-09-19 against plugin build 36 (`0a4a449`) and the
host mail packages of `ade3bd21c`. The manual part is the grep below; the enforcing part is
`mail-core/src/test/.../error/SecretAuditTest.kt` (7 cases), which the release gate runs with
every `:mail-core:test`.

## What was searched

Sources: `mail-core/src/main/kotlin` (51 files) and `app/src/main/java` (42 files). Patterns:
`Log.[vdiwe](`, `println(`, `printStackTrace(`, `System.out` / `System.err`, `Timber.`,
`android.util.Log`, `setDebug(`, `setDebugOut(`, the string `"mail.debug"`, `debug = true`,
`reveal()`, `override fun toString`, and every file mentioning `secret`, `password` or
`accessToken`.

| Pattern | Hits | Notes |
| --- | --- | --- |
| `Log.*(`, `Timber.`, `android.util.Log` | 0 | the plugin logs nothing at all; the only diagnostics are the redacted protocol trace (D28) and the exceptions mapped for the host |
| `println(`, `System.out` / `System.err`, `printStackTrace(` | 0 | |
| `setDebug(`, `"mail.debug"`, `debug = true` | 0 | `MailSessionFactory.session` sets `debug = false` on every Jakarta session; `MailSessionProperties.build` never emits a `mail.debug*` key |
| `reveal()` | 5 | `MailSecret.reveal` itself, the `Redactor` (to know what to mask), and the three Jakarta authentication calls in `MailSessionFactory` (`PasswordAuthentication`, `Store.connect`, `Transport.connect`); the app never calls it |
| `override fun toString` | 10 | `MailSecret` prints `MailSecret(***)`; `MailEndpoint`, `MailException` (code and scrubbed message), `OutgoingAddress`, `OutgoingAttachment`, `MessageUid`, `UidSet`, `SocketRegistry.factory`, `SavedAccount` (alias, secret kind, default flag) and `AccountEnvelope` (sizes only) print no credential; `MailAccount` is a data class without a secret field |

## The secret's path

1. Script to host: `mail.connect({password})` or `{accessToken}` becomes the host's
   `MailAccountSpec`, whose `toString` masks both fields (`password=***`); the host's log lines
   (`MailPluginHost`) name components and counts only.
2. Host to plugin: the Binder `Bundle` carries the secret under `KEY_SECRET_PASSWORD` or
   `KEY_SECRET_ACCESS_TOKEN` and nothing else (`MailBundles.validateAccount` refuses a document
   that carries both, or an alias together with an inline secret). `MailBundles.secret` reads it
   once and `MailPluginBinder.openSession` wraps it in a `MailSecret` (a `CharArray` copy,
   wiped by `clear()` when the session closes).
3. Alias sessions (P4.3): the secret never crosses the Binder; `EncryptedAccountStore.withSecret`
   decrypts (Android Keystore AES-256-GCM, `noBackupFilesDir`) into a `CharArray` for the
   duration of the callback and wipes it afterwards.
4. Plugin to server: `MailSessionFactory` hands the secret to Jakarta Mail through the session
   `Authenticator` and the `connect(host, port, user, password)` calls; Jakarta sends it inside
   `LOGIN` / `AUTH PLAIN` / `AUTH LOGIN` / `AUTHENTICATE XOAUTH2` on the TLS connection the
   account demanded (`MailSessionProperties`: no fallback from SSL / STARTTLS to plain, no
   fallback from XOAUTH2 to a password mechanism).

## Where it is kept out

- JSON: `MailAccountOptions.Fields.FORBIDDEN` (`password`, `accessToken`, `tokenProvider`,
  `token`, `secret`) is refused with `INVALID_ARGUMENT` naming the key, never the value
  (`SecretAuditTest.secretsInsideTheAccountJsonAreRefusedWithoutEchoingThem`). The account
  document scripts see (`SessionTestResult`, `client.account`) and the saved-account listing
  (`SavedAccountsDocument`) are rendered from `MailAccount`, which has no secret field.
- Exceptions: every throwable becomes a `MailException` through `ExceptionMapper`, whose
  message and details pass the `Redactor`: the raw secret, its Base64, the Base64 of the SASL
  PLAIN payload and of the XOAUTH2 payload are replaced by `***`, any Base64 run of 32 or more
  characters is masked, and the text is cut to `MAX_ERROR_MESSAGE_BYTES`
  (`theRedactorMasksEveryFormTheSecretTravelsIn`, `theExceptionMapperScrubsMessagesDetailsAndToString`).
  `MailBundles.error` copies code, message, details and `retryable` from the mapped exception
  only.
- Protocol trace (D28): `ProtocolTrace` records command names, outcomes and durations that the
  mail core itself writes; every line passes the `Redactor` and is cut to
  `MAX_TRACE_LINE_LENGTH`; the trace is off unless the account asked for `debug: true`, and
  Jakarta's own `mail.debug` stays off even then and even with a JVM system property
  `mail.debug=true` (`theJakartaSessionNeverDebugsEvenWhenTheAccountAsksForTheTrace`).
- Settings screens (P4.2): the secret field has `isSaveEnabled = false` (no instance-state
  copy), is read into a `CharArray` only for the store or the connection test
  (`ConnectionTester.start` wipes its copy after the session took it) and the editor opts out
  of autofill (`IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS`, P4.7).
- Sources: `theSourcesCarryNoLoggingNoConsoleOutputAndNoJakartaDebugSwitch` fails the build if
  any of the patterns above returns; `theSecretLeavesMailSecretOnlyForTheThreeJakartaAuthenticationCalls`
  pins the `reveal()` call sites.

## Residual exposure (documented, not fixable in the plugin)

- Binder `Bundle.getString` and Jakarta's `PasswordAuthentication` / `connect` parameters are
  immutable `String`s; they live in the plugin process until garbage collected. `MailSecret`
  wipes its own `CharArray`, but the `String` handed to Jakarta cannot be wiped.
- A server that echoes the credential in a reply is masked by the `Redactor` only in the forms
  listed above; an unusual encoding (for example a hex dump) would pass. No provider in the
  matrix does this.

## Reproducing

```
./gradlew.bat :mail-core:test --tests "*SecretAuditTest"
grep -rn "Log\.\|println\|printStackTrace\|setDebug\|mail.debug" mail-core/src/main app/src/main --include=*.kt
grep -rn "reveal()" mail-core/src/main app/src/main --include=*.kt
```
