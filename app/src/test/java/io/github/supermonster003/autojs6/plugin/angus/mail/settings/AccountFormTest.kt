package io.github.supermonster003.autojs6.plugin.angus.mail.settings

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.OAuthLink
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The editor model of roadmap P4.2: preset fill-in, validation, and the JSON round trip through the mail core. */
class AccountFormTest {

    @Test
    fun `a preset fills the endpoints and the document keeps only what the preset does not know`() {
        val qq = ProviderPresets.require("qq")
        val form = AccountFormPolicy.applyPreset(AccountFormPolicy.blank(), qq).copy(alias = "Work", address = "alice@qq.com")

        assertEquals("imap.qq.com", form.imap.host)
        assertEquals("993", form.imap.port)
        assertEquals(TlsMode.SSL, form.imap.tls)
        assertTrue(form.smtp.enabled)
        assertTrue(form.endpointFixedByPreset(MailProtocol.IMAP))
        assertEquals(MailProtocol.IMAP, form.receive)
        assertTrue(AccountFormPolicy.validate(form).isEmpty())

        val json = MailJson.format.parseToJsonElement(AccountFormPolicy.toAccountJson(form)).jsonObject
        assertEquals(setOf("provider", "address", "auth", "receive"), json.keys)
        assertEquals("qq", json.getValue("provider").jsonPrimitive.content)

        val parsed = MailAccountOptions.parse(json.toString(), SecretKind.PASSWORD)
        assertEquals("imap.qq.com", parsed.imap?.host)
        assertEquals("smtp.qq.com", parsed.smtp?.host)
        assertEquals(AuthMethod.PASSWORD, parsed.auth)
    }

    @Test
    fun `an override of a preset endpoint is written explicitly and survives the round trip`() {
        val gmail = ProviderPresets.require("gmail")
        val form = AccountFormPolicy.applyPreset(AccountFormPolicy.blank(), gmail)
            .copy(alias = "g", address = "bob@gmail.com", user = "bob@gmail.com", name = "Bob")
            .withEndpoint(MailProtocol.SMTP, EndpointFields(true, "smtp.gmail.com", "587", TlsMode.STARTTLS))

        val json = MailJson.format.parseToJsonElement(AccountFormPolicy.toAccountJson(form)).jsonObject
        assertEquals(setOf("provider", "address", "name", "auth", "receive", "smtp"), json.keys)
        assertNull("a login name equal to the address is not repeated", json["user"])
        val smtp = json.getValue("smtp") as JsonObject
        assertEquals("starttls", smtp.getValue("tls").jsonPrimitive.content)
        assertEquals("587", smtp.getValue("port").jsonPrimitive.content)

        val parsed = MailAccountOptions.parse(json.toString(), SecretKind.PASSWORD)
        assertEquals(587, parsed.smtp?.port)
        assertEquals(TlsMode.STARTTLS, parsed.smtp?.tls)
        assertEquals(993, parsed.imap?.port)

        val reopened = AccountFormPolicy.fromAccountJson(json.toString(), SecretKind.PASSWORD)
        assertEquals("gmail", reopened.providerId)
        assertEquals("Bob", reopened.name)
        assertEquals("", reopened.user)
        assertEquals(EndpointFields(true, "smtp.gmail.com", "587", TlsMode.STARTTLS), reopened.smtp)
        assertEquals(EndpointFields(true, "imap.gmail.com", "993", TlsMode.SSL), reopened.imap)
        assertEquals(EndpointFields(true, "pop.gmail.com", "995", TlsMode.SSL), reopened.pop3)
    }

    @Test
    fun `a custom server needs hosts and valid ports and may be send-only`() {
        val blank = AccountFormPolicy.blank().copy(alias = "x", address = "x@example.org")
        assertEquals(listOf(FormError(null, FormProblem.NO_ENDPOINT)), AccountFormPolicy.validate(blank))

        val broken = blank
            .withEndpoint(MailProtocol.IMAP, EndpointFields(true, "", "70000", TlsMode.SSL))
            .withEndpoint(MailProtocol.SMTP, EndpointFields(true, "smtp.example.org", "", TlsMode.STARTTLS))
        assertEquals(
            listOf(FormError(FormField.IMAP_HOST, FormProblem.HOST_REQUIRED), FormError(FormField.IMAP_PORT, FormProblem.PORT_INVALID)),
            AccountFormPolicy.validate(broken),
        )

        val sendOnly = blank.withEndpoint(MailProtocol.SMTP, EndpointFields(true, "smtp.example.org", "", TlsMode.STARTTLS))
        assertTrue(AccountFormPolicy.validate(sendOnly).isEmpty())
        val json = MailJson.format.parseToJsonElement(AccountFormPolicy.toAccountJson(sendOnly)).jsonObject
        assertNull("a send-only account carries no receive field", json["receive"])
        assertNull(json.getValue("smtp").jsonObject["port"])
        val parsed = MailAccountOptions.parse(json.toString(), SecretKind.PASSWORD)
        assertEquals(587, parsed.smtp?.port)
        assertNull(parsed.receiveEndpoint)

        val pop3Only = blank
            .withEndpoint(MailProtocol.POP3, EndpointFields(true, "pop.example.org", "995", TlsMode.SSL))
            .copy(receive = MailProtocol.IMAP)
        assertEquals(listOf(FormError(null, FormProblem.RECEIVE_ENDPOINT_MISSING)), AccountFormPolicy.validate(pop3Only))
        assertTrue(AccountFormPolicy.validate(pop3Only.copy(receive = MailProtocol.POP3)).isEmpty())
    }

