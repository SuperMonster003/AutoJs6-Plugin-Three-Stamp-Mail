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
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.MailPluginBinder
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountsActivity
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
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Verifies the host-facing activation and discovery contract against the installed APK: the Wake
 * Activity, the INFO service (with a real `getInfo()` round trip), and the `org.autojs.plugin.MAIL`
 * service whose `IMailPlugin` Binder answers info, capabilities, listings and the session envelope
 * of `mail-api.aar` (roadmap P1.3), and the descriptor ownership rules of `call` for the transfer
 * ops (roadmap P2.2: copies are validated on the session thread and closed before `onResult`;
 * roadmap P2.3: a download takes exactly one write end, which is closed before `onResult` so the
 * host's read end sees EOF). The installed service refuses this instrumentation as a session
 * caller (roadmap P2.5 `CallerGuard`: the test runs under the plugin's UID, not the host's), so
 * the session envelope is exercised on an in-process `MailPluginBinder` with a trusting guard.
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
    fun settingsEntryResolvesBehindThePluginPermission() {
        val intent = Intent(AngusMailPlugin.SETTINGS_ACTION).addCategory(Intent.CATEGORY_DEFAULT).setPackage(packageName)
        @Suppress("DEPRECATION")
        val matches = context.packageManager.queryIntentActivities(intent, 0)
        assertEquals("The MAIL_SETTINGS action must resolve to exactly one activity", 1, matches.size)
        val activityInfo = matches.single().activityInfo
        assertEquals(MailSettingsActivity::class.java.name, activityInfo.name)
        assertTrue("the settings entry must be exported", activityInfo.exported)
        assertTrue("the settings entry must be enabled", activityInfo.enabled)
        assertEquals(PLUGIN_PERMISSION, activityInfo.permission)
        assertEquals(android.R.style.Theme_NoDisplay, activityInfo.theme)
        assertTrue(activityInfo.flags and ActivityInfo.FLAG_EXCLUDE_FROM_RECENTS != 0)

        // The launcher entry stays reachable without the permission.
        val launcher = requireNotNull(context.packageManager.getLaunchIntentForPackage(packageName)) { "no launcher entry" }
        val launcherComponent = requireNotNull(launcher.component)
        assertEquals(AccountsActivity::class.java.name, launcherComponent.className)
        assertNull(context.packageManager.getActivityInfo(launcherComponent, 0).permission)
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
            val providers = JSONObject(requireNotNull(plugin.listProviders().getString(MailContract.KEY_PROVIDERS_JSON)))
            assertEquals(AngusMailPlugin.PROVIDERS_VERSION, providers.getInt("version"))
            val providerIds = (0 until providers.getJSONArray("providers").length()).map { providers.getJSONArray("providers").getJSONObject(it).getString("id") }
            assertTrue(providerIds.toString(), "qq" in providerIds && "gmail" in providerIds && "163" in providerIds)
            assertFalse("presets never carry an account", providers.toString().contains("@"))
            assertEquals(MailContract.CONTRACT_VERSION, plugin.listProviders().getInt(MailContract.KEY_CONTRACT_VERSION))

            // Roadmap P2.5 CallerGuard: this instrumentation runs under the plugin's own UID, which is not the
            // installed AutoJs6 host (whether or not the host is installed on this device), so the session
            // entry points refuse it with the same SecurityException the MCP Server plugin raises.
            val statuses = LinkedBlockingQueue<String>()
            val sessionCallback = object : IMailSessionCallback.Stub() {
                override fun onStatus(status: Bundle?) {
                    statuses.add(status?.getString(MailContract.KEY_STATUS_JSON).orEmpty())
                }
            }
            val account = accountBundle(
                """{"address":"alice@localhost","user":"alice",
                    "imap":{"host":"127.0.0.1","port":3143,"tls":"none"},
                    "smtp":{"host":"127.0.0.1","port":3025,"tls":"none"}}""",
            )
            listOf<Pair<String, () -> Any?>>(
                "openSession" to { plugin.openSession(account, sessionCallback) },
                "listSavedAccounts" to { plugin.listSavedAccounts() },
            ).forEach { (name, invoke) ->
                try {
                    invoke()
                    fail("$name must refuse a caller that is not the AutoJs6 host")
                } catch (expected: SecurityException) {
                    android.util.Log.i("AngusMailContractTest", "$name refused: ${expected.message}")
                    assertTrue(expected.message.orEmpty(), expected.message.orEmpty().contains("AutoJs6"))
                    assertTrue(expected.message.orEmpty(), expected.message.orEmpty().startsWith("Caller is not the installed same-signer AutoJs6 host"))
                }
            }
            assertNull("a refused open reports nothing through the callback", statuses.poll(500, TimeUnit.MILLISECONDS))
        }
    }

    @Test
    fun mailBinderAnswersTheSessionEnvelope() {
        val plugin = MailPluginBinder(context, CallerGuard.trusting())
        assertCapabilities(requireNotNull(plugin.info.capabilities))
        assertEquals("[]", plugin.listSavedAccounts().getString(MailContract.KEY_ACCOUNTS_JSON))
        run {
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

            val endpointless = accountBundle("""{"address":"alice@example.org","auth":"password"}""")
            assertNull("an account without any endpoint must be refused by the mail core", plugin.openSession(endpointless, sessionCallback))
            val noEndpoint = JSONObject(requireNotNull(statuses.poll(5, TimeUnit.SECONDS)))
            assertEquals(MailErrorCodes.INVALID_ARGUMENT, noEndpoint.getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_CODE))
            assertTrue(noEndpoint.getJSONObject(MailContract.FIELD_LAST_ERROR).getString(MailContract.FIELD_ERROR_MESSAGE).contains("no imap, pop3 or smtp endpoint"))
            assertFalse(noEndpoint.toString().contains(FAKE_SECRET))

            // Loopback endpoints: nothing listens there unless the maintainer mapped GreenMail with adb reverse,
            // so session.test either succeeds or reports CONNECT_FAILED; both prove the round trip.
            val account = accountBundle(
                """{"address":"alice@localhost","user":"alice","debug":true,
                    "imap":{"host":"127.0.0.1","port":3143,"tls":"none"},
                    "smtp":{"host":"127.0.0.1","port":3025,"tls":"none"},
                    "timeout":{"connect":2000,"read":3000}}""",
            )
            val session = requireNotNull(plugin.openSession(account, sessionCallback)) { "a valid account bundle must open a session" }
            val openStatus = JSONObject(session.status.getString(MailContract.KEY_STATUS_JSON)!!)
            assertEquals(MailContract.STATE_OPEN, openStatus.getString(MailContract.FIELD_STATE))
            assertEquals("no network before the first call", 0, openStatus.getJSONArray("connected").length())
            assertEquals(0, openStatus.getInt("queued"))
            assertFalse(openStatus.has("active"))

            val results = LinkedBlockingQueue<Pair<String, String>>()
            val progressDocuments = LinkedBlockingQueue<String>()
            val callCallback = object : IMailCallCallback.Stub() {
                override fun onProgress(progress: Bundle?) {
                    progress?.getString(MailContract.KEY_PROGRESS_JSON)?.let(progressDocuments::add)
                }

                override fun onResult(response: Bundle?) {
                    results.add(Thread.currentThread().name to response?.getString(MailContract.KEY_RESPONSE_JSON).orEmpty())
                }
            }
            val test = call(session::call, callCallback, results, "req-1", MailContract.OP_SESSION_TEST)
            assertTrue(test.second.toString(), test.second.getBoolean(MailContract.FIELD_OK))
            assertEquals("results must come from the session executor, not the Binder thread", "angus-mail-session", test.first)
            val result = test.second.getJSONObject(MailContract.FIELD_RESULT)
            assertEquals("alice@localhost", result.getJSONObject("account").getString("address"))
            assertTrue(result.getJSONObject("account").getBoolean("insecure"))
            val imap = result.getJSONObject("imap")
            assertEquals(3143, imap.getInt("port"))
            if (!imap.getBoolean("ok")) {
                assertEquals(imap.toString(), MailErrorCodes.CONNECT_FAILED, imap.getJSONObject("error").getString(MailContract.FIELD_ERROR_CODE))
                assertTrue(imap.getJSONObject("error").getBoolean(MailContract.FIELD_ERROR_RETRYABLE))
            }
            assertFalse("the secret never enters a document", test.second.toString().contains(FAKE_SECRET))
            val debug = JSONObject(requireNotNull(progressDocuments.poll(1, TimeUnit.SECONDS)) { "debug:true must deliver the redacted trace before the result" })
            assertEquals("req-1", debug.getString(MailContract.FIELD_ID))
            val lines = debug.getJSONArray("debug")
            val traced = (0 until lines.length()).joinToString(" | ") { lines.getString(it) }
            assertTrue(traced, lines.length() >= 2 && traced.contains("imap connect 127.0.0.1:3143/none password"))
            assertFalse(traced.contains(FAKE_SECRET))

            // Every contract op is routed since P2.3: a well-formed receive op reaches the mail core (the loopback
            // port answers CONNECT_FAILED unless GreenMail is mapped), a malformed one is refused before any connection.
            val listing = call(session::call, callCallback, results, "req-2", MailContract.OP_MESSAGES_LIST, """{"limit": 3}""")
            if (!listing.second.getBoolean(MailContract.FIELD_OK)) {
                assertEquals(listing.second.toString(), MailErrorCodes.CONNECT_FAILED, listing.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))
            }
            val malformed = call(session::call, callCallback, results, "req-2b", MailContract.OP_FOLDERS_STATUS)
            assertEquals(MailErrorCodes.INVALID_ARGUMENT, malformed.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))
            assertTrue(malformed.second.toString(), malformed.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_MESSAGE).contains("'folder' is required"))

            val unknown = call(session::call, callCallback, results, "req-3", "messages.purge")
            assertEquals(MailErrorCodes.INVALID_ARGUMENT, unknown.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))

            // Descriptor ownership (contract B.3, roadmap P2.2). The service runs in this process, so the
            // descriptors reach the Binder as the very objects created here and their state is observable.
            val attachment = File(context.cacheDir, "contract-attachment.bin").apply { writeBytes(ByteArray(64) { it.toByte() }) }
            try {
                fun open() = ParcelFileDescriptor.open(attachment, ParcelFileDescriptor.MODE_READ_ONLY)
                val misplaced = open()
                val notATransfer = call(session::call, callCallback, results, "req-d1", MailContract.OP_SESSION_TEST, descriptors = arrayOf(misplaced))
                assertEquals(MailErrorCodes.INVALID_ARGUMENT, notATransfer.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))
                assertTrue(notATransfer.second.toString(), notATransfer.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_MESSAGE).contains("does not take descriptors"))
                assertFalse("the plugin closes its copy before onResult", misplaced.fileDescriptor.valid())

                val unbound = open()
                val appendArgs = """{"folder":"Drafts","message":{"to":"bob@example.org","subject":"s","attachments":[{"descriptorIndex":1,"fileName":"a.bin"}]}}"""
                val outOfRange = call(session::call, callCallback, results, "req-d2", MailContract.OP_MESSAGES_APPEND, appendArgs, arrayOf(unbound))
                assertEquals(MailErrorCodes.INVALID_ARGUMENT, outOfRange.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))
                assertTrue(outOfRange.second.toString(), outOfRange.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_MESSAGE).contains("has no descriptor (1 supplied)"))
                assertFalse(unbound.fileDescriptor.valid())

                val pipe = ParcelFileDescriptor.createPipe()
                val notSeekable = call(session::call, callCallback, results, "req-d3", MailContract.OP_MAIL_SEND, """{"message":{"to":"bob@example.org","subject":"s"}}""", arrayOf(pipe[0]))
                pipe[1].close()
                assertEquals(MailErrorCodes.INVALID_ARGUMENT, notSeekable.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))
                assertTrue(notSeekable.second.toString(), notSeekable.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_MESSAGE).contains("descriptor 0 is not a seekable file"))
                assertFalse(pipe[0].fileDescriptor.valid())

                val tooMany = Array(MailContract.MAX_DESCRIPTORS + 1) { open() }
                val overLimit = call(session::call, callCallback, results, "req-d4", MailContract.OP_MAIL_SEND, """{"message":{"to":"bob@example.org","subject":"s"}}""", tooMany)
                assertEquals(MailErrorCodes.LIMIT_EXCEEDED, overLimit.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))
                assertFalse(overLimit.second.getJSONObject(MailContract.FIELD_ERROR).getBoolean(MailContract.FIELD_ERROR_RETRYABLE))
                assertTrue("every copy is closed after a rejected call", tooMany.none { it.fileDescriptor.valid() })

                // A well-formed append reaches the mail core: nothing listens on the loopback port unless GreenMail
                // is mapped, so the answer is either the appended folder or CONNECT_FAILED, never an argument error.
                val bound = open()
                val wellFormed = call(session::call, callCallback, results, "req-d5", MailContract.OP_MESSAGES_APPEND, appendArgs.replace("\"descriptorIndex\":1", "\"descriptorIndex\":0"), arrayOf(bound))
                if (wellFormed.second.getBoolean(MailContract.FIELD_OK)) {
                    assertEquals("Drafts", wellFormed.second.getJSONObject(MailContract.FIELD_RESULT).getString("folder"))
                } else {
                    val error = wellFormed.second.getJSONObject(MailContract.FIELD_ERROR)
                    assertTrue(error.toString(), error.getString(MailContract.FIELD_ERROR_CODE) in setOf(MailErrorCodes.CONNECT_FAILED, MailErrorCodes.FOLDER_NOT_FOUND))
                }
                assertFalse(bound.fileDescriptor.valid())
                assertFalse(results.toString().contains(FAKE_SECRET))

                // Sink descriptors (roadmap P2.3): a download takes exactly one write end. The plugin closes it before
                // onResult, so the read end reaches EOF whether bytes were streamed (GreenMail mapped) or not.
                val pipe1 = ParcelFileDescriptor.createPipe()
                val raw = call(session::call, callCallback, results, "req-s1", MailContract.OP_MESSAGES_RAW, """{"uid": 1}""", arrayOf(pipe1[1]))
                if (!raw.second.getBoolean(MailContract.FIELD_OK)) {
                    val error = raw.second.getJSONObject(MailContract.FIELD_ERROR)
                    assertTrue(error.toString(), error.getString(MailContract.FIELD_ERROR_CODE) in setOf(MailErrorCodes.CONNECT_FAILED, MailErrorCodes.MESSAGE_NOT_FOUND))
                }
                assertFalse("the write end is closed before onResult", pipe1[1].fileDescriptor.valid())
                val drained = ParcelFileDescriptor.AutoCloseInputStream(pipe1[0]).use { it.readBytes() }
                if (!raw.second.getBoolean(MailContract.FIELD_OK)) assertEquals("nothing is written when the transfer fails before it starts", 0, drained.size)

                val pipe2 = ParcelFileDescriptor.createPipe()
                val pipe3 = ParcelFileDescriptor.createPipe()
                val twoSinks = call(session::call, callCallback, results, "req-s2", MailContract.OP_ATTACHMENTS_DOWNLOAD, """{"uid": 1, "partId": "2"}""", arrayOf(pipe2[1], pipe3[1]))
                assertEquals(MailErrorCodes.INVALID_ARGUMENT, twoSinks.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))
                assertTrue(twoSinks.second.toString(), twoSinks.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_MESSAGE).contains("single descriptor (2 supplied)"))
                assertFalse(pipe2[1].fileDescriptor.valid())
                assertFalse(pipe3[1].fileDescriptor.valid())
                pipe2[0].close()
                pipe3[0].close()

                val noSink = call(session::call, callCallback, results, "req-s3", MailContract.OP_MESSAGES_RAW, """{"uid": 1}""")
                assertEquals(MailErrorCodes.INVALID_ARGUMENT, noSink.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))
                assertTrue(noSink.second.toString(), noSink.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_MESSAGE).contains("(0 supplied)"))

                val pipe4 = ParcelFileDescriptor.createPipe()
                val badArgs = call(session::call, callCallback, results, "req-s4", MailContract.OP_ATTACHMENTS_DOWNLOAD, """{"uid": 1}""", arrayOf(pipe4[1]))
                assertTrue(badArgs.second.toString(), badArgs.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_MESSAGE).contains("'partId' is required"))
                assertFalse("argument errors still release the sink", pipe4[1].fileDescriptor.valid())
                assertEquals(0, ParcelFileDescriptor.AutoCloseInputStream(pipe4[0]).use { it.readBytes() }.size)

                val sinkOnSourceOp = call(session::call, callCallback, results, "req-s5", MailContract.OP_MESSAGES_GET, """{"uid": 1}""", arrayOf(open()))
                assertTrue(sinkOnSourceOp.second.toString(), sinkOnSourceOp.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_MESSAGE).contains("does not take descriptors"))
            } finally {
                attachment.delete()
            }

            val closed = call(session::call, callCallback, results, "req-4", MailContract.OP_SESSION_CLOSE)
            assertTrue(closed.second.getBoolean(MailContract.FIELD_OK))
            val closedStatus = JSONObject(requireNotNull(statuses.poll(5, TimeUnit.SECONDS)))
            assertEquals(MailContract.STATE_CLOSED, closedStatus.getString(MailContract.FIELD_STATE))
            assertEquals(MailContract.STATE_CLOSED, JSONObject(session.status.getString(MailContract.KEY_STATUS_JSON)!!).getString(MailContract.FIELD_STATE))
            val afterClose = call(session::call, callCallback, results, "req-5", MailContract.OP_SESSION_TEST)
            assertEquals(MailErrorCodes.SESSION_CLOSED, afterClose.second.getJSONObject(MailContract.FIELD_ERROR).getString(MailContract.FIELD_ERROR_CODE))
            session.close()
            assertNull("no further status after an idempotent close", statuses.poll(500, TimeUnit.MILLISECONDS))
        }
    }

    private fun accountBundle(json: String): Bundle = Bundle().apply {
        putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
        putLong(MailContract.KEY_HOST_VERSION_CODE, AngusMailPlugin.REQUIRED_HOST_VERSION)
        putString(MailContract.KEY_ACCOUNT_JSON, json)
        putString(MailContract.KEY_SECRET_PASSWORD, FAKE_SECRET)
    }

    private fun call(
        submit: (Bundle, Array<ParcelFileDescriptor>?, IMailCallCallback) -> String,
        callback: IMailCallCallback,
        results: LinkedBlockingQueue<Pair<String, String>>,
        id: String,
        op: String,
        args: String = "{}",
        descriptors: Array<ParcelFileDescriptor>? = null,
    ): Pair<String, JSONObject> {
        val request = Bundle().apply {
            putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
            putString(MailContract.KEY_REQUEST_JSON, """{"id":"$id","op":"$op","args":$args}""")
        }
        assertEquals(id, submit(request, descriptors, callback))
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
        assertEquals(AngusMailPlugin.SETTINGS_VERSION, capabilities.getInt(MailCapabilityKeys.SETTINGS_VERSION))
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
        const val FAKE_SECRET = "not-a-real-secret"
        const val PLUGIN_PERMISSION = "org.autojs.permission.PLUGIN"
        const val WAKE_ACTION = "org.autojs.plugin.action.WAKE"
        const val WAKE_ACTIVITY_META_DATA = "org.autojs.plugin.WAKE_ACTIVITY"
        const val AUTHOR_META_DATA = "org.autojs.plugin.info.AUTHOR"
        const val NATIVE_PAGE_ALIGNMENT_META_DATA = "org.autojs.plugin.contract.NATIVE_PAGE_ALIGNMENT"
        const val REQUIRES_HOST_VERSION_META_DATA = "requiresHostVersion"
    }
}
