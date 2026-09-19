package io.github.supermonster003.autojs6.plugin.angus.mail.settings

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.annotation.StringRes
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.R
import io.github.supermonster003.autojs6.plugin.angus.mail.angusMailAccountDefaults
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.AuthMethod
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccountOptions
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailProtocol
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.ProviderPresets
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.SecretKind
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.TlsMode
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountAlias
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStore
import io.github.supermonster003.autojs6.plugin.angus.mail.store.AccountStores
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.ContentPadding
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.ProgressPanel
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Scaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.SettingRow
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Ui
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.filledButton
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.formLabel
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.formTextField
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.hairline
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.messageDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.settingRow
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.showSnackbar
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.singleChoiceDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.switchRow
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.textButton
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.tonalButton
import java.util.EnumMap

/**
 * Add / edit form of one saved account (roadmap P4.2). Choosing a preset fills the server fields;
 * the server section stays available for overrides and custom servers. The secret is read from
 * the field into a `CharArray` only for the store or for a connection test and never enters the
 * saved instance state; editing keeps the stored secret unless a new one is typed.
 */
class AccountEditorActivity : ConfiguredActivity() {

    private lateinit var store: AccountStore
    private var editingAlias: String? = null
    private var savedSecretKind: SecretKind? = null
    private var form = AccountFormPolicy.blank()
    private var serversVisible = false
    private val tester = ConnectionTester()

    private lateinit var scaffold: Scaffold
    private lateinit var aliasField: Pair<TextInputLayout, TextInputEditText>
    private lateinit var addressField: Pair<TextInputLayout, TextInputEditText>
    private lateinit var userField: Pair<TextInputLayout, TextInputEditText>
    private lateinit var nameField: Pair<TextInputLayout, TextInputEditText>
    private lateinit var secretField: Pair<TextInputLayout, TextInputEditText>
    private lateinit var providerRow: SettingRow
    private lateinit var authRow: SettingRow
    private lateinit var receiveRow: SettingRow
    private lateinit var helpRow: SettingRow
    private lateinit var serversToggle: MaterialButton
    private lateinit var serversSection: LinearLayout
    private val endpointViews = EnumMap<MailProtocol, EndpointViews>(MailProtocol::class.java)
    private lateinit var progress: ProgressPanel
    private lateinit var testButton: MaterialButton
    private lateinit var saveButton: MaterialButton

    private class EndpointViews(
        val block: LinearLayout,
        val switchRow: SettingRow,
        val fields: LinearLayout,
        val host: Pair<TextInputLayout, TextInputEditText>,
        val port: Pair<TextInputLayout, TextInputEditText>,
        val tlsRow: SettingRow,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AccountStores.of(applicationContext)
        editingAlias = intent.getStringExtra(EXTRA_ALIAS)?.takeIf { it.isNotBlank() }
        val restored = savedInstanceState?.let(::formFromBundle)
        if (!loadInitialForm(restored)) return
        serversVisible = savedInstanceState?.getBoolean(STATE_SERVERS_VISIBLE) ?: (form.providerId == null)

        scaffold = buildScaffold(
            if (editingAlias == null) R.string.editor_title_add else R.string.editor_title_edit,
            contentPadding = ContentPadding.NONE,
        )
        buildContent(scaffold.content)
        buildActions(scaffold.root)
        applyThemeToControls(scaffold.content)
        renderChoices()
        renderEndpoints(writeTexts = true)
        setContentView(scaffold.root)
    }

    /** Returns false after finishing the screen because the record could not be opened. */
    private fun loadInitialForm(restored: AccountForm?): Boolean {
        val alias = editingAlias
        if (alias != null) {
            val saved = try {
                store.get(alias) ?: throw MailException.invalidArgument("no saved account named '$alias'")
            } catch (e: MailException) {
                Toast.makeText(this, e.message, Toast.LENGTH_LONG).show()
                finish()
                return false
            }
            savedSecretKind = saved.secretKind
            form = restored ?: AccountFormPolicy.fromAccountJson(saved.accountJson, saved.secretKind).copy(alias = saved.alias)
        } else {
            form = restored ?: AccountFormPolicy.blank()
        }
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (!::aliasField.isInitialized) return
        collect()
        formToBundle(form, outState)
        outState.putBoolean(STATE_SERVERS_VISIBLE, serversVisible)
    }

    override fun onDestroy() {
        tester.shutdown()
        super.onDestroy()
    }

