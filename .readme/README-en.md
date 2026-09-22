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

Version 1.2.1 adds the browser sign-in for Google and Microsoft accounts (roadmap P9) on top of the background watches of 1.1.0 (roadmap P8); every item of phases P0 to P8 shipped with 1.0.0 to 1.1.0, with evidence in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Requires AutoJs6 6.8.0 (build 5282) or later; the "On mail arrived" task needs the host build with mail contract version 2; the full script API reference is in the [AutoJs6 documentation](https://docs.autojs6.com/#/mail).

******

### Features

******

The plugin provides the following capabilities:

- Send: plain text or HTML, several recipients, attachments and inline images, custom headers, and priority, with the sent copy stored on the server when the provider does not do it itself.
- Receive: list a folder page by page, search on the server (with a client-side fallback for providers that reject non-ASCII searches), read text and HTML bodies, and download attachments straight into the script's working directory.
- Organize: mark messages as read or flagged, move, copy, delete, expunge, and create, rename, or delete folders; POP3 accounts get the read-only subset.
- Watch: receive new-mail events for as long as the script runs, through IMAP IDLE where the server really pushes and by polling (60 s by default, adjustable) where it does not: QQ and Sina accept IDLE but stay silent, 163 and 126 have no IDLE, and POP3 accounts are always polled; the watch survives a lost network and a restarted plugin process.
- Background watches: the Watches page of the settings keeps IMAP IDLE or polling connections to saved accounts in a foreground service while no script runs, records every new message, and wakes the "On mail arrived" task of AutoJs6 for the chosen watch, optionally filtered by sender and subject, with the message in `engines.myEngine().execArgv.mail`.
- Providers: presets for Gmail, Outlook.com, Microsoft 365, QQ, 163, 126, iCloud, Yahoo, Sina, and Aliyun fill in hosts, ports, and encryption; any field can be overridden for other servers.
- Authentication: passwords and provider authorization codes, or XOAUTH2 access tokens supplied by the script together with a refresh callback.
- Browser sign-in: a Gmail, Outlook.com or Microsoft 365 account can be added by signing in with the Google or Microsoft account in the system browser from the plugin settings (OAuth 2.0 authorization code with PKCE); the plugin keeps the refresh token encrypted on the device, renews the access token before every session and shows the sign-in state with "Sign in again" and "Revoke" on the accounts page. Scripts keep connecting by alias and never see a token.

******

### Usage

******

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) on a device with AutoJs6 build 5282 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `Angus Mail` is recognized, and enable it.
3. Prepare the account: turn on IMAP or POP3 and SMTP in the provider's web settings and obtain an authorization code (QQ, 163, 126, Sina) or an app password (Gmail, iCloud, Yahoo); the login password itself is usually not accepted. Gmail, Outlook.com and Microsoft 365 accounts can instead be signed in with the Google or Microsoft account in the browser from the plugin settings (choose "Sign in with Google / Microsoft (browser)" as the authentication), or given an OAuth 2.0 access token obtained elsewhere.
4. Call `mail.connect(...)` in a script, or save the account on the plugin's settings page (its launcher icon, or AutoJs6 developer options > Mail account settings) and connect by alias.
5. To run a script on new mail without keeping one running: add a watch on the plugin's Watches page (settings > Watches: account alias, folder, mode, filters), allow the notification when asked, then create a task in AutoJs6 (long-press the script > timed task > run on broadcast > On mail arrived) and pick the watch; the task needs an AutoJs6 build with mail contract version 2.

******

### Provider Preparation

******

Every provider needs IMAP (or POP3) and SMTP turned on in its web settings first, and an authorization code, app password or access token in place of the login password; the essentials per preset (the value of `provider`):