    @Test
    fun `alias and address problems point at their fields`() {
        val form = AccountFormPolicy.applyPreset(AccountFormPolicy.blank(), ProviderPresets.require("163"))
        assertEquals(
            listOf(FormError(FormField.ALIAS, FormProblem.ALIAS_REQUIRED), FormError(FormField.ADDRESS, FormProblem.ADDRESS_REQUIRED)),
            AccountFormPolicy.validate(form),
        )
        assertEquals(
            listOf(FormError(FormField.ALIAS, FormProblem.ALIAS_INVALID), FormError(FormField.ADDRESS, FormProblem.ADDRESS_INVALID)),
            AccountFormPolicy.validate(form.copy(alias = "bad alias!", address = "no-at-sign")),
        )
        assertTrue(AccountFormPolicy.validate(form.copy(alias = "  Work.1 ", address = "a@163.com")).isEmpty())
    }

    @Test
    fun `switching presets keeps the login and follows the accepted authentication`() {
        val outlook = ProviderPresets.all.first { it.accepts(AuthMethod.XOAUTH2) && !it.accepts(AuthMethod.PASSWORD) }
        val tokenForm = AccountFormPolicy.applyPreset(AccountFormPolicy.blank().copy(address = "c@example.org"), outlook)
        assertEquals(AuthMethod.XOAUTH2, tokenForm.auth)
        assertEquals(SecretKind.ACCESS_TOKEN, tokenForm.secretKind)

        val passwordOnly = ProviderPresets.all.first { it.accepts(AuthMethod.PASSWORD) && !it.accepts(AuthMethod.XOAUTH2) }
        val switched = AccountFormPolicy.applyPreset(tokenForm, passwordOnly)
        assertEquals("c@example.org", switched.address)
        assertEquals(AuthMethod.PASSWORD, switched.auth)

        val custom = AccountFormPolicy.applyPreset(switched, null)
        assertNull(custom.providerId)
        assertEquals(switched.imap, custom.imap)
        assertFalse(custom.endpointFixedByPreset(MailProtocol.IMAP))
    }

    @Test
    fun `a document whose preset vanished reopens as a custom server`() {
        val reopened = AccountFormPolicy.fromAccountJson("""{"provider":"gone","address":"d@example.org","imap":{"host":"imap.example.org","port":143,"tls":"starttls"}}""")
        assertNull(reopened.providerId)
        assertEquals(EndpointFields(true, "imap.example.org", "143", TlsMode.STARTTLS), reopened.imap)
        assertEquals(EndpointFields.DISABLED, reopened.smtp)
        assertEquals(MailProtocol.IMAP, reopened.receive)
        assertEquals(AccountFormPolicy.blank(), AccountFormPolicy.fromAccountJson("not json"))
    }

    @Test
    fun `a browser sign-in is an XOAUTH2 form whose document carries the oauth link and whose secret is the token record`() {
        val gmail = ProviderPresets.require("gmail")
        val form = AccountFormPolicy.applyPreset(AccountFormPolicy.blank(), gmail).copy(alias = "g", address = "alice@gmail.com", auth = AuthMethod.XOAUTH2, oauthProvider = OAuthProviderId.GOOGLE)
        assertEquals(SecretKind.OAUTH2, form.secretKind)
        assertEquals(OAuthProviderId.GOOGLE, form.offeredOAuthProvider)
        assertNull(AccountFormPolicy.applyPreset(AccountFormPolicy.blank(), ProviderPresets.require("qq")).offeredOAuthProvider)
        assertTrue(AccountFormPolicy.validate(form).isEmpty())

        assertThrows("the link is mandatory for a browser sign-in", IllegalArgumentException::class.java) { AccountFormPolicy.toAccountJson(form) }
        assertThrows("and forbidden otherwise", IllegalArgumentException::class.java) { AccountFormPolicy.toAccountJson(form.copy(oauthProvider = null), OAuthLink("google")) }

        val link = OAuthLink("google", authorizedAt = 5L, expiresAt = 9L, needsReauth = false)
        val document = AccountFormPolicy.toAccountJson(form, link)
        val json = MailJson.format.parseToJsonElement(document).jsonObject
        assertEquals(setOf("provider", "address", "auth", "oauth", "receive"), json.keys)
        assertEquals("xoauth2", json.getValue("auth").jsonPrimitive.content)
        assertEquals("google", json.getValue("oauth").jsonObject.getValue("provider").jsonPrimitive.content)
        assertEquals("5", json.getValue("oauth").jsonObject.getValue("authorizedAt").jsonPrimitive.content)

        val parsed = MailAccountOptions.parse(document, SecretKind.OAUTH2)
        assertEquals(AuthMethod.XOAUTH2, parsed.auth)
        assertEquals(link, parsed.oauth)

        val reopened = AccountFormPolicy.fromAccountJson(document, SecretKind.OAUTH2)
        assertEquals(OAuthProviderId.GOOGLE, reopened.oauthProvider)
        assertEquals(AuthMethod.XOAUTH2, reopened.auth)
        assertEquals(SecretKind.OAUTH2, reopened.secretKind)
        assertNull("a pasted-token record never claims a browser sign-in", AccountFormPolicy.fromAccountJson("""{"address":"a@gmail.com","provider":"gmail","auth":"xoauth2"}""", SecretKind.ACCESS_TOKEN).oauthProvider)

        val moved = AccountFormPolicy.applyPreset(reopened, ProviderPresets.require("outlook"))
        assertNull("a preset of another provider drops the sign-in", moved.oauthProvider)
        assertEquals(AuthMethod.XOAUTH2, moved.auth)
        assertNull(AccountFormPolicy.applyPreset(reopened, null).oauthProvider)
        assertEquals(OAuthProviderId.GOOGLE, AccountFormPolicy.applyPreset(reopened, gmail).oauthProvider)
    }
}
