# P4 settings and account store evidence (P4.1 account store, P4.2 settings screens, P4.3 alias sessions)

Evidence for roadmap P4.1 (the encrypted saved-account store), P4.2 (the settings screens) and
P4.3 (`openSession` by alias, `listSavedAccounts`, the `savedAccounts` capability) collected on
2026-09-19 in this repository (Gradle 9.5.0, AGP 9.3.2, Kotlin 2.3.20, JDK 21, Windows 11). The
host entry (P4.4), the release history (P4.5) and the battery-optimization guide (P4.6) extend
this file as they land.

No real account is involved in this phase: the store tests use a throw-away directory under
`noBackupFilesDir` and a test Keystore alias, the screen test uses the installed plugin's store
under a throw-away `ui-smoke-*` alias that is removed afterwards, and every server is a scripted
loopback IMAP or SMTP server (`ScriptedServers.kt`), so no log line carries a secret.

## Devices

| Serial | Device | Android | Role |
| --- | --- | --- | --- |
| emulator-5554 | AVD_API_24 (x86) | 7.0 (API 24) | oldest supported API, Keystore AES-GCM |
| bek749scrwv4wo8h | Redmi 22120RN86C | 13 (API 33) | current Keystore implementation; night-mode screenshots |
| BH900ASK9E | Sony G8441 | 9 (API 28) | light-mode screenshots of the settings screens only |

## P4.1 `AccountStore` (plugin `store/` package)

Shape (`app/src/main/java/.../store/`):

- `AccountAlias`: trim, NFC, lower-case (root locale), `^[\p{L}\p{N}][\p{L}\p{N}._-]{0,63}$`;
  anything else is `INVALID_ARGUMENT` with a bounded, control-character-free rendering of the
  rejected text.
- `AccountRecords`: `SavedAccount` (non-secret view), `AccountEnvelope` (defensive copies,
  `toString` prints sizes only), `AccountAssociatedData` (store domain + alias + secret kind +
  account JSON, length-prefixed), `AccountRecordCodec` (magic `AMAC`, version 1, AES-GCM
  algorithm id, secret kind, `updatedAt`, alias, JSON, 12-byte IV, ciphertext; every length
  bounded, trailing data refused, alias must be canonical).
- `AccountStore` / `EncryptedAccountStore`: `put` validates the document through
  `MailAccountOptions.parse` (secrets inside the JSON, unknown fields, unknown presets and
  auth mismatches are refused before anything is written), compacts it, UTF-8 encodes the
  `CharArray` secret strictly, encrypts under the AAD and publishes under the storage lock; the
  input `CharArray`, the plaintext bytes and the encoded record are wiped in `finally` blocks.
  `withSecret` decrypts for the duration of a synchronous callback and wipes afterwards.
  `list` only counts a record whose own alias reads back to the same bytes (a copy stored under
  another alias or a damaged file is left out); `get` reports a damaged record as `INTERNAL`.
  `setDefault` / `defaultAlias` keep one alias in a marker file; `remove` clears the mark.
  Storage and cipher failures map to `INTERNAL` with the fixed message
  "the saved-account store is unavailable" (the cause text never leaves the store).
- `AndroidKeystoreAccountCipher`: AES-256-GCM, key alias
  `io.github.supermonster003.autojs6.plugin.angus.mail.accounts.v1`, randomized encryption,
  no user authentication; `FileAccountRecordStorage`: `<noBackupFilesDir>/mail-accounts/`
  with `account-<sha256(alias)>.bin`, `default.alias` and `.records.lock`; JVM mutex + OS file
  lock, fsync + atomic rename + directory fsync, `PrivatePathGuard` against links and escapes.
  `AccountStores.of(context)` is the process-wide instance.

JVM tests (`app/src/test/.../store/`, `:app:testDebugUnitTest`, 2026-09-19):

| Class | Cases | Covers |
| --- | --- | --- |
| `AccountAliasTest` | 3 | trim / NFC / lower-case, refused shapes, bounded messages |
| `AccountRecordCodecTest` | 4 | round trip, damaged envelopes (magic, version, algorithm, kind, flags, truncated, trailing, non-canonical alias), size limits, AAD binding |
| `EncryptedAccountStoreTest` | 8 | ciphertext-only persistence and wiping, callback failure keeps its error, replace keeps the default mark, refused aliases / documents / secrets, tampering (flipped byte, record copied under another alias, edited document, undecodable file), default mark, fixed failure message, sorted list |

