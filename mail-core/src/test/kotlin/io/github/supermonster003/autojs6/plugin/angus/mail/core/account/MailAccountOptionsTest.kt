package io.github.supermonster003.autojs6.plugin.angus.mail.core.account

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MailAccountOptionsTest {

    @Test
    fun providerPresetFillsEveryEndpoint() {
        val account = parse("""{"provider":"qq","address":"user@qq.com"}""")
        assertEquals("user@qq.com", account.username)
        assertEquals(AuthMethod.PASSWORD, account.auth)
        assertEquals(MailProtocol.IMAP, account.receive)
        assertEquals(MailEndpoint("imap.qq.com", 993, TlsMode.SSL), account.imap)
        assertEquals(MailEndpoint("pop.qq.com", 995, TlsMode.SSL), account.pop3)
        assertEquals(MailEndpoint("smtp.qq.com", 465, TlsMode.SSL), account.smtp)
        assertEquals("qq", account.provider!!.id)
        assertEquals(MailTimeouts(), account.timeouts)
        assertFalse(account.trustAll)
        assertFalse(account.debug)
        assertFalse(account.insecure)
        assertTrue(account.clientId.isEmpty())
        assertNull(account.displayName)
    }

    @Test
    fun explicitFieldsOverrideThePreset() {
        val account = parse(
            """{"provider":"gmail","address":"me@gmail.com","user":"me","name":"Me",
               "smtp":{"port":587,"tls":"starttls"},"imap":{"host":"imap.example.org"},
               "timeout":{"connect":1000,"read":2000},"tls":{"trustAll":true},"debug":true,"receive":"pop3"}""",
        )
        assertEquals("me", account.username)
        assertEquals("Me", account.displayName)
        assertEquals(MailEndpoint("smtp.gmail.com", 587, TlsMode.STARTTLS), account.smtp)
        assertEquals("a new host without a port falls back to the protocol default", MailEndpoint("imap.example.org", 993, TlsMode.SSL), account.imap)
        assertEquals(MailEndpoint("pop.gmail.com", 995, TlsMode.SSL), account.pop3)
        assertEquals(MailProtocol.POP3, account.receive)
        assertEquals(MailTimeouts(1000, 2000, 2000), account.timeouts)
        assertTrue(account.trustAll)
        assertTrue(account.insecure)
        assertTrue(account.debug)
    }

    @Test
    fun tlsOverrideWithoutPortPicksTheConventionalPort() {
        val account = parse("""{"provider":"gmail","address":"me@gmail.com","imap":{"tls":"starttls"},"smtp":{"host":"smtp.example.org","tls":"none"}}""")
        assertEquals(MailEndpoint("imap.gmail.com", 143, TlsMode.STARTTLS), account.imap)
        assertEquals(MailEndpoint("smtp.example.org", 25, TlsMode.NONE), account.smtp)
        assertTrue(account.insecure)
    }

    @Test
    fun hostsWithoutPresetUseSecureDefaults() {
        val account = parse("""{"address":"me@example.org","imap":{"host":"mail.example.org"},"smtp":{"host":"mail.example.org"}}""")
        assertEquals(MailEndpoint("mail.example.org", 993, TlsMode.SSL), account.imap)
        assertEquals(MailEndpoint("mail.example.org", 465, TlsMode.SSL), account.smtp)
        assertNull(account.pop3)
        assertNull(account.provider)
        assertEquals(listOf(MailProtocol.IMAP, MailProtocol.SMTP), account.configuredProtocols)
    }

    @Test
    fun receiveDefaultsToPop3WhenOnlyPop3Exists() {
        val account = parse("""{"address":"me@example.org","pop3":{"host":"pop.example.org"}}""")
        assertEquals(MailProtocol.POP3, account.receive)
        assertEquals(account.pop3, account.receiveEndpoint)
        val sendOnly = parse("""{"address":"me@example.org","smtp":{"host":"smtp.example.org"}}""")
        assertEquals(MailProtocol.IMAP, sendOnly.receive)
        assertNull(sendOnly.receiveEndpoint)
    }

    @Test
    fun accessTokenSelectsXoauth2AndPasswordSelectsPassword() {
        assertEquals(AuthMethod.XOAUTH2, parse("""{"provider":"gmail","address":"me@gmail.com"}""", SecretKind.ACCESS_TOKEN).auth)
        assertEquals(AuthMethod.XOAUTH2, parse("""{"provider":"gmail","address":"me@gmail.com","auth":"xoauth2"}""", SecretKind.ACCESS_TOKEN).auth)
        assertEquals(AuthMethod.PASSWORD, parse("""{"provider":"gmail","address":"me@gmail.com","auth":"password"}""").auth)
    }

    @Test
    fun illegalAuthCombinationsAreExplained() {
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com"}""", SecretKind.NONE, "password or an access token")
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com","auth":"xoauth2"}""", SecretKind.PASSWORD, "access token was not supplied")
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com","auth":"password"}""", SecretKind.ACCESS_TOKEN, "password was not supplied")
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com","auth":"ntlm"}""", SecretKind.PASSWORD, "'auth' must be one of")
        val unsupported = failure("""{"provider":"outlook","address":"me@outlook.com"}""", SecretKind.PASSWORD)
        assertEquals(MailErrorCode.AUTH_MECHANISM_UNSUPPORTED, unsupported.code)
        assertTrue(unsupported.message.contains("xoauth2"))
        assertEquals(ProviderPresets.require("outlook").authHint, unsupported.details)
    }

    @Test
    fun secretsInsideTheJsonAreRejected() {
        listOf("password", "accessToken", "tokenProvider", "token", "secret").forEach { field ->
            val error = failure("""{"provider":"qq","address":"me@qq.com","$field":"hunter2"}""", SecretKind.PASSWORD)
            assertEquals(MailErrorCode.INVALID_ARGUMENT, error.code)
            assertTrue(error.message.contains("'$field'"))
            assertFalse("the value must not be echoed", error.message.contains("hunter2") || error.details.orEmpty().contains("hunter2"))
        }
    }

    @Test
    fun shapeErrorsNameTheField() {
        assertInvalid("[]", SecretKind.PASSWORD, "JSON object")
        assertInvalid("{", SecretKind.PASSWORD, "not valid JSON")
        assertInvalid("""{"provider":"qq"}""", SecretKind.PASSWORD, "'address' is required")
        assertInvalid("""{"address":"not an address","imap":{"host":"x.example.org"}}""", SecretKind.PASSWORD, "not a valid email address")
        assertInvalid("""{"address":"me@example.org"}""", SecretKind.PASSWORD, "no imap, pop3 or smtp endpoint")
        assertInvalid("""{"address":"me@example.org","imap":{"port":993}}""", SecretKind.PASSWORD, "'imap.host' is required")
        assertInvalid("""{"address":"me@example.org","imap":"imap.example.org"}""", SecretKind.PASSWORD, "'imap' must be an object")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org","port":"993"}}""", SecretKind.PASSWORD, "'imap.port' must be an integer")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org","port":70000}}""", SecretKind.PASSWORD, "'imap.port' must be within")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org","tls":"tls"}}""", SecretKind.PASSWORD, "'imap.tls' must be one of")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org","ssl":true}}""", SecretKind.PASSWORD, "unknown field 'imap.ssl'")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org"},"receive":"pop3"}""", SecretKind.PASSWORD, "no pop3 endpoint")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org"},"receive":"smtp"}""", SecretKind.PASSWORD, "'receive' must be imap or pop3")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org"},"timeout":{"connect":0}}""", SecretKind.PASSWORD, "connect timeout must be within")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org"},"timeout":{"idle":5}}""", SecretKind.PASSWORD, "unknown field 'timeout.idle'")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org"},"tls":{"trustAll":"yes"}}""", SecretKind.PASSWORD, "'tls.trustAll' must be a boolean")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org"},"debug":1}""", SecretKind.PASSWORD, "'debug' must be a boolean")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org"},"clientId":{"name":1}}""", SecretKind.PASSWORD, "'clientId.name' must be a string")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org"},"user":""}""", SecretKind.PASSWORD, "'user' must not be blank")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org"},"nickname":"x"}""", SecretKind.PASSWORD, "unknown account field 'nickname'")
        assertInvalid("""{"address":"me@example.org","imap":{"host":"h.example.org"},"name":"${"n".repeat(MailLimits.MAX_OPTION_STRING_LENGTH + 1)}"}""", SecretKind.PASSWORD, "'name' exceeds")
        assertEquals(MailErrorCode.PROVIDER_UNKNOWN, failure("""{"provider":"aol","address":"me@aol.com"}""", SecretKind.PASSWORD).code)
    }

    @Test
    fun clientIdDefaultsOnlyForProvidersThatRequireIt() {
        val defaults = MailAccountOptions.Defaults(clientId = mapOf("name" to "AutoJs6-Plugin-Angus-Mail", "version" to "1.0.0"))
        assertEquals(defaults.clientId, parse("""{"provider":"163","address":"me@163.com"}""", defaults = defaults).clientId)
        assertEquals(emptyMap<String, String>(), parse("""{"provider":"qq","address":"me@qq.com"}""", defaults = defaults).clientId)
        assertEquals(mapOf("name" to "Script", "vendor" to "me"), parse("""{"provider":"163","address":"me@163.com","clientId":{"name":"Script","vendor":"me"}}""", defaults = defaults).clientId)
        val tooMany = (1..MailLimits.MAX_CLIENT_ID_ENTRIES + 1).joinToString(",") { "\"k$it\":\"v\"" }
        assertInvalid("""{"provider":"163","address":"me@163.com","clientId":{$tooMany}}""", SecretKind.PASSWORD, "more than")
    }

    @Test
    fun nullFieldsBehaveLikeAbsentFields() {
        val account = parse("""{"provider":"qq","address":"me@qq.com","user":null,"name":null,"pop3":null,"timeout":null,"tls":null,"debug":null,"clientId":null}""")
        assertEquals("me@qq.com", account.username)
        assertEquals(MailEndpoint("pop.qq.com", 995, TlsMode.SSL), account.pop3)
        assertEquals(MailTimeouts(), account.timeouts)
    }

    @Test
    fun browserSignInRecordsCarryTheOAuthLinkAndAuthenticateWithXoauth2() {
        val account = parse(
            """{"provider":"gmail","address":"me@gmail.com","auth":"xoauth2","oauth":{"provider":"google","authorizedAt":1700000000000,"expiresAt":1700003600000,"needsReauth":false}}""",
            SecretKind.OAUTH2,
        )
        assertEquals(AuthMethod.XOAUTH2, account.auth)
        val link = account.oauth!!
        assertEquals("google", link.provider)
        assertEquals(1700000000000L, link.authorizedAt)
        assertEquals(1700003600000L, link.expiresAt)
        assertFalse(link.needsReauth)
        assertNull("no oauth object on a password account", parse("""{"provider":"qq","address":"a@qq.com"}""").oauth)
        val minimal = parse("""{"provider":"outlook","address":"b@outlook.com","oauth":{"provider":"microsoft"}}""", SecretKind.OAUTH2)
        assertEquals(AuthMethod.XOAUTH2, minimal.auth)
        assertEquals(0L, minimal.oauth!!.authorizedAt)
        assertFalse(minimal.oauth!!.needsReauth)
    }

    @Test
    fun theOAuthLinkGoesWithTheBrowserSignInOnly() {
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com"}""", SecretKind.OAUTH2, "'oauth' is required")
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com","oauth":{"provider":"google"}}""", SecretKind.PASSWORD, "browser sign-in only")
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com","oauth":{"provider":"google"}}""", SecretKind.ACCESS_TOKEN, "browser sign-in only")
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com","oauth":{}}""", SecretKind.OAUTH2, "'oauth.provider' is required")
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com","oauth":{"provider":"yahoo"}}""", SecretKind.OAUTH2, "must be one of google, microsoft")
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com","oauth":{"provider":"google","accessToken":"x"}}""", SecretKind.OAUTH2, "accessToken")
        assertInvalid("""{"provider":"gmail","address":"me@gmail.com","auth":"password","oauth":{"provider":"google"}}""", SecretKind.OAUTH2, "signed in through the browser")
    }

    private fun parse(json: String, secret: SecretKind = SecretKind.PASSWORD, defaults: MailAccountOptions.Defaults = MailAccountOptions.Defaults()): MailAccount =
        MailAccountOptions.parse(json, secret, defaults)

    private fun failure(json: String, secret: SecretKind): MailException {
        try {
            MailAccountOptions.parse(json, secret)
        } catch (e: MailException) {
            return e
        }
        fail("expected a MailException for $json")
        throw AssertionError()
    }

    private fun assertInvalid(json: String, secret: SecretKind, expectedMessage: String) {
        val error = failure(json, secret)
        assertEquals(error.message, MailErrorCode.INVALID_ARGUMENT, error.code)
        assertTrue("expected '$expectedMessage' in '${error.message}'", error.message.contains(expectedMessage))
        assertFalse(error.retryable)
    }
}
