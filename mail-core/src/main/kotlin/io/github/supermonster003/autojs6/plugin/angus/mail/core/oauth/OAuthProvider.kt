package io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth

/** The identity providers the plugin can sign in with through the system browser (roadmap P9). */
enum class OAuthProviderId(val id: String) {
    GOOGLE("google"),
    MICROSOFT("microsoft");

    companion object {
        fun fromId(id: String?): OAuthProviderId? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The public parameters of one OAuth 2.0 authorization-code flow with PKCE: where the browser is
 * sent, where the code is exchanged, which scopes the mail protocols need and which extra
 * parameters the provider wants on the authorization request. Client ids and redirect URIs are
 * the application's business (they come from the build); nothing here is secret. Every endpoint
 * must be `https`, which is also what the application's HTTP client enforces.
 */
data class OAuthProvider(
    val id: OAuthProviderId,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    /** The scopes the mail protocols need plus the refresh-token scope; sent with the authorization and the refresh grants. */
    val scopes: List<String>,
    /** OpenID scopes asked once at sign-in so the account's address is known from the id token; never on a refresh. */
    val identityScopes: List<String> = emptyList(),
    /** Extra query parameters of the authorization request (Google needs offline access and a consent prompt for a refresh token). */
    val authorizationParameters: Map<String, String> = emptyMap(),
    /** Where a token can be revoked, when the provider offers it. */
    val revocationEndpoint: String? = null,
    /** The provider presets (`providers.json`) whose accounts sign in through this provider. */
    val presetIds: List<String>,
) {
    init {
        requireHttps("authorizationEndpoint", authorizationEndpoint)
        requireHttps("tokenEndpoint", tokenEndpoint)
        revocationEndpoint?.let { requireHttps("revocationEndpoint", it) }
        require(scopes.isNotEmpty()) { "scopes must not be empty" }
    }

    /** True when a refresh grant should repeat the scopes (Microsoft answers a refresh without them with a token of another audience). */
    val refreshWithScopes: Boolean get() = id == OAuthProviderId.MICROSOFT

    private companion object {
        fun requireHttps(name: String, url: String) {
            require(url.startsWith("https://") && url.length > "https://".length) { "$name must be an https URL" }
        }
    }
}

/** The provider table (roadmap P9, D31): Google for the Gmail preset, Microsoft for Outlook.com and Microsoft 365. */
object OAuthProviders {

    /** The Microsoft tenant of personal accounts; `common` also admits work and school accounts. */
    const val MICROSOFT_DEFAULT_TENANT = "consumers"

    val GOOGLE = OAuthProvider(
        id = OAuthProviderId.GOOGLE,
        authorizationEndpoint = "https://accounts.google.com/o/oauth2/v2/auth",
        tokenEndpoint = "https://oauth2.googleapis.com/token",
        scopes = listOf("https://mail.google.com/"),
        identityScopes = listOf("openid", "email"),
        authorizationParameters = mapOf("access_type" to "offline", "prompt" to "consent"),
        revocationEndpoint = "https://oauth2.googleapis.com/revoke",
        presetIds = listOf("gmail"),
    )

    fun microsoft(tenant: String = MICROSOFT_DEFAULT_TENANT): OAuthProvider {
        require(TENANT.matches(tenant)) { "tenant must be 'consumers', 'common', 'organizations', a tenant id or a verified domain" }
        val authority = "https://login.microsoftonline.com/$tenant/oauth2/v2.0"
        return OAuthProvider(
            id = OAuthProviderId.MICROSOFT,
            authorizationEndpoint = "$authority/authorize",
            tokenEndpoint = "$authority/token",
            scopes = listOf(
                "https://outlook.office.com/IMAP.AccessAsUser.All",
                "https://outlook.office.com/POP.AccessAsUser.All",
                "https://outlook.office.com/SMTP.Send",
                "offline_access",
            ),
            identityScopes = listOf("openid", "email"),
            presetIds = listOf("outlook", "office365"),
        )
    }

    /** The provider a preset's accounts sign in with, or null when the preset has no browser sign-in. */
    fun forPreset(presetId: String?): OAuthProviderId? = when (presetId) {
        in GOOGLE.presetIds -> OAuthProviderId.GOOGLE
        in MICROSOFT_PRESET_IDS -> OAuthProviderId.MICROSOFT
        else -> null
    }

    private val MICROSOFT_PRESET_IDS = listOf("outlook", "office365")
    private val TENANT = Regex("^[A-Za-z0-9][A-Za-z0-9.\\-]{0,127}$")
}
