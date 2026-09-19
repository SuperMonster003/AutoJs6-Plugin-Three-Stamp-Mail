# P4 settings and account store evidence (P4.1 account store, P4.3 alias sessions)

Evidence for roadmap P4.1 (the encrypted saved-account store) and P4.3 (`openSession` by
alias, `listSavedAccounts`, the `savedAccounts` capability) collected on 2026-09-19 in this
repository (Gradle 9.5.0, AGP 9.3.2, Kotlin 2.3.20, JDK 21, Windows 11). The settings page
(P4.2), the host entry (P4.4), the release history (P4.5) and the battery-optimization guide
(P4.6) extend this file as they land.

No real account is involved in this phase: every device test uses a throw-away directory under
`noBackupFilesDir`, a test Keystore alias and a scripted loopback IMAP server, so the store of the
installed plugin is never touched and no log line carries a secret.

## Devices

| Serial | Device | Android | Role |
| --- | --- | --- | --- |
| emulator-5554 | AVD_API_24 (x86) | 7.0 (API 24) | oldest supported API, Keystore AES-GCM |
| bek749scrwv4wo8h | Redmi 22120RN86C | 13 (API 33) | current Keystore implementation |

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

## Credential audit

- `grep` of the new sources for `Log.` / `println`: none; the store never logs.
- Error messages name only aliases (never secrets); `MailException` messages of storage /
  cipher failures are fixed strings; `EncryptedAccountStoreTest.storageFailuresNeverLeakTheirText`
  proves a storage exception text does not reach the caller.
- `AccountEnvelope.toString` / `SavedAccount.toString` print alias and sizes only.

## Reproducing

```
./gradlew :app:testDebugUnitTest --tests "io.github.supermonster003.autojs6.plugin.angus.mail.store.*"
ANDROID_SERIAL=<serial> ./gradlew :app:connectedDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=io.github.supermonster003.autojs6.plugin.angus.mail.AccountStoreDeviceTest"
```

The connected run uninstalls the plugin from the device afterwards; reinstall
`app/build/outputs/apk/debug/autojs6-plugin-angus-mail-v1.0.0.apk` before host smokes.
