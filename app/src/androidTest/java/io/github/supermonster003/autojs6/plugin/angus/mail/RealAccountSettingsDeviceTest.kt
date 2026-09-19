package io.github.supermonster003.autojs6.plugin.angus.mail

import android.app.Activity
import android.content.Context
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.textfield.TextInputLayout
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountEditorActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountForm
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountFormPolicy
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountsActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The settings page with a real provider account (mail roadmap P4.7): the account editor gets the
 * address, the provider preset and the authorization code from instrumentation arguments
 * (`mailAddress`, `mailProvider`, `mailSecret`, `mailAlias`; never stored in the repository),
 * "Test connection" reaches the provider's IMAP and SMTP endpoints, "Save" writes the record into
 * the installed plugin's real store, and the record stays there for the host's
 * `MailScriptSmokeDeviceTest#savedAccountScript` (`mail.connect(alias)`).
 *
 * Run through `.python/run_settings_real_account.py`, which uses `am instrument` directly so the
 * plugin and the saved account survive the run (a Gradle connected run uninstalls the plugin);
 * [removesTheSavedAccount] (`mailAlias` only) cleans up. Both cases skip without their arguments.
 * Logs carry the alias, the provider and durations only, never the address or the secret.
 */
@RunWith(AndroidJUnit4::class)
class RealAccountSettingsDeviceTest {

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    /** Resources in the language the plugin's screens render (may differ from the system locale). */
    private val strings: Context
        get() = AppConfiguration.wrap(context)

    private val arguments
        get() = InstrumentationRegistry.getArguments()

    private val store: AccountStore
        get() = AccountStores.of(context)

    @Test
    fun savesAnAccountThroughTheEditorForTheHost() {
        val alias = arguments.getString("mailAlias")
        val address = arguments.getString("mailAddress")
        val secret = arguments.getString("mailSecret")
        val provider = arguments.getString("mailProvider")
        assumeTrue(
            "mailAlias / mailAddress / mailSecret / mailProvider not given",
            !alias.isNullOrBlank() && !address.isNullOrBlank() && !secret.isNullOrBlank() && !provider.isNullOrBlank(),
        )
        val preset = requireNotNull(ProviderPresets.resolve(provider)) { "unknown provider preset $provider" }
        val form = AccountFormPolicy.applyPreset(AccountForm(alias = alias!!, address = address!!), preset)
        val secretHint = strings.getString(R.string.editor_field_password)
        val testLabel = strings.getString(R.string.editor_action_test)
        val saveLabel = strings.getString(R.string.action_save)
        val endpointOk = strings.getString(R.string.editor_test_endpoint_ok)
        val endpointFailed = strings.getString(R.string.editor_test_endpoint_failed)
        val okPrefix = strings.getString(R.string.editor_test_ok, 0).substringBefore('0')
        runCatching { store.remove(alias) }

        val started = System.currentTimeMillis()
        var testedMs = 0L
        ActivityScenario.launch<AccountEditorActivity>(AccountEditorActivity.intent(context, null)).use { scenario ->
            scenario.onActivity { activity ->
                activity.replaceForm(form)
                activity.field(secretHint).editText!!.setText(secret)
                activity.clickable(testLabel).performClick()
            }
            scenario.waitUntil("the connection test finishes", TEST_TIMEOUT_MS) { activity -> activity.clickable(testLabel).isEnabled }
            testedMs = System.currentTimeMillis() - started
            scenario.onActivity { activity ->
                val dialog = requireNotNull(activity.presentedDialog) { "the result dialog is missing" }
                assertTrue(dialog.isShowing)
                val views = dialog.window!!.decorView.descendants().toList()
                val texts = views.filterIsInstance<TextView>().map { it.text.toString() }
                val icons = views.filterIsInstance<ImageView>().mapNotNull { it.contentDescription?.toString() }
                val shown = (texts + icons).joinToString("\n")
                assertFalse("the secret never enters the result dialog", shown.contains(secret!!))
                assertTrue(shown, texts.any { it.startsWith(okPrefix) })
                assertTrue("IMAP and SMTP both reachable:\n$shown", icons.count { it == endpointOk } >= 2)
                assertFalse(shown, icons.any { it == endpointFailed })
                dialog.dismiss()
                activity.clickable(saveLabel).performClick()
            }
            waitUntil("the editor closes after saving", TEST_TIMEOUT_MS) { scenario.state == Lifecycle.State.DESTROYED }
        }

        val saved = requireNotNull(store.get(alias)) { "the account was not saved" }
        assertEquals(SecretKind.PASSWORD, saved.secretKind)
        assertFalse("the secret never enters the account document", saved.accountJson.contains(secret!!))
        assertEquals(address, MailAccountOptions.parse(saved.accountJson, saved.secretKind).address)
        assertEquals(provider, AccountFormPolicy.fromAccountJson(saved.accountJson, saved.secretKind).providerId)
        assertEquals(secret, store.withSecret(alias) { _, chars -> String(chars) })

        // The accounts page lists the alias and never the secret.
        ActivityScenario.launch(AccountsActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val texts = activity.root().descendants().filterIsInstance<TextView>().map { it.text.toString() }.toList()
                assertTrue(texts.toString(), alias in texts)
                assertFalse(texts.toString(), texts.any { it.contains(secret) })
            }
        }
        Log.i(TAG, "saved alias=$alias provider=$provider: test ${testedMs} ms, total ${System.currentTimeMillis() - started} ms")
    }

    @Test
    fun removesTheSavedAccount() {
        val alias = arguments.getString("mailAlias")
        assumeTrue("mailAlias not given", !alias.isNullOrBlank())
        val existed = store.get(alias!!) != null
        runCatching { store.remove(alias) }
        assertNull(store.get(alias))
        Log.i(TAG, "removed alias=$alias existed=$existed")
    }

    /** Polls a main-thread predicate. */
    private fun ActivityScenario<AccountEditorActivity>.waitUntil(what: String, timeoutMs: Long, predicate: (AccountEditorActivity) -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            var done = false
            onActivity { activity -> done = predicate(activity) }
            if (done) return
            check(System.currentTimeMillis() < deadline) { "timed out waiting until $what" }
            Thread.sleep(200)
        }
    }

    private fun waitUntil(what: String, timeoutMs: Long, condition: () -> Boolean) {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (!condition()) {
            check(System.currentTimeMillis() < deadline) { "timed out waiting until $what" }
            Thread.sleep(200)
        }
    }

    private fun Activity.root(): View = findViewById(android.R.id.content)

    private fun Activity.field(hint: CharSequence): TextInputLayout =
        root().descendants().filterIsInstance<TextInputLayout>().filter { it.hint?.toString() == hint.toString() }.toList().singleOrNull()
            ?: error("no single field with hint '$hint'")

    private fun Activity.clickable(text: CharSequence): View =
        root().descendants().filterIsInstance<TextView>().first { it.text.toString() == text.toString() && it.isClickable }

    private fun View.descendants(): Sequence<View> = sequence {
        yield(this@descendants)
        if (this@descendants is ViewGroup) {
            for (index in 0 until childCount) yieldAll(getChildAt(index).descendants())
        }
    }

    private companion object {
        const val TAG = "RealAccountSettingsDeviceTest"
        const val TEST_TIMEOUT_MS = 90_000L
    }
}
