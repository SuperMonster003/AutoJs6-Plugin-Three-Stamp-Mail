package io.github.supermonster003.autojs6.plugin.angus.mail

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Locale

/**
 * The release-history screen of roadmap P4.5 (decision D29): the locale-to-asset policy, the
 * markdown dialect parser, and the bundled changelogs themselves, which every language must
 * ship with the same versions and item counts as the English one.
 */
class ReleaseHistoryTest {

    private val docs: Path = findProjectRoot().resolve("app/src/main/assets/doc")

    @Test
    fun `the asset policy picks the bundled language and falls back to English`() {
        assertEquals("CHANGELOG-zh-Hans.md", ReleaseHistoryAssetPolicy.assetFor(Locale.SIMPLIFIED_CHINESE))
        assertEquals("CHANGELOG-zh-Hans.md", ReleaseHistoryAssetPolicy.assetFor(Locale.forLanguageTag("zh-Hans-SG")))
        assertEquals("CHANGELOG-zh-Hant-TW.md", ReleaseHistoryAssetPolicy.assetFor(Locale.TRADITIONAL_CHINESE))
        assertEquals("CHANGELOG-zh-Hant-TW.md", ReleaseHistoryAssetPolicy.assetFor(Locale.forLanguageTag("zh-Hant")))
        assertEquals("CHANGELOG-zh-Hant-HK.md", ReleaseHistoryAssetPolicy.assetFor(Locale.forLanguageTag("zh-HK")))
        assertEquals("CHANGELOG-zh-Hant-HK.md", ReleaseHistoryAssetPolicy.assetFor(Locale.forLanguageTag("zh-Hant-MO")))
        assertEquals("CHANGELOG-ar.md", ReleaseHistoryAssetPolicy.assetFor(Locale.forLanguageTag("ar-EG")))
        assertEquals("CHANGELOG-es.md", ReleaseHistoryAssetPolicy.assetFor(Locale.forLanguageTag("es-MX")))
        assertEquals("CHANGELOG-fr.md", ReleaseHistoryAssetPolicy.assetFor(Locale.CANADA_FRENCH))
        assertEquals("CHANGELOG-ja.md", ReleaseHistoryAssetPolicy.assetFor(Locale.JAPAN))
        assertEquals("CHANGELOG-ko.md", ReleaseHistoryAssetPolicy.assetFor(Locale.KOREA))
        assertEquals("CHANGELOG-ru.md", ReleaseHistoryAssetPolicy.assetFor(Locale.forLanguageTag("ru-RU")))
        assertEquals("CHANGELOG-en.md", ReleaseHistoryAssetPolicy.assetFor(Locale.US))
        assertEquals("CHANGELOG-en.md", ReleaseHistoryAssetPolicy.assetFor(Locale.GERMANY))
        assertEquals("CHANGELOG-en.md", ReleaseHistoryAssetPolicy.assetFor(Locale.ROOT))

        // Every asset the policy can name ships, and every shipped translation is reachable.
        val reachable = listOf(
            Locale.SIMPLIFIED_CHINESE, Locale.TRADITIONAL_CHINESE, Locale.forLanguageTag("zh-HK"), Locale.forLanguageTag("ar"),
            Locale.forLanguageTag("es"), Locale.FRENCH, Locale.JAPANESE, Locale.KOREAN, Locale.forLanguageTag("ru"), Locale.ENGLISH,
        ).map(ReleaseHistoryAssetPolicy::assetFor).toSet()
        assertEquals(ReleaseHistoryAssetPolicy.BUNDLED.toSet(), reachable)
        ReleaseHistoryAssetPolicy.BUNDLED.forEach { asset -> assertTrue("$asset ships", Files.isRegularFile(docs.resolve(asset))) }
    }

    @Test
    fun `the parser reads version headings dates and tagged bullets and ignores the rest`() {
        val markdown = """
            ******

            ### Release History

            ******

            # v1.1.0

            ###### 2026/10/01

            * `Feature` first
            * `Fix` second `with code`
            * plain bullet without a tag

            # v1.0.0

            * `Hint` only item
            not a bullet
            *
        """.trimIndent()

        val entries = ReleaseHistoryParser.parse(markdown)

        assertEquals(listOf("v1.1.0", "v1.0.0"), entries.map { it.version })
        assertEquals("2026/10/01", entries[0].date)
        assertNull(entries[1].date)
        assertEquals(
            listOf(
                ReleaseHistoryItem("Feature", "first"),
                ReleaseHistoryItem("Fix", "second `with code`"),
                ReleaseHistoryItem(null, "plain bullet without a tag"),
            ),
            entries[0].items,
        )
        assertEquals(listOf(ReleaseHistoryItem("Hint", "only item")), entries[1].items)
        assertTrue(ReleaseHistoryParser.parse("* `Feature` before any heading").isEmpty())
    }

    @Test
    fun `every bundled changelog carries the English versions with the same tagged item counts`() {
        val english = ReleaseHistoryParser.parse(Files.readString(docs.resolve("CHANGELOG-en.md")))
        assertTrue("the English changelog has at least one version", english.isNotEmpty())
        english.forEach { entry ->
            assertTrue("${entry.version} is a version tag", entry.version.matches(Regex("v\\d+\\.\\d+\\.\\d+(-[A-Za-z0-9.]+)?")))
            assertTrue("${entry.version} has a yyyy/MM/dd date", entry.date?.matches(Regex("\\d{4}/\\d{2}/\\d{2}")) == true)
            assertTrue("${entry.version} has items", entry.items.isNotEmpty())
        }
        ReleaseHistoryAssetPolicy.BUNDLED.forEach { asset ->
            val entries = ReleaseHistoryParser.parse(Files.readString(docs.resolve(asset)))
            assertEquals("$asset versions", english.map { it.version }, entries.map { it.version })
            entries.zip(english).forEach { (entry, reference) ->
                assertEquals("$asset ${entry.version} date", reference.date, entry.date)
                assertEquals("$asset ${entry.version} item count", reference.items.size, entry.items.size)
                entry.items.forEach { item ->
                    assertTrue("$asset ${entry.version} item has a tag: $item", !item.tag.isNullOrBlank())
                    assertTrue("$asset ${entry.version} item has text: $item", item.text.isNotBlank())
                }
            }
        }
    }

    private fun findProjectRoot(): Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
        .first { Files.exists(it.resolve("settings.gradle.kts")) }
}
