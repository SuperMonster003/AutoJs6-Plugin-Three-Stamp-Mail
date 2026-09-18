package io.github.supermonster003.autojs6.plugin.angus.mail

import org.autojs.plugin.common.api.PluginActions
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

class AngusMailPluginRuntimeInfoTest {

    @Test
    fun `runtime fields are assembled without losing the plugin identity`() {
        val info = AngusMailPluginRuntimeInfo(
            name = "Angus Mail",
            description = "Sends, receives, searches, and watches mail over IMAP, POP3, and SMTP",
            instruction = "# Angus Mail",
            versionName = "1.0.0",
            versionCode = 1L,
            versionDate = "Sep 18, 2026",
        )

        assertEquals("Angus Mail", info.name)
        assertEquals("Sends, receives, searches, and watches mail over IMAP, POP3, and SMTP", info.description)
        assertEquals("# Angus Mail", info.instruction)
        assertEquals("SuperMonster003", info.author)
        assertEquals("angus-mail", info.id)
        assertEquals("mail", info.engine)
        assertEquals("default", info.variant)
        assertEquals("1.0.0", info.versionName)
        assertEquals(1L, info.versionCode)
        assertEquals("Sep 18, 2026", info.versionDate)
        assertArrayEquals(emptyArray<String>(), info.supportedAbis)
        assertEquals(5282L, info.requiresHostVersion)
        assertEquals(AngusMailPlugin.REQUIRED_HOST_VERSION, info.requiresHostVersion)
    }

    @Test
    fun `identity constants follow the host discovery contract`() {
        assertEquals("io.github.supermonster003.autojs6.plugin.angus.mail", AngusMailPlugin.PACKAGE_NAME)
        assertEquals("org.autojs.autojs6", AngusMailPlugin.HOST_PACKAGE_NAME)
        assertEquals("angus-mail", AngusMailPlugin.ID)
        assertEquals("mail", AngusMailPlugin.ENGINE)
        assertEquals("default", AngusMailPlugin.VARIANT)
        assertEquals("SuperMonster003", AngusMailPlugin.AUTHOR)
        assertEquals("org.autojs.plugin.MAIL", AngusMailPlugin.SERVICE_ACTION)
        assertEquals("mail", AngusMailPlugin.SERVICE_CATEGORY)
        assertEquals("org.autojs.plugin.INFO", AngusMailPlugin.INFO_ACTION)
        assertEquals(PluginActions.INFO, AngusMailPlugin.INFO_ACTION)
        assertEquals("org.autojs.plugin.mail.api.IMailPlugin", AngusMailPlugin.SERVICE_DESCRIPTOR)
    }

    @Test
    fun `identity constants match the values the documentation and build publish`() {
        val root = findProjectRoot()
        val common = Files.readString(root.resolve(".readme/common.json"))
        assertTrue(common.contains("\"plugin_application_id\": \"${AngusMailPlugin.PACKAGE_NAME}\""))
        assertTrue(common.contains("\"plugin_id\": \"${AngusMailPlugin.ID}\""))
        assertTrue(common.contains("\"plugin_engine\": \"${AngusMailPlugin.ENGINE}\""))
        assertTrue(common.contains("\"plugin_variant\": \"${AngusMailPlugin.VARIANT}\""))
        assertTrue(common.contains("\"plugin_service_action\": \"${AngusMailPlugin.SERVICE_ACTION}\""))
        assertTrue(common.contains("\"plugin_service_category\": \"${AngusMailPlugin.SERVICE_CATEGORY}\""))
        assertTrue(common.contains("\"plugin_aidl_interface\": \"${AngusMailPlugin.SERVICE_DESCRIPTOR}\""))
        assertTrue(common.contains("\"required_host_version_code\": \"${AngusMailPlugin.REQUIRED_HOST_VERSION}\""))

        val build = Files.readString(root.resolve("app/build.gradle.kts"))
        // app/build.gradle.kts binds the application id once and reuses it for namespace and applicationId.
        assertTrue(build.contains("val globalApplicationId = \"${AngusMailPlugin.PACKAGE_NAME}\""))
        assertTrue(build.contains("applicationId = globalApplicationId"))
        assertTrue(build.contains("\"plugin_id\", \"${AngusMailPlugin.ID}\""))
        assertTrue(build.contains("\"plugin_engine\", \"${AngusMailPlugin.ENGINE}\""))
        assertTrue(build.contains("\"plugin_variant\", \"${AngusMailPlugin.VARIANT}\""))
        assertTrue(build.contains("\"plugin_author\", \"${AngusMailPlugin.AUTHOR}\""))
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }
}
