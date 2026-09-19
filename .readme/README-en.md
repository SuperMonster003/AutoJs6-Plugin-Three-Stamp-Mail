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
- Watch: receive new-mail events for as long as the script runs, through IMAP IDLE where the server really pushes and by polling (60 s by default, adjustable) where it does not: QQ and Sina accept IDLE but stay silent, 163 and 126 have no IDLE, and POP3 accounts are always polled; the watch survives a lost network and a restarted plugin process.
- Providers: presets for Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, and Aliyun fill in hosts, ports, and encryption; any field can be overridden for other servers.
- Authentication: passwords and provider authorization codes, or XOAUTH2 access tokens supplied by the script together with a refresh callback.

******

### Usage

******

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) on a device with AutoJs6 build 5282 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `Angus Mail` is recognized, and enable it.
3. Prepare the account: turn on IMAP or POP3 in your mail provider's settings and obtain an authorization code or app password (QQ, 163, 126, Gmail, iCloud), or an OAuth 2.0 access token (Outlook.com).
4. Call `mail.connect(...)` in a script, or save the account on the plugin's settings page (its launcher icon, or AutoJs6 developer options > Mail account settings) and connect by alias.

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
- The REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission only backs the guide button on the settings page: it shows whether the system may pause the plugin in the background and, on request, opens the system dialog; the plugin never asks on its own, and no feature depends on the exclusion. The P5 watch matrix measured what the exclusion is for: with the screen off for a while (Doze) Android freezes the network of background apps, a watch loses its connection, its reconnects time out and new mail is reported a few minutes after the device wakes up (about four minutes on Android 9; the plugin reconnects at once when Doze ends); with the exclusion the watch stays connected.

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
- `Feature` Saved-account store (roadmap P4.1): an account saved in the plugin keeps its non-secret document next to the password or access token encrypted with AES-256-GCM under an Android Keystore master key; the authenticated data binds the alias, the secret kind and the document, so a record that was edited or moved on disk no longer decrypts; records live in `noBackupFilesDir` (already excluded from backups), are published atomically under a file lock, and secrets only pass through `CharArray` / `ByteArray` buffers that are wiped afterwards; aliases are trimmed, NFC-normalized and case-insensitive
- `Feature` Saved-account sessions (roadmap P4.3): `openSession` accepts the alias form (`accountAlias`) and decrypts the secret inside the plugin process, so `mail.connect('alias')` never carries a credential over the Binder; `listSavedAccounts` returns the alias, address, user, provider, authentication, receive protocol, endpoints and default mark of every saved account without any secret; the capability set now advertises `savedAccounts`
- `Feature` Settings screens (roadmap P4.2): the launcher icon opens an accounts page that lists every saved account with its address, provider, receive protocol and authentication and offers edit, test connection, set or clear default and remove; the account editor prefills a provider preset or takes custom IMAP / POP3 / SMTP servers with explicit encryption and ports, reads the password or access token straight from the field into a `CharArray` that is wiped after use, keeps the stored secret when the field is left empty while editing, and runs `session.test` against the typed servers before saving, showing per-protocol results and durations without writing anything to disk; the screens follow the AutoJs6 host theme, night mode and language, and a recreated editor restores every field except the secret
- `Feature` Settings entry (roadmap P4.3): the AutoJs6 host opens the accounts page through the exported `org.autojs.plugin.MAIL_SETTINGS` activity, which requires the plugin permission, accepts only the parameterless request and finishes at once; the capabilities advertise `mailSettingsVersion` 1; the launcher entry itself stays free of the permission
- `Feature` Release history (roadmap P4.5): the settings and about screens open a release-history page rendered from the bundled changelog of the current language (English when no translation ships), one card per version with its date and tagged entries; the plugin performs no update check of its own, updates follow the AutoJs6 plugin center
- `Feature` Battery-optimization guide (roadmap P4.6): the settings page shows whether the system may pause this plugin in the background (`PowerManager.isIgnoringBatteryOptimizations`) and, after explaining what changes, opens the system dialog through `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; the manifest therefore declares `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`; nothing is requested on start and no feature depends on the exclusion
- `Feature` New-mail watches, IMAP IDLE (roadmap P5): the mail core watches a folder on a connection of its own with `IMAPFolder.idle`, renews the IDLE every 24 minutes, fetches what arrived by UID (envelopes, or the body on request) exactly once, reconnects after a loss with an exponential backoff (1 s to 5 min, with jitter) and reports a `resync` when the folder's UIDVALIDITY changed; a server without IDLE or three failed IDLEs in a row switch the watch to polling with a `mode` event; a session holds at most `MAX_WATCHES_PER_SESSION` watches and closes them with itself
- `Feature` New-mail watches, polling (roadmap P5): a watch with `mode: "poll"` diffs the folder by UID every `pollIntervalMs` (default 60 s, at least `MIN_POLL_INTERVAL_MS`, at most one hour) on a connection kept between polls; POP3 accounts are always polled, by UIDL with one login per poll so the maildrop stays free in between, and report additions only, never deletions; the first poll takes a snapshot and reports no backlog
- `Feature` New-mail watches over the Binder (roadmap P5): `IMailSession.watch` opens a watch on the mail core's watcher and answers at once (null with the reason in the session status when the session is closed, `MAX_WATCHES_PER_SESSION` is reached, the options are unusable or the host's callback is already dead); events reach the host's `oneway` callback from a delivery thread with the host's `generation` and a `seq` counted from 1, a queue of `MAX_WATCH_QUEUE` events collapses into one `resync` when the host stops draining, an event beyond `MAX_ENVELOPE_BYTES` goes out without its body or as a `resync`, `stop`, the session's close and the host's death end the watch with a single `closed` event, a change or loss of the default network reconnects the running watches at once, and the capabilities now advertise `idle`
- `Fix` Provider presets (roadmap P3.2): 163 Mail and 126 Mail keep a server copy of every message sent through SMTP, so `autoSavesSent` is now true for both and the default `saveToSent` no longer appends a second copy to `已发送` (verified with a real 163 account: a message sent with `saveToSent: false` appeared in the sent folder a few minutes later)
- `Fix` SDK XML v4 parsing warnings with AGP 9.1 and APK native alignment checks incorrectly triggered by JVM unit-test assembly tasks, using shared build plugins 1.8.3
- `Fix` Account editor (roadmap P4.7): the whole form now stays out of the Android Autofill framework, so no password manager offers to capture the authorization code; HyperOS on API 35 otherwise raised its "save account and password" sheet when the editor closed after saving.
- `Fix` Watches on QQ, Sina, 163 and 126 (mail roadmap P5 device matrix): the provider presets gained `idlePush` (catalog version 2) and `mode: auto` now polls on these four from the start instead of idling, because QQ and Sina accept IMAP IDLE but never push while a client idles (real accounts, 2026-09-19: no untagged response within 10 minutes; Sina also drops the connection after 60 s) and 163 and 126 have no IDLE at all; an explicit `mode: 'idle'` still idles. The matrix itself (QQ on the API 24 emulator and two Sony phones, 163 on a Redmi: plugin kill, network loss, Wi-Fi to cellular, forced Doze) is recorded in `docs/dev/p5-watch-evidence.md` with the smoke script `docs/smoke/watch.js` and the driver `.python/run_watch_matrix.py`; the same matrix showed that Doze freezes the network of background apps (a watch's reconnects time out and new mail was reported about four minutes after the wake on Android 9), so the plugin now reconnects its watches the moment the device leaves Doze and the battery guide of the settings page says what the exclusion is for
- `Fix` TLS matrix (mail roadmap P6): implicit SSL, STARTTLS (through a STARTTLS front in front of GreenMail), plain text, the self-signed certificate with and without `tls.trustAll`, a trusted certificate with a mismatching host name, the wrong mode for a port and a port that offers no upgrade are now tested for IMAP, POP3 and SMTP (`TlsMatrixTest`, `docs/dev/p6-tls-matrix.md`); the run showed that Angus's POP3 store reports a missing STLS upgrade and a timed-out greeting as authentication failures, which the error mapper now reports as `TLS_FAILED` and `TIMEOUT` instead of `AUTH_FAILED`; `TlsDeviceTest` confirms on API 24, 28 and 33 that the mail core connects to a TLS 1.2-only server with the platform defaults and negotiates TLS 1.3 from API 29 on
- `Fix` Charset matrix (mail roadmap P6): GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR and UTF-8 in subjects, display names, bodies and both file-name forms are now asserted declared, undeclared and mis-declared (`CharsetMatrixTest`, `docs/dev/p6-charset-matrix.md`) and checked on API 24 / 28 / 33 (`CharsetDeviceTest`); the run fixed two decoding gaps: a body or header declared `us-ascii` / ISO-8859-1 but carrying UTF-8 or GB 18030 bytes came out as Latin-1 mojibake (Jakarta maps `us-ascii` to the never-failing ISO-8859-1) and now goes through the guessing chain, and raw ISO-2022-JP subjects and bodies without an encoded word are recognized by their escape sequences; Big5 and EUC-KR still need their declaration (their byte pairs are valid GB 18030)
- `Fix` Provider matrix (mail roadmap P6): QQ, 163, 126, yeah.net and Sina were run through send, listing, Chinese search on the server and on the client, body, attachment bytes, flags, folder creation, move, watch and POP3 with real accounts (`ProviderMatrixProbe`, `.python/run_provider_matrix.py`, `docs/dev/p6-provider-matrix.md`) and the preset notes record what differs: QQ answers OK with no hits to a Chinese SEARCH (use `fallback: 'always'`), rewrites the Message-ID and the ENVELOPE To name, refuses CREATE and keeps its own sent copies unaddressable by UID for a while; 163 answers text searches for recent mail with no hits; Sina accepts only ALL, SINCE and flag search keys; none of the five stores custom keywords. The run also fixed a client defect: a POP3 XOAUTH2 login refused as a SASL continuation (Gmail) was reported as a retryable `IO_FAILED` because Angus Mail ignores the refusal; the mail core now verifies the login and answers `AUTH_FAILED` (`Pop3OAuthScriptedTest`). Gmail (expired token), Outlook.com and iCloud (no accounts) stay unverified
- `Fix` Lifecycle matrix (mail roadmap P6): a script that holds a session and a watch was ended eight ways on a Redmi (API 33): normal exit, `exit()` with the watch and the client open, `engines.stopAll()`, host force-stopped, plugin force-stopped, plugin upgraded in place, plugin disabled, plugin uninstalled; in every case the plugin's connections, bindings and file descriptors were back to their pre-script state within 30 s and the host never connected to the server itself (`docs/dev/p6-lifecycle-matrix.md`, `.python/run_lifecycle_matrix.py`, `docs/smoke/lifecycle.js`). The matrix found one leak and it is fixed: Angus Mail creates a thread pool per socket for the write timeout and only shuts it down when the TLS socket is closed through that wrapper, which Android skips once the plain socket underneath was closed by a cancel or a watch stop, so every such connection left a thread behind for the life of the process; the mail core now hands Angus one shared daemon timer (`WriteTimeouts`, `WriteTimeoutExecutorTest`)
- `Improvement` Error mapping: a POP3 server that refuses the mailbox after login because POP access is disabled for the account (Gmail answers STAT with `[SYS/PERM] Your account is not enabled for POP access`) now yields `UNSUPPORTED_OPERATION` with a message naming the cause instead of a retryable `IO_FAILED` "I/O failed"
- `Improvement` Provider presets: Sina Mail names its sent folder (`已发送`) and notes that the server keeps no copy of sent mail and refuses IMAP CREATE (folders exist only through the web UI); the 126 Mail server copy of sent mail is now verified with a real account
- `Improvement` Provider presets: the Yahoo Mail and Aliyun Mail notes now state that these presets are unverified against a real account (none is available to the project), so their sent-copy behavior follows public documentation.
- `Improvement` Secret audit (mail roadmap P6): the mail core and the app were searched for logging, console output, Jakarta debug switches and every place the secret is materialized; the result (`docs/dev/p6-secret-audit.md`) is enforced by `SecretAuditTest`, which fails the build on any log or debug statement, pins `reveal()` to the three Jakarta authentication calls, and checks that the Jakarta session never debugs, that secrets inside the account JSON are refused without being echoed, and that value objects, the exception mapper and the protocol trace mask the secret in every form it travels in
- `Improvement` Hostile input (mail roadmap P6): a message document is now bounded whatever the message carries (the four address lists share 500 entries, addresses and names are cut to 320 characters, the subject, ids and header values to 4096, the headers map to 64 KiB, the MIME tree to a depth of 32 and 256 parts, and a body of unknown size is read no further than the inline budget), so one hostile mail can no longer block a listing page with `LIMIT_EXCEEDED`; damaged base64, unknown transfer encodings and boundary-less or empty multiparts are decoded leniently instead of being taken for a lost connection; deep or unparsable multiparts stay downloadable as one leaf; `HostileInputTest` and `HostileInputGreenMailTest` (17 cases, `docs/dev/p6-hostile-input.md`) cover deep and wide MIME, 20000 recipients, header bombs, missing content types, invalid base64, recursive `message/rfc822`, hostile file names, size mismatches and invalid UTF-8
- `Improvement` Performance baseline (mail roadmap P6): a seeded local server (10,000-message inbox, a 50 MiB attachment) is run through listing, searching, download, send and a one-hour IDLE standby on the JVM, a Redmi (API 33) and a Sony (API 28) with the figures in `docs/dev/p6-performance-baseline.md`. Two things changed because of it: the IMAP fetch size grows from 64 KiB to 1 MiB, so a 50 MiB attachment takes 69 round trips instead of about 1,100 (10 MiB/s on the loopback, the download still holds about 2 MiB of memory), and a client-side search stops at `limit` matches instead of scanning every candidate. The standby shows the plugin at about 32 MiB PSS, 3.5 s of CPU and 40 KB of traffic per hour, and that a long-running watch needs the host's foreground service (without it Android 9 killed the host and the plugin as cached empty processes after 31 minutes)
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
