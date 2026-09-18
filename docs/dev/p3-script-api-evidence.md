# P3 script API evidence (P3.1 connection and global object)

Evidence for roadmap P3.1 (the `mail` global, `MailClient`, `MailError`, argument
normalization, the connect smoke script), collected on 2026-09-18 in the host repository
`../AutoJs6` (Gradle 9.7.1, AGP 9.3, Kotlin 2.3, JDK 21, Windows 11) against plugin build 12
installed on the devices below. P3.2 (send and receive methods) and P3.3 (protocol document
table, host changelog, full host build) will extend this file.

Real accounts come from the git-ignored `mail-test-accounts.properties` and reach the device only
as instrumentation arguments; the runner `.python/run_host_script_smoke.py` masks every secret in
the Gradle output and checks the log afterwards (`report leak check: clean` on every run below).
Logs carry provider ids, address domains and durations only.

## Devices

| Device | API | Network | Used for |
| --- | --- | --- | --- |
| AVD_API_24 (x86 emulator) | 24 | host network | connect smoke against the loopback fake IMAP server, QQ Mail |
| Redmi 22120RN86C (bek749scrwv4wo8h) | 33 | Wi-Fi | connect smoke against the loopback fake IMAP server, QQ Mail |

## P3.1: host code

Everything lives in the host repository; the plugin needed no change.

| Area | Files | Notes |
| --- | --- | --- |
| Pure Kotlin (JVM-testable) | `runtime/api/mail/MailScriptOptions.kt`, `MailProviderCatalog.kt`, `MailScriptValues.kt` | `mail.connect` argument shapes (unknown keys refused by name, `password` / `accessToken` / `tokenProvider` rules, `auth` consistency, endpoint / timeout / tls / clientId shapes, secrets split from the `client.account` snapshot), the provider table (`list`, `get` by id, `resolve` by address domain), Gson tree to plain JVM values |
| Per-script owner | `runtime/api/mail/MailService.kt` | open clients, the default client, provider catalog (fetched once), saved accounts; closed from the `ScriptRuntime` exit hook so no plugin session outlives its script |
| Rhino boundary | `runtime/api/augment/mail/Mail.kt`, `MailClientNativeObject.kt`, `MailJsErrors.kt`, `MailPromises.kt`, `MailAsyncDispatcher.kt` | the `mail` global (`connect` / `connectAsync`, `setDefault`, `default`, `close`, `providers.*`, `accounts.*`, `MailError`, default-client forwarders that throw or reject `NO_DEFAULT_ACCOUNT`), `MailClient` (`account`, `isConnected`, `isClosed`, `test` / `testAsync`, `close`; sync via `runBlocking(scriptRuntime.coroutineContext)`, async via `ScriptAsyncDispatcher` and `ScriptPromiseAdapter`; `debug: true` trace lines to `console.verbose`), the script-visible `MailError` class (`instanceof Error` and `instanceof mail.MailError`, `code` / `details` / `retryable`, hidden `javaException`) |
| Registration | `runtime/ScriptRuntime.kt` | `Mail(this, mail).augment(target)` next to `Mediainfo`, `mail.close()` in the exit hook |
| Client protocol layer | `core/plugin/mail/MailJson.kt`, `MailBinders.kt`, `MailPluginHost.kt` | `Progress.debug` carries the redacted trace lines (decision D28); a refused `openSession` waits up to 3 s for the `closed` status of the oneway session callback (see lessons) |
| Protocol document | `docs/dev/mail-plugin-protocol-v1.md` | the `openSession` row notes the oneway wait |

`mail.connect(options)` opens the plugin session eagerly (`openSession`, no network); the first
operation connects, and `test()` reports an unreachable endpoint inside its result
(`ok: false`, `imap.error.code === 'CONNECT_FAILED'`) instead of throwing. `mail.connect('alias')`
reaches the plugin and comes back as `MailError` `ACCOUNT_NOT_FOUND` until roadmap P4.

## JVM tests (host `:app:testAppDebugUnitTest`)

| Class | Cases | Covers |
| --- | --- | --- |
| `runtime/api/mail/MailScriptOptionsTest` | 7 | alias form, password and token accounts, secret / auth consistency, address and unknown keys, endpoint / timeout / tls / clientId / debug shapes, `null` as absent |
| `runtime/api/mail/MailProviderCatalogTest` | 4 | empty or invalid catalogs, entries without an id, case-insensitive `get`, `resolve` by domain |
| `runtime/api/mail/MailScriptValuesTest` | 4 | blank / invalid JSON, key order and nesting, `Int` versus `Double`, scalars |
| `runtime/api/augment/mail/MailJsErrorsTest` (bare Rhino, ES6, interpreted) | 4 | one constructor per scope, `instanceof Error` / `MailError`, own keys, `toString`, JSON details versus text details, hidden `javaException`, `WrappedIllegalArgumentException` to `INVALID_ARGUMENT`, script values and `EcmaError` pass through, `jsException` |
| `core/plugin/mail/MailJsonTest` | 13 | one new case: `debug` lines of a progress document (null entries skipped, empty array and non-array ignored) |

`org.autojs.autojs.core.plugin.mail.*` stays at 64 passing cases after the `MailBinders` /
`MailPluginHost` change.

## Device smoke: `connect-smoke.js` against the loopback fake IMAP server

