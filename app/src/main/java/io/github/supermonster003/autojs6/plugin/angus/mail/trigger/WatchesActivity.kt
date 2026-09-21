package io.github.supermonster003.autojs6.plugin.angus.mail.trigger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.materialswitch.MaterialSwitch
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.R
import io.github.supermonster003.autojs6.plugin.angus.mail.core.error.MailException
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerRecord
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.TriggerStatusDocument
import io.github.supermonster003.autojs6.plugin.angus.mail.core.trigger.TriggerConfig
import io.github.supermonster003.autojs6.plugin.angus.mail.core.watch.WatchMode
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.ContentPadding
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Scaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.SettingRow
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Ui
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.buildScaffold
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.cardContainer
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.cardListParams
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.confirmDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.emptyStateView
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.filledButton
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.hairline
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.iconButton
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.messageDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.sectionHeader
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.settingRow
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.showSnackbar
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.switchRow
import java.text.DateFormat
import java.util.Date

/**
 * The background watches page (roadmap P8): the service, notification-permission and
 * start-after-boot rows, then one card per watch with its state, last mail, an on / off switch
 * and the actions edit / records / reconnect / remove. The page re-renders whenever the
 * [WatchKeeper] reports a change, so connection states update while it is open.
 */
class WatchesActivity : ConfiguredActivity() {

