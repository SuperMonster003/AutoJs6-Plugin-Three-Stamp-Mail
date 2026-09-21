package io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth

import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import kotlinx.serialization.Serializable
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException

/** One HTTPS `application/x-www-form-urlencoded` POST; the application supplies the transport. */
fun interface FormPoster {
    /** Posts [form] to [url]; throws [IOException] when the endpoint is unreachable. The body is text (UTF-8), capped by the transport. */
    fun post(url: String, form: List<Pair<String, String>>): FormAnswer
}

class FormAnswer(val status: Int, val body: String)

/** What a successful code exchange yields: the tokens to store and the address the id token named, if any. */
class TokenGrant(val tokens: OAuthTokens, val email: String?)

/**
 * The token endpoint side of the flow (RFC 6749 sections 4.1.3 and 6, RFC 7636): exchanges an
 * authorization code for tokens and refreshes an access token. Public client: no client secret,
 * the PKCE verifier proves the exchange. Every failure is a [MailException]: `AUTH_FAILED` when
 * the provider refuses the grant (the account then needs a new sign-in), `SERVER_ERROR` when the
 * provider is broken, `CONNECT_FAILED` / `TIMEOUT` when it cannot be reached. No token, code or
 * verifier appears in any message.
 */
class TokenClient(private val http: FormPoster, private val clock: () -> Long = System::currentTimeMillis) {

    fun exchange(provider: OAuthProvider, clientId: String, redirectUri: String, code: String, verifier: String): TokenGrant {
        require(code.isNotBlank() && verifier.isNotBlank()) { "code and verifier must not be blank" }
        val answer = post(
            provider.tokenEndpoint,
            listOf(
                "grant_type" to "authorization_code",
                "client_id" to clientId,
                "redirect_uri" to redirectUri,
                "code" to code,
                "code_verifier" to verifier,
            ),
        )
        val response = parse(answer, "exchange")
        return TokenGrant(tokens(response, previousRefreshToken = null), IdTokenClaims.emailOf(response.idToken))
    }

    /** A new access token for [tokens]; the refresh token is replaced when the provider rotates it and kept otherwise. */
    fun refresh(provider: OAuthProvider, clientId: String, tokens: OAuthTokens): OAuthTokens {
        val refreshToken = tokens.refreshToken?.takeIf { it.isNotBlank() }
            ?: throw MailException(MailErrorCode.AUTH_FAILED, "the account has no refresh token; sign in again", REAUTHORIZE, retryable = false)
        val form = ArrayList<Pair<String, String>>()
        form += "grant_type" to "refresh_token"
        form += "client_id" to clientId
        form += "refresh_token" to refreshToken
        if (provider.refreshWithScopes) form += "scope" to provider.scopes.joinToString(" ")
        val response = parse(post(provider.tokenEndpoint, form), "refresh")
        return tokens(response, previousRefreshToken = refreshToken)
    }

    /** Best effort: tells the provider to forget a token; false when the provider has no revocation endpoint or refused. */
    fun revoke(provider: OAuthProvider, clientId: String, token: String): Boolean {
        val endpoint = provider.revocationEndpoint ?: return false
        return try {
            val answer = http.post(endpoint, listOf("token" to token, "client_id" to clientId))
            answer.status in 200..299
        } catch (e: IOException) {
            false
        }
    }

    private fun post(url: String, form: List<Pair<String, String>>): FormAnswer = try {
        http.post(url, form)
    } catch (e: SocketTimeoutException) {
        throw MailException(MailErrorCode.TIMEOUT, "the token endpoint did not answer in time", hostOf(url), retryable = true)
    } catch (e: InterruptedIOException) {
        throw MailException(MailErrorCode.CANCELLED, "the token request was interrupted", hostOf(url), retryable = false)
    } catch (e: IOException) {
        throw MailException(MailErrorCode.CONNECT_FAILED, "the token endpoint could not be reached", "${hostOf(url)}: ${e.javaClass.simpleName}", retryable = true)
    }

