******

### Release History

******

# v1.0.0

###### 2026/09/18

* `Hint` P0 development preview: repository skeleton, the mail core with local-server tests, and the plugin identity for the AutoJs6 plugin center. The Binder contract, the script API, and the settings page follow the phases of ROADMAP.md.
* `Feature` Plugin identity `angus-mail` (engine `mail`) with the INFO service, the Wake Activity, and the `org.autojs.plugin.MAIL` service whose `IMailPlugin` Binder answers plugin info, capabilities, provider and saved-account listings, and the session envelope (operations follow in P2)
* `Feature` Mail core on Eclipse Angus Mail: session properties for IMAP / POP3 / SMTP with SSL or STARTTLS, password and XOAUTH2 authentication, SMTP sending and IMAP inbox listing, verified against a local GreenMail server
* `Feature` README, plugin-center instructions, and changelog in 10 languages
* `Dependency` Eclipse Angus Mail 2.0.5 (`org.eclipse.angus:jakarta.mail`) with Angus Activation 2.0.3 and Jakarta Activation API 2.1.4
* `Dependency` GreenMail 2.1.13 added for the JVM mail core tests (test scope only)
* `Dependency` Added `common-plugin-api.aar` (AutoJs6 module `plugin-api/common-plugin-api`, host build 6.8.0 / 5282, MPL 2.0) as the shared plugin contract, hash-locked in `locks/host-api-aars.lock`
* `Dependency` Added `mail-api.aar` (AutoJs6 module `plugin-api/mail-api`, host build 6.8.0 / 5282, MPL 2.0) as the mail Binder contract (six AIDL interfaces, `MailContract`, `MailActions`, `MailIds`, `MailCapabilityKeys`, `MailErrorCodes`), hash-locked in `locks/host-api-aars.lock`
