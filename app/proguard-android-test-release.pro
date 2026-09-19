# Added to the application rules only for -PandroidTestRelease builds (roadmap P0.2 / AGENTS.md 5.3):
# the instrumentation tests call into the plugin and mail core surface by name, so a release build
# under test must keep those members that the shipped build is free to strip. The test APK is
# shrunk against the application mapping and does not bundle its own Kotlin runtime, so the
# application must also keep the Kotlin standard library classes the test code links against.
# Never part of the regular release build.
-keep class io.github.supermonster003.autojs6.plugin.angus.mail.** { *; }
-keep class kotlin.** { *; }
-dontwarn kotlin.**

# The device tests also build messages and read flags through Jakarta Mail directly (P6 "size"
# narrowed the application rules to the classes loaded by name, so members the mail core never calls
# are stripped from the shipped build); the classes the test code links against stay whole here.
-keep class jakarta.mail.internet.MimeMessage { *; }
-keep class jakarta.mail.internet.MimeBodyPart { *; }
-keep class jakarta.mail.internet.MimeMultipart { *; }
-keep class jakarta.mail.Flags { *; }
-keep class jakarta.activation.CommandMap { *; }

# The test APK also shares the AndroidX classes the application bundles (androidx.tracing.Trace is
# the first one AndroidJUnitRunner.onCreate touches); the shipped build is free to strip them.
-keep class androidx.** { *; }
-dontwarn androidx.**