    private fun parse(answer: FormAnswer, grant: String): TokenResponse {
        val body = answer.body
        if (answer.status in 200..299) {
            val response = try {
                MailJson.format.decodeFromString(TokenResponse.serializer(), body)
            } catch (e: Exception) {
                throw MailException(MailErrorCode.SERVER_ERROR, "the token endpoint answered the $grant with an unreadable document", "HTTP ${answer.status}", retryable = true)
            }
            if (response.accessToken.isNullOrBlank()) {
                throw MailException(MailErrorCode.SERVER_ERROR, "the token endpoint answered the $grant without an access token", "HTTP ${answer.status}", retryable = false)
            }
            return response
        }
        val error = try {
            MailJson.format.decodeFromString(TokenErrorResponse.serializer(), body)
        } catch (e: Exception) {
            null
        }
        val code = error?.error?.take(MAX_ERROR_LENGTH)
        val description = error?.errorDescription?.take(MAX_DESCRIPTION_LENGTH)
        if (answer.status >= 500 || code == "server_error" || code == "temporarily_unavailable") {
            throw MailException(MailErrorCode.SERVER_ERROR, "the token endpoint failed the $grant", listOfNotNull("HTTP ${answer.status}", code, description).joinToString(": "), retryable = true)
        }
        if (code == null) {
            throw MailException(MailErrorCode.SERVER_ERROR, "the token endpoint refused the $grant without an OAuth error", "HTTP ${answer.status}", retryable = false)
        }
        val message = when (code) {
            "invalid_grant" -> if (grant == "refresh") "the authorization expired or was revoked; sign in again" else "the authorization code was refused; sign in again"
            "invalid_client", "unauthorized_client" -> "the provider does not accept this build's client id"
            "invalid_scope" -> "the provider refused the requested scopes"
            "access_denied", "consent_required", "interaction_required" -> "the provider needs the user to sign in again"
            else -> "the provider refused the $grant"
        }
        throw MailException(MailErrorCode.AUTH_FAILED, message, listOfNotNull(code, description).joinToString(": ").ifEmpty { REAUTHORIZE }, retryable = false)
    }

    private fun tokens(response: TokenResponse, previousRefreshToken: String?): OAuthTokens {
        val lifetimeMs = (response.expiresIn?.takeIf { it > 0 }?.times(1000L)) ?: OAuthTokens.DEFAULT_LIFETIME_MS
        return OAuthTokens(
            accessToken = requireNotNull(response.accessToken),
            refreshToken = response.refreshToken?.takeIf { it.isNotBlank() } ?: previousRefreshToken,
            expiresAt = clock() + lifetimeMs,
            scope = response.scope?.takeIf { it.isNotBlank() },
        )
    }

    private fun hostOf(url: String): String = url.substringAfter("://").substringBefore('/')

    @Serializable
    private class TokenResponse(
        @kotlinx.serialization.SerialName("access_token") val accessToken: String? = null,
        @kotlinx.serialization.SerialName("token_type") val tokenType: String? = null,
        @kotlinx.serialization.SerialName("expires_in") val expiresIn: Long? = null,
        @kotlinx.serialization.SerialName("refresh_token") val refreshToken: String? = null,
        val scope: String? = null,
        @kotlinx.serialization.SerialName("id_token") val idToken: String? = null,
    )

    @Serializable
    private class TokenErrorResponse(
        val error: String? = null,
        @kotlinx.serialization.SerialName("error_description") val errorDescription: String? = null,
    )

    companion object {
        /** The details text of an `AUTH_FAILED` that a new sign-in cures. */
        const val REAUTHORIZE = "reauthorize"

        /** True for the failures of [refresh] that only a new browser sign-in cures (the settings page marks the account). */
        fun needsReauthorization(error: MailException): Boolean = error.code == MailErrorCode.AUTH_FAILED

        private const val MAX_ERROR_LENGTH = 64
        private const val MAX_DESCRIPTION_LENGTH = 300
    }
}
