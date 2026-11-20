import org.autojs.build.alignment.VerifyNativePageAlignment
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RelativePath
import org.gradle.api.provider.Property
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.Properties
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

plugins {
    id("io.github.supermonster003.autojs6-native-alignment")
    id("org.autojs.build.utils")
    id("org.autojs.build.versions")
    id("org.autojs.build.signs")
    id("org.autojs.build.jvm-convention")
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val globalApplicationId = "io.github.supermonster003.autojs6.plugin.three.stamp.mail"

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
        resValue("string", "plugin_id", "three-stamp-mail")
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
        compose = true
        resValues = true
    }

    compileOptions {
        // java.time and friends for API 24 devices; the mail core is compiled against the JDK.
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets.named("main") {
        kotlin.directories += "src/main/java"
    }

    // No ABI splits on purpose: the plugin has no native code of its own (Angus Mail is pure Java), so every
    // device installs the same single APK and getInfo() reports supportedAbis = emptyArray(). The only native
    // code is the Compose graphics-path library (see the native code inventory below), kept uncompressed so it
    // maps straight from the APK; the supported ABIs reach the host and the official index through the
    // SUPPORTED_ABIS meta-data written by `nativeAlignment`.
    packaging {
        jniLibs.useLegacyPackaging = false
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
    // Jetpack Compose, only for the release history page (AUTOJS6_PLUGIN_RELEASE_HISTORY_AGENTS.md); the BOM is the
    // single version source and also manages activity-compose.
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.animation)
    implementation(libs.compose.activity)

    testImplementation(libs.junit)

    androidTestImplementation(libs.test.runner)
    androidTestImplementation(libs.test.rules)
    androidTestImplementation(libs.test.ext.junit)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    // ui-test-junit4 brings an older espresso-core that fails on API 36; pin a current one.
    androidTestImplementation(libs.test.espresso.core)
    // No ui-test-manifest: the page test starts the real activity (createEmptyComposeRule), and the debug APK keeps
    // the exact activity inventory that ManifestContractTest checks.
}

// Native code inventory: the plugin has no native code of its own, but Compose ui-graphics depends on
// androidx.graphics:graphics-path, whose libandroidx.graphics.path.so is packaged for the four Android ABIs. The APK
// stays one universal package without ABI splits; the release task verifies that exactly these libraries are present,
// stored uncompressed and 16 KB page aligned (ELF PT_LOAD and zip data offset), the form of 3-Chime Alarm.
val nativeAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
val allowedNativeLibraries = listOf("libandroidx.graphics.path.so")
val nativePageAlignment = 16384L

// The shared gate (platform-versions native-alignment) also checks every assembled APK and writes the
// org.autojs.plugin.contract.SUPPORTED_ABIS meta-data (the four ABIs above), which the host's Plugin Center and the
// official index read; with no native libraries both showed the supported ABIs as unknown.
nativeAlignment { expectNoNativeLibraries.set(false) }

fun elfMinimumLoadAlignment(bytes: ByteArray): Long {
    check(bytes.size > 0x40 && bytes[0] == 0x7F.toByte() && bytes[1] == 'E'.code.toByte() && bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte()) {
        "Not an ELF file"
    }
    val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
    val is64 = bytes[4].toInt() == 2
    val programHeaderOffset = if (is64) buffer.getLong(0x20) else (buffer.getInt(0x1C).toLong() and 0xFFFFFFFFL)
    val entrySize = buffer.getShort(if (is64) 0x36 else 0x2A).toInt() and 0xFFFF
    val entryCount = buffer.getShort(if (is64) 0x38 else 0x2C).toInt() and 0xFFFF
    var minimum = Long.MAX_VALUE
    repeat(entryCount) { index ->
        val offset = (programHeaderOffset + index.toLong() * entrySize).toInt()
        if (buffer.getInt(offset) == 1) { // PT_LOAD
            val alignment = if (is64) buffer.getLong(offset + 48) else (buffer.getInt(offset + 28).toLong() and 0xFFFFFFFFL)
            minimum = minOf(minimum, alignment)
        }
    }
    check(minimum != Long.MAX_VALUE) { "No PT_LOAD segment" }
    return minimum
}

