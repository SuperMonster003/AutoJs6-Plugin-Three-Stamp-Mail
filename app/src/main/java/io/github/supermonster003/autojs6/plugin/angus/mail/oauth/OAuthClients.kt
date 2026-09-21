package io.github.supermonster003.autojs6.plugin.angus.mail.oauth

import android.content.Context
import io.github.supermonster003.autojs6.plugin.angus.mail.R
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProvider
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviders

/**
 * The OAuth 2.0 client registrations this build carries (roadmap P9): the client ids come from the
 * git-ignored `oauth-clients.properties` through `resValue` (no secret: both providers are public
 * clients with PKCE), the Microsoft tenant likewise, and the redirect URIs follow from them.
 * A provider without a client id is "not configured": the settings page hides its sign-in and a
 * refresh of an account that needs it fails with `AUTH_FAILED`.
 *
 * Redirect URIs the maintainer registers with the providers:
 * - Microsoft: `<applicationId>://oauth2/microsoft` (a custom scheme of the "Mobile and desktop
 *   applications" platform);
 * - Google: `com.googleusercontent.apps.<client id prefix>:/oauth2redirect`, the reversed client
 *   id scheme an Android client type implies (the manifest placeholder `oauthGoogleScheme`).
 */
class OAuthClients(
    googleClientId: String?,
    microsoftClientId: String?,
    microsoftTenant: String?,
    private val applicationId: String,
) {
    private val googleClientId = googleClientId?.trim().orEmpty()
    private val microsoftClientId = microsoftClientId?.trim().orEmpty()
    private val microsoft: OAuthProvider = OAuthProviders.microsoft(microsoftTenant?.trim()?.takeIf { it.isNotEmpty() } ?: OAuthProviders.MICROSOFT_DEFAULT_TENANT)

    val configured: List<OAuthProviderId> get() = OAuthProviderId.entries.filter { isConfigured(it) }

    fun isConfigured(id: OAuthProviderId): Boolean = clientIdOrNull(id) != null

    fun clientIdOrNull(id: OAuthProviderId): String? = when (id) {
        OAuthProviderId.GOOGLE -> googleClientId.takeIf { it.isNotEmpty() }
        OAuthProviderId.MICROSOFT -> microsoftClientId.takeIf { it.isNotEmpty() }
    }

    fun clientId(id: OAuthProviderId): String = clientIdOrNull(id)
        ?: throw MailException(MailErrorCode.AUTH_FAILED, "this build has no OAuth 2.0 client id for ${id.id}; sign in with a token or an app password instead", NOT_CONFIGURED, retryable = false)

    fun provider(id: OAuthProviderId): OAuthProvider = when (id) {
        OAuthProviderId.GOOGLE -> OAuthProviders.GOOGLE
        OAuthProviderId.MICROSOFT -> microsoft
    }

    fun redirectUri(id: OAuthProviderId): String = when (id) {
        OAuthProviderId.GOOGLE -> googleScheme(clientId(id)) + ":" + GOOGLE_REDIRECT_PATH
        OAuthProviderId.MICROSOFT -> "$applicationId://$MICROSOFT_REDIRECT_HOST$MICROSOFT_REDIRECT_PATH"
    }

    companion object {
        const val NOT_CONFIGURED = "client not configured"
        const val MICROSOFT_REDIRECT_HOST = "oauth2"
        const val MICROSOFT_REDIRECT_PATH = "/microsoft"
        const val GOOGLE_REDIRECT_PATH = "/oauth2redirect"
        const val GOOGLE_SCHEME_PREFIX = "com.googleusercontent.apps."
        private const val GOOGLE_CLIENT_SUFFIX = ".apps.googleusercontent.com"

        /** The reversed-client-id scheme Google's Android clients redirect to. */
        fun googleScheme(clientId: String): String = GOOGLE_SCHEME_PREFIX + clientId.trim().removeSuffix(GOOGLE_CLIENT_SUFFIX)

        fun of(context: Context): OAuthClients {
            val app = context.applicationContext
            return OAuthClients(
                googleClientId = app.getString(R.string.oauth_google_client_id),
                microsoftClientId = app.getString(R.string.oauth_microsoft_client_id),
                microsoftTenant = app.getString(R.string.oauth_microsoft_tenant),
                applicationId = app.packageName,
            )
        }
    }
}
