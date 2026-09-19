package io.github.supermonster003.autojs6.plugin.angus.mail.settings

import android.content.Intent
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import io.github.supermonster003.autojs6.plugin.angus.mail.AppSettingsActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.R
import io.github.supermonster003.autojs6.plugin.angus.mail.angusMailAccountDefaults
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import io.github.supermonster003.autojs6.plugin.angus.mail.store.SavedAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.ContentPadding
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.ProgressPanel
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Scaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Ui
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.accentTone
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.cardContainer
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.cardListParams
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.confirmDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.emptyStateView
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.filledButton
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.iconButton
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.roundedFill
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.showSnackbar

/**
 * The plugin's main screen (roadmap P4.2): the accounts saved in the encrypted store, one card
 * each with alias, address, provider, authentication and receive protocol, the default mark, and
 * the actions edit / test connection / set default / remove. Secrets are read only for a
 * connection test, inside this process, and wiped afterwards.
 */
class AccountsActivity : ConfiguredActivity() {

    private lateinit var store: AccountStore
    private lateinit var scaffold: Scaffold
    private lateinit var listContainer: LinearLayout
    private lateinit var progress: ProgressPanel
    private val tester = ConnectionTester()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AccountStores.of(applicationContext)
        scaffold = buildScaffold(R.string.accounts_title, showBack = false, contentPadding = ContentPadding.SCREEN)
        progress = ProgressPanel(this).withCancelAction(R.string.editor_test_cancel) { cancelTest() }
        scaffold.content.addView(progress.view, cardListParams())
        listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scaffold.content.addView(
            listContainer,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
        )
        setContentView(scaffold.root)
    }

    override fun onResume() {
        super.onResume()
        renderAccounts()
    }

    override fun onDestroy() {
        tester.shutdown()
        super.onDestroy()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_ADD, 0, R.string.accounts_add).apply {
            setIcon(R.drawable.ic_add_24)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        menu.add(Menu.NONE, MENU_SETTINGS, 1, R.string.settings_title).setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
        scaffold.toolbar.post { tintToolbarIcons(scaffold.toolbar) }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        MENU_ADD -> {
            openEditor(null)
            true
        }
        MENU_SETTINGS -> {
            startActivity(Intent(this, AppSettingsActivity::class.java))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun renderAccounts() {
        listContainer.removeAllViews()
        val accounts = try {
            store.list()
        } catch (e: MailException) {
            listContainer.addView(
                emptyStateView(
                    getString(R.string.accounts_store_unavailable),
                    e.message,
                    R.drawable.ic_warning_24,
                ),
            )
            return
        }
        if (accounts.isEmpty()) {
            listContainer.addView(
                emptyStateView(
                    getString(R.string.accounts_empty_title),
                    getString(R.string.accounts_empty_description),
                    R.drawable.ic_mail_24,
                    filledButton(R.string.accounts_add) { openEditor(null) },
                ),
            )
            return
        }
        accounts.forEach { account -> listContainer.addView(accountCard(account), cardListParams()) }
    }

    private fun accountCard(account: SavedAccount): View {
        val summary = AccountSummary.of(this, account)
        val card = cardContainer(interactive = true, selected = account.isDefault)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.addView(
            ImageView(this).apply {
                setImageDrawable(tintedDrawable(if (account.isDefault) R.drawable.ic_star_24 else R.drawable.ic_mail_24, appPalette.accent))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(uiDp(Ui.ICON_SIZE), uiDp(Ui.ICON_SIZE)).apply { marginEnd = uiDp(Ui.SPACE_LG) },
        )
        card.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(
                    LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        addView(
                            TextView(context).apply {
                                text = account.alias
                                setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_ITEM)
                                typeface = Ui.mediumTypeface
                                setTextColor(appPalette.primaryText)
                            },
                        )
                        if (account.isDefault) {
                            addView(
                                TextView(context).apply {
                                    text = getString(R.string.accounts_default_mark)
                                    setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_CAPTION)
                                    typeface = Ui.mediumTypeface
                                    setTextColor(appPalette.accent)
                                    background = roundedFill(accentTone(appPalette.accent), Ui.RADIUS_CONTROL)
                                    setPaddingRelative(uiDp(Ui.SPACE_SM), uiDp(2), uiDp(Ui.SPACE_SM), uiDp(2))
                                },
                                LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                                    marginStart = uiDp(Ui.SPACE_SM)
                                },
                            )
                        }
                    },
                )
                addView(
                    TextView(context).apply {
                        text = summary.address
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
                        setTextColor(appPalette.primaryText)
                        setPaddingRelative(0, uiDp(2), 0, 0)
                    },
                )
                addView(
                    TextView(context).apply {
                        text = summary.detail
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECONDARY)
                        setTextColor(if (summary.damaged) getColor(R.color.mail_error) else appPalette.secondaryText)
                        setPaddingRelative(0, uiDp(2), 0, 0)
                    },
                )
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        val more = iconButton(R.drawable.ic_more_vert_24, R.string.accounts_more_options) { }
        more.setOnClickListener { showAccountMenu(more, account) }
        card.addView(more)
        card.contentDescription = listOf(account.alias, summary.address, summary.detail).joinToString(", ")
        card.setOnClickListener { openEditor(account.alias) }
        return card
    }

    private fun showAccountMenu(anchor: View, account: SavedAccount) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(Menu.NONE, ACTION_EDIT, 0, R.string.accounts_action_edit)
        popup.menu.add(Menu.NONE, ACTION_TEST, 1, R.string.accounts_action_test)
        popup.menu.add(
            Menu.NONE,
            ACTION_DEFAULT,
            2,
            if (account.isDefault) R.string.accounts_action_clear_default else R.string.accounts_action_set_default,
        )
        popup.menu.add(Menu.NONE, ACTION_REMOVE, 3, R.string.accounts_action_remove)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                ACTION_EDIT -> openEditor(account.alias)
                ACTION_TEST -> testSavedAccount(account.alias)
                ACTION_DEFAULT -> toggleDefault(account)
                ACTION_REMOVE -> confirmRemove(account)
            }
            true
        }
        popup.show()
    }

    private fun openEditor(alias: String?) {
        startActivity(AccountEditorActivity.intent(this, alias))
    }

    private fun toggleDefault(account: SavedAccount) {
        runStoreAction {
            if (account.isDefault) {
                store.setDefault(null)
                showSnackbar(scaffold.root, getString(R.string.accounts_default_cleared))
            } else {
                store.setDefault(account.alias)
                showSnackbar(scaffold.root, getString(R.string.accounts_default_set, account.alias))
            }
            renderAccounts()
        }
    }

    private fun confirmRemove(account: SavedAccount) {
        confirmDialog(
            getString(R.string.accounts_remove_title),
            getString(R.string.accounts_remove_message, account.alias),
            R.string.accounts_action_remove,
            destructive = true,
        ) {
            runStoreAction {
                store.remove(account.alias)
                showSnackbar(scaffold.root, getString(R.string.accounts_removed))
                renderAccounts()
            }
        }
    }

    /** Decrypts the saved secret for the probe only; the copy is wiped by the tester. */
    private fun testSavedAccount(alias: String) {
        if (tester.isRunning) return
        val prepared: Pair<MailAccount, CharArray> = try {
            store.withSecret(alias) { saved, secret ->
                MailAccountOptions.parse(saved.accountJson, saved.secretKind, angusMailAccountDefaults()) to secret.copyOf()
            }
        } catch (e: MailException) {
            showSnackbar(scaffold.root, e.message)
            return
        }
        val (account, secret) = prepared
        progress.showIndeterminate(getString(R.string.editor_testing), account.address)
        progress.setCancelEnabled(true)
        scaffold.scroll?.smoothScrollTo(0, 0)
        tester.start(account, secret) { outcome ->
            progress.hide()
            showConnectionTestResult(account, outcome)
        }
    }

    private fun cancelTest() {
        tester.cancel()
        progress.hide()
        showSnackbar(scaffold.root, getString(R.string.editor_test_cancelled))
    }

    private inline fun runStoreAction(action: () -> Unit) {
        try {
            action()
        } catch (e: MailException) {
            showSnackbar(scaffold.root, e.message)
        }
    }

    private companion object {
        const val MENU_ADD = 1
        const val MENU_SETTINGS = 2
        const val ACTION_EDIT = 10
        const val ACTION_TEST = 11
        const val ACTION_DEFAULT = 12
        const val ACTION_REMOVE = 13
    }
}

/** The two summary lines of an account card; a record that no longer normalizes shows its error. */
internal class AccountSummary(val address: String, val detail: String, val damaged: Boolean) {
    companion object {
        fun of(activity: ConfiguredActivity, account: SavedAccount): AccountSummary {
            return try {
                val normalized = MailAccountOptions.parse(account.accountJson, account.secretKind)
                val provider = normalized.provider?.name ?: activity.getString(R.string.accounts_provider_custom)
                val auth = activity.getString(
                    if (normalized.auth == AuthMethod.XOAUTH2) R.string.accounts_summary_auth_token else R.string.accounts_summary_auth_password,
                )
                val receive = normalized.receiveEndpoint?.let { normalized.receive.id.uppercase() }
                    ?: activity.getString(R.string.accounts_summary_send_only)
                AccountSummary(normalized.address, listOf(provider, receive, auth).joinToString(" - "), damaged = false)
            } catch (e: MailException) {
                AccountSummary(
                    AccountFormPolicy.fromAccountJson(account.accountJson, account.secretKind).address,
                    activity.getString(R.string.accounts_record_invalid, e.message),
                    damaged = true,
                )
            }
        }
    }
}