/** Data offsets of the `lib/` entries inside [apk], from the central directory and local headers. */
fun nativeEntryDataOffsets(apk: File): Map<String, Long> = RandomAccessFile(apk, "r").use { file ->
    val length = file.length()
    val tailLength = minOf(length, 22L + 65535L).toInt()
    val tail = ByteArray(tailLength).also { file.seek(length - tailLength); file.readFully(it) }
    val tailBuffer = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
    val eocd = (tail.size - 22 downTo 0).firstOrNull { tailBuffer.getInt(it) == 0x06054b50 }
    checkNotNull(eocd) { "End of central directory not found in ${apk.name}" }
    val entryCount = tailBuffer.getShort(eocd + 10).toInt() and 0xFFFF
    val directorySize = tailBuffer.getInt(eocd + 12).toLong() and 0xFFFFFFFFL
    val directoryOffset = tailBuffer.getInt(eocd + 16).toLong() and 0xFFFFFFFFL
    val directory = ByteArray(directorySize.toInt()).also { file.seek(directoryOffset); file.readFully(it) }
    val directoryBuffer = ByteBuffer.wrap(directory).order(ByteOrder.LITTLE_ENDIAN)
    val offsets = LinkedHashMap<String, Long>()
    var position = 0
    repeat(entryCount) {
        check(directoryBuffer.getInt(position) == 0x02014b50) { "Corrupt central directory in ${apk.name}" }
        val nameLength = directoryBuffer.getShort(position + 28).toInt() and 0xFFFF
        val extraLength = directoryBuffer.getShort(position + 30).toInt() and 0xFFFF
        val commentLength = directoryBuffer.getShort(position + 32).toInt() and 0xFFFF
        val localHeaderOffset = directoryBuffer.getInt(position + 42).toLong() and 0xFFFFFFFFL
        val name = String(directory, position + 46, nameLength, Charsets.UTF_8)
        if (name.startsWith("lib/")) {
            val local = ByteArray(30).also { file.seek(localHeaderOffset); file.readFully(it) }
            val localBuffer = ByteBuffer.wrap(local).order(ByteOrder.LITTLE_ENDIAN)
            check(localBuffer.getInt(0) == 0x04034b50) { "Corrupt local header for $name" }
            val localNameLength = localBuffer.getShort(26).toInt() and 0xFFFF
            val localExtraLength = localBuffer.getShort(28).toInt() and 0xFFFF
            offsets[name] = localHeaderOffset + 30 + localNameLength + localExtraLength
        }
        position += 46 + nameLength + extraLength + commentLength
    }
    offsets
}

/** The native library whitelist check; returns the library sizes for the log. */
fun verifyNativeLibraries(apk: File): Map<String, Long> {
    val expected = nativeAbis.flatMap { abi -> allowedNativeLibraries.map { "lib/$abi/$it" } }.toSet()
    val sizes = LinkedHashMap<String, Long>()
    ZipFile(apk).use { zip ->
        val entries = zip.entries().asSequence().filter { it.name.startsWith("lib/") || it.name.endsWith(".so") }.toList()
        check(entries.map { it.name }.toSet() == expected) {
            "Unexpected native library set in ${apk.name}: expected ${expected.sorted()}, actual ${entries.map { it.name }.sorted()}"
        }
        entries.sortedBy { it.name }.forEach { entry ->
            check(entry.method == ZipEntry.STORED) { "${entry.name} must be stored uncompressed so it can be mapped from the APK" }
            val alignment = elfMinimumLoadAlignment(zip.getInputStream(entry).use { it.readBytes() })
            check(alignment >= nativePageAlignment) { "${entry.name} PT_LOAD alignment $alignment is below $nativePageAlignment" }
            sizes[entry.name] = entry.size
        }
    }
    nativeEntryDataOffsets(apk).forEach { (name, offset) ->
        check(offset % nativePageAlignment == 0L) { "$name data offset $offset is not $nativePageAlignment-byte aligned in the APK" }
    }
    return sizes
}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    register<Copy>("appendDigestToReleasedFiles") {
        description = "Appends CRC32 digest to the released 3-Stamp Mail APK file"
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
            verifyNativeLibraries(src.get().asFile.resolve(expectedApkNames.single()))
                .forEach { (name, size) -> println("Native library: $name $size B") }
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
