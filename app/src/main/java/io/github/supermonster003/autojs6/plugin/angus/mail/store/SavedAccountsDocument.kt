package io.github.supermonster003.autojs6.plugin.angus.mail.store

import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.OAuthLink
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.MailJson
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * The `accountsJson` document of `listSavedAccounts` (contract `KEY_ACCOUNTS_JSON`, roadmap P4.3):
 * a JSON array with one object per saved account and no secret in it.
 *
 * ```
 * {alias, address, user, name?, provider?, auth, oauth?, receive, imap?, pop3?, smtp?, default, updatedAt}
 * ```
 *
 * `auth` is `password` or `xoauth2`, `receive` is `imap` or `pop3`, and each endpoint object is
 * `{host, port, tls}`. An account signed in through the browser (roadmap P9) is `xoauth2` with an
 * `oauth` object `{provider, authorizedAt, expiresAt, needsReauth}`: the provider (`google` or
 * `microsoft`), when the sign-in happened, when the current access token expires (it renews itself)
 * and whether the sign-in has to be repeated in the plugin settings; the tokens themselves never
 * appear here. A record whose document no longer normalizes (a preset that vanished from
 * the catalog) is listed with `alias`, `default`, `updatedAt` and an `error` object instead.
 */
object SavedAccountsDocument {

    object Fields {
        const val ALIAS = "alias"
        const val ADDRESS = MailAccountOptions.Fields.ADDRESS
        const val USER = MailAccountOptions.Fields.USER
        const val NAME = MailAccountOptions.Fields.NAME
        const val PROVIDER = MailAccountOptions.Fields.PROVIDER
        const val AUTH = MailAccountOptions.Fields.AUTH
        const val OAUTH = MailAccountOptions.Fields.OAUTH
        const val RECEIVE = MailAccountOptions.Fields.RECEIVE
        const val DEFAULT = "default"
        const val UPDATED_AT = "updatedAt"
        const val ERROR = "error"
        const val HOST = MailAccountOptions.Fields.HOST
        const val PORT = MailAccountOptions.Fields.PORT
        const val TLS = MailAccountOptions.Fields.ENDPOINT_TLS
    }

    fun render(accounts: List<SavedAccount>): String = JsonArray(accounts.map(::entry)).toString()

    fun entry(account: SavedAccount): JsonObject = buildJsonObject {
        put(Fields.ALIAS, account.alias)
        try {
            val normalized = MailAccountOptions.parse(account.accountJson, account.secretKind)
            put(Fields.ADDRESS, normalized.address)
            put(Fields.USER, normalized.username)
            normalized.displayName?.let { put(Fields.NAME, it) }
            normalized.provider?.let { put(Fields.PROVIDER, it.id) }
            put(Fields.AUTH, normalized.auth.id)
            normalized.oauth?.let { link ->
                put(
                    Fields.OAUTH,
                    buildJsonObject {
                        put(OAuthLink.FIELD_PROVIDER, link.provider)
                        put(OAuthLink.FIELD_AUTHORIZED_AT, link.authorizedAt)
                        put(OAuthLink.FIELD_EXPIRES_AT, link.expiresAt)
                        put(OAuthLink.FIELD_NEEDS_REAUTH, link.needsReauth)
                    },
                )
            }
            put(Fields.RECEIVE, normalized.receive.id)
            MailProtocol.entries.forEach { protocol ->
                normalized.endpointOrNull(protocol)?.let { endpoint ->
                    put(
                        protocol.id,
                        buildJsonObject {
                            put(Fields.HOST, endpoint.host)
                            put(Fields.PORT, endpoint.port)
                            put(Fields.TLS, endpoint.tls.id)
                        },
                    )
                }
            }
        } catch (e: MailException) {
            (MailJson.format.parseToJsonElement(account.accountJson) as? JsonObject)
                ?.get(Fields.ADDRESS)
                ?.let { it as? JsonPrimitive }
                ?.takeIf { it.isString }
                ?.let { put(Fields.ADDRESS, it.content) }
            put(
                Fields.ERROR,
                buildJsonObject {
                    put("code", e.code)
                    put("message", e.message)
                },
            )
        }
        put(Fields.DEFAULT, account.isDefault)
        put(Fields.UPDATED_AT, account.updatedAt)
    }
}
