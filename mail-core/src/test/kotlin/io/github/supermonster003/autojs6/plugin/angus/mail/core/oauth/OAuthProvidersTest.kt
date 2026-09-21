package io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The provider table of the browser sign-in (roadmap P9, D31). */
class OAuthProvidersTest {

    @Test
    fun `Gmail signs in with Google, Outlook and Microsoft 365 with Microsoft, nothing else at all`() {
        assertEquals(OAuthProviderId.GOOGLE, OAuthProviders.forPreset("gmail"))
        assertEquals(OAuthProviderId.MICROSOFT, OAuthProviders.forPreset("outlook"))
        assertEquals(OAuthProviderId.MICROSOFT, OAuthProviders.forPreset("office365"))
        assertNull(OAuthProviders.forPreset("qq"))
        assertNull(OAuthProviders.forPreset(null))
        val withSignIn = ProviderPresets.all.filter { OAuthProviders.forPreset(it.id) != null }.map { it.id }
        assertEquals(listOf("gmail", "outlook", "office365"), withSignIn)
        assertTrue("every preset with a browser sign-in accepts XOAUTH2", withSignIn.all { "xoauth2" in ProviderPresets.require(it).auth })
        assertEquals(OAuthProviderId.GOOGLE, OAuthProviderId.fromId("google"))
        assertNull(OAuthProviderId.fromId("GOOGLE"))
        assertNull(OAuthProviderId.fromId(null))
    }

    @Test
    fun `Google needs offline access and a consent prompt, Microsoft repeats its scopes on a refresh`() {
        assertEquals(mapOf("access_type" to "offline", "prompt" to "consent"), OAuthProviders.GOOGLE.authorizationParameters)
        assertEquals("https://oauth2.googleapis.com/revoke", OAuthProviders.GOOGLE.revocationEndpoint)
        assertFalse(OAuthProviders.GOOGLE.refreshWithScopes)
        val microsoft = OAuthProviders.microsoft()
        assertTrue(microsoft.refreshWithScopes)
        assertNull(microsoft.revocationEndpoint)
        assertTrue(microsoft.authorizationParameters.isEmpty())
        assertEquals(listOf("openid", "email"), microsoft.identityScopes)
        assertTrue(microsoft.scopes.contains("offline_access"))
    }

    @Test
    fun `the Microsoft tenant is validated and lands in both endpoints`() {
        val common = OAuthProviders.microsoft("common")
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/authorize", common.authorizationEndpoint)
        assertEquals("https://login.microsoftonline.com/common/oauth2/v2.0/token", common.tokenEndpoint)
        assertEquals("consumers", OAuthProviders.MICROSOFT_DEFAULT_TENANT)
        OAuthProviders.microsoft("contoso.onmicrosoft.com")
        OAuthProviders.microsoft("9188040d-6c67-4c5b-b112-36a304b66dad")
        assertThrows(IllegalArgumentException::class.java) { OAuthProviders.microsoft("") }
        assertThrows(IllegalArgumentException::class.java) { OAuthProviders.microsoft("a/b") }
        assertThrows(IllegalArgumentException::class.java) { OAuthProviders.microsoft("x".repeat(129)) }
    }

    @Test
    fun `every endpoint must be https`() {
        assertThrows(IllegalArgumentException::class.java) {
            OAuthProviders.GOOGLE.copy(tokenEndpoint = "http://oauth2.googleapis.com/token")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OAuthProviders.GOOGLE.copy(revocationEndpoint = "https://")
        }
        assertThrows(IllegalArgumentException::class.java) {
            OAuthProviders.GOOGLE.copy(scopes = emptyList())
        }
    }
}
