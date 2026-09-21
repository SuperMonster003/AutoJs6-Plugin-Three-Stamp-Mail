package io.github.supermonster003.autojs6.plugin.angus.mail.oauth

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.R
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.AuthorizationRequest
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.AuthorizationResponse
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.AuthorizationResponses
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.OAuthProviderId
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.Pkce
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.PkceChallenge
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.TokenClient
import io.github.supermonster003.autojs6.plugin.angus.mail.core.oauth.TokenGrant
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.ContentPadding
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.ProgressPanel
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Scaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Ui
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.filledButton
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.messageDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.textButton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * The browser sign-in of an account (roadmap P9): generates the PKCE verifier and the `state`,
 * opens the provider's authorization page in a Custom Tab (any browser as the fallback), waits
 * below the tab for [OAuthRedirectActivity] to hand the redirect back through `onNewIntent`,
 * checks the `state`, exchanges the code off the main thread and returns the grant: to the
 * account editor through [PendingGrants] (a new account) or straight into the store (a
 * re-authorization of [EXTRA_ALIAS]). Nothing but the authorization code ever comes back from
 * the browser; the password stays with the provider.
 */
class OAuthSignInActivity : ConfiguredActivity() {

    private lateinit var clients: OAuthClients
    private lateinit var providerId: OAuthProviderId
    private var alias: String? = null
    private var loginHint: String? = null
    private var state: String? = null
    private var pkce: PkceChallenge? = null
    private var browserOpened = false
    private var busy = false
    private var executor: ExecutorService? = null

    private lateinit var scaffold: Scaffold
    private lateinit var statusView: TextView
    private lateinit var progress: ProgressPanel
    private lateinit var openButton: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clients = OAuthClients.of(this)
        providerId = OAuthProviderId.fromId(intent.getStringExtra(EXTRA_PROVIDER)) ?: run {
            finish()
            return
        }
        alias = intent.getStringExtra(EXTRA_ALIAS)?.takeIf { it.isNotBlank() }
        loginHint = intent.getStringExtra(EXTRA_LOGIN_HINT)?.takeIf { it.isNotBlank() }
        state = savedInstanceState?.getString(STATE_STATE)
        pkce = savedInstanceState?.getString(STATE_VERIFIER)?.let { PkceChallenge(it, Pkce.challengeOf(it)) }
        browserOpened = savedInstanceState?.getBoolean(STATE_BROWSER_OPENED) ?: false

        scaffold = buildScaffold(R.string.oauth_title, contentPadding = ContentPadding.SCREEN)
        buildContent(scaffold.content)
        setContentView(scaffold.root)

