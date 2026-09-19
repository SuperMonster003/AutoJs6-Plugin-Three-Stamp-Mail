package io.github.supermonster003.autojs6.plugin.angus.mail

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import io.github.supermonster003.autojs6.plugin.angus.mail.settings.AccountsActivity

/**
 * Invisible entry the AutoJs6 host starts to open the accounts page
 * (`org.autojs.plugin.MAIL_SETTINGS`, roadmap P4.3). It is exported only behind the plugin
 * permission, which keeps that permission off the launcher entry, and it forwards nothing but
 * the request itself: the contract is parameterless, so an intent carrying data, a clip or
 * extras is not a settings request and only finishes.
 */
class MailSettingsActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null && acceptsSettingsIntent(intent)) {
            startActivity(Intent(this, AccountsActivity::class.java))
        }
        finish()
    }

    companion object {
        internal fun acceptsSettingsIntent(intent: Intent?): Boolean =
            intent != null &&
                intent.action == AngusMailPlugin.SETTINGS_ACTION &&
                intent.data == null &&
                intent.clipData == null &&
                intent.extras == null
    }
}
