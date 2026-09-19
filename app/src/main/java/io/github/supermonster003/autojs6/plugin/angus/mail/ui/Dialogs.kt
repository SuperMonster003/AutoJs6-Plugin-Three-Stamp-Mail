package io.github.supermonster003.autojs6.plugin.angus.mail.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.text.InputFilter
import android.text.InputType
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AbsListView
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import io.github.supermonster003.autojs6.plugin.angus.mail.ConfiguredActivity
import io.github.supermonster003.autojs6.plugin.angus.mail.R

/** Base builder used by every dialog in the app. */
internal fun ConfiguredActivity.materialDialog(): MaterialAlertDialogBuilder =
    MaterialAlertDialogBuilder(this)

/** Confirmation dialog; destructive actions color the positive button with the error tone. */
internal fun ConfiguredActivity.confirmDialog(
    title: CharSequence,
    message: CharSequence?,
    @StringRes positiveResource: Int,
    destructive: Boolean = false,
    onPositive: () -> Unit,
): AlertDialog = materialDialog()
    .setTitle(title)
    .setMessage(message)
    .setNegativeButton(android.R.string.cancel, null)
    .setPositiveButton(positiveResource) { _, _ -> onPositive() }
    .show()
    .also { dialog ->
        presentedDialog = dialog
        tintDialogButtons(dialog)
        if (destructive) {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setTextColor(getColor(R.color.mail_error))
        }
    }

/** Plain message dialog with a single dismiss button. */
internal fun ConfiguredActivity.messageDialog(
    title: CharSequence,
    message: CharSequence?,
    view: View? = null,
): AlertDialog = materialDialog()
    .setTitle(title)
    .apply { if (message != null) setMessage(message) }
    .apply { if (view != null) setView(view) }
    .setPositiveButton(android.R.string.ok, null)
    .show()
    .also { dialog ->
        presentedDialog = dialog
        tintDialogButtons(dialog)
    }

/**
 * Text-entry dialog on an outlined field. `validate` returns an error message to keep the dialog
 * open, or null to accept; only accepted values reach `onSubmit`.
 */
internal fun ConfiguredActivity.inputDialog(
    title: CharSequence,
    initialValue: CharSequence,
    message: CharSequence? = null,
    hint: CharSequence? = null,
    inputType: Int = InputType.TYPE_CLASS_TEXT,
    maxLength: Int? = null,
    @StringRes positiveResource: Int = android.R.string.ok,
    validate: (String) -> CharSequence? = { null },
    onSubmit: (String) -> Unit,
) {
    val (inputLayout, editText) = formTextField(initialValue, hint, inputType, maxLength)
    val container = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPaddingRelative(uiDp(Ui.SPACE_XXL), uiDp(Ui.SPACE_SM), uiDp(Ui.SPACE_XXL), 0)
        addView(
            inputLayout,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
            ),
        )
    }
    val dialog = materialDialog()
        .setTitle(title)
        .apply { if (message != null) setMessage(message) }
        .setView(container)
        .setNegativeButton(android.R.string.cancel, null)
        .setPositiveButton(positiveResource, null)
        .create()
    dialog.setOnShowListener {
        tintDialogButtons(dialog)
        dialog.getButton(AlertDialog.BUTTON_POSITIVE)?.setOnClickListener {
            val text = editText.text?.toString().orEmpty()
            val error = validate(text)
            if (error != null) {
                inputLayout.error = error
            } else {
                dialog.dismiss()
                onSubmit(text)
            }
        }
        editText.requestFocus()
    }
    dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    dialog.show()
    presentedDialog = dialog
}

/**
 * Single-choice list adapter matching the app palette, with support for multi-line labels and
 * per-item enablement.
 */
