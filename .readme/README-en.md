<!--suppress HtmlDeprecatedAttribute, HttpUrlsUsage -->

<div align="center">
  <p>
    <picture>
      <source srcset="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap-night/ic_launcher.png?raw=true" media="(prefers-color-scheme: dark)" />
      <img src="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/res/mipmap/ic_launcher.png?raw=true" alt="autojs6-plugin-angus-mail-ic-launcher" border="0" width="128" />
    </picture>
  </p>

  <p>Sends, receives, searches, and watches mail from AutoJs6 scripts over IMAP, POP3, and SMTP</p>

  <p>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases"><img alt="GitHub release (latest by date)" src="https://img.shields.io/github/v/release/SuperMonster003/AutoJs6-Plugin-Angus-Mail?label=Release"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/issues"><img alt="GitHub closed issues" src="https://img.shields.io/github/issues/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=A24232&label=Issues"/></a>
    <a href="https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE"><img alt="GitHub License" src="https://img.shields.io/github/license/SuperMonster003/AutoJs6-Plugin-Angus-Mail?color=534BAE&label=License"/></a>
  </p>
</div>

******

### Languages

******

The current README.md supports the following languages:

- [简体中文 [zh-Hans]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hans.md)
- [繁體中文 (香港) [zh-Hant-HK]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-HK.md)
- [繁體中文 (台灣) [zh-Hant-TW]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-zh-Hant-TW.md)
- English [en] # current
- [Français [fr]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-fr.md)
- [Español [es]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-es.md)
- [日本語 [ja]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ja.md)
- [한국어 [ko]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ko.md)
- [Русский [ru]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ru.md)
- [العربية [ar]](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/.readme/README-ar.md)

******

### Introduction

******

