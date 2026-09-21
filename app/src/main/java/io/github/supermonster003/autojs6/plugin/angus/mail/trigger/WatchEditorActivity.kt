package io.github.supermonster003.autojs6.plugin.angus.mail.trigger

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.R
import io.github.supermonster003.autojs6.plugin.angus.mail.angusMailAccountDefaults
import io.github.supermonster003.autojs6.plugin.angus.mail.core.MailLimits
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerConfig
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerFilter
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerId
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchMode
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.ContentPadding
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Scaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.SettingRow
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Ui
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.filledButton
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.formLabel
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.formTextField
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.settingRow
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.singleChoiceDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.switchRow

/**
 * Add / edit form of one background watch (roadmap P8): name, saved account, folder, mode, poll
 * interval, the optional sender and subject filters, and the on / off switch. Saving validates
 * through the mail core (`TriggerConfig.validated`, and the account's protocol for the folder
 * and mode, so a POP3 account keeps `INBOX` and polling), stores the watch and starts the
 * service when the watch is enabled.
 */
class WatchEditorActivity : ConfiguredActivity() {

    private lateinit var store: TriggerStore
    private var editingId: String? = null
    private var existing: TriggerConfig? = null
    private var aliases: List<String> = emptyList()
    private var alias: String? = null
    private var mode: WatchMode? = null
    private var enabled = true

