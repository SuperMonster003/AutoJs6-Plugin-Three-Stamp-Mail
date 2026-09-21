package io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth

import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

/**
 * One authorization request of the code flow with PKCE (roadmap P9): the URL the system browser
 * is sent to. The verifier and the state stay with the request that opened the browser; the
 * redirect that comes back is matched against them by [AuthorizationResponses.parse].
 */
class AuthorizationRequest(
    val provider: OAuthProvider,
    val clientId: String,
    val redirectUri: String,
    val state: String,
    val pkce: PkceChallenge,
    /** The address the browser should offer first, when known (a re-authorization). */
    val loginHint: String? = null,
) {
    init {
        require(clientId.isNotBlank()) { "clientId must not be blank" }
        require(redirectUri.isNotBlank()) { "redirectUri must not be blank" }
        require(state.isNotBlank()) { "state must not be blank" }
    }

    /** The scopes of the authorization request: the mail scopes and the identity scopes, once each. */
    val scopes: List<String> get() = (provider.scopes + provider.identityScopes).distinct()

    fun url(): String {
        val parameters = LinkedHashMap<String, String>()
        parameters["response_type"] = "code"
        parameters["client_id"] = clientId
        parameters["redirect_uri"] = redirectUri
        parameters["scope"] = scopes.joinToString(" ")
        parameters["state"] = state
        parameters["code_challenge"] = pkce.challenge
        parameters["code_challenge_method"] = pkce.method
        loginHint?.takeIf { it.isNotBlank() }?.let { parameters["login_hint"] = it }
        provider.authorizationParameters.forEach { (key, value) -> parameters.putIfAbsent(key, value) }
        val separator = if ('?' in provider.authorizationEndpoint) "&" else "?"
        return provider.authorizationEndpoint + separator + parameters.entries.joinToString("&") { (key, value) -> encode(key) + "=" + encode(value) }
    }

    private fun encode(text: String): String = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
}

/** What the browser's redirect carried. */
sealed class AuthorizationResponse {
    /** The provider issued a code for this request. */
    data class Granted(val code: String) : AuthorizationResponse()

    /** The provider (or the user) refused; `error` is the OAuth error code, the description is optional. */
    data class Denied(val error: String, val description: String?) : AuthorizationResponse()

    /** The redirect does not belong to this request: another redirect URI, a missing or foreign `state`, or no code. */
    data class Rejected(val reason: String) : AuthorizationResponse()
}

object AuthorizationResponses {

    /**
     * Matches a redirect URI the browser delivered against the request that opened it: the
     * redirect must start with the registered redirect URI (scheme, authority and path compared
     * literally, case-insensitive for the scheme), its `state` must equal the request's in constant
     * time, and only then are `code` or `error` looked at. Nothing of the redirect is echoed into
     * the reasons except the error code of a refusal.
     */
    fun parse(redirect: String, expectedRedirectUri: String, expectedState: String): AuthorizationResponse {
        val uri = try {
            URI(redirect)
        } catch (e: Exception) {
            return AuthorizationResponse.Rejected("the redirect is not a URI")
        }
        val expected = try {
            URI(expectedRedirectUri)
        } catch (e: Exception) {
            return AuthorizationResponse.Rejected("the registered redirect URI is not a URI")
        }
        if (!sameTarget(uri, expected)) return AuthorizationResponse.Rejected("the redirect targets another URI")
        val query = queryOf(uri)
        if (!Pkce.sameState(expectedState, query["state"])) return AuthorizationResponse.Rejected("the redirect carries no matching state")
        query["error"]?.takeIf { it.isNotBlank() }?.let { error ->
            return AuthorizationResponse.Denied(error.take(MAX_ERROR_LENGTH), query["error_description"]?.take(MAX_DESCRIPTION_LENGTH))
        }
        val code = query["code"]?.takeIf { it.isNotBlank() } ?: return AuthorizationResponse.Rejected("the redirect carries no code")
        return AuthorizationResponse.Granted(code)
    }

    private fun sameTarget(actual: URI, expected: URI): Boolean =
        actual.scheme.equals(expected.scheme, ignoreCase = true) &&
            (actual.rawAuthority ?: "").equals(expected.rawAuthority ?: "", ignoreCase = true) &&
            (actual.rawPath ?: "").trimEnd('/') == (expected.rawPath ?: "").trimEnd('/')

    /** The query parameters, decoded once; a repeated key keeps its first value. */
    fun queryOf(uri: URI): Map<String, String> {
        val raw = uri.rawQuery ?: return emptyMap()
        val result = LinkedHashMap<String, String>()
        raw.split('&').forEach { pair ->
            if (pair.isEmpty()) return@forEach
            val key = decode(pair.substringBefore('='))
            val value = if ('=' in pair) decode(pair.substringAfter('=')) else ""
            result.putIfAbsent(key, value)
        }
        return result
    }

    private fun decode(text: String): String = try {
        URLDecoder.decode(text, "UTF-8")
    } catch (e: IllegalArgumentException) {
        text
    }

    private const val MAX_ERROR_LENGTH = 64
    private const val MAX_DESCRIPTION_LENGTH = 300
}
