package io.github.supermonster003.autojs6.plugin.angus.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.xml.parsers.DocumentBuilderFactory

/** Every locale carries the same string keys, sorted by name, and the descriptions end without terminal punctuation. */
class StringResourceParityTest {

    private val resourceRoot: Path = findProjectRoot().resolve("app/src/main/res")

    @Test
    fun `every locale defines the same translatable strings in sorted order`() {
        val reference = strings("values")
        assertTrue("the plugin description must stay", "plugin_description" in reference.keys)
        assertTrue("the settings screens ship their strings (roadmap P4.2)", reference.keys.any { it.startsWith("accounts_") })
        assertEquals("values strings must be sorted by name", reference.keys.sorted(), reference.keys.toList())
        assertTrue("no string may carry a secret-looking default", reference.values.none { it.contains("password=", ignoreCase = true) })
        LOCALE_DIRECTORIES.forEach { directory ->
            val localized = strings(directory)
            assertEquals("$directory must define the same keys as values", reference.keys, localized.keys)
            assertEquals("$directory strings must be sorted by name", localized.keys.sorted(), localized.keys.toList())
            localized.values.forEach { value -> assertTrue("$directory has an empty string", value.isNotBlank()) }
        }
        assertEquals("values and values-en must be identical", reference, strings("values-en"))
    }

    @Test
    fun `plugin descriptions end without terminal punctuation and avoid host qualifiers`() {
        (LOCALE_DIRECTORIES + "values").forEach { directory ->
            val description = strings(directory).getValue("plugin_description")
            assertFalse("$directory plugin_description must not end with punctuation", description.last() in ".!?。")
            assertFalse("$directory plugin_description must not mention AutoJs6", description.contains("AutoJs6"))
        }
    }

    @Test
    fun `the application title is not translatable and matches the plugin name`() {
        val document = parse(resourceRoot.resolve("values/strings_donottranslate.xml"))
        val appName = document.elements("string").single()
        assertEquals("app_name", appName.getAttribute("name"))
        assertEquals("false", appName.getAttribute("translatable"))
        assertEquals("Angus Mail", appName.textContent)
    }

    @Test
    fun `the locale configuration lists exactly the supported languages`() {
        val document = parse(resourceRoot.resolve("xml/locales_config.xml"))
        val locales = document.elements("locale").map { it.getAttributeNS(ANDROID_NAMESPACE, "name") }
        // BCP 47 tags of the ten values-* directories, in the order shared with the other official plugins.
        assertEquals(listOf("zh-Hans", "zh-Hant-HK", "zh-Hant-TW", "en", "fr", "es", "ja", "ko", "ru", "ar"), locales)
        assertEquals("every locale directory needs a locales_config entry", LOCALE_DIRECTORIES.size, locales.size)
    }

    private fun strings(directory: String): Map<String, String> {
        val document = parse(resourceRoot.resolve("$directory/strings.xml"))
        return LinkedHashMap<String, String>().also { map ->
            document.elements("string").forEach { element -> map[element.getAttribute("name")] = element.textContent }
        }
    }

    private fun parse(path: Path): Element {
        assertTrue("missing resource file $path", Files.isRegularFile(path))
        val factory = DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true }
        return factory.newDocumentBuilder().parse(path.toFile()).documentElement
    }

    private fun Element.elements(tag: String): List<Element> {
        val nodes = getElementsByTagName(tag)
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { path ->
        path.parent
    }.first { path -> Files.isDirectory(path.resolve("app/src/main")) }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
        val LOCALE_DIRECTORIES = listOf(
            "values-ar", "values-en", "values-es", "values-fr", "values-ja", "values-ko", "values-ru",
            "values-zh", "values-zh-rHK", "values-zh-rTW",
        )
    }
}
