package io.github.supermonster003.autojs6.plugin.angus.mail.oauth

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * The browser's landing point of a sign-in (roadmap P9): exported without a permission because
 * the browser delivers the redirect URI to it, it does nothing but forward the URI to the
 * sign-in screen that is waiting below the browser tab in this task (`CLEAR_TOP` pops the tab,
 * `SINGLE_TOP` delivers `onNewIntent`) and finish. The sign-in screen validates the `state`;
 * an unexpected redirect therefore reaches a screen that knows no request and is refused there.
 */
class OAuthRedirectActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val data = intent?.data
        if (data != null) {
            startActivity(
                Intent(this, OAuthSignInActivity::class.java)
                    .setData(data)
                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            )
        }
        finish()
    }
}
