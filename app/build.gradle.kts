import org.autojs.build.alignment.VerifyNativePageAlignment
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import java.security.MessageDigest
import java.util.Properties

plugins {
    id("io.github.supermonster003.autojs6-native-alignment")
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.angus.mail"

// ---------------------------------------------------------------------------
// OAuth 2.0 client registrations (roadmap P9): public clients with PKCE, so only client ids and
// the Microsoft tenant live here, read from the git-ignored oauth-clients.properties next to this
// file's project root (keys googleClientId, microsoftClientId, microsoftTenant). A missing file or
// key leaves the provider "not configured": the settings page then offers no browser sign-in for
// it and the redirect filter keeps a placeholder scheme.
// ---------------------------------------------------------------------------

val oauthClients = Properties().apply {
    val file = rootProject.file("oauth-clients.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val oauthGoogleClientId = oauthClients.getProperty("googleClientId")?.trim().orEmpty()
val oauthMicrosoftClientId = oauthClients.getProperty("microsoftClientId")?.trim().orEmpty()
val oauthMicrosoftTenant = oauthClients.getProperty("microsoftTenant")?.trim().orEmpty()
val oauthGoogleScheme = if (oauthGoogleClientId.isEmpty()) {
    "com.googleusercontent.apps.unconfigured"
} else {
    "com.googleusercontent.apps." + oauthGoogleClientId.removeSuffix(".apps.googleusercontent.com")
}
val buildTypeDebug = "debug"
val buildTypeRelease = "release"

// ---------------------------------------------------------------------------
// Host protocol AARs are consumed only from libs/ and are pinned by locks/host-api-aars.lock.
// The build refuses missing files, debug artifacts, placeholder hashes, and digest mismatches.
// ---------------------------------------------------------------------------

fun File.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    inputStream().buffered().use { input ->
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}

fun Properties.requiredValue(key: String): String =
    getProperty(key)?.trim()?.takeIf(String::isNotEmpty)
        ?: error("Missing required lock value: $key")

fun File.loadUniqueLock(): Properties {
    val lock = Properties()
    useLines(Charsets.UTF_8) { lines ->
        lines.forEachIndexed { index, line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith('#') || trimmed.startsWith('!')) {
                return@forEachIndexed
            }
            val separator = trimmed.indexOf('=')
            require(separator > 0) { "Malformed lock line ${index + 1} in $name" }
            val key = trimmed.substring(0, separator).trim()
            val value = trimmed.substring(separator + 1).trim()
            require(!lock.containsKey(key)) { "Duplicate lock key in $name: $key" }
            lock.setProperty(key, value)
        }
    }
    return lock
}

val sha256Pattern = Regex("[0-9a-f]{64}")
val hostApiLockFile = rootProject.file("locks/host-api-aars.lock")
require(hostApiLockFile.isFile) {
    "Missing host API lock: ${hostApiLockFile.relativeTo(rootProject.projectDir)}"
}
val hostApiLock = hostApiLockFile.loadUniqueLock()
val hostApiIds = listOf("common-plugin-api", "mail-api")
val expectedHostApiLockKeys = setOf("format") + hostApiIds.flatMap { id -> listOf("$id.file", "$id.sha256") }
require(hostApiLock.stringPropertyNames() == expectedHostApiLockKeys) {
    "Host API AAR lock must contain exactly these keys: ${expectedHostApiLockKeys.sorted()}"
}
require(hostApiLock.requiredValue("format") == "1") {
    "Unsupported host API AAR lock format"
}

fun lockedHostApiAar(id: String): File {
    val fileName = hostApiLock.requiredValue("$id.file")
    val expectedSha256 = hostApiLock.requiredValue("$id.sha256").lowercase()
    require(fileName == File(fileName).name && fileName.endsWith(".aar")) {
        "Invalid $id.file in ${hostApiLockFile.name}"
    }
    require(!fileName.endsWith("-debug.aar")) {
        "Debug AARs are forbidden: $fileName"
    }
    require(sha256Pattern.matches(expectedSha256)) {
        "Replace $id.sha256 with the audited release AAR SHA-256 before Gradle configuration"
    }
    val artifact = rootProject.file("libs/$fileName")
    require(artifact.isFile) {
        "Missing locked host API AAR: ${artifact.relativeTo(rootProject.projectDir)}"
    }
    val actualSha256 = artifact.sha256()
    require(actualSha256 == expectedSha256) {
        "SHA-256 mismatch for $fileName: expected $expectedSha256, actual $actualSha256"
    }
    return artifact
}

val commonPluginApiAar = lockedHostApiAar("common-plugin-api")
val mailApiAar = lockedHostApiAar("mail-api")

android {
    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    // `-PandroidTestRelease` runs the instrumentation tests against the R8-processed release build
    // (roadmap P0.2: the Angus Mail handlers must still resolve after shrinking). Requires signing.
    if (project.hasProperty("androidTestRelease")) {
        testBuildType = buildTypeRelease
    }

    defaultConfig {
        applicationId = globalApplicationId
        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget
        versionCode = versions.appVersionCode
        versionName = versions.appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        resValue("string", "plugin_author", "SuperMonster003")
        resValue("string", "plugin_engine", "mail")
        resValue("string", "plugin_id", "angus-mail")
        resValue("string", "plugin_variant", "default")
        resValue("string", "plugin_version_date", utils.getDateString("MMM d, yyyy", "GMT+08:00"))

        // OAuth 2.0 client ids (roadmap P9); empty when oauth-clients.properties does not name them.
        resValue("string", "oauth_google_client_id", oauthGoogleClientId)
        resValue("string", "oauth_microsoft_client_id", oauthMicrosoftClientId)
        resValue("string", "oauth_microsoft_tenant", oauthMicrosoftTenant)
        manifestPlaceholders["oauthGoogleScheme"] = oauthGoogleScheme
    }

    lint {
        abortOnError = true
        // Product text intentionally uses ASCII punctuation in every locale.
        disable += "TypographyEllipsis"
    }

    signingConfigs {
        if (signs.isValid) {
            create(buildTypeRelease) {
                storeFile = signs.properties["storeFile"]?.let { file(it as String) }
                keyPassword = signs.properties["keyPassword"] as String
                keyAlias = signs.properties["keyAlias"] as String
                storePassword = signs.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android-optimize.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { signs.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(*proguardFiles)
            if (project.hasProperty("androidTestRelease")) {
                // Keeps the surface the instrumentation tests reach into; see the file header.
                proguardFile("proguard-android-test-release.pro")
            }
            // Only consulted when this build type is the test build type (-PandroidTestRelease).
            testProguardFiles("proguard-test-rules.pro")
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true
        resValues = true
    }

    compileOptions {
        // java.time and friends for API 24 devices; the mail core is compiled against the JDK.
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets.named("main") {
        kotlin.directories += "src/main/java"
    }

    // No ABI splits on purpose: the plugin ships Kotlin / Java bytecode and resources only (Angus Mail
    // is pure Java), so every device installs the same single APK and getInfo() reports
    // supportedAbis = emptyArray().
    packaging {
        resources.pickFirsts.addAll(
            listOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.*",
                "META-INF/NOTICE",
                "META-INF/NOTICE.*",
                "META-INF/*.kotlin_module",
            ),
        )
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val outputFileNameProperty = output.javaClass.methods.firstOrNull {
                it.name == "getOutputFileName" && it.parameterTypes.isEmpty()
            }?.invoke(output) as? Property<*>

            @Suppress("UNCHECKED_CAST")
            (outputFileNameProperty as? Property<String>)?.set(
                output.versionName.map { versionName ->
                    val version = versionName.replace("\\s".toRegex(), "-")
                    "${rootProject.name}-v$version.${utils.FILE_EXTENSION_APK}".lowercase()
                },
            )
        }
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)

    implementation(files(commonPluginApiAar))
    implementation(files(mailApiAar))
    // Every IMAP / POP3 / SMTP operation (roadmap D13); the Binder layer only routes requests to it.
    implementation(project(":mail-core"))

    // The settings screens (roadmap P4.2): AppCompat activities and Material components, built in code.
    implementation(libs.appcompat)
    implementation(libs.material)
    // The browser sign-in of roadmap P9 opens the provider's authorization page in a Custom Tab.
    implementation(libs.browser)

    testImplementation(libs.junit)

    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.ext.junit)
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    register<Copy>("appendDigestToReleasedFiles") {
        description = "Appends CRC32 digest to the released Angus Mail APK file"
        dependsOn("assembleRelease")

        val ext = utils.FILE_EXTENSION_APK
        val src = layout.buildDirectory.dir("outputs/apk/$buildTypeRelease")
        val dst = file("$rootDir/${buildTypeRelease}s")
        val releaseSigningReady = signs.isValid
        val expectedApkNames = listOf(
            "${rootProject.name}-v${versions.appVersionName.replace("\\s".toRegex(), "-")}.$ext".lowercase(),
        )

        from(src)
        into(dst)
        include("*.$ext")
        includeEmptyDirs = false
        duplicatesStrategy = DuplicatesStrategy.FAIL

        doFirst {
            require(releaseSigningReady) {
                "Release signing is not configured (sign.properties); refusing to collect an unsigned APK"
            }
            val actualApkNames = src.get().asFile.listFiles { file -> file.extension == ext }
                .orEmpty()
                .map { it.name }
                .sorted()
            require(actualApkNames == expectedApkNames) {
                "Unexpected release APK set: expected $expectedApkNames, actual $actualApkNames"
            }
        }

        eachFile {
            val digest = utils.digestCRC32(file)
            relativePath = RelativePath(true, "${name.removeSuffix(".$ext")}-$digest.$ext")
        }

        doLast { println("Destination: $dst") }
    }
}

extra {
    versions.handleIfNeeded(project, "", listOf(buildTypeDebug, buildTypeRelease))
}

// Reject accidental native dependencies on every ABI.
nativeAlignment { expectNoNativeLibraries.set(true) }

tasks.withType<VerifyNativePageAlignment>().configureEach {
    // The alignment plugin also finalizes assemble*UnitTest, which never produces an APK.
    // Keep verification on APK variants and let standalone checks build their inputs.
    val variantSuffix = name.removePrefix("verify").removeSuffix("NativePageAlignment")
    if (variantSuffix.endsWith("UnitTest")) {
        enabled = false
    } else {
        dependsOn("assemble$variantSuffix")
    }
}
