package io.github.supermonster003.autojs6.plugin.angus.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Keeps `AndroidManifest.xml` and [AngusMailPlugin] from drifting apart: the host discovers the
 * plugin through the manifest, while the services and tests use the Kotlin constants.
 */
class ManifestContractTest {

    private val manifest: Element by lazy {
        val path = findProjectRoot().resolve("app/src/main/AndroidManifest.xml")
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        factory.newDocumentBuilder().parse(path.toFile()).documentElement
    }

    @Test
    fun `manifest declares exactly the plugin and network permissions and queries the host package`() {
        val permissions = manifest.children("uses-permission").map { it.androidAttribute("name") }
        assertEquals(
            listOf(PLUGIN_PERMISSION, "android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"),
            permissions,
        )

        val queried = manifest.child("queries").children("package").map { it.androidAttribute("name") }
        assertEquals(listOf(AngusMailPlugin.HOST_PACKAGE_NAME), queried)
    }

    @Test
    fun `application metadata points at the wake activity and the author string`() {
        val application = manifest.child("application")
        assertEquals("false", application.androidAttribute("allowBackup"))
        assertEquals("false", application.androidAttribute("fullBackupContent"))
        assertEquals("@xml/data_extraction_rules", application.androidAttribute("dataExtractionRules"))
        assertEquals("@string/app_name", application.androidAttribute("label"))
        assertEquals("@mipmap/ic_launcher", application.androidAttribute("icon"))
        assertEquals("@xml/locales_config", application.androidAttribute("localeConfig"))
        assertEquals("true", application.androidAttribute("supportsRtl"))
        assertNull("mail sessions choose TLS at the socket layer; keep the platform default", application.androidAttributeOrNull("usesCleartextTraffic"))

        val metaData = application.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(".WakeActivity", metaData["org.autojs.plugin.WAKE_ACTIVITY"])
        assertEquals("@string/plugin_author", metaData["org.autojs.plugin.info.AUTHOR"])
        assertEquals("0", metaData["org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"])

        val activities = application.children("activity").associateBy { it.androidAttribute("name") }
        assertEquals(
            setOf(".settings.AccountsActivity", ".settings.AccountEditorActivity", ".AppSettingsActivity", ".AboutActivity", ".WakeActivity"),
            activities.keys,
        )
        val wake = activities.getValue(".WakeActivity")
        assertEquals("true", wake.androidAttribute("exported"))
        assertEquals("true", wake.androidAttribute("excludeFromRecents"))
        assertEquals("true", wake.androidAttribute("finishOnTaskLaunch"))
        assertEquals(PLUGIN_PERMISSION, wake.androidAttribute("permission"))
        assertEquals("@android:style/Theme.NoDisplay", wake.androidAttribute("theme"))
        val filter = wake.child("intent-filter")
        assertEquals(listOf("org.autojs.plugin.action.WAKE"), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.DEFAULT"), filter.children("category").map { it.androidAttribute("name") })

        // AppCompat / Material contribute auto-start components; the manifest only removes them.
        val removals = (application.children("receiver") + application.children("provider"))
        assertEquals(
            listOf("androidx.startup.InitializationProvider", "androidx.profileinstaller.ProfileInstallReceiver").sorted(),
            removals.map { it.androidAttribute("name") }.sorted(),
        )
        removals.forEach { component -> assertEquals("remove", component.getAttributeNS(TOOLS_NAMESPACE, "node")) }
    }

    @Test
    fun `settings screens follow the launcher and parent chain conventions`() {
        val activities = manifest.child("application").children("activity").associateBy { it.androidAttribute("name") }

        val accounts = activities.getValue(".settings.AccountsActivity")
        assertEquals("true", accounts.androidAttribute("exported"))
        assertNull("the launcher entry carries no permission", accounts.androidAttributeOrNull("permission"))
        assertEquals("@style/Theme.AngusMail", accounts.androidAttribute("theme"))
        assertEquals("@string/accounts_title", accounts.androidAttribute("label"))
        val launcher = accounts.child("intent-filter")
        assertEquals(listOf("android.intent.action.MAIN"), launcher.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf("android.intent.category.LAUNCHER"), launcher.children("category").map { it.androidAttribute("name") })

        val chain = mapOf(
            ".settings.AccountEditorActivity" to ".settings.AccountsActivity",
            ".AppSettingsActivity" to ".settings.AccountsActivity",
            ".AboutActivity" to ".AppSettingsActivity",
        )
        chain.forEach { (name, parent) ->
            val activity = activities.getValue(name)
            assertEquals("$name must stay internal", "false", activity.androidAttribute("exported"))
            assertEquals("$name parent", parent, activity.androidAttribute("parentActivityName"))
            assertEquals("$name theme", "@style/Theme.AngusMail", activity.androidAttribute("theme"))
            assertTrue("$name needs a label", activity.androidAttribute("label").startsWith("@string/"))
            assertTrue("$name declares no intent filter", activity.children("intent-filter").isEmpty())
        }
        assertEquals("adjustResize", activities.getValue(".settings.AccountEditorActivity").androidAttribute("windowSoftInputMode"))
    }

    @Test
    fun `info service and mail service match the identity constants`() {
        val services = manifest.child("application").children("service").associateBy { it.androidAttribute("name") }
        assertEquals(setOf(".AngusMailPluginInfoService", ".AngusMailPluginService"), services.keys)
        assertDiscoveryContract(services.getValue(".AngusMailPluginInfoService"), AngusMailPlugin.INFO_ACTION)
        assertDiscoveryContract(services.getValue(".AngusMailPluginService"), AngusMailPlugin.SERVICE_ACTION)
    }

    private fun assertDiscoveryContract(service: Element, action: String) {
        assertEquals("true", service.androidAttribute("exported"))
        assertEquals("true", service.androidAttribute("enabled"))
        assertEquals(PLUGIN_PERMISSION, service.androidAttribute("permission"))
        assertNull("both services run in the default process", service.androidAttributeOrNull("process"))
        val filter = service.child("intent-filter")
        assertEquals(listOf(action), filter.children("action").map { it.androidAttribute("name") })
        assertEquals(listOf(AngusMailPlugin.SERVICE_CATEGORY), filter.children("category").map { it.androidAttribute("name") })
        val metaData = service.children("meta-data").associate { it.androidAttribute("name") to it.androidAttribute("value") }
        assertEquals(AngusMailPlugin.REQUIRED_HOST_VERSION.toString(), metaData["requiresHostVersion"])
    }

    private fun Element.children(tag: String): List<Element> {
        val nodes = childNodes
        return (0 until nodes.length)
            .map { nodes.item(it) }
            .filterIsInstance<Element>()
            .filter { it.tagName == tag }
    }

    private fun Element.child(tag: String): Element = children(tag).single()

    private fun Element.androidAttribute(name: String): String =
        androidAttributeOrNull(name) ?: error("Missing android:$name on <$tagName>")

    private fun Element.androidAttributeOrNull(name: String): String? =
        if (hasAttributeNS(ANDROID_NAMESPACE, name)) getAttributeNS(ANDROID_NAMESPACE, name) else null

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        const val TOOLS_NAMESPACE = "http://schemas.android.com/tools"
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
    }
}
