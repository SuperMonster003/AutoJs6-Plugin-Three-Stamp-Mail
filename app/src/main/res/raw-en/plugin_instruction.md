# AutoJs6 3-Stamp Mail

3-Stamp Mail gives AutoJs6 scripts a global `mail` object for sending messages, listing and searching mailboxes, reading bodies, downloading attachments, managing flags and folders, and watching a folder for new mail. It is built on [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/) 2.0.5, the reference implementation of Jakarta Mail, and speaks IMAP, POP3, and SMTP over TLS.

Email operations, saved accounts, background watches and Google/Microsoft browser sign-in. Requires AutoJs6 6.8.0 build 5316 or later. See the [mail API documentation](https://docs.autojs6.com/#/mail).

### Usage

1. Install the plugin APK from [Releases](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/releases) on a device with AutoJs6 build 5316 (6.8.0) or later.
2. Open the AutoJs6 plugin center, confirm that `3-Stamp Mail` is recognized, and enable it.
3. Prepare the account: turn on IMAP or POP3 and SMTP in the provider's web settings and obtain an authorization code (QQ, 163, 126, Sina) or an app password (Gmail, iCloud, Yahoo); the login password itself is usually not accepted. Gmail, Outlook.com and Microsoft 365 accounts can instead be signed in with the Google or Microsoft account in the browser from the plugin settings (choose "Sign in with Google / Microsoft (browser)" as the authentication), or given an OAuth 2.0 access token obtained elsewhere.
4. Call `mail.connect(...)` in a script, or save the account on the plugin's settings page (its launcher icon, or AutoJs6 developer options > Mail account settings) and connect by alias.
5. To run a script on new mail without keeping one running: add a watch on the plugin's Watches page (settings > Watches: account alias, folder, mode, filters), allow the notification when asked, then create a task in AutoJs6 (long-press the script > timed task > run on broadcast > On mail arrived) and pick the watch; the task needs an AutoJs6 build with mail contract version 2.

See the [project README](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail) and [ROADMAP.md](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/ROADMAP.md) for the connection guide and the current progress.


Thanks to [Eclipse Angus Mail](https://eclipse-ee4j.github.io/angus-mail/), Jakarta Mail/Activation and GreenMail, and to AutoJs6, OpenCC, 3-Stone AI, MCP Server and Pinyin4j for the implementation and engineering references. [Sources and licenses](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/THIRD_PARTY_NOTICES.md#project-origins-and-acknowledgements). This independent project is not endorsed by those developers; names and rights remain with their owners. [Rights concerns and cooperation](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail/blob/master/RIGHTS_AND_TAKEDOWN.md).
