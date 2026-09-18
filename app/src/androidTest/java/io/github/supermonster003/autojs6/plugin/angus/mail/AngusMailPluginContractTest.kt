package io.github.supermonster003.autojs6.plugin.angus.mail

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.autojs.plugin.common.api.IPluginInfoProvider
import org.autojs.plugin.common.api.PluginCapabilityKeys
import org.autojs.plugin.mail.api.IMailCallCallback
import org.autojs.plugin.mail.api.IMailPlugin
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.MailCapabilityKeys
import org.autojs.plugin.mail.api.MailContract
import org.autojs.plugin.mail.api.MailErrorCodes
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Verifies the host-facing activation and discovery contract against the installed APK: the Wake
 * Activity, the INFO service (with a real `getInfo()` round trip), and the `org.autojs.plugin.MAIL`
 * service whose `IMailPlugin` Binder answers info, capabilities, listings and the session envelope
 * of `mail-api.aar` (roadmap P1.3; operations themselves arrive with P2).
 */
@RunWith(AndroidJUnit4::class)
class AngusMailPluginContractTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val packageName: String
        get() = context.packageName

    @Test
    fun wakeActivityFollowsTheHostActivationContract() {
        val applicationInfo = context.packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA)
        val wakeActivity = applicationInfo.metaData?.getString(WAKE_ACTIVITY_META_DATA)
        assertEquals(".WakeActivity", wakeActivity)
        assertEquals(context.getString(R.string.plugin_author), applicationInfo.metaData?.getString(AUTHOR_META_DATA))
        assertEquals(0, applicationInfo.metaData?.getInt(NATIVE_PAGE_ALIGNMENT_META_DATA, -1))

        val component = ComponentName(packageName, packageName + wakeActivity)
        val activityInfo = context.packageManager.getActivityInfo(component, 0)
        assertTrue("Wake Activity must be exported", activityInfo.exported)
        assertTrue("Wake Activity must be enabled", activityInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, activityInfo.permission)
        assertEquals(android.R.style.Theme_NoDisplay, activityInfo.theme)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_FINISH_ON_TASK_LAUNCH != 0)

        val wakeIntent = Intent(WAKE_ACTION).addCategory(Intent.CATEGORY_DEFAULT).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentActivities(wakeIntent, 0)
        assertEquals("The WAKE action must resolve to exactly one activity", 1, matches.size)
        assertEquals(component.className, matches.single().activityInfo.name)
    }

    @Test
    fun infoServiceIsDiscoverableAndReportsPluginInfo() {
        val serviceInfo = discoverSingleService(AngusMailPlugin.INFO_ACTION, AngusMailPluginInfoService::class.java.name)
        assertEquals(packageName, serviceInfo.processName)

        withBoundService(serviceInfo) { binder ->
            assertEquals(IPluginInfoProvider.DESCRIPTOR, binder.interfaceDescriptor)
            val info = IPluginInfoProvider.Stub.asInterface(binder).info
            val packageInfo = context.packageManager.getPackageInfo(packageName, 0)
            val expectedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }

            assertEquals("Angus Mail", info.name)
            assertEquals(context.getString(R.string.app_name), info.name)
            assertEquals(context.getString(R.string.plugin_description), info.description)
            assertTrue("instruction must be read from the raw resource", info.instruction?.isNotBlank() == true)
            assertEquals(AngusMailPlugin.AUTHOR, info.author)
            assertEquals(context.getString(R.string.plugin_author), info.author)
            assertEquals(AngusMailPlugin.ID, info.id)
            assertEquals(context.getString(R.string.plugin_id), info.id)
            assertEquals(AngusMailPlugin.ENGINE, info.engine)
            assertEquals(context.getString(R.string.plugin_engine), info.engine)
            assertEquals(AngusMailPlugin.VARIANT, info.variant)
            assertEquals(context.getString(R.string.plugin_variant), info.variant)
            assertEquals(packageInfo.versionName, info.versionName)
            assertEquals(expectedVersionCode, info.versionCode)
            assertEquals(context.getString(R.string.plugin_version_date), info.versionDate)
            assertTrue(info.versionDate?.isNotBlank() == true)
            // Explicit empty array: no ABI restriction, as opposed to a null (unspecified) value.
            assertArrayEquals(emptyArray<String>(), info.supportedAbis)
            assertCapabilities(requireNotNull(info.capabilities))
        }
    }

    @Test
    fun mailServiceAnswersTheContractBinder() {
        val serviceInfo = discoverSingleService(AngusMailPlugin.SERVICE_ACTION, AngusMailPluginService::class.java.name)
        assertEquals(packageName, serviceInfo.processName)

        withBoundService(serviceInfo) { binder ->
            assertEquals(AngusMailPlugin.SERVICE_DESCRIPTOR, binder.interfaceDescriptor)
            assertEquals(IMailPlugin.DESCRIPTOR, binder.interfaceDescriptor)
            assertTrue(binder.isBinderAlive)
            assertTrue(binder.pingBinder())

            val plugin = IMailPlugin.Stub.asInterface(binder)
            val info = plugin.info
            assertEquals(AngusMailPlugin.ID, info.id)
            assertEquals(AngusMailPlugin.ENGINE, info.engine)
            assertEquals(AngusMailPlugin.VARIANT, info.variant)
            assertCapabilities(requireNotNull(info.capabilities))
            assertCapabilities(requireNotNull(plugin.capabilities))
            assertEquals("[]", plugin.listProviders().getString(MailContract.KEY_PROVIDERS_JSON))
            assertEquals("[]", plugin.listSavedAccounts().getString(MailContract.KEY_ACCOUNTS_JSON))
            assertEquals(MailContract.CONTRACT_VERSION, plugin.listProviders().getInt(MailContract.KEY_CONTRACT_VERSION))

            val statuses = LinkedBlockingQueue<String>()
            val sessionCallback = object : IMailSessionCallback.Stub() {
                override fun onStatus(status: Bundle?) {
                    statuses.add(status?.getString(MailContract.KEY_STATUS_JSON).orEmpty())
                }
            }

            assertNull("an empty account bundle must be refused", plugin.openSession(Bundle(), sessionCallback))
            val refused = JSONObject(requireNotNull(statuses.poll(5, TimeUnit.SECONDS)))
            assertEquals(MailContract.STATE_CLOSED, refused.getString(MailContract.FIELD_STATE))
            assertEquals(MailErrorCodes.INVALID_ARGUMENT, refused.getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_CODE))

            val account = Bundle().apply {
                putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
                putLong(MailContract.KEY_HOST_VERSION_CODE, AngusMailPlugin.REQUIRED_HOST_VERSION)
                putString(MailContract.KEY_ACCOUNT_JSON, """{"address":"alice@example.org","auth":"password"}""")
                putString(MailContract.KEY_SECRET_PASSWORD, "not-a-real-secret")
            }
            val session = requireNotNull(plugin.openSession(account, sessionCallback)) { "a valid account bundle must open a session" }
            assertEquals(MailContract.STATE_OPEN, JSONObject(session.status.getString(MailContract.KEY_STATUS_JSON)!!).getString(MailContract.FIELD_STATE))

            val results = LinkedBlockingQueue<Pair<String, String>>()
            val callCallback = object : IMailCallCallback.Stub() {
                override fun onProgress(progress: Bundle?) = Unit

                override fun onResult(response: Bundle?) {
                    results.add(Thread.currentThread().name to response?.getString(MailContract.KEY_RESPONSE_JSON).orEmpty())
                }
            }
            val response = call(session::call, callCallback, results, "req-1", MailContract.OP_SESSION_TEST)
            assertFalse(response.second.getBoolean(MailContract.FIELD_OK))
            assertEquals(MailErrorCodes.UNSUPPORTED_OPERATION, response.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))
            assertFalse(response.second.getJSONObject(MailContract.FIELD_ERROR).getBoolean(MailContract.FIELD_ERROR_RETRYABLE))
            assertEquals("results must come from the session executor, not the Binder thread", "angus-mail-session", response.first)

            val unknown = call(session::call, callCallback, results, "req-2", "messages.purge")
            assertEquals(MailErrorCodes.INVALID_ARGUMENT, unknown.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))

            val closed = call(session::call, callCallback, results, "req-3", MailContract.OP_SESSION_CLOSE)
            assertTrue(closed.second.getBoolean(MailContract.FIELD_OK))
            val closedStatus = JSONObject(requireNotNull(statuses.poll(5, TimeUnit.SECONDS)))
            assertEquals(MailContract.STATE_CLOSED, closedStatus.getString(MailContract.FIELD_STATE))
            assertEquals(MailContract.STATE_CLOSED, JSONObject(session.status.getString(MailContract.KEY_STATUS_JSON)!!).getString(MailContract.FIELD_STATE))
            session.close()
            assertNull("no further status after an idempotent close", statuses.poll(500, TimeUnit.MILLISECONDS))
        }
    }

    private fun call(
        submit: (Bundle, Array<android.os.ParcelFileDescriptor>?, IMailCallCallback) -> String,
        callback: IMailCallCallback,
        results: LinkedBlockingQueue<Pair<String, String>>,
        id: String,
        op: String,
    ): Pair<String, JSONObject> {
        val request = Bundle().apply {
            putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
            putString(MailContract.KEY_REQUEST_JSON, """{"id":"$id","op":"$op","args":{}}""")
        }
        assertEquals(id, submit(request, null, callback))
        val (thread, json) = requireNotNull(results.poll(5, TimeUnit.SECONDS)) { "no onResult for $id" }
        val response = JSONObject(json)
        assertEquals(id, response.getString(MailContract.FIELD_ID))
        return thread to response
    }

    private fun assertCapabilities(capabilities: Bundle) {
        assertEquals(AngusMailPlugin.REQUIRED_HOST_VERSION, capabilities.getLong(PluginCapabilityKeys.REQUIRES_HOST_VERSION))
        assertEquals(MailContract.CONTRACT_VERSION, capabilities.getInt(MailCapabilityKeys.CONTRACT_VERSION))
        assertArrayEquals(AngusMailPlugin.PROTOCOLS.toTypedArray(), capabilities.getStringArray(MailCapabilityKeys.PROTOCOLS))
        assertArrayEquals(AngusMailPlugin.AUTH_MECHANISMS.toTypedArray(), capabilities.getStringArray(MailCapabilityKeys.AUTH_MECHANISMS))
        assertArrayEquals(AngusMailPlugin.FEATURES.toTypedArray(), capabilities.getStringArray(MailCapabilityKeys.FEATURES))
        assertEquals(AngusMailPlugin.PROVIDERS_VERSION, capabilities.getInt(MailCapabilityKeys.PROVIDERS_VERSION))
        assertEquals(AngusMailPlugin.MAIL_LIBRARY_VERSION, capabilities.getString(MailCapabilityKeys.LIBRARY_VERSION))
    }

    private fun discoverSingleService(action: String, expectedClassName: String): ServiceInfo {
        val discoveryIntent = Intent(action)
            .addCategory(AngusMailPlugin.SERVICE_CATEGORY)
            .setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentServices(discoveryIntent, PackageManager.GET_META_DATA)
        assertEquals("The discovery contract for $action must resolve exactly one service", 1, matches.size)

        val serviceInfo = matches.single().serviceInfo
        assertEquals(packageName, serviceInfo.packageName)
        assertEquals(expectedClassName, serviceInfo.name)
        assertTrue("$expectedClassName must be exported", serviceInfo.exported)
        assertTrue("$expectedClassName must be enabled", serviceInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, serviceInfo.permission)
        val requiresHostVersion = requireNotNull(serviceInfo.metaData) { "requiresHostVersion meta-data is missing" }
            .getInt(REQUIRES_HOST_VERSION_META_DATA)
        assertEquals(AngusMailPlugin.REQUIRED_HOST_VERSION, requiresHostVersion.toLong())
        return serviceInfo
    }

    private fun withBoundService(serviceInfo: ServiceInfo, block: (IBinder) -> Unit) {
        val binderReference = AtomicReference<IBinder>()
        val connected = CountDownLatch(1)
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, service: IBinder) {
                binderReference.set(service)
                connected.countDown()
            }

            override fun onServiceDisconnected(name: ComponentName) = Unit

            override fun onNullBinding(name: ComponentName) {
                connected.countDown()
            }
        }

        val explicitIntent = Intent().setComponent(ComponentName(serviceInfo.packageName, serviceInfo.name))
        assertTrue("bindService returned false", context.bindService(explicitIntent, connection, Context.BIND_AUTO_CREATE))
        try {
            assertTrue("Timed out waiting for the Binder service", connected.await(10, TimeUnit.SECONDS))
            val binder = binderReference.get()
            assertNotNull("The service returned a null Binder", binder)
            block(binder)
        } finally {
            context.unbindService(connection)
        }
    }

    private companion object {
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val WAKE_ACTION = "org.autojs.plugin.action.WAKE"
        const val WAKE_ACTIVITY_META_DATA = "org.autojs.plugin.WAKE_ACTIVITY"
        const val AUTHOR_META_DATA = "org.autojs.plugin.info.AUTHOR"
        const val NATIVE_PAGE_ALIGNMENT_META_DATA = "org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"
        const val REQUIRES_HOST_VERSION_META_DATA = "requiresHostVersion"
    }
}