- QQ Mail (`qq`): enable the IMAP/SMTP service in the web account settings and generate an authorization code, which is used as `password`.
- 163 / 126 / yeah.net (`163`, `126`; yeah.net uses the `163` preset with its hosts overridden): enable the services on the POP3/SMTP/IMAP page of the web settings and generate an authorization code; POP3 is enabled separately, otherwise POP3 refuses the code that IMAP and SMTP accept. The servers require every IMAP connection to send the `ID` command first (or answer `Unsafe Login`); the plugin does that itself.
- Sina Mail (`sina`): enable the IMAP/SMTP service in the web client settings and use the authorization code. The server keeps no copy of sent mail (the plugin appends one to the sent folder), refuses folder creation over IMAP, and text searches run on the client.
- The simplest path is the browser sign-in on the plugin settings page ("Sign in with Google (browser)"), which needs no app password and renews its token by itself. Otherwise: Gmail (`gmail`): with 2-Step Verification on, generate an app password in the Google account and use it as `password`, or supply an OAuth 2.0 access token with the `https://mail.google.com/` scope (`accessToken` plus `tokenProvider`); folders live under the `[Gmail]` namespace and new mail is pushed by IDLE. The project verified the real account with a token.
- The simplest path is the browser sign-in on the plugin settings page ("Sign in with Microsoft (browser)"), which obtains and renews the token by itself. Otherwise: Outlook.com / Hotmail (`outlook`) and Microsoft 365 (`office365`): Microsoft has disabled basic authentication for personal accounts, so app passwords are refused on IMAP, POP3 and SMTP and the `outlook` preset accepts only an OAuth 2.0 access token (`accessToken` plus `tokenProvider`); work or school accounts (`office365`) take a password or a token, but tenant policy may disable IMAP, POP3 or SMTP AUTH. The token needs the delegated scopes `IMAP.AccessAsUser.All`, `POP.AccessAsUser.All` and `SMTP.Send` of `https://outlook.office.com/` (a personal account was verified with such a token: IMAP, POP3, SMTP and IDLE push); on some newer personal mailboxes Microsoft has SMTP AUTH disabled (`535 5.7.139`) and no user setting turns it on.
- iCloud (`icloud`): generate an app-specific password in the Apple account; there is no POP3 service.
- Yahoo (`yahoo`) and Aliyun personal mail (`aliyun`): generate an app password or authorization code; these two presets follow the public documentation and are unverified because the project has no test account.