All 15 pass; the whole `app` unit suite is 48 cases / 0 failures (31 pre-existing, these 15 and the 2 of `SavedAccountsDocumentTest` below).

Device test `AccountStoreDeviceTest` (`:app:connectedDebugAndroidTest`, 2026-09-19):

| Case | AVD API 24 | Redmi API 33 | Checks |
| --- | --- | --- | --- |
| `keystoreRoundTripSharesFilesBetweenInstancesAndKeepsSecretsOutOfThem` | 368 ms | 147 ms | Keystore key generated on first `put`; one `account-*.bin` without the secret bytes and with the document bytes; no `.tmp` left; a second store instance over the same directory decrypts the record and its `setDefault` is seen by the first; `remove` deletes the file and the default mark |
| `aDeletedMasterKeyLeavesRecordsListableButUnreadableUntilTheyAreSavedAgain` | 41 ms | 9 ms | after `deleteKey()` the record still lists, `withSecret` is `INTERNAL` "cannot be read" without the old secret in the message, a new `put` re-keys and decrypts |
| `openSessionByAliasDecryptsInsideThePluginAndListsWithoutSecrets` (P4.3) | 324 ms | 275 ms | see below |

## P4.3 alias sessions and `listSavedAccounts`

- `MailPluginBinder(context, guard, accounts)`: `openSession` with `accountAlias` resolves the
  record through `accounts.withSecret`, normalizes the stored document like an inline one
  (`MailAccountOptions.parse` with the plugin's `ID` defaults) and copies the secret into the
  `MailSession` only; `MailBundles.validateAccount` refuses an alias accompanied by
  `accountJson`, `secretPassword` or `secretAccessToken` (`INVALID_ARGUMENT`); an unknown alias
  is `ACCOUNT_NOT_FOUND`, a malformed one `INVALID_ARGUMENT`, all through the session callback
  as before.
- `listSavedAccounts` renders `SavedAccountsDocument`: `{alias, address, user, name?, provider?,
  auth, receive, imap?, pop3?, smtp?, default, updatedAt}` per account, endpoints as
  `{host, port, tls}`; a record whose preset vanished lists `alias`, `address`, `error`,
  `default`, `updatedAt` (`SavedAccountsDocumentTest`, 2 cases).
- `AngusMailPlugin.FEATURES` now carries `savedAccounts`; `AngusMailPluginService` hands the
  process-wide store to the binder.

Device case `openSessionByAliasDecryptsInsideThePluginAndListsWithoutSecrets` (both devices):
a record `smoke` pointing at a scripted loopback IMAP server (`tls: none`) is saved; the
`accountsJson` of `listSavedAccounts` names the alias, address, `auth: password`,
`receive: imap` and the server port and does not contain the secret; `openSession` with the
alias `SMOKE` (case-insensitive) returns a session whose `session.test` answers `imap.ok = true`
and the server records a `LOGIN` line carrying the saved secret, while the response envelope
does not; `close` reports `closed`; alias + `secretPassword` -> `INVALID_ARGUMENT`, `nobody` ->
`ACCOUNT_NOT_FOUND`, `bad alias` -> `INVALID_ARGUMENT`, none of the status documents carrying
the secret.

Regression on the AVD after the change: `AngusMailPluginContractTest` (4) and
`MailSessionBinderTest` (3) pass; the installed service still answers `[]` for an empty store
and the capability array equals `AngusMailPlugin.FEATURES`.

Settings entry (the fourth P4.3 item, landed with P4.2): `MailSettingsActivity` is the exported,
`Theme.NoDisplay`, `excludeFromRecents` activity behind `org.autojs.permission.PLUGIN` that answers
`org.autojs.plugin.MAIL_SETTINGS` (`MailActions.OPEN_SETTINGS` on the host side); it forwards a
parameterless intent to `AccountsActivity` and finishes, and treats an intent with data, a clip or
extras as no request. The launcher entry keeps no permission (a permission on the LAUNCHER activity
hides the icon). The capabilities advertise `mailSettingsVersion = 1`. Device cases (both devices):
`AngusMailPluginContractTest.settingsEntryResolvesBehindThePluginPermission` (exactly one activity
resolves the action, with the permission, the NoDisplay theme and the recents flag; the launcher
intent resolves to `AccountsActivity` without a permission) and
`SettingsScreensDeviceTest.settingsEntryForwardsOnlyTheParameterlessAction` (an `ActivityMonitor` sees
`AccountsActivity` exactly once for the bare action and not at all for the same intent with an extra).

## P4.2 settings screens

Programmatic Android Views on the OpenCC UI kit (`ui/` package; decision D36, no Compose):
`AccountsActivity` (launcher entry, card list, popup menu with edit / test / default / remove),
`AccountEditorActivity` (form + collapsible IMAP / POP3 / SMTP server blocks, `ConnectionTester`
running `MailSession.test` on one worker thread with `abort` as cancel), `AppSettingsActivity`
and `AboutActivity` (host-following language, night mode and theme colour through
`AutoJs6HostSettingsContract`). `AccountFormPolicy` is pure Kotlin (`AccountFormTest`, 6 cases:
preset prefill, validation of alias / address / host / port / receive endpoint, JSON that omits
endpoints equal to the preset and `receive` for send-only accounts, round trip from JSON).

Secret handling in the editor: the password / token field has `isSaveEnabled = false`, is never
written to `onSaveInstanceState`, is read with `TextUtils.getChars` into a `CharArray` that
`ConnectionTester` and `AccountStore.put` wipe, and is cleared after a save; editing an account
with the field left empty copies the stored secret inside `withSecret` and never shows it.

Device test `SettingsScreensDeviceTest` (`:app:connectedDebugAndroidTest`, in-process
`ActivityScenario` against the installed plugin's real store, 2026-09-19):

| Case | AVD API 24 | Redmi API 33 | Checks |
| --- | --- | --- | --- |
| `editorSavesTestsAndReopensAnAccountWithoutExposingTheSecret` | 2.87 s | 6.75 s | a custom-server form pointing at scripted loopback IMAP and SMTP servers (`tls: none`) is typed with the secret; `recreate()` restores alias and hosts but leaves the secret field empty; "Test connection" makes the IMAP server record a `LOGIN` with the secret and the SMTP server an `AUTH`, the result dialog names IMAP and SMTP and does not contain the secret; "Save" closes the editor, the store holds the alias with `secretKind = PASSWORD`, the parsed document keeps address, display name, both ports and `receive = imap`, and `withSecret` returns the typed secret; reopening the editor for the alias shows the alias, an empty secret field with the keep-helper, and saving a changed display name keeps the stored secret; the accounts page lists the alias and address and no view contains the secret; `MailPluginBinder.openSession(alias)` + `session.test` answers `ok` through the scripted IMAP server without the secret in the response, `close` reports `closed` |

The whole connected suite (14 cases: `AccountStoreDeviceTest` 3, `AngusMailPluginContractTest` 4,
`MailCoreDeviceTest` 3, `MailSessionBinderTest` 3, `SettingsScreensDeviceTest` 1) passes on both
devices after the manifest gained the four activities; `wakeActivityFollowsTheHostActivationContract`
still resolves exactly one WAKE activity. Screenshots (`build/p4/shot-*.png`, not committed):
accounts empty state and editor on the AVD (API 24) and the Sony (API 28) in light mode, on the
Redmi (API 33) in night mode; provider dialog and the QQ preset prefill on the AVD.

## Credential audit

- `grep` of the new sources for `Log.` / `println`: none; the store never logs.
- Error messages name only aliases (never secrets); `MailException` messages of storage /
  cipher failures are fixed strings; `EncryptedAccountStoreTest.storageFailuresNeverLeakTheirText`
  proves a storage exception text does not reach the caller.
- `AccountEnvelope.toString` / `SavedAccount.toString` print alias and sizes only.
- The settings screens (`settings/`, `ui/`) contain no `Log.` / `println`; toasts, snackbars and
  dialogs show alias, address, host, port, error code and message only; `ConnectionTestDialog`
  renders `SessionTestResult` (no credential field exists on it); `SettingsScreensDeviceTest`
  asserts the secret is absent from the result dialog, the accounts page and the Binder response.

## Reproducing

```
./gradlew :app:testDebugUnitTest --tests "io.github.supermonster003.autojs6.plugin.angus.mail.store.*"
ANDROID_SERIAL=<serial> ./gradlew :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=io.github.supermonster003.autojs6.plugin.angus.mail.AccountStoreDeviceTest"
ANDROID_SERIAL=<serial> ./gradlew :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=io.github.supermonster003.autojs6.plugin.angus.mail.SettingsScreensDeviceTest"
```

The connected run uninstalls the plugin from the device afterwards; reinstall
`app/build/outputs/apk/debug/autojs6-plugin-angus-mail-v1.0.0.apk` before host smokes.
