package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import org.autojs.plugin.common.api.PluginActions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class ThreeStampMailPluginRuntimeInfoTest {

    @Test
    fun `runtime fields are assembled without losing the plugin identity`() {
        val info = ThreeStampMailPluginRuntimeInfo(
            name = "3-Stamp Mail",
            description = "Sends, receives, searches, and watches mail over IMAP, POP3, and SMTP",
            instruction = "# 3-Stamp Mail",
            versionName = "1.0.0",
            versionCode = 1L,
            versionDate = "Sep 18, 2026",
        )

        assertEquals("3-Stamp Mail", info.name)
        assertEquals("Sends, receives, searches, and watches mail over IMAP, POP3, and SMTP", info.description)
        assertEquals("# 3-Stamp Mail", info.instruction)
        assertEquals("SuperMonster003", info.author)
        assertEquals("three-stamp-mail", info.id)
        assertEquals("mail", info.engine)
        assertEquals("default", info.variant)
        assertEquals("1.0.0", info.versionName)
        assertEquals(1L, info.versionCode)
        assertEquals("Sep 18, 2026", info.versionDate)
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
        assertEquals(5316L, info.requiresHostVersion)
        assertEquals(ThreeStampMailPlugin.REQUIRED_HOST_VERSION, info.requiresHostVersion)
    }

    @Test
    fun `identity constants follow the host discovery contract`() {
        assertEquals("io.github.supermonster003.autojs6.plugin.three.stamp.mail", ThreeStampMailPlugin.PACKAGE_NAME)
        assertEquals("org.autojs.autojs6", ThreeStampMailPlugin.HOST_PACKAGE_NAME)
        assertEquals("three-stamp-mail", ThreeStampMailPlugin.ID)
        assertEquals("mail", ThreeStampMailPlugin.ENGINE)
        assertEquals("default", ThreeStampMailPlugin.VARIANT)
        assertEquals("SuperMonster003", ThreeStampMailPlugin.AUTHOR)
        assertEquals("org.autojs.plugin.MAIL", ThreeStampMailPlugin.SERVICE_ACTION)
        assertEquals("mail", ThreeStampMailPlugin.SERVICE_CATEGORY)
        assertEquals("org.autojs.plugin.INFO", ThreeStampMailPlugin.INFO_ACTION)
        assertEquals(PluginActions.INFO, ThreeStampMailPlugin.INFO_ACTION)
        assertEquals("org.autojs.plugin.mail.api.IMailPlugin", ThreeStampMailPlugin.SERVICE_DESCRIPTOR)
    }

    @Test
    fun `identity constants match the values the documentation and build publish`() {
        val root = findProjectRoot()
        val common = Files.readString(root.resolve(".readme/common.json"))
        assertTrue(common.contains("\"plugin_application_id\": \"${ThreeStampMailPlugin.PACKAGE_NAME}\""))
        assertTrue(common.contains("\"plugin_id\": \"${ThreeStampMailPlugin.ID}\""))
        assertTrue(common.contains("\"plugin_engine\": \"${ThreeStampMailPlugin.ENGINE}\""))
        assertTrue(common.contains("\"plugin_variant\": \"${ThreeStampMailPlugin.VARIANT}\""))
        assertTrue(common.contains("\"plugin_service_action\": \"${ThreeStampMailPlugin.SERVICE_ACTION}\""))
        assertTrue(common.contains("\"plugin_service_category\": \"${ThreeStampMailPlugin.SERVICE_CATEGORY}\""))
        assertTrue(common.contains("\"plugin_aidl_interface\": \"${ThreeStampMailPlugin.SERVICE_DESCRIPTOR}\""))
        assertTrue(common.contains("\"required_host_version_code\": \"${ThreeStampMailPlugin.REQUIRED_HOST_VERSION}\""))

        val build = Files.readString(root.resolve("app/build.gradle.kts"))
        // app/build.gradle.kts binds the application id once and reuses it for namespace and applicationId.
        assertTrue(build.contains("val globalApplicationId = \"${ThreeStampMailPlugin.PACKAGE_NAME}\""))
        assertTrue(build.contains("applicationId = globalApplicationId"))
        assertTrue(build.contains("\"plugin_id\", \"${ThreeStampMailPlugin.ID}\""))
        assertTrue(build.contains("\"plugin_engine\", \"${ThreeStampMailPlugin.ENGINE}\""))
        assertTrue(build.contains("\"plugin_variant\", \"${ThreeStampMailPlugin.VARIANT}\""))
        assertTrue(build.contains("\"plugin_author\", \"${ThreeStampMailPlugin.AUTHOR}\""))
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
