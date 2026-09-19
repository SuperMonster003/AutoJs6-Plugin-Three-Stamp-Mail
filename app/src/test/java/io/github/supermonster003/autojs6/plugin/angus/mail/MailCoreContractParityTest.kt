package io.github.supermonster003.autojs6.plugin.angus.mail

import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.WatchEventDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchMode
import org.autojs.plugin.mail.api.MailActions
import org.autojs.plugin.mail.api.MailContract
import org.autojs.plugin.mail.api.MailErrorCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * `:mail-core` is a pure JVM module and cannot load the host contract AAR, so it mirrors the
 * error codes, ceilings and enum ids it needs; this test keeps the mirrors identical to the
 * contract (roadmap 4.2 "single source of truth").
 */
class MailCoreContractParityTest {

    @Test
    fun errorCodesMatchTheContract() {
        assertEquals(MailErrorCodes.ALL, MailErrorCode.ALL)
        assertEquals(MailErrorCodes.RETRYABLE_DEFAULTS, MailErrorCode.RETRYABLE_DEFAULTS)
        MailErrorCodes.ALL.forEach { code ->
            assertEquals(code, MailErrorCodes.isRetryableByDefault(code), MailErrorCode.isRetryableByDefault(code))
        }
    }

    @Test
    fun limitsMatchTheContractConstantsOfTheSameName() {
        val contract = MailContract::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && java.lang.reflect.Modifier.isPublic(it.modifiers) && it.name != "INSTANCE" }
            .associate { it.name to it.get(null) }
        val mirrored = MailLimits::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && java.lang.reflect.Modifier.isPublic(it.modifiers) && it.name in contract }
            .associate { it.name to it.get(null) }
        assertTrue("MailLimits must mirror at least the timeouts and ceilings", mirrored.size >= 17)
        mirrored.forEach { (name, value) -> assertEquals(name, contract.getValue(name), value) }
    }

    @Test
    fun enumIdsMatchTheContractVocabulary() {
        assertEquals(MailContract.PROTOCOLS.toList(), MailProtocol.entries.map { it.id })
        assertEquals(MailContract.AUTH_MECHANISMS.toList(), AuthMethod.entries.map { it.id })
        assertEquals(MailContract.TLS_MODES.toList(), TlsMode.entries.map { it.id })
        assertEquals(MailContract.WATCH_MODES, WatchMode.entries.map { it.id }.toSet())
        assertEquals(MailContract.EVENT_TYPES, WatchEventDocument.TYPES)
        assertEquals(MailContract.WATCH_MODE_IDLE, WatchMode.IDLE.id)
        assertEquals(MailContract.WATCH_MODE_POLL, WatchMode.POLL.id)
    }

    @Test
    fun capabilitiesDescribeTheMailCore() {
        assertEquals(ProviderPresets.version, AngusMailPlugin.PROVIDERS_VERSION)
        assertEquals(MailActions.SERVICE_ACTION, AngusMailPlugin.SERVICE_ACTION)
        assertEquals(MailActions.SERVICE_CATEGORY, AngusMailPlugin.SERVICE_CATEGORY)
        assertEquals(MailActions.OPEN_SETTINGS, AngusMailPlugin.SETTINGS_ACTION)
        assertEquals(MailProtocol.entries.map { it.id }, AngusMailPlugin.PROTOCOLS)
        assertEquals(AuthMethod.entries.map { it.id }, AngusMailPlugin.AUTH_MECHANISMS)
        assertTrue(MailContract.FEATURE_IDLE in AngusMailPlugin.FEATURES)
        AngusMailPlugin.FEATURES.forEach { assertTrue(it, it in MailContract.FEATURES) }
        ProviderPresets.all.forEach { preset -> preset.auth.forEach { assertTrue(it in AngusMailPlugin.AUTH_MECHANISMS) } }
    }
}
