package io.github.supermonster003.autojs6.plugin.angus.mail.store

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The `accountsJson` rendering of `listSavedAccounts` (roadmap P4.3). */
class SavedAccountsDocumentTest {

    @Test
    fun rendersNormalizedFieldsWithoutSecrets() {
        val accounts = listOf(
            SavedAccount("work", """{"address":"alice@example.com","provider":"qq","name":"Alice","timeout":{"connect":5000}}""", SecretKind.PASSWORD, 1000L, true),
            SavedAccount("token", """{"address":"bob@gmail.com","provider":"gmail","receive":"pop3"}""", SecretKind.ACCESS_TOKEN, 2000L, false),
            SavedAccount("bare", """{"address":"carol@example.org","user":"carol","imap":{"host":"imap.example.org"},"smtp":{"host":"smtp.example.org","tls":"starttls"}}""", SecretKind.PASSWORD, 3000L, false),
        )

        val rendered = SavedAccountsDocument.render(accounts)
        val array = Json.parseToJsonElement(rendered).jsonArray
        assertEquals(3, array.size)
        assertFalse(rendered, rendered.contains("secret"))

        val work = array[0].jsonObject
        assertEquals("work", work.string("alias"))
        assertEquals("alice@example.com", work.string("address"))
        assertEquals("alice@example.com", work.string("user"))
        assertEquals("Alice", work.string("name"))
        assertEquals("qq", work.string("provider"))
        assertEquals("password", work.string("auth"))
        assertEquals("imap", work.string("receive"))
        assertEquals("imap.qq.com", work.getValue("imap").jsonObject.string("host"))
        assertEquals("993", work.getValue("imap").jsonObject.getValue("port").jsonPrimitive.content)
        assertEquals("ssl", work.getValue("imap").jsonObject.string("tls"))
        assertEquals("smtp.qq.com", work.getValue("smtp").jsonObject.string("host"))
        assertEquals("true", work.getValue("default").jsonPrimitive.content)
        assertEquals("1000", work.getValue("updatedAt").jsonPrimitive.content)
        assertNull(work["error"])
        assertNull(work["timeout"])

        val token = array[1].jsonObject
        assertEquals("xoauth2", token.string("auth"))
        assertEquals("pop3", token.string("receive"))
        assertEquals("pop.gmail.com", token.getValue("pop3").jsonObject.string("host"))
        assertEquals("false", token.getValue("default").jsonPrimitive.content)

        val bare = array[2].jsonObject
        assertNull(bare["provider"])
        assertNull(bare["name"])
        assertNull(bare["pop3"])
        assertEquals("carol", bare.string("user"))
        assertEquals("imap.example.org", bare.getValue("imap").jsonObject.string("host"))
        assertEquals("587", bare.getValue("smtp").jsonObject.getValue("port").jsonPrimitive.content)
        assertEquals("starttls", bare.getValue("smtp").jsonObject.string("tls"))
    }

    @Test
    fun aRecordThatNoLongerNormalizesIsListedWithAnError() {
        val stale = SavedAccount("old", """{"address":"dave@example.com","provider":"vanished"}""", SecretKind.PASSWORD, 5L, false)

        val entry = SavedAccountsDocument.entry(stale)

        assertEquals("old", entry.string("alias"))
        assertEquals("dave@example.com", entry.string("address"))
        assertEquals(MailErrorCode.PROVIDER_UNKNOWN, entry.getValue("error").jsonObject.string("code"))
        assertTrue(entry.getValue("error").jsonObject.string("message").contains("vanished"))
        assertNull(entry["auth"])
        assertEquals("false", entry.getValue("default").jsonPrimitive.content)
        assertEquals("[]", SavedAccountsDocument.render(emptyList()))
    }

    private fun JsonObject.string(key: String): String = getValue(key).jsonPrimitive.content
}
