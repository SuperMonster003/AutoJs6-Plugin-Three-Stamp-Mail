import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Pure JVM mail core (roadmap D22): every Jakarta Mail interaction lives here, has no Android
// dependency, and is exercised by the GreenMail tests of this module before it reaches Binder.
plugins {
    `java-library`
    // Applied for its side effect only: it forces build-logic to publish the JDK selection
    // system properties this script reads below, whatever the module evaluation order is.
    id("org.autojs.build.versions")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.serialization")
}

val selectedJdk = System.getProperty("gradle.java.version.select").toInt()
val effectiveJvmTarget = System.getProperty("gradle.jvm.target.effective")

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(selectedJdk))
    }
    sourceCompatibility = JavaVersion.toVersion(effectiveJvmTarget)
    targetCompatibility = JavaVersion.toVersion(effectiveJvmTarget)
}

kotlin {
    jvmToolchain(selectedJdk)
    compilerOptions {
        jvmTarget.set(JvmTarget.fromTarget(effectiveJvmTarget))
    }
}

dependencies {
    // Jakarta Mail 2.1 API + Angus Mail implementation in one bundle (IMAP / POP3 / SMTP, MIME, XOAUTH2).
    api(libs.angus.jakarta.mail)
    api(libs.jakarta.activation.api)
    // Jakarta Activation implementation: the MIME data handlers Angus Mail resolves through mailcap.
    implementation(libs.angus.activation)
    // Public: MessageDocument.uid and UidsResult carry JsonPrimitive (IMAP numbers or POP3 UIDL strings, roadmap P2.4).
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    // GreenMail also declares jakarta.mail-api; the bundle above already contains those classes.
    testImplementation(libs.greenmail) {
        exclude(group = "jakarta.mail", module = "jakarta.mail-api")
    }
    // GreenMail logs through SLF4J; the tests do not need its output.
    testRuntimeOnly(libs.slf4j.nop)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.withType<Test>().configureEach {
    useJUnit()
    // The static Jakarta switches of MimeLeniency (roadmap P6): a test class may load Jakarta before any mail-core object installs them.
    systemProperty("mail.mime.ignoreunknownencoding", "true")
    systemProperty("mail.mime.decodetext.strict", "false")
    systemProperty("mail.mime.parameters.strict", "false")
    systemProperty("mail.mime.decodefilename", "true")
    testLogging {
        events("failed", "skipped")
        showStackTraces = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}
