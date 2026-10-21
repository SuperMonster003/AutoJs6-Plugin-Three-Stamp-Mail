package io.github.supermonster003.autojs6.plugin.three.stamp.mail

import android.content.Intent
import android.os.Bundle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.three.stamp.mail.binder.MailPluginBinder
import org.autojs.plugin.mail.api.MailActions
import org.autojs.plugin.mail.api.MailCapabilityKeys
import org.autojs.plugin.mail.api.MailContract
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Packaged routing cannot expose the plugin's stored account or background-watch state. */
@RunWith(AndroidJUnit4::class)
class PackagedMailBoundaryTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun packaged() = MailPluginBinder(context, CallerGuard.trusting(), allowStoredAccounts = false)

    @Test fun discoveryKeepsTheTwoPermissionRoutesDistinct() {
        for ((action, permission) in listOf(MailActions.SERVICE_ACTION to MailActions.PLUGIN_PERMISSION,
            MailActions.PACKAGED_SERVICE_ACTION to MailActions.PACKAGED_PERMISSION)) {
            val intent = Intent(action).addCategory(MailActions.SERVICE_CATEGORY).setPackage(context.packageName)
            @Suppress("DEPRECATION")
            val service = context.packageManager.queryIntentServices(intent, 0).single().serviceInfo
            assertTrue(service.exported)
            assertEquals(permission, service.permission)
        }
        @Suppress("DEPRECATION")
        val permission = context.packageManager.getPermissionInfo(MailActions.PACKAGED_PERMISSION, 0)
        @Suppress("DEPRECATION")
        assertEquals(android.content.pm.PermissionInfo.PROTECTION_NORMAL, permission.protectionLevel)
    }

    @Test fun packagedCapabilitiesDoNotAdvertiseStoredAccountsOrBackgroundWatches() {
        val binder = packaged()
        for (capabilities in listOf(binder.getCapabilities(), binder.getInfo().capabilities)) {
            val features = capabilities!!.getStringArray(MailCapabilityKeys.FEATURES).orEmpty()
            assertFalse(MailContract.FEATURE_SAVED_ACCOUNTS in features)
            assertFalse(MailContract.FEATURE_BACKGROUND_WATCH in features)
            assertTrue(MailContract.FEATURE_IDLE in features)
        }
    }

    @Test fun savedAccountAndBackgroundWatchCallsAreDeniedBeforeAccess() {
        val binder = packaged()
        val calls = listOf<() -> Any?>(
            { binder.listSavedAccounts() },
            { binder.listTriggers() },
            { binder.openTrigger(null, null) },
            { binder.openSession(Bundle().apply { putString(MailContract.KEY_ACCOUNT_ALIAS, "not-a-real-account") }, null) },
        )
        calls.forEach { call ->
            try { call(); fail("Packaged caller reached stored account state") }
            catch (expected: SecurityException) { assertTrue(expected.message.orEmpty().contains("signed host")) }
        }
    }
}
