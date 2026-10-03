# Third-party notices

This file records third-party components shipped with or consumed by 3-Stamp Mail. The
plugin itself is licensed under the Mozilla Public License 2.0; the components below retain their
own licenses. Runtime dependencies are added to this list in the same commit that introduces them.

## Project origins and acknowledgements

3-Stamp Mail is an independent AutoJs6 plugin, originally published as Angus Mail. Its mail
implementation uses Eclipse Angus Mail and the Jakarta Mail/Activation APIs; GreenMail provides
local test servers. We thank their developers and preserve the component names and licenses below.

The initial engineering references were [AutoJs6](https://github.com/SuperMonster003/AutoJs6),
[OpenCC](https://github.com/SuperMonster003/AutoJs6-Plugin-OpenCC) (View-based UI and build conventions),
[3-Stone AI](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stone-AI) (account storage and appearance),
[MCP Server](https://github.com/SuperMonster003/AutoJs6-Plugin-MCP-Server) (plugin skeleton and Binder bridge),
and [Pinyin4j](https://github.com/SuperMonster003/AutoJs6-Plugin-Pinyin4j) (contract tests), under MPL-2.0.
The planning record also evaluated [MailCore2](https://github.com/MailCore/mailcore2); it is not shipped.
The envelope artwork for this release was supplied by the maintainer.

The recorded implementation references are open-source projects. Mail-provider names identify
interoperability targets, not copied clients or endorsements. No closed-source design reference has
been identified in the project records reviewed for this release. Names and other third-party rights
remain with their owners; attribution does not itself grant additional rights. See the concise
[rights-concern process](RIGHTS_AND_TAKEDOWN.md) for corrections, replacement or removal requests.

## AutoJs6 common plugin API

- Component: `common-plugin-api.aar` (Binder contract shared by AutoJs6 and its plugins: `PluginInfo`, `IPluginInfoProvider`, `PluginActions`, `PluginCapabilityKeys`)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/common-plugin-api`), host build 5316 (6.8.0), commit `5b5841bfbf60c0000b525ca69dd906ade642e5ee`, release build
- SHA-256: `ee7eb7879a53506c4cca5e2d19d3058e28df2168fb33351a52302a3b9e532e15` (pinned in `locks/host-api-aars.lock`)
- License: Mozilla Public License 2.0

## AutoJs6 mail plugin API

- Component: `mail-api.aar` (mail Binder contract of AutoJs6: `IMailPlugin`, `IMailSession`, `IMailCallCallback`, `IMailWatch`, `IMailWatchCallback`, `IMailSessionCallback`, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`)
- Source: <https://github.com/SuperMonster003/AutoJs6> (`plugin-api/mail-api`), host build 5316 (6.8.0), commit `5b5841bfbf60c0000b525ca69dd906ade642e5ee`, release build
- SHA-256: `c7c629d2e0fe0b430c2c4b659414766abe134fd6350d0b885aa5c13c70732923` (pinned in `locks/host-api-aars.lock`)
- License: Mozilla Public License 2.0

Both host AARs must come from the same host commit; restage and re-lock them together when the contract changes.

## Kotlin standard library

- Component: `org.jetbrains.kotlin:kotlin-stdlib` (provided through the Android Gradle Plugin built-in Kotlin support and the Kotlin JVM plugin of `:mail-core`)
- Source: <https://github.com/JetBrains/kotlin>
- License: Apache License 2.0

## Eclipse Angus Mail

- Component: `org.eclipse.angus:jakarta.mail` 2.0.5, the bundle of the Jakarta Mail 2.1 API (`jakarta.mail.*`) and the Angus Mail implementation (`org.eclipse.angus.mail.*`: IMAP, POP3, SMTP providers, MIME, XOAUTH2)
- Source: <https://github.com/eclipse-ee4j/angus-mail> (API: <https://github.com/jakartaee/mail-api>)
- License: Eclipse Public License 2.0, with the upstream secondary-license option of GPL 2.0 with the Classpath Exception where its conditions apply. This plugin uses the EPL-2.0 option. Source and notices are available at [Angus Mail 2.0.5](https://github.com/eclipse-ee4j/angus-mail/tree/2.0.5), its [LICENSE](https://github.com/eclipse-ee4j/angus-mail/blob/2.0.5/LICENSE.md) and [NOTICE](https://github.com/eclipse-ee4j/angus-mail/blob/2.0.5/NOTICE.md), and [Jakarta Mail API 2.1.5](https://github.com/jakartaee/mail-api/tree/2.1.5). The parent's aggregate POM also lists EDL-1.0 for project components; that entry is not treated here as a blanket relicensing of the mail implementation. Upstream notices and the packaged license remain applicable.

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

## Android interface libraries

- AndroidX AppCompat 1.7.1 and Browser 1.10.0: <https://android.googlesource.com/platform/frameworks/support/>, Apache License 2.0.
- Material Components for Android 1.13.0: <https://github.com/material-components/material-components-android>, Apache License 2.0.

## Test-only dependencies

These libraries are used by the JVM and instrumentation test source sets only and are not shipped
in the APK.

- GreenMail (`com.icegreen:greenmail` 2.1.13, local SMTP / IMAP / POP3 server for the `:mail-core` tests): Apache License 2.0
- SLF4J (`org.slf4j:slf4j-nop` 2.0.19, silences GreenMail logging): MIT License
- JUnit 4 (`junit:junit`): Eclipse Public License 1.0
- AndroidX Test (`androidx.test:runner`, `androidx.test:rules`, `androidx.test.ext:junit`): Apache License 2.0
