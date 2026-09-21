package io.github.supermonster003.autojs6.plugin.angus.mail.oauth

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The build's OAuth 2.0 clients and their redirect URIs (roadmap P9). */
class OAuthClientsTest {

    @Test
    fun `the redirect URIs follow the application id and the reversed Google client id`() {
        val clients = OAuthClients(" 123-abc.apps.googleusercontent.com ", "7d0f-ms", "consumers", "io.github.example.mail")
        assertEquals("io.github.example.mail://oauth2/microsoft", clients.redirectUri(OAuthProviderId.MICROSOFT))
        assertEquals("com.googleusercontent.apps.123-abc:/oauth2redirect", clients.redirectUri(OAuthProviderId.GOOGLE))
        assertEquals("com.googleusercontent.apps.123-abc", OAuthClients.googleScheme("123-abc.apps.googleusercontent.com"))
        assertEquals("com.googleusercontent.apps.raw", OAuthClients.googleScheme("raw"))
        assertEquals("123-abc.apps.googleusercontent.com", clients.clientId(OAuthProviderId.GOOGLE))
        assertEquals("7d0f-ms", clients.clientId(OAuthProviderId.MICROSOFT))
        assertEquals(listOf(OAuthProviderId.GOOGLE, OAuthProviderId.MICROSOFT), clients.configured)
        assertEquals("https://login.microsoftonline.com/consumers/oauth2/v2.0/token", clients.provider(OAuthProviderId.MICROSOFT).tokenEndpoint)
    }

    @Test
    fun `a blank client id means the provider is not configured in this build`() {
        val clients = OAuthClients("", "  ", "", "io.github.example.mail")
        assertTrue(clients.configured.isEmpty())
        assertFalse(clients.isConfigured(OAuthProviderId.GOOGLE))
        assertNull(clients.clientIdOrNull(OAuthProviderId.MICROSOFT))
        val error = assertThrows(MailException::class.java) { clients.clientId(OAuthProviderId.GOOGLE) }
        assertEquals(MailErrorCode.AUTH_FAILED, error.code)
        assertEquals(OAuthClients.NOT_CONFIGURED, error.details)
        assertFalse(error.retryable)
        assertThrows(MailException::class.java) { clients.redirectUri(OAuthProviderId.GOOGLE) }
        assertEquals("the Microsoft redirect needs no client", "io.github.example.mail://oauth2/microsoft", clients.redirectUri(OAuthProviderId.MICROSOFT))
        assertEquals("an empty tenant falls back to consumers", "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize", clients.provider(OAuthProviderId.MICROSOFT).authorizationEndpoint)
    }

    @Test
    fun `a custom tenant lands in the Microsoft endpoints`() {
        val clients = OAuthClients(null, "ms", "common", "app")
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/authorize", clients.provider(OAuthProviderId.MICROSOFT).authorizationEndpoint)
        assertThrows(IllegalArgumentException::class.java) { OAuthClients(null, "ms", "bad tenant", "app") }
    }
}