        if (!clients.isConfigured(providerId)) {
            messageDialog(getString(R.string.oauth_title), getString(R.string.oauth_missing_client, providerName())).setOnDismissListener { finish() }
            return
        }
        val redirect = intent.data
        when {
            redirect != null -> handleRedirect(redirect)
            !browserOpened -> openBrowser()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let(::handleRedirect)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_STATE, state)
        outState.putString(STATE_VERIFIER, pkce?.verifier)
        outState.putBoolean(STATE_BROWSER_OPENED, browserOpened)
    }

    override fun onDestroy() {
        executor?.shutdownNow()
        super.onDestroy()
    }

    private fun buildContent(content: LinearLayout) {
        statusView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
            setTextColor(appPalette.primaryText)
            setLineSpacing(0f, Ui.LINE_SPACING_BODY)
            text = getString(R.string.oauth_waiting, providerName())
        }
        content.addView(statusView, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val hint = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECONDARY)
            setTextColor(appPalette.secondaryText)
            setLineSpacing(0f, Ui.LINE_SPACING_BODY)
            text = getString(R.string.oauth_waiting_hint)
        }
        content.addView(
            hint,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = uiDp(Ui.SPACE_MD) },
        )
        progress = ProgressPanel(this)
        content.addView(
            progress.view,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = uiDp(Ui.SPACE_LG) },
        )
        openButton = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            addView(textButton(R.string.oauth_action_cancel) { cancel() })
            addView(
                filledButton(R.string.oauth_action_open_browser) { openBrowser() },
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = uiDp(Ui.SPACE_MD) },
            )
        }
        content.addView(
            openButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = uiDp(Ui.SECTION_GAP) },
        )
        applyThemeToControls(content)
    }

    private fun providerName(): String = getString(
        when (providerId) {
            OAuthProviderId.GOOGLE -> R.string.editor_oauth_provider_google
            OAuthProviderId.MICROSOFT -> R.string.editor_oauth_provider_microsoft
        },
    )

    private fun openBrowser() {
        if (busy) return
        val challenge = Pkce.generate()
        val requestState = Pkce.state()
        val request = try {
            AuthorizationRequest(clients.provider(providerId), clients.clientId(providerId), clients.redirectUri(providerId), requestState, challenge, loginHint)
        } catch (e: MailException) {
            messageDialog(getString(R.string.oauth_title), e.message).setOnDismissListener { finish() }
            return
        }
        pkce = challenge
        state = requestState
        val uri = Uri.parse(request.url())
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, uri)
        } catch (e: ActivityNotFoundException) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (e2: ActivityNotFoundException) {
                messageDialog(getString(R.string.oauth_title), getString(R.string.oauth_no_browser)).setOnDismissListener { finish() }
                return
            }
        }
        browserOpened = true
        statusView.text = getString(R.string.oauth_waiting, providerName())
        Log.i(TAG, "provider=${providerId.id} browser opened" + (if (alias != null) " (re-authorization)" else ""))
    }

    private fun handleRedirect(redirect: Uri) {
        val expectedState = state
        val challenge = pkce
        if (expectedState == null || challenge == null) {
            showRejected(getString(R.string.oauth_rejected, "no sign-in is waiting"))
            return
        }
        when (val response = AuthorizationResponses.parse(redirect.toString(), clients.redirectUri(providerId), expectedState)) {
            is AuthorizationResponse.Granted -> exchange(response.code, challenge)
            is AuthorizationResponse.Denied -> {
                Log.w(TAG, "provider=${providerId.id} denied: ${response.error}")
                showRejected(getString(R.string.oauth_denied, listOfNotNull(response.error, response.description).joinToString(": ")))
            }
            is AuthorizationResponse.Rejected -> {
                Log.w(TAG, "provider=${providerId.id} redirect rejected: ${response.reason}")
                showRejected(getString(R.string.oauth_rejected, response.reason))
            }
        }
    }

    private fun exchange(code: String, challenge: PkceChallenge) {
        if (busy) return
        busy = true
        // one code, one exchange: the request is spent whatever the outcome
        state = null
        pkce = null
        progress.showIndeterminate(getString(R.string.oauth_exchanging))
        openButton.visibility = LinearLayout.GONE
        val provider = clients.provider(providerId)
        val clientId = clients.clientId(providerId)
        val redirectUri = clients.redirectUri(providerId)
        val targetAlias = alias
        executor().execute {
            val outcome = try {
                val grant = TokenClient(HttpsFormPoster()).exchange(provider, clientId, redirectUri, code, challenge.verifier)
                if (targetAlias != null) AccountSecrets.of(this).storeGrant(targetAlias, providerId, grant.tokens)
                Result.success(grant)
            } catch (e: MailException) {
                Result.failure<TokenGrant>(e)
            } catch (e: RuntimeException) {
                Result.failure<TokenGrant>(MailException(io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailErrorCode.INTERNAL, "the sign-in failed unexpectedly", e.javaClass.simpleName, retryable = false))
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                busy = false
                progress.hide()
                outcome.fold({ grant -> finishWithGrant(grant, targetAlias) }, { error -> showFailure(error as MailException) })
            }
        }
    }

    private fun finishWithGrant(grant: TokenGrant, targetAlias: String?) {
        val renewable = grant.tokens.refreshToken != null
        val secondsLeft = (grant.tokens.expiresAt - System.currentTimeMillis()) / 1000
        Log.i(TAG, "provider=${providerId.id} signed in, renewable=$renewable, expires in $secondsLeft s")
        if (targetAlias != null) {
            Toast.makeText(this, R.string.oauth_reauthorized, Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK)
        } else {
            val id = PendingGrants.put(PendingGrant(providerId, grant.tokens, grant.email, System.currentTimeMillis()))
            Toast.makeText(this, grant.email?.let { getString(R.string.oauth_signed_in, it) } ?: getString(R.string.oauth_signed_in_unknown), Toast.LENGTH_SHORT).show()
            setResult(RESULT_OK, Intent().putExtra(EXTRA_GRANT_ID, id).putExtra(EXTRA_PROVIDER, providerId.id))
        }
        finish()
    }

    private fun showFailure(error: MailException) {
        openButton.visibility = LinearLayout.VISIBLE
        statusView.text = getString(R.string.oauth_failed, listOfNotNull(error.message, error.details).joinToString(" - "))
    }

    private fun showRejected(message: String) {
        openButton.visibility = LinearLayout.VISIBLE
        statusView.text = message
    }

    private fun cancel() {
        setResult(RESULT_CANCELED)
        finish()
    }

    private fun executor(): ExecutorService = executor ?: Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "mail-oauth").apply { isDaemon = true }
    }.also { executor = it }

    companion object {
        private const val TAG = "MailOAuth"
        const val EXTRA_PROVIDER = "oauth.provider"
        const val EXTRA_ALIAS = "oauth.alias"
        const val EXTRA_LOGIN_HINT = "oauth.loginHint"
        const val EXTRA_GRANT_ID = "oauth.grantId"
        private const val STATE_STATE = "oauth.state"
        private const val STATE_VERIFIER = "oauth.verifier"
        private const val STATE_BROWSER_OPENED = "oauth.browserOpened"

        /** A sign-in for a new account ([alias] null) or a re-authorization of a saved one. */
        fun intent(context: Context, provider: OAuthProviderId, alias: String? = null, loginHint: String? = null): Intent =
            Intent(context, OAuthSignInActivity::class.java)
                .putExtra(EXTRA_PROVIDER, provider.id)
                .apply {
                    alias?.let { putExtra(EXTRA_ALIAS, it) }
                    loginHint?.let { putExtra(EXTRA_LOGIN_HINT, it) }
                }
    }
}
