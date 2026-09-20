Angus Mail gives AutoJs6 scripts a global `mail` object for sending messages, listing and searching mailboxes, reading bodies, downloading attachments, managing flags and folders, and watching a folder for new mail. It is built on [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, the reference implementation of Jakarta Mail, and speaks IMAP, POP3, and SMTP over TLS.

Version 1.0.0 is the first release: every item of roadmap phases P0 to P6 (the mail core, the Binder contract, the script API, the settings page with saved accounts, new-mail watching, and the TLS, charset, provider, lifecycle, hostile-input, secret-audit and performance matrices) is complete with evidence in [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md). Requires AutoJs6 6.8.0 (build 5282) or later; the full script API reference is in the [AutoJs6 documentation](https://docs.autojs6.com/#/mail).

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/releases) on a device with AutoJs6 build 5282 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `Angus Mail` is recognized, and enable it.
3. Prepare the account: turn on IMAP or POP3 and SMTP in the provider's web settings and obtain an authorization code (QQ, 163, 126, Sina), an app password (Gmail, iCloud, Yahoo) or an OAuth 2.0 access token (Outlook.com); the login password itself is usually not accepted.
4. Call `mail.connect(...)` in a script, or save the account on the plugin's settings page (its launcher icon, or AutoJs6 developer options > Mail account settings) and connect by alias.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Angus-Mail/blob/master/ROADMAP.md) for the connection guide and the current progress.