    private lateinit var store: TriggerStore
    private lateinit var keeper: WatchKeeper
    private lateinit var scaffold: Scaffold
    private lateinit var serviceRow: SettingRow
    private lateinit var permissionRow: SettingRow
    private lateinit var listContainer: LinearLayout
    private val changed: () -> Unit = { runOnUiThread { if (!isFinishing && !isDestroyed) render() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = TriggerStores.of(applicationContext)
        keeper = WatchKeeper.of(applicationContext)
        scaffold = buildScaffold(R.string.watches_title, contentPadding = ContentPadding.NONE)
        buildContent(scaffold.content)
        applyThemeToControls(scaffold.content)
        setContentView(scaffold.root)
    }

    override fun onResume() {
        super.onResume()
        keeper.addListener(changed)
        render()
    }

    override fun onPause() {
        keeper.removeListener(changed)
        super.onPause()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(Menu.NONE, MENU_ADD, 0, R.string.watches_add).apply {
            setIcon(R.drawable.ic_add_24)
            setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS)
        }
        scaffold.toolbar.post { tintToolbarIcons(scaffold.toolbar) }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        MENU_ADD -> {
            startActivity(WatchEditorActivity.intent(this, null))
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun buildContent(content: LinearLayout) {
        content.addView(sectionHeader(R.string.watches_section_service))
        serviceRow = settingRow(
            title = getString(R.string.watches_service_title),
            summary = "",
            iconResource = R.drawable.ic_visibility_24,
            showChevron = false,
        )
        content.addView(serviceRow.view)
        permissionRow = settingRow(
            title = getString(R.string.watches_permission_title),
            summary = "",
            iconResource = R.drawable.ic_notifications_24,
            onClick = ::requestNotificationPermission,
        )
        content.addView(permissionRow.view)
        content.addView(
            switchRow(
                title = getString(R.string.watches_boot_title),
                summary = getString(R.string.watches_boot_summary),
                iconResource = R.drawable.ic_power_24,
                checked = BootReceiver.isEnabled(this),
            ) { enabled -> BootReceiver.setEnabled(this, enabled) }.view,
        )
        content.addView(hairline())
        content.addView(sectionHeader(R.string.watches_section_list))
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(uiDp(Ui.SCREEN_MARGIN), 0, uiDp(Ui.SCREEN_MARGIN), uiDp(Ui.SECTION_GAP))
        }
        content.addView(listContainer, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
    }

    // ------------------------------------------------------------------ rendering

    private fun render() {
        val configs = try {
            store.list()
        } catch (e: MailException) {
            listContainer.removeAllViews()
            listContainer.addView(emptyStateView(getString(R.string.watches_store_unavailable), e.message, R.drawable.ic_warning_24))
            return
        }
        val enabled = configs.count { it.enabled }
        serviceRow.summaryView.text = if (keeper.running > 0) {
            getString(R.string.watches_service_running, keeper.connected, enabled)
        } else {
            getString(R.string.watches_service_stopped)
        }
        permissionRow.summaryView.text = getString(
            if (notificationsAllowed()) R.string.watches_permission_granted else R.string.watches_permission_missing,
        )
        listContainer.removeAllViews()
        if (!HostTriggerSender(this).isHostReceiverPresent()) {
            listContainer.addView(
                TextView(this).apply {
                    text = getString(R.string.watches_host_missing)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECONDARY)
                    setTextColor(getColor(R.color.mail_error))
                    setPaddingRelative(0, 0, 0, uiDp(Ui.SPACE_MD))
                },
            )
        }
        if (configs.isEmpty()) {
            listContainer.addView(
                emptyStateView(
                    getString(R.string.watches_empty_title),
                    getString(R.string.watches_empty_description),
                    R.drawable.ic_visibility_24,
                    filledButton(R.string.watches_add) { startActivity(WatchEditorActivity.intent(this, null)) },
                ),
            )
            return
        }
        configs.forEach { config -> listContainer.addView(watchCard(config, keeper.status(config.triggerId)), cardListParams()) }
    }

    private fun watchCard(config: TriggerConfig, status: TriggerStatusDocument): View {
        val connected = status.state == TriggerStatusDocument.STATE_CONNECTED
        val card = cardContainer(interactive = true, selected = connected)
        card.orientation = LinearLayout.HORIZONTAL
        card.gravity = Gravity.CENTER_VERTICAL
        card.addView(
            ImageView(this).apply {
                setImageDrawable(tintedDrawable(R.drawable.ic_mail_24, if (connected) appPalette.accent else appPalette.secondaryText))
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            },
            LinearLayout.LayoutParams(uiDp(Ui.ICON_SIZE), uiDp(Ui.ICON_SIZE)).apply { marginEnd = uiDp(Ui.SPACE_LG) },
        )
        val stateText = stateText(config, status)
        card.addView(
            LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                addView(
                    TextView(context).apply {
                        text = config.triggerId
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_ITEM)
                        typeface = Ui.mediumTypeface
                        setTextColor(appPalette.primaryText)
                    },
                )
                addView(
                    TextView(context).apply {
                        text = listOf(config.alias, config.folder, modeLabel(config.watchMode)).joinToString(" - ")
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
                        setTextColor(appPalette.primaryText)
                        setPaddingRelative(0, uiDp(2), 0, 0)
                    },
                )
                addView(
                    TextView(context).apply {
                        text = stateText
                        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECONDARY)
                        setTextColor(if (status.state == TriggerStatusDocument.STATE_FAILED) getColor(R.color.mail_error) else appPalette.secondaryText)
                        setPaddingRelative(0, uiDp(2), 0, 0)
                    },
                )
            },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
        )
        card.addView(
            MaterialSwitch(this).apply {
                isChecked = config.enabled
                thumbTintList = switchThumbTintList()
                trackTintList = switchTrackTintList()
                contentDescription = getString(R.string.watch_editor_enabled)
                setOnCheckedChangeListener { _, checked -> if (checked != config.enabled) setEnabled(config, checked) }
            },
            LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { marginStart = uiDp(Ui.SPACE_SM) },
        )
        val more = iconButton(R.drawable.ic_more_vert_24, R.string.watches_more_options) { }
        more.setOnClickListener { showWatchMenu(more, config, status) }
        card.addView(more)
        card.contentDescription = listOf(config.triggerId, config.alias, config.folder, stateText).joinToString(", ")
        card.setOnClickListener { startActivity(WatchEditorActivity.intent(this, config.triggerId)) }
        return card
    }

    private fun stateText(config: TriggerConfig, status: TriggerStatusDocument): String {
        val state = when {
            !config.enabled -> getString(R.string.watches_state_disabled)
            status.state == TriggerStatusDocument.STATE_CONNECTED -> getString(R.string.watches_state_connected, modeLabel(status.mode?.let(WatchMode::fromId)))
            status.state == TriggerStatusDocument.STATE_CONNECTING -> getString(R.string.watches_state_connecting)
            status.state == TriggerStatusDocument.STATE_FAILED -> getString(R.string.watches_state_failed, status.lastError?.message ?: status.lastError?.code ?: "")
            else -> getString(R.string.watches_state_stopped)
        }
        val mail = status.lastMailAt?.let { getString(R.string.watches_last_mail, DateUtils.getRelativeTimeSpanString(it, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS)) }
            ?: getString(R.string.watches_no_mail_yet)
        return "$state - $mail"
    }

    private fun modeLabel(mode: WatchMode?): String = getString(
        when (mode) {
            WatchMode.IDLE -> R.string.watches_mode_idle
            WatchMode.POLL -> R.string.watches_mode_poll
            null -> R.string.watches_mode_auto
        },
    )

    // ------------------------------------------------------------------ actions

    private fun showWatchMenu(anchor: View, config: TriggerConfig, status: TriggerStatusDocument) {
        val popup = PopupMenu(this, anchor)
        popup.menu.add(Menu.NONE, ACTION_EDIT, 0, R.string.watches_action_edit)
        popup.menu.add(Menu.NONE, ACTION_RECORDS, 1, R.string.watches_action_records)
        if (status.state == TriggerStatusDocument.STATE_FAILED) popup.menu.add(Menu.NONE, ACTION_RETRY, 2, R.string.watches_action_retry)
        popup.menu.add(Menu.NONE, ACTION_REMOVE, 3, R.string.watches_action_remove)
        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                ACTION_EDIT -> startActivity(WatchEditorActivity.intent(this, config.triggerId))
                ACTION_RECORDS -> showRecords(config)
                ACTION_RETRY -> keeper.retry(config.triggerId)
                ACTION_REMOVE -> confirmRemove(config)
            }
            true
        }
        popup.show()
    }

    private fun setEnabled(config: TriggerConfig, enabled: Boolean) {
        runStoreAction {
            store.setEnabled(config.triggerId, enabled)
            applyWatches()
            if (enabled) requestNotificationPermissionIfMissing()
            render()
        }
    }

    private fun confirmRemove(config: TriggerConfig) {
        confirmDialog(
            getString(R.string.watches_remove_title),
            getString(R.string.watches_remove_message, config.triggerId),
            R.string.watches_action_remove,
            destructive = true,
        ) {
            runStoreAction {
                store.remove(config.triggerId)
                applyWatches()
                showSnackbar(scaffold.root, getString(R.string.watches_removed))
                render()
            }
        }
    }

    private fun showRecords(config: TriggerConfig) {
        val records = try {
            store.records(config.triggerId)
        } catch (e: MailException) {
            showSnackbar(scaffold.root, e.message)
            return
        }
        val format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
        val text = if (records.isEmpty()) getString(R.string.watches_records_empty) else records.joinToString("\n\n") { it.describe(format) }
        messageDialog(getString(R.string.watches_records_title, config.triggerId), text).apply {
            if (records.isNotEmpty()) {
                setButton(android.app.AlertDialog.BUTTON_NEUTRAL, getString(R.string.watches_records_clear)) { _, _ ->
                    runStoreAction {
                        store.clearRecords(config.triggerId)
                        showSnackbar(scaffold.root, getString(R.string.watches_records_cleared))
                    }
                }
            }
        }
    }

    private fun TriggerRecord.describe(format: DateFormat): String {
        val sender = from?.let { address -> address.name?.takeIf { it.isNotBlank() }?.let { "$it <${address.address}>" } ?: address.address }.orEmpty()
        return listOf(format.format(Date(receivedAt)), sender, subject.ifBlank { "-" }).filter { it.isNotEmpty() }.joinToString("\n")
    }

    /** Starts the service when a watch is enabled (it stops itself when none is) or re-syncs in-process when Android refuses the start. */
    private fun applyWatches() {
        if (!MailWatchService.start(this)) keeper.sync()
    }

    private fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun requestNotificationPermissionIfMissing() {
        if (!notificationsAllowed()) requestNotificationPermission()
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || notificationsAllowed()) return
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_NOTIFICATIONS) render()
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
        const val ACTION_EDIT = 10
        const val ACTION_RECORDS = 11
        const val ACTION_RETRY = 12
        const val ACTION_REMOVE = 13
        const val REQUEST_NOTIFICATIONS = 41
    }
}
