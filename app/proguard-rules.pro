-dontwarn kotlinx.parcelize.Parcelize

-keep class io.github.supermonster003.autojs6.plugin.angus.mail.AngusMailPluginInfoService { *; }
-keep class io.github.supermonster003.autojs6.plugin.angus.mail.AngusMailPluginService { *; }
-keep class io.github.supermonster003.autojs6.plugin.angus.mail.WakeActivity { *; }

-keep class org.autojs.plugin.common.api.** { *; }

# Jakarta Mail / Angus Mail (roadmap P0.2, narrowed in P6 "size"): everything the mail core calls
# directly is reached by R8 through the code and needs no rule. Rules are only needed for the classes
# the two libraries instantiate by name, listed below with the resource that names them. Everything
# else of the two namespaces (the `util.logging` MailHandler, the `nativeimage` feature, the NTLM /
# SASL authenticators, the image handlers, the unused `gimap` / `dsn` providers, which the bundle
# does not even ship) is shrunk or dropped like any other library code. `docs/dev/p6-size.md` records
# the figures and the release-build device run that verifies the set.

# META-INF/javamail.default.providers: `jakarta.mail.Session.getService` loads the store or transport
# of a protocol with `Class.forName(className)` and `getConstructor(Session.class, URLName.class)`.
-keep class org.eclipse.angus.mail.imap.IMAPStore { <init>(jakarta.mail.Session, jakarta.mail.URLName); }
-keep class org.eclipse.angus.mail.imap.IMAPSSLStore { <init>(jakarta.mail.Session, jakarta.mail.URLName); }
-keep class org.eclipse.angus.mail.pop3.POP3Store { <init>(jakarta.mail.Session, jakarta.mail.URLName); }
-keep class org.eclipse.angus.mail.pop3.POP3SSLStore { <init>(jakarta.mail.Session, jakarta.mail.URLName); }
-keep class org.eclipse.angus.mail.smtp.SMTPTransport { <init>(jakarta.mail.Session, jakarta.mail.URLName); }
-keep class org.eclipse.angus.mail.smtp.SMTPSSLTransport { <init>(jakarta.mail.Session, jakarta.mail.URLName); }

# META-INF/services/jakarta.mail.Provider: `Session` also discovers the providers with ServiceLoader
# (no-argument constructors).
-keep class org.eclipse.angus.mail.imap.IMAPProvider { <init>(); }
-keep class org.eclipse.angus.mail.imap.IMAPSSLProvider { <init>(); }
-keep class org.eclipse.angus.mail.pop3.POP3Provider { <init>(); }
-keep class org.eclipse.angus.mail.pop3.POP3SSLProvider { <init>(); }
-keep class org.eclipse.angus.mail.smtp.SMTPProvider { <init>(); }
-keep class org.eclipse.angus.mail.smtp.SMTPSSLProvider { <init>(); }

# META-INF/services/jakarta.mail.util.StreamProvider: `jakarta.mail.util.FactoryFinder` loads the
# stream provider with ServiceLoader and falls back to this class name.
-keep class org.eclipse.angus.mail.util.MailStreamProvider { <init>(); }

# META-INF/services/jakarta.activation.spi.*RegistryProvider: `jakarta.activation.FactoryFinder`
# loads the mailcap and MIME type registries the same way.
-keep class org.eclipse.angus.activation.MailcapRegistryProviderImpl { <init>(); }
-keep class org.eclipse.angus.activation.MimeTypeRegistryProviderImpl { <init>(); }

# META-INF/mailcap and `MailcapRegistry.HANDLERS` (mail core): `jakarta.activation.MailcapCommandMap`
# creates the data content handler of a MIME type with `Class.forName(name).newInstance()`.
-keep class org.eclipse.angus.mail.handlers.text_plain { <init>(); }
-keep class org.eclipse.angus.mail.handlers.text_html { <init>(); }
-keep class org.eclipse.angus.mail.handlers.text_xml { <init>(); }
-keep class org.eclipse.angus.mail.handlers.multipart_mixed { <init>(); }
-keep class org.eclipse.angus.mail.handlers.message_rfc822 { <init>(); }

# Optional integrations Angus Mail references but Android never provides. Verified by
# :app:assembleRelease on 2026-09-18 (R8 8.13.19): java.awt.* is reached only from the image_gif /
# image_jpeg handlers, javax.security.auth.callback.NameCallback only from the SASL client
# (OAuth2SaslClient), which Android cannot use anyway; the built-in XOAUTH2 path stays intact.
-dontwarn java.awt.**
-dontwarn java.beans.**
-dontwarn javax.naming.**
-dontwarn javax.security.auth.callback.**
-dontwarn javax.security.sasl.**
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.graalvm.**
-dontwarn jdk.**