Other servers leave `provider` out and give `host`, `port` and `tls` (`ssl`, `starttls` or `none`) for `imap` (or `pop3`) and `smtp`; any field of a preset can be overridden as well. All options are described in [MailAccountOptions](https://docs.autojs6.com/#/mailAccountOptionsType), and `mail.providers.list()` shows the built-in presets.

******

### Quick Start

******

A script that connects by alias, sends a report, reads unread mail with attachments, watches for a verification code and searches asynchronously:

```js
// A saved alias keeps the credential inside the plugin; an inline account works as well:
// mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' })
let client = mail.connect('work');

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    let full = m.load();
    full.attachments.forEach(a => a.download(files.join(files.cwd(), 'mail-attachments')));
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => { if (/code/i.test(m.subject)) console.log(m.text); });
watch.on('error', e => console.warn(e.code, e.message));

// Every network method also has an Async form; every failure is a MailError with a code.
mail.setDefault(client);
mail.searchAsync({ subject: 'invoice', since: '2026-09-01' }).then(list => console.log(list.length, list.fallback));
```

******

### Saved Accounts and Aliases

******

An alias is an account saved inside the plugin. After the account is entered, tested and saved on the plugin's settings page (its launcher icon, or AutoJs6 developer options > Mail account settings), the password or token is stored encrypted with an Android Keystore key in the plugin's private directory and excluded from backups; scripts then connect with `mail.connect('alias')`, and the credential passes neither through the script nor over Binder.

The settings page can mark one account as the default; the entries returned by `mail.accounts.list()` carry `default: true` for it, and `mail.accounts.has(alias)` checks whether an alias exists. Create one client per alias when several accounts are used at once; after `mail.setDefault(client)`, forwarded methods such as `mail.fetch(...)` act on the default client.

******

### Compatibility

******

The findings below come from the real-account matrix of roadmap phase P6 (2026-09-19 and 09-20: every provider sent itself one message with a Chinese subject, body, display names and file name, then every operation was verified) and from the TLS, charset, lifecycle and performance matrices:

- QQ Mail: sending, listing, bodies, attachments, flags, move (`MOVE`) and POP3 all pass; a Chinese search is answered by the server with zero hits instead of an error, so it needs `fallback: 'always'`; the Message-ID of outgoing mail is rewritten by the server; folder creation is refused; watching polls (IDLE never pushes), and new mail becomes visible on the server 15-40 s after delivery.
- 163 / 126 / yeah.net: all pass; the server keeps the sent copy; no IDLE, so watching polls; 163 answers text searches for recent mail with zero hits (126 and yeah.net are fine); spaces in the sender display name come back as underscores; POP3 on yeah.net has to be enabled separately in the web settings.
- Sina Mail: passes; the server accepts only ALL, SINCE and flag conditions, so text searches fall back to client-side filtering automatically; no IDLE; folder creation is refused; the sent copy is appended by the plugin.
- Gmail: every row passes with an OAuth 2.0 token, new mail is pushed by IDLE (about 30 s, Gmail's own notification cadence); server searches including Chinese all hit (the plugin does not enable `UTF8=ACCEPT`); custom IMAP keywords are stored (the only one of the six); the POP3 view does not include mail the account sent to itself.
- Outlook.com / Hotmail: with an OAuth 2.0 token every row passes on one personal account (2026-09-21): folder roles from the conventional names, the server keeps the sent copy and rewrites the Message-ID, `MOVE` and folder creation work, POP3 logs in with the two-line `AUTH XOAUTH2`, and IDLE pushes within about 10 s (the fastest of the seven providers); a server search for a Chinese subject hits but can take minutes; app passwords stay refused on IMAP, POP3 and SMTP (`AUTH_MECHANISM_UNSUPPORTED`), and on some newer personal mailboxes Microsoft has SMTP AUTH disabled (`535 5.7.139`); iCloud, Yahoo and Aliyun have no test account and their presets are unverified.
- TLS and charsets: implicit SSL, STARTTLS, plain text, self-signed certificates (with and without `tls.trustAll`), host name mismatches and port mode mismatches are tested one by one on IMAP, POP3 and SMTP, and failures map to distinguishable codes such as `TLS_FAILED` and `TIMEOUT`; subjects, display names, bodies and file names in GB18030, GBK, GB2312, Big5, ISO-2022-JP, EUC-KR and UTF-8 are asserted declared, undeclared and misdeclared.
- Devices and lifecycle: an Android 7.0 (API 24) emulator plus Sony (Android 9) and Redmi (Android 13) phones; connections, bindings and threads are released under eight ways of ending a script (normal exit, `exit()`, `engines.stopAll()`, force-stopping the host or the plugin, in-place upgrade, disabling and uninstalling the plugin); with the screen off (Doze) a watch loses its connection and recovers once the device wakes up, and the settings page can request the battery optimization exemption for uninterrupted watching.
- Performance baseline: listing, searching, downloading, sending and a one-hour IDLE standby against a local 10000-message inbox with a 50 MiB attachment on the JVM, the Redmi and the Sony are recorded in `docs/dev/p6-performance-baseline.md`; message documents are bounded (addresses, headers, the MIME tree and inline bodies have limits), so hostile input cannot blow up a session.

******

### FAQ

******

- **How do I troubleshoot `AUTH_FAILED`?** Make sure you use the authorization code or app password rather than the login password, and that the protocol is enabled in the web settings (IMAP and POP3 are enabled separately); call `client.test()` to see the result and error code of the receive endpoint and of SMTP separately. For token accounts `AUTH_FAILED` usually means an expired token; with a `tokenProvider` the plugin refreshes it and retries once. The `code`, `details` and `retryable` fields of the error say whether a retry is worthwhile.
- **163 / 126 answer `Unsafe Login`?** NetEase's IMAP servers refuse connections that have not sent the `ID` command; the plugin sends `ID` right after login on every IMAP connection, so this should not occur. If it still does, re-enable the IMAP service in the web settings and generate a new authorization code.
- **A Chinese search finds nothing?** Servers treat non-ASCII searches differently: Sina rejects them (the plugin falls back to client-side filtering by itself), while QQ and 163 answer zero hits without an error (the default `fallback: 'client'` does not trigger). Use `fallback: 'always'` on those accounts and narrow the window with `since` or `limit`; client-side body filtering fetches every candidate and can be slow on a large mailbox.
- **`mail.connect` succeeded but the first `fetch` fails?** `connect` only opens the plugin session and does not contact the mail server; the first network method logs in (SMTP on the first send). Call `client.test()` to verify an account up front.
- **The watch stops after the screen goes off?** Android's Doze freezes the network of background apps, so the watch loses its connection and new mail is reported a few minutes after the device wakes up (the plugin reconnects as soon as Doze ends). For uninterrupted watching, use the guide button on the settings page to request the battery optimization exemption; a watch only lives while the script runs and closes when the script exits.
- **How do I connect Outlook.com / Hotmail?** Microsoft has disabled basic authentication for personal accounts, so the preset accepts no password. Save the account on the plugin settings page and choose "Sign in with Microsoft (browser)" as the authentication: the Microsoft sign-in page opens in the browser, only the authorization code returns to the plugin, and the plugin renews the access token by itself; scripts then connect by alias. A script can still pass an access token obtained elsewhere as `accessToken` and refresh it through `tokenProvider`. If the account page says "sign in again", the refresh was refused (the sign-in was revoked or expired): open the account's menu and choose "Sign in again".
- **What can a POP3 account do?** Only `INBOX` exists and `uid` is the UIDL string; listing, reading, downloading, deleting and polling watches work, while flags, move, copy, append, expunge and folder management answer `UNSUPPORTED_OPERATION`; searches run on the client with envelope conditions only.

******

### Permissions and Security

******

The plugin follows explicit boundaries:

- The Binder entry points are protected by the `org.autojs.permission.PLUGIN` signature permission, so only AutoJs6 can reach them; the plugin exports no other components.
- The INTERNET permission serves the IMAP, POP3, and SMTP connections to the servers a script names and, for accounts signed in through the browser, HTTPS requests to the token endpoints of Google (`oauth2.googleapis.com`) and Microsoft (`login.microsoftonline.com`) only: to exchange the authorization code, to renew an access token that is about to expire, and to revoke a Google token on request. The plugin makes no other request and collects no data.
- Passwords and tokens travel from the script to the plugin in dedicated Binder fields, never appear in logs, JSON documents, error messages, or crash reports, and stay in memory only for the lifetime of a session. Accounts saved on the settings page are encrypted with an Android Keystore key and excluded from backups. The tokens of a browser sign-in are stored the same way: the refresh token never leaves the device, only the access token is handed to a mail session, and neither AutoJs6 nor a script can read them; "Revoke sign-in" discards them at once.
- Connections use TLS by default (SSL or STARTTLS as the provider requires); plain connections and self-signed certificates must be requested explicitly per account.
- The REQUEST_IGNORE_BATTERY_OPTIMIZATIONS permission only backs the guide button on the settings page: it shows whether the system may pause the plugin in the background and, on request, opens the system dialog; the plugin never asks on its own, and no feature depends on the exclusion. The P5 watch matrix measured what the exclusion is for: with the screen off for a while (Doze) Android freezes the network of background apps, a watch loses its connection, its reconnects time out and new mail is reported a few minutes after the device wakes up (about four minutes on Android 9; the plugin reconnects at once when Doze ends); with the exclusion the watch stays connected.
- Four permissions serve the background watches of version 1.1.0 and nothing else. FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE run the watch service (type `specialUse`, subtype `mail_background_watch`, because a mail watch is an open connection waiting for the server's push rather than a bounded data sync) with one low-priority notification; POST_NOTIFICATIONS is requested on the Watches page only when a watch is enabled, so that the notification can be shown on Android 13 and later; RECEIVE_BOOT_COMPLETED backs the boot switch of that page, which is off by default and enables the receiver only when turned on. The service starts only from the Watches page or from a host subscription, connects only to saved accounts, keeps the secrets inside the plugin process, and the wake-up broadcast to AutoJs6 carries the message envelope (never a body) and reaches only a receiver behind the PLUGIN signature permission.

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

#### v1.2.1

_2026/09/22_

- `Fix` The `closed` event of a watch on a session close always names the reason `closed`: the session's worker thread could stop some of the watches first with `session-closed` (seen once in the connected suite on the API 24 emulator).

#### v1.2.0

_2026/09/22_

- `Hint` The browser sign-in is offered only by builds that carry an OAuth 2.0 client id for the provider (the maintainer's registrations, read at build time from the git-ignored `oauth-clients.properties`); a build without them keeps the token and app-password paths and says so in the authentication dialog. The Google client needs the sensitive-scope verification of the Google Cloud project before the sign-in works for arbitrary accounts; until then Google limits it to test users of the project.
- `Feature` Browser sign-in for Google and Microsoft accounts (mail roadmap P9): the account editor offers "Sign in with Google (browser)" for the Gmail preset and "Sign in with Microsoft (browser)" for the Outlook.com and Microsoft 365 presets; the sign-in opens the provider's page in a Custom Tab (any browser as the fallback) with an OAuth 2.0 authorization-code request carrying PKCE (`S256`) and a random `state`, the redirect (`<applicationId>://oauth2/microsoft`, or the reversed Google client id scheme) lands on `OAuthRedirectActivity`, which hands it to the waiting sign-in screen; the screen refuses any redirect whose `state` does not match, exchanges the code at the token endpoint over HTTPS (`HttpsFormPoster`, the only HTTP client of the plugin) and prefills the address from the id token
- `Feature` Token storage and renewal: the tokens of a browser sign-in are one encrypted record of the new kind `OAUTH2` in the account store (never a Binder field, never in `mail.accounts.list()`), the account document carries an `oauth` object (`provider`, `authorizedAt`, `expiresAt`, `needsReauth`) that `mail.accounts.list()` reports; every session of such an alias (scripts, the connection test, background watches) takes its access token from `AccountSecrets`, which renews it through the refresh token when less than five minutes remain, serialized per account; a refused refresh (`invalid_grant`) marks the record `needsReauth`, fails the session with `AUTH_FAILED` ("sign in again") and the accounts page shows "sign in again" next to the account
- `Feature` Accounts page actions "Sign in again" (a new browser sign-in stored on the same record) and "Revoke sign-in" (the record's tokens are replaced at once by a revoked marker, Google is asked to revoke the refresh token, and the account stops working until a new sign-in); the `gmail`, `outlook` and `office365` presets' `authHint` names the browser sign-in first (`providers.json` version 4); 35 new strings in 11 languages; JVM tests for PKCE, the authorization request and redirect parsing, the token client against a scripted transport, the token document, the provider table, the build's clients and redirect URIs, `AccountSecrets` (refresh, refusal marking, revoked records, wiping) and the `oauth` object in the account options, the form and the accounts document

#### v1.1.0

_2026/09/21_

- `Hint` Background watches are new in 1.1.0 (mail roadmap P8): the Watches page of the settings keeps saved accounts watched in a foreground service while no script runs and wakes the "On mail arrived" task of AutoJs6. That task and its watch picker need the host build that carries mail contract version 2 (AutoJs6 6.8.0 after build 5282); on an older host the page says that AutoJs6 cannot be woken and the new messages only reach the watch's record list. The feature adds four permissions, each explained in the README security section: FOREGROUND_SERVICE and FOREGROUND_SERVICE_SPECIAL_USE (the watch service), POST_NOTIFICATIONS (its persistent notification, requested only when a watch is enabled) and RECEIVE_BOOT_COMPLETED (the boot switch of the Watches page, off by default).
- `Feature` Background watches (mail roadmap P8): the settings gained a Watches page that configures up to 16 watches (`MAX_TRIGGERS`) on saved accounts, each with a name, the account alias, the folder, the mode (auto, IDLE or polling with its interval) and optional sender and subject filters, stored as `mail-triggers/triggers.json` under the no-backup directory; the `specialUse` foreground service `MailWatchService` runs the enabled watches on the P5 watchers (IDLE where the server pushes, polling elsewhere, reconnects with backoff, an immediate reconnect on a network change) while no script runs and shows one low-priority notification; every new message is added to the watch's record list (the last 100 envelope summaries, `MAX_TRIGGER_RECORDS`, never a body) and the page shows the connection state, the last error, the records and a reconnect action; the boot switch (off by default) enables the `BOOT_COMPLETED` receiver that restarts the service after a reboot
- `Feature` Mail contract version 2 (`IMailPlugin.openTrigger` / `listTriggers`, `IMailTrigger`, `IMailTriggerCallback`, capability feature `backgroundWatch`): the host subscribes to a configured watch with a generation and an optional filter, gets the current status at once and then `onStatus` (stopped, connecting, connected or failed with the reason and the last error) and `onMail(generation, seq, event)` for every matching message, the `mail` event carrying the watch id, alias, address, folder, the message envelope and `receivedAt`; a watch takes at most 4 subscribers (`MAX_TRIGGER_SUBSCRIBERS`), a disabled or unknown watch and unusable options are refused with a `stopped` status whose reason is `refused`, `update` replaces the subscriber's filter and `stop` ends only the subscription; without a live subscriber every event goes to AutoJs6 as the explicit broadcast `org.autojs.autojs6.action.MAIL_TRIGGER` behind its `PLUGIN` signature permission, which starts the host's "On mail arrived" task even when no script is running (`MailTriggerBinderTest` on an API 37 AVD; `TriggerStoreTest`, `TriggerFilterTest`, `TriggerConfigTest`, `TriggerDocumentsTest`)
- `Feature` The mail core gained the trigger documents and rules shared by the page, the service and the Binder (`TriggerConfig`, `TriggerFilter` with case-insensitive sender and subject substrings, `TriggerOptions`, `TriggerStatusDocument`, `TriggerEventDocument`, `TriggerRecord`) and the ceilings `MAX_TRIGGERS`, `MAX_TRIGGER_SUBSCRIBERS`, `MAX_TRIGGER_RECORDS` and `MIN_TRIGGER_INTERVAL_MS` (3 s, the host's throttle per task), and the watchers report `onConnected` so that a background watch shows `connected` as soon as its folder is open

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
- Mail module documentation: https://docs.autojs6.com/#/mail
- Eclipse Angus Mail: https://eclipse-ee4j.github.io/angus-mail/
- Third-party notices: https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/THIRD_PARTY_NOTICES.md
