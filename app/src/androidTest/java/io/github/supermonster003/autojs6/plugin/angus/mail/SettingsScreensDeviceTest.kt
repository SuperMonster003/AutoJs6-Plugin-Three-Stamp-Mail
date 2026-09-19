package io.github.supermonster003.autojs6.plugin.angus.mail

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.textfield.TextInputLayout
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.CallerGuard
import io.github.supermonster003.autojs6.plugin.angus.mail.binder.MailPluginBinder
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountEditorActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountForm
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountsActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.EndpointFields
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import org.autojs.plugin.mail.api.IMailCallCallback
import org.autojs.plugin.mail.api.IMailSessionCallback
import org.autojs.plugin.mail.api.MailContract
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

/**
 * The settings screens of roadmap P4.2 driven in-process: the editor saves a custom-server
 * account against scripted loopback IMAP and SMTP servers, survives a recreation without keeping
 * the secret, tests the connection (the servers see the secret, the screens never show it),
 * reopens the record for editing while keeping the stored secret, lists it on the accounts page,
 * and the Binder then opens a session by that alias. Runs against the installed plugin's real
 * store under a throw-away alias that is removed afterwards.
 */
@RunWith(AndroidJUnit4::class)
class SettingsScreensDeviceTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val store: AccountStore
        get() = AccountStores.of(context)

    private val alias = "ui-smoke-" + UUID.randomUUID().toString().take(8)

    @After
    fun cleanUp() {
        runCatching { store.remove(alias) }
    }

    @Test
    fun editorSavesTestsAndReopensAnAccountWithoutExposingTheSecret() {
        val imap = ScriptedImapServer()
        val smtp = ScriptedSmtpServer()
        try {
            val form = AccountForm(
                alias = alias,
                address = "alice@localhost",
                name = "Alice",
                imap = EndpointFields(true, "127.0.0.1", imap.port.toString(), TlsMode.NONE),
                smtp = EndpointFields(true, "127.0.0.1", smtp.port.toString(), TlsMode.NONE),
            )
            val aliasHint = context.getString(R.string.editor_field_alias)
            val secretHint = context.getString(R.string.editor_field_password)
            val testLabel = context.getString(R.string.editor_action_test)
            val saveLabel = context.getString(R.string.action_save)

            ActivityScenario.launch<AccountEditorActivity>(AccountEditorActivity.intent(context, null)).use { scenario ->
                scenario.onActivity { activity ->
                    activity.replaceForm(form)
                    activity.field(secretHint).editText!!.setText(SECRET)
                }

                // Recreation (rotation, process restore) keeps every typed value except the secret.
                scenario.recreate()
                scenario.onActivity { activity ->
                    assertEquals(alias, activity.field(aliasHint).editText!!.text.toString())
                    assertEquals("127.0.0.1", activity.fields(context.getString(R.string.editor_field_host)).first().editText!!.text.toString())
                    assertEquals("", activity.field(secretHint).editText!!.text.toString())
                    activity.field(secretHint).editText!!.setText(SECRET)
                }

                // Test connection: the probe reaches both loopback servers with the typed secret.
                scenario.onActivity { activity -> activity.clickable(testLabel).performClick() }
                val login = requireNotNull(imap.logins.poll(20, TimeUnit.SECONDS)) { "the IMAP server saw no LOGIN" }
                assertTrue(login, login.contains(SECRET))
                assertNotNull("the SMTP server saw no AUTH", smtp.auths.poll(20, TimeUnit.SECONDS))
                scenario.waitUntil("the test finishes") { activity -> activity.clickable(testLabel).isEnabled }
                scenario.onActivity { activity ->
                    val dialog = requireNotNull(activity.presentedDialog) { "the result dialog is missing" }
                    assertTrue(dialog.isShowing)
                    val shown = dialog.window!!.decorView.descendants().filterIsInstance<TextView>().joinToString("\n") { it.text.toString() }
                    assertTrue(shown, shown.contains("IMAP") && shown.contains("SMTP"))
                    assertFalse(shown, shown.contains(SECRET))
                    dialog.dismiss()
                    activity.clickable(saveLabel).performClick()
                }
                waitUntil("the editor closes after saving") { scenario.state == Lifecycle.State.DESTROYED }
            }

            val saved = requireNotNull(store.get(alias))
            assertEquals(SecretKind.PASSWORD, saved.secretKind)
            val account = MailAccountOptions.parse(saved.accountJson, saved.secretKind)
            assertEquals("alice@localhost", account.address)
            assertEquals("Alice", account.displayName)
            assertEquals(imap.port, account.imap?.port)
            assertEquals(smtp.port, account.smtp?.port)
            assertEquals(MailProtocol.IMAP, account.receive)
            assertEquals(SECRET, store.withSecret(alias) { _, chars -> String(chars) })

            // Editing keeps the stored secret when the field stays empty.
            ActivityScenario.launch<AccountEditorActivity>(AccountEditorActivity.intent(context, alias)).use { scenario ->
                scenario.onActivity { activity ->
                    assertEquals(alias, activity.field(aliasHint).editText!!.text.toString())
                    assertEquals("", activity.field(secretHint).editText!!.text.toString())
                    assertEquals(context.getString(R.string.editor_secret_keep_helper), activity.field(secretHint).helperText)
                    activity.field(context.getString(R.string.editor_field_name)).editText!!.setText("Alice Again")
                    activity.clickable(saveLabel).performClick()
                }
                waitUntil("the editor closes after the edit") { scenario.state == Lifecycle.State.DESTROYED }
            }
            assertEquals("Alice Again", MailAccountOptions.parse(requireNotNull(store.get(alias)).accountJson, SecretKind.PASSWORD).displayName)
            assertEquals(SECRET, store.withSecret(alias) { _, chars -> String(chars) })

            // The accounts page lists the record with its address and no secret.
            ActivityScenario.launch(AccountsActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val texts = activity.root().descendants().filterIsInstance<TextView>().map { it.text.toString() }.toList()
                    assertTrue(texts.toString(), alias in texts)
                    assertTrue(texts.toString(), "alice@localhost" in texts)
                    assertFalse(texts.toString(), texts.any { it.contains(SECRET) })
                }
            }

            // And the Binder opens a session by the alias that the screens saved.
            val plugin = MailPluginBinder(context, CallerGuard.trusting(), store)
            val statuses = LinkedBlockingQueue<JSONObject>()
            val sessionCallback = object : IMailSessionCallback.Stub() {
                override fun onStatus(status: Bundle?) {
                    statuses.add(JSONObject(status?.getString(MailContract.KEY_STATUS_JSON).orEmpty()))
                }
            }
            val session = requireNotNull(plugin.openSession(aliasBundle(alias), sessionCallback)) { "the alias must open" }
            val results = LinkedBlockingQueue<JSONObject>()
            val request = Bundle().apply {
                putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
                putString(MailContract.KEY_REQUEST_JSON, """{"id":"t1","op":"session.test","args":{}}""")
            }
            session.call(
                request,
                null,
                object : IMailCallCallback.Stub() {
                    override fun onProgress(progress: Bundle?) = Unit
                    override fun onResult(response: Bundle?) {
                        results.add(JSONObject(response?.getString(MailContract.KEY_RESPONSE_JSON).orEmpty()))
                    }
                },
            )
            val response = requireNotNull(results.poll(20, TimeUnit.SECONDS)) { "no session.test result" }
            assertTrue(response.toString(), response.getBoolean(MailContract.FIELD_OK))
            assertTrue(response.toString(), response.getJSONObject(MailContract.FIELD_RESULT).getBoolean("ok"))
            assertFalse(response.toString(), response.toString().contains(SECRET))
            session.close()
            assertEquals(MailContract.STATE_CLOSED, requireNotNull(statuses.poll(5, TimeUnit.SECONDS)).getString(MailContract.FIELD_STATE))
        } finally {
            imap.close()
            smtp.close()
        }
    }

    @Test
    fun settingsEntryForwardsOnlyTheParameterlessAction() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(AccountsActivity::class.java.name, null, false)
        try {
            val entry = Intent(AngusMailPlugin.SETTINGS_ACTION)
                .addCategory(Intent.CATEGORY_DEFAULT)
                .setClassName(context, MailSettingsActivity::class.java.name)
            ActivityScenario.launch<MailSettingsActivity>(entry).use { scenario ->
                waitUntil("the settings entry finishes") { scenario.state == Lifecycle.State.DESTROYED }
            }
            val accounts = requireNotNull(monitor.waitForActivityWithTimeout(10_000)) { "the accounts page did not open" }
            assertEquals(1, monitor.hits)
            accounts.finish()

            // Anything beyond the bare action is not a settings request: the entry finishes without forwarding.
            val decorated = Intent(entry).putExtra("alias", alias)
            ActivityScenario.launch<MailSettingsActivity>(decorated).use { scenario ->
                waitUntil("the decorated entry finishes") { scenario.state == Lifecycle.State.DESTROYED }
            }
            Thread.sleep(1_000)
            assertEquals(1, monitor.hits)
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun releaseHistoryRendersTheBundledChangelog() {
        ActivityScenario.launch(ReleaseHistoryActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val texts = activity.root().descendants().filterIsInstance<TextView>().map { it.text.toString() }.toList()
                assertTrue(texts.toString(), "v1.0.0" in texts)
                assertFalse(texts.toString(), context.getString(R.string.release_history_load_failed) in texts)
                assertTrue(texts.toString(), texts.any { it.contains("P4.2") })
            }
        }
    }

    @Test
    fun batteryGuideShowsTheSystemStateAndTheSystemDialogIsReachable() {
        val ignored = BatteryOptimization.isIgnored(context)
        val expected = context.getString(if (ignored) R.string.battery_summary_ignored else R.string.battery_summary_optimized)
        ActivityScenario.launch(AppSettingsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val texts = activity.root().descendants().filterIsInstance<TextView>().map { it.text.toString() }.toList()
                assertTrue(texts.toString(), context.getString(R.string.battery_title) in texts)
                assertTrue(texts.toString(), expected in texts)
            }
        }
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            context.packageManager.checkPermission(BatteryOptimization.PERMISSION, context.packageName),
        )
        val request = BatteryOptimization.exclusionRequest(context)
        assertEquals("package:" + context.packageName, request.dataString)
        @Suppress("DEPRECATION")
        assertTrue("the system offers the exclusion dialog", context.packageManager.queryIntentActivities(request, 0).isNotEmpty())
    }

    private fun aliasBundle(alias: String): Bundle = Bundle().apply {
        putInt(MailContract.KEY_CONTRACT_VERSION, MailContract.CONTRACT_VERSION)
        putLong(MailContract.KEY_HOST_VERSION_CODE, AngusMailPlugin.REQUIRED_HOST_VERSION)
        putString(MailContract.KEY_ACCOUNT_ALIAS, alias)
    }

    /** Polls a main-thread predicate for up to 20 seconds. */
    private fun ActivityScenario<AccountEditorActivity>.waitUntil(what: String, predicate: (AccountEditorActivity) -> Boolean) {
        val deadline = System.currentTimeMillis() + 20_000
        while (true) {
            var done = false
            onActivity { activity -> done = predicate(activity) }
            if (done) return
            check(System.currentTimeMillis() < deadline) { "timed out waiting until $what" }
            Thread.sleep(100)
        }
    }

    private fun waitUntil(what: String, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + 20_000
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "timed out waiting until $what" }
            Thread.sleep(100)
        }
    }

    private fun Activity.root(): View = findViewById(android.R.id.content)

    private fun Activity.fields(hint: CharSequence): List<TextInputLayout> =
        root().descendants().filterIsInstance<TextInputLayout>().filter { it.hint?.toString() == hint.toString() }.toList()

    private fun Activity.field(hint: CharSequence): TextInputLayout =
        fields(hint).singleOrNull() ?: error("no single field with hint '$hint'")

    private fun Activity.clickable(text: CharSequence): View =
        root().descendants().filterIsInstance<TextView>().first { it.text.toString() == text.toString() && it.isClickable }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) {
            for (index in 0 until childCount) yieldAll(getChildAt(index).descendants())
        }
    }

    private companion object {
        const val SECRET = "ui-secret-otter-42"
    }
}
