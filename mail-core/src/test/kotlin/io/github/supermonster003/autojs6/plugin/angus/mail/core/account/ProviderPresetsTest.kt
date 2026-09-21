package io.github.supermonster003.autojs6.plugin.angus.mail.core.account

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** Snapshot of `providers.json` (roadmap appendix C, verified against the live hosts on 2026-09-18). */
class ProviderPresetsTest {

    @Test
    fun catalogMatchesAppendixC() {
        assertEquals(3, ProviderPresets.version)
        assertEquals(listOf("gmail", "outlook", "office365", "qq", "163", "126", "icloud", "yahoo", "sina", "aliyun"), ProviderPresets.ids)
    }

    @Test
    fun everyPresetIsInternallyConsistent() {
        ProviderPresets.all.forEach { preset ->
            assertEquals(preset.id, preset.id.trim().lowercase())
            assertTrue("${preset.id}: authHint", preset.authHint.isNotBlank())
            assertTrue("${preset.id}: docsUrl", preset.docsUrl.startsWith("https://"))
            assertTrue("${preset.id}: name", preset.name.isNotBlank())
            assertTrue("${preset.id}: auth", preset.auth.isNotEmpty() && preset.auth.all { AuthMethod.fromId(it) != null })
            assertNotNull("${preset.id}: smtp", preset.smtp)
            assertTrue("${preset.id}: receive endpoint", preset.imap != null || preset.pop3 != null)
            listOfNotNull(preset.imap to MailProtocol.IMAP, preset.pop3?.let { it to MailProtocol.POP3 }, preset.smtp to MailProtocol.SMTP).forEach { (endpoint, protocol) ->
                endpoint ?: return@forEach
                val tls = TlsMode.fromId(endpoint.tls)
                assertNotNull("${preset.id}: ${protocol.id} tls", tls)
                assertTrue("${preset.id}: ${protocol.id} never plain", tls != TlsMode.NONE)
                assertEquals("${preset.id}: ${protocol.id} port", protocol.defaultPort(tls!!), endpoint.port)
                assertTrue("${preset.id}: ${protocol.id} host", endpoint.host.matches(Regex("[a-z0-9.-]+")) && endpoint.host.contains('.'))
                assertEquals(MailEndpoint(endpoint.host, endpoint.port, tls), endpoint.toEndpoint())
            }
            if (preset.requiresClientId) assertTrue("${preset.id}: only 163 and 126 require ID", preset.id in setOf("163", "126"))
            if (preset.autoSavesSent) assertNotNull("${preset.id}: autoSavesSent needs sentFolder", preset.sentFolder)
        }
    }

    @Test
    fun presetsHoldOnlyPublicFacts() {
        val json = ProviderPresets.toJson()
        assertFalse(json.contains("@"))
        assertFalse(json.contains("password\":\""))
        val document = Json.parseToJsonElement(json).jsonObject
        assertEquals(3, document["version"]!!.jsonPrimitive.content.toInt())
        assertEquals(ProviderPresets.all.size, document["providers"]!!.jsonArray.size)
        assertEquals(ProviderPresets.catalog, ProviderPresets.parse(json))
    }

    @Test
    fun resolveIsCaseInsensitiveAndTrimmed() {
        assertSame(ProviderPresets.resolve("qq"), ProviderPresets.resolve(" QQ "))
        assertNull(ProviderPresets.resolve("unknown"))
        assertNull(ProviderPresets.resolve(null))
        try {
            ProviderPresets.require("gmail.com")
            fail("unknown provider accepted")
        } catch (e: MailException) {
            assertEquals(MailErrorCode.PROVIDER_UNKNOWN, e.code)
            assertFalse(e.retryable)
            assertTrue(e.details!!.contains("gmail"))
        }
    }

    @Test
    fun providerSpecificsOfTheRoadmapTable() {
        val gmail = ProviderPresets.require("gmail")
        assertTrue(gmail.autoSavesSent)
        assertEquals("[Gmail]/Sent Mail", gmail.sentFolder)
        assertTrue(gmail.accepts(AuthMethod.XOAUTH2) && gmail.accepts(AuthMethod.PASSWORD))
        val outlook = ProviderPresets.require("outlook")
        assertEquals(listOf("xoauth2"), outlook.auth)
        assertEquals(TlsMode.STARTTLS, outlook.smtp!!.toEndpoint().tls)
        val mail163 = ProviderPresets.require("163")
        assertTrue(mail163.requiresClientId)
        assertTrue("163 keeps a server copy of sent mail (real account, 2026-09-19)", mail163.autoSavesSent)
        assertEquals("已发送", mail163.sentFolder)
        val mail126 = ProviderPresets.require("126")
        assertTrue(mail126.requiresClientId)
        assertTrue("126 keeps a server copy of sent mail (real account, 2026-09-19)", mail126.autoSavesSent)
        val sina = ProviderPresets.require("sina")
        assertFalse("Sina keeps no server copy of sent mail (real account, 2026-09-19)", sina.autoSavesSent)
        assertEquals("已发送", sina.sentFolder)
        listOf("qq", "sina").forEach { assertFalse("$it accepts IDLE but pushes nothing (real account, 2026-09-19)", ProviderPresets.require(it).idlePush) }
        listOf("163", "126").forEach { assertFalse("$it has no IDLE (real account, 2026-09-19)", ProviderPresets.require(it).idlePush) }
        listOf("gmail", "outlook", "office365", "icloud", "yahoo", "aliyun").forEach { assertTrue("$it: idlePush stays on", ProviderPresets.require(it).idlePush) }
        listOf("outlook", "office365").forEach { assertTrue("$it takes POP3 AUTH XOAUTH2 only in the two-line form (real account, 2026-09-21)", ProviderPresets.require(it).pop3Xoauth2TwoLine) }
        listOf("gmail", "qq", "163", "126", "icloud", "yahoo", "sina", "aliyun").forEach { assertFalse("$it: one-line POP3 XOAUTH2", ProviderPresets.require(it).pop3Xoauth2TwoLine) }
        assertEquals("imap.126.com", mail126.imap!!.host)
        assertEquals("imap.163.com", mail163.imap!!.host)
        assertNull(ProviderPresets.require("icloud").pop3)
    }
}