    // region Layout

    private fun buildContent(content: LinearLayout) {
        val fields = fieldColumn()
        aliasField = formTextField(form.alias, getString(R.string.editor_field_alias), maxLength = AccountAlias.MAX_LENGTH)
        aliasField.first.helperText = getString(R.string.editor_field_alias_helper)
        fields.addView(aliasField.first, fieldParams())
        content.addView(fields)

        providerRow = settingRow(
            title = getString(R.string.editor_field_provider),
            summary = "",
            iconResource = R.drawable.ic_cloud_24,
            onClick = ::showProviderDialog,
        )
        content.addView(providerRow.view)

        val identity = fieldColumn()
        addressField = formTextField(
            form.address,
            getString(R.string.editor_field_address),
            InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
        )
        identity.addView(addressField.first, fieldParams())
        userField = formTextField(form.user, getString(R.string.editor_field_user))
        userField.first.helperText = getString(R.string.editor_field_user_helper)
        identity.addView(userField.first, fieldParams())
        nameField = formTextField(form.name, getString(R.string.editor_field_name), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        nameField.first.helperText = getString(R.string.editor_field_name_helper)
        identity.addView(nameField.first, fieldParams())
        content.addView(identity)

        authRow = settingRow(
            title = getString(R.string.editor_field_auth),
            summary = "",
            iconResource = R.drawable.ic_key_24,
            onClick = ::showAuthDialog,
        )
        content.addView(authRow.view)

        val secretColumn = fieldColumn()
        secretField = formTextField(null, "", InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD)
        secretField.second.isSaveEnabled = false
        // Password fields default to a monospace face; keep the form's type consistent.
        secretField.second.typeface = Typeface.DEFAULT
        secretField.first.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
        secretField.first.setEndIconContentDescription(R.string.editor_secret_toggle)
        secretColumn.addView(secretField.first, fieldParams())
        content.addView(secretColumn)

        helpRow = settingRow(
            title = getString(R.string.editor_help_provider_docs),
            summary = "",
            iconResource = R.drawable.ic_info_24,
            onClick = ::openProviderDocs,
        )
        content.addView(helpRow.view)

        receiveRow = settingRow(
            title = getString(R.string.editor_field_receive),
            summary = "",
            iconResource = R.drawable.ic_tune_24,
            onClick = ::showReceiveDialog,
        )
        content.addView(receiveRow.view)
        content.addView(hairline())

        serversToggle = textButton(R.string.editor_servers_show) { toggleServers() }
        content.addView(
            serversToggle,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = uiDp(Ui.SCREEN_MARGIN - Ui.SPACE_MD)
                topMargin = uiDp(Ui.SPACE_SM)
            },
        )
        serversSection = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        MailProtocol.entries.forEach { protocol -> serversSection.addView(endpointBlock(protocol)) }
        content.addView(serversSection)

        progress = ProgressPanel(this).withCancelAction(R.string.editor_test_cancel) { cancelTest() }
        content.addView(
            progress.view,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = uiDp(Ui.SCREEN_MARGIN)
                marginEnd = uiDp(Ui.SCREEN_MARGIN)
                topMargin = uiDp(Ui.SPACE_LG)
            },
        )
    }

    private fun endpointBlock(protocol: MailProtocol): LinearLayout {
        val block = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val label = formLabel(R.string.editor_section_servers).apply {
            text = protocol.id.uppercase()
            setPaddingRelative(uiDp(Ui.SCREEN_MARGIN), uiDp(Ui.SPACE_LG), uiDp(Ui.SCREEN_MARGIN), 0)
        }
        block.addView(label)
        val switch = switchRow(
            title = getString(R.string.editor_endpoint_enabled, protocol.id.uppercase()),
            checked = form.endpoint(protocol).enabled,
        ) { checked ->
            form = form.withEndpoint(protocol, form.endpoint(protocol).copy(enabled = checked))
            endpointViews.getValue(protocol).fields.visibility = if (checked) View.VISIBLE else View.GONE
        }
        block.addView(switch.view)
        val fields = fieldColumn()
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        val host = formTextField(form.endpoint(protocol).host, getString(R.string.editor_field_host), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        val port = formTextField(form.endpoint(protocol).port, getString(R.string.editor_field_port), InputType.TYPE_CLASS_NUMBER, maxLength = 5)
        row.addView(host.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 2f).apply { marginEnd = uiDp(Ui.SPACE_MD) })
        row.addView(port.first, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        fields.addView(row, fieldParams())
        block.addView(fields)
        val tlsRow = settingRow(
            title = getString(R.string.editor_field_tls),
            summary = "",
            iconResource = null,
            onClick = { showTlsDialog(protocol) },
        )
        block.addView(tlsRow.view)
        endpointViews[protocol] = EndpointViews(block, switch, fields, host, port, tlsRow)
        return block
    }

    private fun buildActions(root: LinearLayout) {
        root.addView(hairline(insetStartDp = 0))
        val bar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            setBackgroundColor(appPalette.windowBackground)
            setPaddingRelative(uiDp(Ui.SCREEN_MARGIN), uiDp(Ui.SPACE_MD), uiDp(Ui.SCREEN_MARGIN), uiDp(Ui.SPACE_MD))
        }
        testButton = tonalButton(R.string.editor_action_test) { runTest() }
        saveButton = filledButton(R.string.action_save) { save() }
        bar.addView(testButton)
        bar.addView(
            saveButton,
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                marginStart = uiDp(Ui.SPACE_MD)
            },
        )
        root.addView(bar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    private fun fieldColumn(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPaddingRelative(uiDp(Ui.SCREEN_MARGIN), uiDp(Ui.SPACE_SM), uiDp(Ui.SCREEN_MARGIN), 0)
    }

    private fun fieldParams(): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
            bottomMargin = uiDp(Ui.SPACE_SM)
        }

    // endregion

    // region Rendering the choice state

    private fun renderChoices() {
        val preset = form.provider
        providerRow.showSummary(preset?.name ?: getString(R.string.accounts_provider_custom))
        authRow.showSummary(getString(authLabel(form.auth)))
        receiveRow.showSummary(form.receive.id.uppercase())
        secretField.first.hint = getString(
            if (form.auth == AuthMethod.XOAUTH2) R.string.editor_field_token else R.string.editor_field_password,
        )
        secretField.first.helperText = when {
            editingAlias != null && savedSecretKind == form.secretKind -> getString(R.string.editor_secret_keep_helper)
            preset != null -> preset.authHint
            else -> null
        }
        helpRow.view.visibility = if (preset?.docsUrl.isNullOrBlank()) View.GONE else View.VISIBLE
        helpRow.summaryView.text = preset?.docsUrl.orEmpty()
        helpRow.summaryView.visibility = if (preset?.docsUrl.isNullOrBlank()) View.GONE else View.VISIBLE
        serversToggle.text = getString(if (serversVisible) R.string.editor_servers_hide else R.string.editor_servers_show)
        serversSection.visibility = if (serversVisible) View.VISIBLE else View.GONE
    }

    /** Pushes the endpoint state into the server blocks; [writeTexts] also replaces host / port texts. */
    private fun renderEndpoints(writeTexts: Boolean) {
        MailProtocol.entries.forEach { protocol ->
            val views = endpointViews.getValue(protocol)
            val fields = form.endpoint(protocol)
            val fixed = form.endpointFixedByPreset(protocol)
            views.switchRow.view.visibility = if (fixed) View.GONE else View.VISIBLE
            views.switchRow.switchView?.isChecked = fields.enabled
            views.fields.visibility = if (fields.enabled) View.VISIBLE else View.GONE
            views.tlsRow.view.visibility = if (fields.enabled) View.VISIBLE else View.GONE
            views.tlsRow.showSummary(getString(tlsLabel(fields.tls)))
            if (writeTexts) {
                views.host.second.setText(fields.host)
                views.port.second.setText(fields.port)
            }
        }
    }

    /** Reads the text fields back into [form]; the choice state is already there. */
    private fun collect(): AccountForm {
        var updated = form.copy(
            alias = aliasField.second.text?.toString().orEmpty(),
            address = addressField.second.text?.toString().orEmpty(),
            user = userField.second.text?.toString().orEmpty(),
            name = nameField.second.text?.toString().orEmpty(),
        )
        MailProtocol.entries.forEach { protocol ->
            val views = endpointViews.getValue(protocol)
            updated = updated.withEndpoint(
                protocol,
                updated.endpoint(protocol).copy(
                    host = views.host.second.text?.toString().orEmpty(),
                    port = views.port.second.text?.toString().orEmpty(),
                ),
            )
        }
        form = updated
        return updated
    }

    /** Replaces the whole model and rewrites every field from it; the secret field is left alone. */
    internal fun replaceForm(updated: AccountForm) {
        form = updated
        aliasField.second.setText(updated.alias)
        addressField.second.setText(updated.address)
        userField.second.setText(updated.user)
        nameField.second.setText(updated.name)
        renderChoices()
        renderEndpoints(writeTexts = true)
    }

    /** Rows are created without a summary; the value line appears once a choice is rendered. */
    private fun SettingRow.showSummary(text: CharSequence) {
        summaryView.text = text
        summaryView.visibility = View.VISIBLE
    }

    private fun toggleServers() {
        serversVisible = !serversVisible
        renderChoices()
    }

    // endregion

    // region Choice dialogs

    private fun showProviderDialog() {
        val presets = ProviderPresets.all
        val labels: List<CharSequence> = listOf(getString(R.string.accounts_provider_custom)) + presets.map { it.name }
        val checked = presets.indexOfFirst { it.id == form.providerId }.let { if (it < 0) 0 else it + 1 }
        singleChoiceDialog(getString(R.string.editor_field_provider), labels, checked) { index ->
            val updated = AccountFormPolicy.applyPreset(collect(), presets.getOrNull(index - 1))
            // A preset needs no server editing; a custom server is nothing but its servers.
            serversVisible = updated.providerId == null
            replaceForm(updated)
        }
    }

    private fun showAuthDialog() {
        val methods = AuthMethod.entries
        val preset = form.provider
        singleChoiceDialog(
            getString(R.string.editor_field_auth),
            methods.map { getString(authLabel(it)) },
            methods.indexOf(form.auth),
            enabledAt = { index -> preset?.accepts(methods[index]) ?: true },
        ) { index ->
            form = form.copy(auth = methods[index])
            renderChoices()
        }
    }

    private fun showReceiveDialog() {
        val protocols = listOf(MailProtocol.IMAP, MailProtocol.POP3)
        singleChoiceDialog(
            getString(R.string.editor_field_receive),
            protocols.map { it.id.uppercase() },
            protocols.indexOf(form.receive),
        ) { index ->
            form = form.copy(receive = protocols[index])
            renderChoices()
        }
    }

    private fun showTlsDialog(protocol: MailProtocol) {
        val modes = TlsMode.entries
        val current = form.endpoint(protocol)
        singleChoiceDialog(
            protocol.id.uppercase() + " - " + getString(R.string.editor_field_tls),
            modes.map { getString(tlsLabel(it)) },
            modes.indexOf(current.tls),
        ) { index ->
            collect()
            val before = form.endpoint(protocol)
            val tls = modes[index]
            // A port that still holds the old mode's convention follows the new one.
            val port = before.port.trim()
            val followedPort = if (port.isEmpty() || port == AccountFormPolicy.defaultPort(protocol, before.tls).toString()) {
                AccountFormPolicy.defaultPort(protocol, tls).toString()
            } else {
                port
            }
            form = form.withEndpoint(protocol, before.copy(tls = tls, port = followedPort))
            endpointViews.getValue(protocol).port.second.setText(followedPort)
            renderEndpoints(writeTexts = false)
        }
    }

    private fun openProviderDocs() {
        val url = form.provider?.docsUrl?.takeIf { it.isNotBlank() } ?: return
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
            .onFailure { showSnackbar(scaffold.root, getString(R.string.open_link_failed)) }
    }

    // endregion

    // region Validation, test and save

    private class Prepared(val account: MailAccount, val json: String, val secret: CharArray?)

    private fun prepare(forSave: Boolean): Prepared? {
        collect()
        clearFieldErrors()
        val errors = ArrayList(AccountFormPolicy.validate(form))
        if (forSave && errors.none { it.field == FormField.ALIAS }) {
            val alias = AccountAlias.normalize(form.alias)
            if (alias != editingAlias && aliasExists(alias)) {
                aliasField.first.error = getString(R.string.editor_error_alias_taken)
                return null
            }
        }
        if (errors.isNotEmpty()) {
            showFieldErrors(errors)
            return null
        }
        val json = AccountFormPolicy.toAccountJson(form)
        val account = try {
            MailAccountOptions.parse(json, form.secretKind, angusMailAccountDefaults())
        } catch (e: MailException) {
            messageDialog(getString(R.string.editor_error_invalid_title), getString(R.string.editor_error_invalid, e.message))
            return null
        }
        val secret = readSecret()
        if (secret == null && !(editingAlias != null && savedSecretKind == form.secretKind)) {
            secretField.first.error = getString(R.string.editor_error_secret_required)
            return null
        }
        return Prepared(account, json, secret)
    }

    private fun aliasExists(alias: String): Boolean = try {
        store.has(alias)
    } catch (e: MailException) {
        // A record that exists but cannot be read still occupies the alias.
        true
    }

    /** The secret typed into the field, or null when it is empty; the caller owns the array. */
    private fun readSecret(): CharArray? {
        val editable = secretField.second.text ?: return null
        if (editable.isEmpty()) return null
        val chars = CharArray(editable.length)
        TextUtils.getChars(editable, 0, editable.length, chars, 0)
        return chars
    }

    private fun copySavedSecret(alias: String): CharArray? = try {
        store.withSecret(alias) { _, chars -> chars.copyOf() }
    } catch (e: MailException) {
        messageDialog(getString(R.string.editor_error_invalid_title), e.message)
        null
    }

    private fun runTest() {
        if (tester.isRunning) return
        val prepared = prepare(forSave = false) ?: return
        val secret = prepared.secret ?: editingAlias?.let(::copySavedSecret) ?: return
        progress.showIndeterminate(getString(R.string.editor_testing), prepared.account.address)
        progress.setCancelEnabled(true)
        setActionsEnabled(false)
        scaffold.scroll?.let { scroll -> scroll.post { scroll.fullScroll(View.FOCUS_DOWN) } }
        tester.start(prepared.account, secret) { outcome ->
            progress.hide()
            setActionsEnabled(true)
            showConnectionTestResult(prepared.account, outcome)
        }
    }

    private fun cancelTest() {
        tester.cancel()
        progress.hide()
        setActionsEnabled(true)
        showSnackbar(scaffold.root, getString(R.string.editor_test_cancelled))
    }

    private fun save() {
        val prepared = prepare(forSave = true) ?: return
        val alias = AccountAlias.normalize(form.alias)
        val secret = prepared.secret ?: editingAlias?.let(::copySavedSecret) ?: return
        try {
            store.put(alias, prepared.json, form.secretKind, secret)
            val previous = editingAlias
            if (previous != null && previous != alias) {
                val wasDefault = store.defaultAlias() == previous
                store.remove(previous)
                if (wasDefault) store.setDefault(alias)
            }
        } catch (e: MailException) {
            messageDialog(getString(R.string.editor_save_failed), e.message)
            return
        }
        secretField.second.text?.clear()
        Toast.makeText(this, R.string.editor_saved, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun setActionsEnabled(enabled: Boolean) {
        testButton.isEnabled = enabled
        saveButton.isEnabled = enabled
    }

    private fun clearFieldErrors() {
        listOf(aliasField, addressField, secretField).forEach { it.first.error = null }
        endpointViews.values.forEach { views ->
            views.host.first.error = null
            views.port.first.error = null
        }
    }

    private fun showFieldErrors(errors: List<FormError>) {
        val general = ArrayList<String>()
        errors.forEach { error ->
            val message = getString(problemLabel(error.problem), form.receive.id.uppercase())
            when (error.field) {
                FormField.ALIAS -> aliasField.first.error = message
                FormField.ADDRESS -> addressField.first.error = message
                FormField.IMAP_HOST -> endpointViews.getValue(MailProtocol.IMAP).host.first.error = message
                FormField.IMAP_PORT -> endpointViews.getValue(MailProtocol.IMAP).port.first.error = message
                FormField.POP3_HOST -> endpointViews.getValue(MailProtocol.POP3).host.first.error = message
                FormField.POP3_PORT -> endpointViews.getValue(MailProtocol.POP3).port.first.error = message
                FormField.SMTP_HOST -> endpointViews.getValue(MailProtocol.SMTP).host.first.error = message
                FormField.SMTP_PORT -> endpointViews.getValue(MailProtocol.SMTP).port.first.error = message
                null -> general += message
            }
        }
        if (errors.any { it.field != null && it.field != FormField.ALIAS && it.field != FormField.ADDRESS } || general.isNotEmpty()) {
            serversVisible = true
            renderChoices()
        }
        if (general.isNotEmpty()) {
            showSnackbar(scaffold.root, general.joinToString("\n"))
        }
    }

    // endregion

    // region Labels and state

    @StringRes
    private fun authLabel(method: AuthMethod): Int = when (method) {
        AuthMethod.PASSWORD -> R.string.accounts_summary_auth_password
        AuthMethod.XOAUTH2 -> R.string.editor_auth_token
    }

    @StringRes
    private fun tlsLabel(mode: TlsMode): Int = when (mode) {
        TlsMode.SSL -> R.string.editor_tls_ssl
        TlsMode.STARTTLS -> R.string.editor_tls_starttls
        TlsMode.NONE -> R.string.editor_tls_none
    }

    @StringRes
    private fun problemLabel(problem: FormProblem): Int = when (problem) {
        FormProblem.ALIAS_REQUIRED -> R.string.editor_error_alias_required
        FormProblem.ALIAS_INVALID -> R.string.editor_error_alias_invalid
        FormProblem.ADDRESS_REQUIRED -> R.string.editor_error_address_required
        FormProblem.ADDRESS_INVALID -> R.string.editor_error_address_invalid
        FormProblem.HOST_REQUIRED -> R.string.editor_error_host_required
        FormProblem.PORT_INVALID -> R.string.editor_error_port_invalid
        FormProblem.RECEIVE_ENDPOINT_MISSING -> R.string.editor_error_receive_required
        FormProblem.NO_ENDPOINT -> R.string.editor_error_no_endpoint
    }

    private fun formToBundle(form: AccountForm, bundle: Bundle) {
        bundle.putString(STATE_ALIAS, form.alias)
        bundle.putString(STATE_PROVIDER, form.providerId)
        bundle.putString(STATE_ADDRESS, form.address)
        bundle.putString(STATE_USER, form.user)
        bundle.putString(STATE_NAME, form.name)
        bundle.putString(STATE_AUTH, form.auth.name)
        bundle.putString(STATE_RECEIVE, form.receive.name)
        MailProtocol.entries.forEach { protocol ->
            val fields = form.endpoint(protocol)
            bundle.putBoolean(protocol.id + STATE_ENABLED, fields.enabled)
            bundle.putString(protocol.id + STATE_HOST, fields.host)
            bundle.putString(protocol.id + STATE_PORT, fields.port)
            bundle.putString(protocol.id + STATE_TLS, fields.tls.name)
        }
    }

    private fun formFromBundle(bundle: Bundle): AccountForm? {
        if (!bundle.containsKey(STATE_ALIAS)) return null
        var restored = AccountForm(
            alias = bundle.getString(STATE_ALIAS).orEmpty(),
            providerId = bundle.getString(STATE_PROVIDER),
            address = bundle.getString(STATE_ADDRESS).orEmpty(),
            user = bundle.getString(STATE_USER).orEmpty(),
            name = bundle.getString(STATE_NAME).orEmpty(),
            auth = bundle.getString(STATE_AUTH)?.let { runCatching { AuthMethod.valueOf(it) }.getOrNull() } ?: AuthMethod.PASSWORD,
            receive = bundle.getString(STATE_RECEIVE)?.let { runCatching { MailProtocol.valueOf(it) }.getOrNull() } ?: MailProtocol.IMAP,
        )
        MailProtocol.entries.forEach { protocol ->
            restored = restored.withEndpoint(
                protocol,
                EndpointFields(
                    enabled = bundle.getBoolean(protocol.id + STATE_ENABLED),
                    host = bundle.getString(protocol.id + STATE_HOST).orEmpty(),
                    port = bundle.getString(protocol.id + STATE_PORT).orEmpty(),
                    tls = bundle.getString(protocol.id + STATE_TLS)?.let { runCatching { TlsMode.valueOf(it) }.getOrNull() } ?: TlsMode.SSL,
                ),
            )
        }
        return restored
    }

    // endregion

    companion object {
        private const val EXTRA_ALIAS = "alias"
        private const val STATE_ALIAS = "form.alias"
        private const val STATE_PROVIDER = "form.provider"
        private const val STATE_ADDRESS = "form.address"
        private const val STATE_USER = "form.user"
        private const val STATE_NAME = "form.name"
        private const val STATE_AUTH = "form.auth"
        private const val STATE_RECEIVE = "form.receive"
        private const val STATE_ENABLED = ".enabled"
        private const val STATE_HOST = ".host"
        private const val STATE_PORT = ".port"
        private const val STATE_TLS = ".tls"
        private const val STATE_SERVERS_VISIBLE = "form.serversVisible"

        fun intent(context: Context, alias: String?): Intent =
            Intent(context, AccountEditorActivity::class.java).apply {
                if (alias != null) putExtra(EXTRA_ALIAS, alias)
            }
    }
}