internal class PaletteChoiceAdapter(
    private val activity: ConfiguredActivity,
    labels: List<CharSequence>,
    private val enabledAt: (Int) -> Boolean = { true },
) : ArrayAdapter<CharSequence>(
    activity,
    android.R.layout.simple_list_item_single_choice,
    labels,
) {
    override fun areAllItemsEnabled(): Boolean = false

    override fun isEnabled(position: Int): Boolean = enabledAt(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent) as CheckedTextView
        view.layoutParams = AbsListView.LayoutParams(
            AbsListView.LayoutParams.MATCH_PARENT,
            AbsListView.LayoutParams.WRAP_CONTENT,
        )
        view.minHeight = activity.uiDp(52)
        view.isSingleLine = false
        view.maxLines = Int.MAX_VALUE
        view.ellipsize = null
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
        view.setLineSpacing(0f, 1.08f)
        view.setPaddingRelative(
            activity.uiDp(Ui.SPACE_XXL),
            activity.uiDp(Ui.SPACE_MD),
            activity.uiDp(Ui.SPACE_XXL),
            activity.uiDp(Ui.SPACE_MD),
        )
        view.setTextColor(activity.appPalette.primaryText)
        view.checkMarkTintList = activity.controlTintList()
        view.alpha = if (isEnabled(position)) 1f else Ui.DISABLED_ALPHA
        return view
    }
}

internal fun ConfiguredActivity.singleChoiceDialog(
    title: CharSequence,
    labels: List<CharSequence>,
    checkedIndex: Int,
    enabledAt: (Int) -> Boolean = { true },
    onSelect: (Int) -> Unit,
) {
    val dialog = materialDialog()
        .setTitle(title)
        .setSingleChoiceItems(
            PaletteChoiceAdapter(this, labels, enabledAt),
            checkedIndex,
        ) { dialog, index ->
            dialog.dismiss()
            onSelect(index)
        }
        .setNegativeButton(android.R.string.cancel, null)
        .show()
    presentedDialog = dialog
    tintDialogButtons(dialog)
}

/** Themed form field label used above a group of fields. */
internal fun ConfiguredActivity.formLabel(@StringRes textResource: Int): TextView =
    TextView(this).apply {
        text = getString(textResource)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_SECTION)
        typeface = Ui.mediumTypeface
        setTextColor(appPalette.secondaryText)
        setPaddingRelative(0, uiDp(Ui.SPACE_XL), 0, uiDp(Ui.SPACE_SM))
    }

/** Outlined text field pair for forms; the layout carries the hint, helper and error texts. */
internal fun ConfiguredActivity.formTextField(
    initialValue: CharSequence?,
    hint: CharSequence? = null,
    inputType: Int = InputType.TYPE_CLASS_TEXT,
    maxLength: Int? = null,
    singleLine: Boolean = true,
): Pair<TextInputLayout, TextInputEditText> {
    val editText = TextInputEditText(this).apply {
        setText(initialValue)
        this.inputType = inputType
        isSingleLine = singleLine
        setTextSize(TypedValue.COMPLEX_UNIT_SP, Ui.TEXT_BODY)
        minimumHeight = uiDp(56)
        setPaddingRelative(
            uiDp(Ui.SPACE_LG),
            uiDp(Ui.SPACE_MD),
            uiDp(Ui.SPACE_LG),
            uiDp(Ui.SPACE_MD),
        )
        maxLength?.let { filters = arrayOf(InputFilter.LengthFilter(it)) }
        setTextColor(appPalette.primaryText)
        tintEditText(this)
        // Drop the platform underline before TextInputLayout installs the outlined box.
        backgroundTintList = null
        background = null
    }
    val layout = TextInputLayout(this).apply {
        boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        boxBackgroundColor = Color.TRANSPARENT
        // Programmatic TextInputLayouts default to a filled box with zero stroke widths.
        setBoxStrokeWidth(uiDp(1))
        setBoxStrokeWidthFocused(uiDp(2))
        val radius = uiDpF(Ui.RADIUS_CONTROL.toFloat())
        setBoxCornerRadii(radius, radius, radius, radius)
        setBoxStrokeColorStateList(
            ColorStateList(
                arrayOf(intArrayOf(android.R.attr.state_focused), intArrayOf()),
                intArrayOf(appPalette.accent, appPalette.outline),
            ),
        )
        val errorColor = ColorStateList.valueOf(getColor(R.color.mail_error))
        boxStrokeErrorColor = errorColor
        setErrorTextColor(errorColor)
        setHelperTextColor(ColorStateList.valueOf(appPalette.secondaryText))
        defaultHintTextColor = ColorStateList.valueOf(appPalette.secondaryText)
        hintTextColor = ColorStateList.valueOf(appPalette.accent)
        setEndIconTintList(ColorStateList.valueOf(appPalette.secondaryText))
        if (hint != null) this.hint = hint
        addView(editText)
    }
    // The box drawable is now this field's background; a leftover tint would paint over it.
    editText.backgroundTintList = null
    return layout to editText
}
