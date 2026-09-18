# Third-party notices

This file records third-party components shipped with or consumed by the Angus Mail plugin. The
plugin itself is licensed under the Mozilla Public License 2.0; the components below retain their
own licenses. Runtime dependencies are added to this list in the same commit that introduces them.

## AutoJs6 common plugin API

- Component: `common-plugin-api.aar` (Binder contract shared by AutoJs6 and its plugins: `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/common-plugin-api`), host build 5282 (6.8.0), commit `34d2c8fcd`, release build (byte-identical to the c0d5fecdb build)
- SHA-256: `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15` (pinned in `locks/host-api-aars.lock`)
- License: Mozilla Public License 2.0

## AutoJs6 mail plugin API

- Component: `mail-api.aar` (mail Binder contract of AutoJs6: `IMailPlugin`, `IMailSession`, `IMailCallCallback`, `IMailWatch`, `IMailWatchCallback`, `IMailSessionCallback`, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/mail-api`), host build 5282 (6.8.0), commit `34d2c8fcd`, release build
- SHA-256: `7b7f537a78d520231f90abf4b20f5012c919fef0f8fad03c46327cfdfe9d8821` (pinned in `locks/host-api-aars.lock`)
- License: Mozilla Public License 2.0

Both host AARs must come from the same host commit; restage and re-lock them together when the contract changes.

## Kotlin standard library

- Component: `org.jetbrains.kotlin:kotlin-stdlib` (provided through the Android Gradle Plugin built-in Kotlin support and the Kotlin JVM plugin of `:mail-core`)
- Source: <https://github.com/JetBrains/kotlin>
- License: Apache License 2.0

## Eclipse Angus Mail

- Component: `org.eclipse.angus:jakarta.mail` 2.0.5, the bundle of the Jakarta Mail 2.1 API (`jakarta.mail.*`) and the Angus Mail implementation (`org.eclipse.angus.mail.*`: IMAP, POP3, SMTP providers, MIME, XOAUTH2)
- Source: <https://github.com/eclipse-ee4j/angus-mail> (API: <https://github.com/jakartaee/mail-api>)
- License: Eclipse Public License 2.0, or GNU General Public License version 2 with the Classpath Exception, or Eclipse Distribution License 1.0 (BSD 3-Clause), at the recipient's option; see the upstream `LICENSE.md` and `NOTICE.md`. The plugin distributes the bundle under the Eclipse Distribution License 1.0.

## Eclipse Angus Activation

- Component: `org.eclipse.angus:angus-activation` 2.0.3 (the Jakarta Activation implementation: MIME type and mailcap registries resolved by Jakarta Mail)
- Source: <https://github.com/eclipse-ee4j/angus-activation>
- License: Eclipse Distribution License 1.0 (BSD 3-Clause)

## Jakarta Activation API

- Component: `jakarta.activation:jakarta.activation-api` 2.1.4 (`jakarta.activation.*`: `DataHandler`, `CommandMap`, `MailcapCommandMap`)
- Source: <https://github.com/jakartaee/jaf-api>
- License: Eclipse Distribution License 1.0 (BSD 3-Clause)

## kotlinx.serialization

- Component: `org.jetbrains.kotlinx:kotlinx-serialization-json` 1.11.0 (with `kotlinx-serialization-core`), used by `:mail-core` for the JSON envelopes of roadmap D14
- Source: <https://github.com/Kotlin/kotlinx.serialization>
- License: Apache License 2.0

## Android desugared JDK libraries

- Component: `com.android.tools:desugar_jdk_libs` 2.1.5 (`java.time` and other JDK APIs on API 24 and 25 devices)
- Source: <https://github.com/google/desugar_jdk_libs>
- License: GNU General Public License version 2 with the Classpath Exception

## Test-only dependencies

These libraries are used by the JVM and instrumentation test source sets only and are not shipped
in the APK.

- GreenMail (`com.icegreen:greenmail` 2.1.13, local SMTP / IMAP / POP3 server for the `:mail-core` tests): Apache License 2.0
- SLF4J (`org.slf4j:slf4j-nop` 2.0.19, silences GreenMail logging): MIT License
- JUnit 4 (`junit:junit`): Eclipse Public License 1.0
- AndroidX Test (`androidx.test:runner`, `androidx.test:rules`, `androidx.test.ext:junit`): Apache License 2.0
