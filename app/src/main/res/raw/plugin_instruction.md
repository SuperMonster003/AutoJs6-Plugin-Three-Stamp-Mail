3-Stamp Mail gives AutoJs6 scripts a global `mail` object that sends mail, searches and reads messages, downloads attachments, manages flags and folders, and watches for new mail over IMAP, POP3 and SMTP. Accounts can be saved in the plugin, and scripts connect to them by alias.

### Getting started

1. Turn on IMAP or POP3 and SMTP in your provider's web settings and get an authorization code (QQ, 163, 126, Sina) or an app password (Gmail, iCloud, Yahoo); the login password itself is usually not accepted.
2. Open 3-Stamp Mail from the launcher, tap "Add account" on the "Saved accounts" page, pick a "Provider", fill in the email address, an "Alias" and the authorization code, then save.
3. Tap the menu button next to the account and choose "Test connection" to make sure the servers can be reached.
4. Connect to the account in an AutoJs6 script with `mail.connect('alias')`, as shown below.

### Main features

- **Send and manage mail**: send messages with attachments, search by subject, date and more, read bodies, download attachments, mark messages as read and manage folders.
- **Browser sign-in**: for Gmail, Outlook.com and Microsoft 365 accounts, set "Authentication" to "Sign in with Google (browser)" or "Sign in with Microsoft (browser)", sign in in the browser and save; no authorization code is needed.
- **Background watches**: in "Settings" > "Background watches", tap "Add watch" and pick an account and a folder, and the plugin keeps it connected while no script is running. Then long-press a script in AutoJs6, choose "Timed task", and under "Broadcast task" pick "On mail arrived (mail plugin background watch)"; the script then runs for every new message.
- **Account safety**: passwords and tokens are stored encrypted in the plugin; scripts connect by alias and never see them.

### Using it in scripts

Use the global `mail` object directly in AutoJs6 scripts; no import is needed. The example connects to a saved account by alias, sends a message, reads unread mail and watches the inbox:

```js
let client = mail.connect('work');

client.send({ to: 'you@example.com', subject: 'Report', text: 'See the attachment', attachments: ['/sdcard/report.xlsx'] });

client.fetch({ unseenOnly: true, limit: 10 }).forEach(m => {
    console.log(m.subject);
    client.markRead(m);
});

let watch = client.watch('INBOX', { fetchBody: true });
watch.on('message', m => console.log(m.subject, m.text));
```

- You can also pass the account details directly instead of saving the account, for example `mail.connect({ provider: 'qq', address: 'me@qq.com', password: 'authorization-code' })`. Every network method also has an Async form that returns a Promise.
- Node.js scripts load it with `require('autojs6:mail')`, and Bun scripts use `host.mail`, which needs the separate `mail.control` grant; all calls return Promises.
- A script run by a background watch reads the arrived message from `engines.myEngine().execArgv.mail`.

### Requirements

- Android 7.0 or later, AutoJs6 6.8.0 or later, and this plugin enabled in "Plugin center".
- Running scripts on new mail needs an AutoJs6 version that supports background watches; the "Background watches" page says so when yours does not.
- The network permission is only used to connect to the mail servers you set up and, for browser sign-in, to get and renew tokens from Google or Microsoft.
- Independently signed packaged apps can only use account details passed in by the script; saved accounts and background watches are available to AutoJs6 only.

### FAQ

**Connecting fails with an authentication error**

Most providers do not accept the login password. Make sure IMAP or POP3 and SMTP are turned on in the provider's web settings, use an authorization code or app password instead, and check with "Test connection".

**Watching stops after the screen turns off**

Android freezes the network of background apps a while after the screen turns off. Request an exemption for the plugin in "Settings" > "Battery optimization" to stay connected; this step is optional.

**A script reports that the plugin is missing**

Make sure this plugin is installed and enabled in AutoJs6's "Plugin center", then run the script again; there is no need to restart AutoJs6.

See [AutoJs6 docs > Mail](https://docs.autojs6.com/#/mail) for all methods and options, and the [project home page](https://github.com/SuperMonster003/AutoJs6-Plugin-Three-Stamp-Mail) for provider setup and more.