    private lateinit var scaffold: Scaffold
    private lateinit var nameField: Pair<TextInputLayout, TextInputEditText>
    private lateinit var accountRow: SettingRow
    private lateinit var folderField: Pair<TextInputLayout, TextInputEditText>
    private lateinit var modeRow: SettingRow
    private lateinit var intervalField: Pair<TextInputLayout, TextInputEditText>
    private lateinit var fromField: Pair<TextInputLayout, TextInputEditText>
    private lateinit var subjectField: Pair<TextInputLayout, TextInputEditText>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TriggerStores.of(applicationContext)
        editingId = intent.getStringExtra(EXTRA_TRIGGER_ID)?.takeIf { it.isNotBlank() }
        aliases = runCatching { AccountStores.of(applicationContext).list().map { it.alias } }.getOrDefault(emptyList())
        val id = editingId
        if (id != null) {
            existing = try {
                store.get(id) ?: throw MailException.invalidArgument("no background watch named '$id'")
            } catch (e: MailException) {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }
        val initial = existing ?: TriggerConfig(triggerId = "", alias = aliases.firstOrNull().orEmpty())
        alias = savedInstanceState?.getString(STATE_ALIAS) ?: initial.alias.takeIf { it.isNotEmpty() }
        mode = (savedInstanceState?.getString(STATE_MODE) ?: initial.mode)?.let(WatchMode::fromId)
        enabled = savedInstanceState?.getBoolean(STATE_ENABLED) ?: initial.enabled

        scaffold = buildScaffold(
            if (editingId == null) R.string.watch_editor_title_add else R.string.watch_editor_title_edit,
            contentPadding = ContentPadding.SCREEN,
        )
        buildContent(scaffold.content, initial)
        applyThemeToControls(scaffold.content)
        renderChoices()
        setContentView(scaffold.root)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_ALIAS, alias)
        outState.putString(STATE_MODE, mode?.id)
        outState.putBoolean(STATE_ENABLED, enabled)
    }

    private fun buildContent(content: LinearLayout, initial: TriggerConfig) {
        content.addView(formLabel(R.string.watch_editor_name))
        nameField = formTextField(initial.triggerId, hint = getString(R.string.watch_editor_name_help), maxLength = TriggerId.MAX_LENGTH)
        nameField.second.isEnabled = editingId == null
        content.addView(nameField.first, fieldParams())

        accountRow = settingRow(
            title = getString(R.string.watch_editor_account),
            summary = "",
            iconResource = R.drawable.ic_person_24,
            onClick = ::chooseAccount,
        )
        content.addView(accountRow.view, rowParams())

        content.addView(formLabel(R.string.watch_editor_folder))
        folderField = formTextField(initial.folder, maxLength = MailLimits.MAX_OPTION_STRING_LENGTH)
        content.addView(folderField.first, fieldParams())

        modeRow = settingRow(
            title = getString(R.string.watch_editor_mode),
            summary = "",
            iconResource = R.drawable.ic_tune_24,
            onClick = ::chooseMode,
        )
        content.addView(modeRow.view, rowParams())

        content.addView(formLabel(R.string.watch_editor_interval))
        intervalField = formTextField(
            (initial.pollIntervalMs / 1000).toString(),
            hint = getString(R.string.watch_editor_interval_help, MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS),
            inputType = InputType.TYPE_CLASS_NUMBER,
            maxLength = 5,
        )
        content.addView(intervalField.first, fieldParams())

        content.addView(formLabel(R.string.watch_editor_filter_from))
        fromField = formTextField(initial.filter.from.joinToString(", "), hint = getString(R.string.watch_editor_filter_help), singleLine = false)
        content.addView(fromField.first, fieldParams())
        content.addView(formLabel(R.string.watch_editor_filter_subject))
        subjectField = formTextField(initial.filter.subject.joinToString(", "), hint = getString(R.string.watch_editor_filter_help), singleLine = false)
        content.addView(subjectField.first, fieldParams())

        content.addView(
            switchRow(
                title = getString(R.string.watch_editor_enabled),
                summary = getString(R.string.watch_editor_enabled_summary),
                checked = enabled,
            ) { enabled = it }.view,
            rowParams(),
        )

        content.addView(
            filledButton(R.string.action_save) { save() },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                topMargin = uiDp(Ui.SPACE_XL)
            },
        )
    }

    private fun fieldParams() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

    private fun rowParams() = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = uiDp(Ui.SPACE_SM)
        marginStart = -uiDp(Ui.SCREEN_MARGIN)
        marginEnd = -uiDp(Ui.SCREEN_MARGIN)
    }

    private fun renderChoices() {
        accountRow.summaryView.text = alias ?: getString(if (aliases.isEmpty()) R.string.watch_editor_account_none else R.string.watch_editor_account_hint)
        modeRow.summaryView.text = getString(
            when (mode) {
                WatchMode.IDLE -> R.string.watch_editor_mode_idle
                WatchMode.POLL -> R.string.watch_editor_mode_poll
                null -> R.string.watch_editor_mode_auto
            },
        )
        intervalField.first.visibility = if (mode == WatchMode.IDLE) View.GONE else View.VISIBLE
    }

    private fun chooseAccount() {
        if (aliases.isEmpty()) {
            Toast.makeText(this, R.string.watch_editor_account_none, Toast.LENGTH_LONG).show()
            return
        }
        singleChoiceDialog(getString(R.string.watch_editor_account), aliases, aliases.indexOf(alias)) { index ->
            alias = aliases[index]
            renderChoices()
        }
    }

    private fun chooseMode() {
        val choices = listOf(null, WatchMode.IDLE, WatchMode.POLL)
        val labels = listOf(R.string.watch_editor_mode_auto, R.string.watch_editor_mode_idle, R.string.watch_editor_mode_poll).map { getString(it) }
        singleChoiceDialog(getString(R.string.watch_editor_mode), labels, choices.indexOf(mode)) { index ->
            mode = choices[index]
            renderChoices()
        }
    }

    private fun save() {
        nameField.first.error = null
        folderField.first.error = null
        intervalField.first.error = null
        accountRow.summaryView.setTextColor(appPalette.secondaryText)
        val name = editingId ?: nameField.second.text?.toString().orEmpty().trim()
        if (name.isEmpty()) {
            nameField.first.error = getString(R.string.watch_editor_error_name_required)
            return
        }
        if (!TriggerId.isValid(name)) {
            nameField.first.error = getString(R.string.watch_editor_error_name_invalid)
            return
        }
        val chosenAlias = alias
        if (chosenAlias == null) {
            accountRow.summaryView.text = getString(R.string.watch_editor_error_account_required)
            accountRow.summaryView.setTextColor(getColor(R.color.mail_error))
            return
        }
        val folder = folderField.second.text?.toString().orEmpty().trim()
        if (folder.isEmpty()) {
            folderField.first.error = getString(R.string.watch_editor_error_folder_required)
            return
        }
        val seconds = intervalField.second.text?.toString().orEmpty().trim().toLongOrNull()
        if (seconds == null || seconds !in MIN_INTERVAL_SECONDS..MAX_INTERVAL_SECONDS) {
            intervalField.first.error = getString(R.string.watch_editor_error_interval_invalid, MIN_INTERVAL_SECONDS, MAX_INTERVAL_SECONDS)
            return
        }
        val config = TriggerConfig(
            triggerId = name,
            alias = chosenAlias,
            enabled = enabled,
            folder = folder,
            mode = mode?.id,
            pollIntervalMs = seconds * 1000,
            filter = TriggerFilter(TriggerFilter.split(fromField.second.text?.toString()), TriggerFilter.split(subjectField.second.text?.toString())),
            since = existing?.since ?: 0L,
        )
        try {
            val validated = config.validated()
            // The account's protocol decides whether the folder and the mode are usable (POP3: INBOX, polling).
            AccountStores.of(applicationContext).get(chosenAlias)?.let { saved ->
                validated.watchOptions(MailAccountOptions.parse(saved.accountJson, saved.secretKind, angusMailAccountDefaults()).receive)
            }
            store.put(validated)
        } catch (e: MailException) {
            folderField.first.error = e.message
            return
        }
        if (!MailWatchService.start(this)) WatchKeeper.of(applicationContext).sync()
        Toast.makeText(this, R.string.watch_editor_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    companion object {
        private const val EXTRA_TRIGGER_ID = "triggerId"
        private const val STATE_ALIAS = "alias"
        private const val STATE_MODE = "mode"
        private const val STATE_ENABLED = "enabled"
        private const val MIN_INTERVAL_SECONDS = MailLimits.MIN_POLL_INTERVAL_MS / 1000
        private const val MAX_INTERVAL_SECONDS = MailLimits.MAX_POLL_INTERVAL_MS / 1000

        fun intent(context: Context, triggerId: String?): Intent =
            Intent(context, WatchEditorActivity::class.java).putExtra(EXTRA_TRIGGER_ID, triggerId)
    }
}