Angus Mail gives AutoJs6 scripts a global `mail` object for sending messages, listing and searching mailboxes, reading bodies, downloading attachments, managing flags and folders, and watching a folder for new mail. It is built on [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, the reference implementation of Jakarta Mail, and speaks IMAP, POP3, and SMTP over TLS.

All mail traffic stays inside the plugin process. AutoJs6 discovers the plugin through its Binder service, hands over the account a script provides (or an alias saved on the plugin's settings page), and receives JSON results and attachment streams; the host itself contains no mail code. Credentials stay in memory for the lifetime of a session unless you choose to save an account in the plugin.

******

### Status

******

Version 1.0.0 is in development: the repository skeleton, the mail core with its local-server tests, and the plugin identity for the AutoJs6 plugin center are in place, while the Binder contract, the script API, and the settings page follow the phases of [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Requires AutoJs6 6.8.0 (build 5282) or later.

******

### Features

******

The plugin provides the following capabilities:

- Send: plain text or HTML, several recipients, attachments and inline images, custom headers, and priority, with the sent copy stored on the server when the provider does not do it itself.
- Receive: list a folder page by page, search on the server (with a client-side fallback for providers that reject non-ASCII searches), read text and HTML bodies, and download attachments straight into the script's working directory.
- Organize: mark messages as read or flagged, move, copy, delete, expunge, and create, rename, or delete folders; POP3 accounts get the read-only subset.
- Watch: receive new-mail events through IMAP IDLE, with polling as the fallback for servers and POP3 accounts that do not support it, for as long as the script runs.
- Providers: presets for Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, and Aliyun fill in hosts, ports, and encryption; any field can be overridden for other servers.
- Authentication: passwords and provider authorization codes, or XOAUTH2 access tokens supplied by the script together with a refresh callback.

******

### Usage

******

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) on a device with AutoJs6 build 5282 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `Angus Mail` is recognized, and enable it.
3. Prepare the account: turn on IMAP or POP3 in your mail provider's settings and obtain an authorization code or app password (QQ, 163, 126, Gmail, iCloud), or an OAuth 2.0 access token (Outlook.com).
4. Call `mail.connect(...)` in a script, or save the account on the plugin's settings page and connect by alias.

******

### Quick Start

******

A script that sends a report, reads unread mail with attachments, and waits for a verification code:

```js
let client = mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' });

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
```

******

### Permissions and Security

******

The plugin follows explicit boundaries:

- The Binder entry points are protected by the `org.autojs.permission.PLUGIN` signature permission, so only AutoJs6 can reach them; the plugin exports no other components.
- The INTERNET permission serves only the IMAP, POP3, and SMTP connections to the servers a script names; the plugin makes no other requests and collects no data.
- Passwords and tokens travel from the script to the plugin in dedicated Binder fields, never appear in logs, JSON documents, error messages, or crash reports, and stay in memory only for the lifetime of a session. Accounts saved on the settings page are encrypted with an Android Keystore key and excluded from backups.
- Connections use TLS by default (SSL or STARTTLS as the provider requires); plain connections and self-signed certificates must be requested explicitly per account.

Only obtain the plugin from the official [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) page or the AutoJs6 plugin center. Packages from unknown sources may fail host verification or carry risks even when the version number looks identical.

******

### Plugin Interface

******

The following information targets AutoJs6 host and plugin developers; the host uses these identifiers to discover the plugin and negotiate compatibility:

```text
application id: io.github.supermonster003.autojs6.plugin.angus.mail
plugin id: angus-mail
engine: mail
variant: default
service action: org.autojs.plugin.MAIL
service category: mail
info action: org.autojs.plugin.INFO
aidl interface: org.autojs.plugin.mail.api.IMailPlugin
minimum host build: 5282 (6.8.0)
```

`AngusMailPluginService` implements the host mail-api contract `org.autojs.plugin.mail.api.IMailPlugin` and answers `org.autojs.plugin.MAIL` (category `mail`). `AngusMailPluginInfoService` answers `org.autojs.plugin.INFO` with PluginInfo. `WakeActivity` lets the host activate the plugin.

******

### Roadmap

******

The plugin's plans and progress are maintained as a checkable list in ROADMAP.md, organized by phase with acceptance criteria and evidence levels. Unchecked items express intent rather than current capabilities; discussion via Issues is welcome.

- [View ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md)

******

### Release History

******

#### v1.0.0

_2026/09/19_

- `Hint` P0 development preview: repository skeleton, the mail core with local-server tests, and the plugin identity for the AutoJs6 plugin center. The Binder contract, the script API, and the settings page follow the phases of ROADMAP.md.
- `Feature` Plugin identity `angus-mail` (engine `mail`) with the INFO service, the Wake Activity, and the `org.autojs.plugin.MAIL` service whose `IMailPlugin` Binder answers plugin info, capabilities, provider and saved-account listings, and the session envelope (operations follow in P2)
- `Feature` Mail core on Eclipse Angus Mail: session properties for IMAP / POP3 / SMTP with SSL or STARTTLS, password and XOAUTH2 authentication, SMTP sending and IMAP inbox listing, verified against a local GreenMail server
- `Feature` Mail core account layer (roadmap P2.1): account options with provider presets for Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina and Aliyun, per-protocol timeouts, `tls.trustAll`, the IMAP `ID` command and a redacted `debug` trace; sessions connect lazily, drop idle connections and reconnect after a loss; `session.test` answers through the Binder with capabilities and round-trip times per endpoint
- `Feature` Sending (roadmap P2.2): `mail.send` with to / cc / bcc / replyTo, text and HTML bodies (`multipart/alternative`), attachments and inline images (`multipart/mixed` / `multipart/related`) read from descriptors passed by the host, custom headers, priority, `inReplyTo` / `references` and a date; recipient, attachment and header limits, header injection refused; `saveToSent` appends a copy through IMAP only when the provider does not file one itself; `messages.append` stores drafts and returns the UID; verified against QQ Mail and Gmail on real devices
- `Feature` Receiving (roadmap P2.3): `folders.list` (tree with special-use roles from LIST / XLIST attributes or conventional names, optional counts), `folders.status` / `create` / `delete` / `rename`; `messages.list` with UID cursors (`before` / `after`), `order` and `unseenOnly`, fetching envelopes only (`hasAttachments` from BODYSTRUCTURE); `messages.search` compiles the query JSON (`from` / `to` / `subject` / `body` / `text` / dates / flags / sizes / `header` / `messageId` / `uid` with `and` / `or` / `not`) into IMAP SEARCH and filters on the client when the server refuses (`fallback`); `messages.get` with text and HTML bodies (HTML-only mail gets a derived text), every header, the attachment list with `partId`, an inline budget (`bodyTruncated` / `bodyParts`) and `includeRaw`; `attachments.download` and `messages.raw` stream into the host's descriptor with progress; `messages.setFlags` / `move` / `copy` / `delete` / `expunge`; charset recovery for GBK / GB 18030 / ISO-2022-JP, undeclared and unknown charsets, raw 8-bit headers and RFC 2231 / 2047 file names, safe file names; the IMAP `ID` command goes out on every connection (163 / 126 refuse unidentified connections) and a server that cannot parse `UID EXPUNGE` gets a plain `EXPUNGE`; verified against QQ Mail, 163 Mail and Gmail on real devices
- `Feature` POP3 (roadmap P2.4): accounts with `receive: "pop3"` read their single `INBOX` through the same operations, with the UIDL string as `uid`: `folders.list` answers `INBOX` (count on request), `messages.list` pages by UIDL cursors over headers only (`TOP`), `messages.search` filters headers on the client newest first and stops once `limit` messages match (at most 200 candidates, one `TOP` round trip each), `messages.get` / `messages.raw` / `attachments.download` download the whole message, `messages.delete` issues `DELE` and commits when the mailbox closes; flags, move, copy, expunge, append, `folders.status` / `create` / `delete` / `rename`, `unseenOnly` and body searches answer `UNSUPPORTED_OPERATION` before any connection; verified against QQ Mail on a real device
- `Feature` Binder session control (roadmap P2.5): only the installed AutoJs6 host signed with the same key as this plugin may open sessions or list saved accounts (a `SecurityException` otherwise, the same rule as the MCP Server plugin); request and response envelopes are limited to `MAX_ENVELOPE_BYTES` and error messages to `MAX_ERROR_MESSAGE_BYTES`; each session runs its calls in order and queues up to `MAX_QUEUED_CALLS` behind the one in flight, refusing the next with `LIMIT_EXCEEDED`; `cancel` answers a queued call at once and breaks the call in flight by closing its sockets, so a stalled server no longer costs the read timeout; `close` answers every pending call with `SESSION_CLOSED`; `getStatus` reports `queued` and `active`; the op table names the receive protocols of every op, so POP3 accounts are refused before arguments are parsed; capabilities advertise `append` and `clientSearchFallback`
- `Feature` README, plugin-center instructions, and changelog in 10 languages
- `Fix` Provider presets (roadmap P3.2): 163 Mail and 126 Mail keep a server copy of every message sent through SMTP, so `autoSavesSent` is now true for both and the default `saveToSent` no longer appends a second copy to `已发送` (verified with a real 163 account: a message sent with `saveToSent: false` appeared in the sent folder a few minutes later)
- `Fix` SDK XML v4 parsing warnings with AGP 9.1 and APK native alignment checks incorrectly triggered by JVM unit-test assembly tasks, using shared build plugins 1.8.3
- `Improvement` Error mapping: a POP3 server that refuses the mailbox after login because POP access is disabled for the account (Gmail answers STAT with `[SYS/PERM] Your account is not enabled for POP access`) now yields `UNSUPPORTED_OPERATION` with a message naming the cause instead of a retryable `IO_FAILED` "I/O failed"
- `Improvement` Provider presets: Sina Mail names its sent folder (`已发送`) and notes that the server keeps no copy of sent mail and refuses IMAP CREATE (folders exist only through the web UI); the 126 Mail server copy of sent mail is now verified with a real account
- `Dependency` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) with Angus Activation 2.0.3 and Jakarta Activation API 2.1.4
- `Dependency` GreenMail 2.1.13 added for the JVM mail core tests (test scope only)
- `Dependency` Added `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5282, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`
- `Dependency` Added `mail-api.aar` (AutoJs6 module `plugin-api/mail-api`, host build 6.8.0 / 5282, MPL 2.0) as the mail Binder contract (six AIDL interfaces, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), hash-locked in `locks/host-api-aars.lock`

##### For more release history

* [CHANGELOG.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/app/src/main/assets/doc/CHANGELOG-en.md)

******

### Build and Verification

******

This section targets developers who want to build the plugin from source; regular users can simply install the prebuilt APK from the Releases page.

Build a debug APK:

```powershell
.\gradlew.bat :app:assembleDebug
```

Run JVM unit tests and build the instrumentation test APK:

```powershell
.\gradlew.bat :mail-core:test :app:testDebugUnitTest :app:assembleDebugAndroidTest
```

Build the release APK:

```powershell
.\gradlew.bat :app:assembleRelease
```

Collect the release artifact and append the version and CRC32 digest to its file name:

```powershell
.\gradlew.bat :app:appendDigestToReleasedFiles
```

Verify that the multilingual documentation sources and generated artifacts are in sync (also enforced by CI):

```powershell
py .python\generate_markdown.py --check
```

Building requires JDK 21 or later and Android SDK 37; Gradle and plugin versions are managed centrally by `version.properties` and `io.github.supermonster003.autojs6-platform-versions`.

******

### Localization and Docs Generation

******

```text
.readme/common.json
.readme/lang_*.json
.readme/template_readme.md
.readme/template_plugin_instruction.md
.changelog/lang_*.json
.changelog/template_changelog.md
.python/generate_markdown.py
app/src/main/assets/doc/CHANGELOG-*.md
app/src/main/res/raw-*/plugin_instruction.md
```

The language JSON files under `.readme/` and `.changelog/` are the single source for the README, the plugin-center instructions, and the changelog. Always edit those JSON sources and rerun `py .python/generate_markdown.py`; generated README, `plugin_instruction.md`, and changelog artifacts are never edited by hand. Run `py .python/generate_markdown.py --check` to verify all generated artifacts.

******

### License

******

The project code is licensed under the [Mozilla Public License 2.0](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/LICENSE). Third-party components and their licenses are listed in [Third-Party Notices](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md).

******

### Links

******

- AutoJs6 project: https://github.com/SuperMonster003/AutoJs6
- AutoJs6 documentation: https://docs.autojs6.com
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
