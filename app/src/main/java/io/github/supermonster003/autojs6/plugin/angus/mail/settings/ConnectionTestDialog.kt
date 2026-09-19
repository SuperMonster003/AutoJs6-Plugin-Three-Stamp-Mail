package io.github.supermonster003.autojs6.plugin.angus.mail.settings

import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.R
import io.github.supermonster003.autojs6.plugin.angus.mail.core.account.MailAccount
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.EndpointReport
import io.github.supermonster003.autojs6.plugin.angus.mail.core.json.SessionTestResult
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.Ui
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.messageDialog
import io.github.supermonster003.autojs6.plugin.angus.mail.ui.uiDp

/**
 * Presents the outcome of a [ConnectionTester] probe: one row per endpoint with the host, the TLS
 * mode, the round-trip time and, on failure, the error code and message of the mail core. Nothing
 * here is persisted, and the result document never contains the secret.
 */
internal fun ConfiguredActivity.showConnectionTestResult(account: MailAccount, outcome: ConnectionTester.Outcome) {
    when (outcome) {
        is ConnectionTester.Outcome.Failed -> messageDialog(
            getString(R.string.editor_test_title),
            getString(R.string.editor_test_failed_message, outcome.error.code, outcome.error.message),
        )
        is ConnectionTester.Outcome.Done -> {
            val result = outcome.result
            val scroll = ScrollView(this).apply {
                addView(
                    resultColumn(account, result),
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT),
                )
            }
            messageDialog(getString(R.string.editor_test_title), null, scroll)
        }
    }
}

private fun ConfiguredActivity.resultColumn(account: MailAccount, result: SessionTestResult): LinearLayout =
    LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPaddingRelative(uiDp(Ui.SPACE_XXL), uiDp(Ui.SPACE_SM), uiDp(Ui.SPACE_XXL), uiDp(Ui.SPACE_SM))
        addView(
            TextView(context).apply {
                text = getString(
                    if (result.ok) R.string.editor_test_ok else R.string.editor_test_failed,
                    result.elapsedMs,
                )
                setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
                typeface = Ui.mediumTypeface
                setTextColor(getColor(if (result.ok) R.color.mail_success else R.color.mail_error))
                setPaddingRelative(0, 0, 0, uiDp(Ui.SPACE_SM))
            },
        )
        listOfNotNull(result.imap, result.pop3, result.smtp).forEach { report ->
            addView(
                endpointRow(report),
                LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    topMargin = uiDp(Ui.SPACE_MD)
                },
            )
        }
        if (account.insecure) {
            addView(
                TextView(context).apply {
                    text = getString(R.string.editor_test_insecure)
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECONDARY)
                    setTextColor(getColor(R.color.mail_error))
                    setLineSpacing(0f, Ui.LINE_SPACING_BODY)
                    setPaddingRelative(0, uiDp(Ui.SPACE_LG), 0, 0)
                },
            )
        }
    }

private fun ConfiguredActivity.endpointRow(report: EndpointReport): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.HORIZONTAL
    gravity = Gravity.TOP
    addView(
        ImageView(context).apply {
            val icon = if (report.ok) R.drawable.ic_check_circle_24 else R.drawable.ic_warning_24
            val tint = getColor(if (report.ok) R.color.mail_success else R.color.mail_error)
            setImageDrawable(tintedDrawable(icon, tint))
            contentDescription = getString(if (report.ok) R.string.editor_test_endpoint_ok else R.string.editor_test_endpoint_failed)
        },
        LinearLayout.LayoutParams(uiDp(Ui.ICON_SIZE), uiDp(Ui.ICON_SIZE)).apply {
            marginEnd = uiDp(Ui.SPACE_MD)
            topMargin = uiDp(1)
        },
    )
    addView(
        LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(
                TextView(context).apply {
                    text = report.protocol.uppercase() + "  " + report.host + ":" + report.port + "/" + report.tls
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
                    setTextColor(appPalette.primaryText)
                },
            )
            addView(
                TextView(context).apply {
                    text = if (report.ok) {
                        getString(R.string.editor_test_endpoint_detail, report.elapsedMs, report.capabilities.size)
                    } else {
                        val error = report.error
                        getString(R.string.editor_test_elapsed, report.elapsedMs) + "  " +
                            (error?.let { it.code + " - " + it.message } ?: "")
                    }
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECONDARY)
                    setTextColor(appPalette.secondaryText)
                    setLineSpacing(0f, 1.1f)
                    setPaddingRelative(0, uiDp(2), 0, 0)
                    visibility = View.VISIBLE
                },
            )
        },
        LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f),
    )
}