`MailScriptSmokeDeviceTest.scriptConnectsTestsAndClosesAgainstTheFakeServer` runs
`app/src/androidTest/assets/mail/connect-smoke.js` through the real script engine
(`scriptEngineService.execute`) with a prelude naming the port of `FakeImapServer` (moved out of
`MailPluginRoundTripTest` into its own file) and a closed loopback port. The script performs 75
checks and writes a JSON report into a runtime property:

- the global and its metadata: `mail.connect` / `mail.MailError`, `mail.default` empty,
  `providers.list()` non-empty, `providers.get('qq')`, `providers.get(unknown) === null`,
  `providers.resolve('someone@163.com').id === '163'`, unknown domain `null`,
  `accounts.list()` empty and `accounts.has('work') === false` before P4;
- refusals as `MailError`: `mail.test()` without a default (`NO_DEFAULT_ACCOUNT`),
  `mail.connect()` and bad shapes (`INVALID_ARGUMENT` from the argument guard and from
  `MailScriptOptions`), `mail.connect('no-such-alias')` (`ACCOUNT_NOT_FOUND` from the plugin),
  a closed port (`connect()` succeeds, `test()` reports `CONNECT_FAILED`, no password in the
  result, `close()` works);
- the fake server: `connect` / `client.account` without `password` / `test().ok` /
  `setDefault` / `mail.default` / `mail.test()` / `client.test('extra')` (`INVALID_ARGUMENT`);
- the async forms on the script thread: `testAsync`, `mail.testAsync`, `connectAsync` with
  `debug: true` (the trace line reaches `console.verbose`), `close()` then `SESSION_CLOSED` from
  `test()` and `testAsync()`, `setDefault(closed)` (`INVALID_ARGUMENT`), rejected
  `connectAsync('no-such-alias')` and `connectAsync({address: 'nobody'})`, `mail.close()` clearing
  the default and `NO_DEFAULT_ACCOUNT` afterwards (sync and async).

The test then asserts two IMAP connections on the fake server (one per client that probed, both
with `LOGIN`) and that the secret never enters the report.

| Device | Checks | `connect` | `test()` | `testAsync()` | Script |
| --- | --- | --- | --- | --- | --- |
| AVD_API_24 | 75 | 14 ms | 85 ms | 21 ms | 0.519 s |
| Redmi 22120RN86C | 75 | 61 ms | 137 ms | 63 ms | 1.480 s |

Debug trace line seen on both devices (`console.verbose`):
`+83ms imap connect 127.0.0.1:<port>/none password ok 73ms` (AVD) and
`+133ms imap connect 127.0.0.1:<port>/none password ok 98ms` (Redmi).

## Device smoke: real provider (`provider-smoke.js`)

`MailScriptSmokeDeviceTest.realProviderConnectAndTest` runs
`app/src/androidTest/assets/mail/provider-smoke.js` (`mail.connect({provider, address,
password})`, `test()`, `close()`) with `mail.smoke.provider` / `mail.smoke.address` /
`mail.smoke.password` instrumentation arguments and is skipped without them.

| Device | Provider | `connect` | `test()` | IMAP | SMTP | Leak check |
| --- | --- | --- | --- | --- | --- | --- |
| Redmi 22120RN86C | qq | 111 ms | 1678 ms | ok | ok | clean |
| AVD_API_24 | qq | 103 ms | 1031 ms | ok | ok | clean |

## Lessons

- The host's Rhino 2.0 snapshot derives `TopLevelScope` from `ScopeObject`, a
  `SlotMapOwner<VarScope>` that is not a `ScriptableObject`; a per-scope cache (the `MailError`
  constructor) must be keyed through `SlotMapOwner.associateValue`, otherwise every call
  re-evaluates the constructor and `instanceof` fails.
- `IMailSessionCallback` is `oneway`: the `closed` status of a refused `openSession` may land
  after the null return, so the host used to answer `PLUGIN_UNAVAILABLE` ("mail plugin refused
  the account") instead of the plugin's code. `MailSessionCallbackBinder.awaitClosed` (3 s)
  closes the gap.
- A `BaseFunction` body returning a `LinkedHashMap` hands the script a `NativeJavaObject`
  (`JSON.stringify` gives `undefined`); sync results go through `RhinoUtils.toJsValue` like the
  async dispatcher already does.
- The AGP test engine installs the host APK without `-r`. On the Redmi, `adb install -r` of the
  matching arm64 split made the engine skip the install by digest as in P1.3; on the API 24
  emulator the same recipe still failed with `INSTALL_FAILED_ALREADY_EXISTS`, and
  `adb uninstall org.autojs.autojs6` reported `DELETE_FAILED_INTERNAL_ERROR` while removing the
  package anyway, after which the engine installed fresh.

## Reproducing

```
cd ../AutoJs6
./gradlew :app:testAppDebugUnitTest --tests "org.autojs.autojs.runtime.api.mail.*" --tests "org.autojs.autojs.runtime.api.augment.mail.*" --tests "org.autojs.autojs.core.plugin.mail.*"
ANDROID_SERIAL=<serial> ./gradlew :app:connectedAppDebugAndroidTest "-Pandroid.testInstrumentationRunnerArguments.class=org.autojs.autojs.runtime.api.augment.mail.MailScriptSmokeDeviceTest"
cd ../AutoJs6-Plugin-Angus-Mail
python .python/run_host_script_smoke.py QQ_A <serial>
```
