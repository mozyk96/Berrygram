package org.telegram.ui.fork.settings

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import org.telegram.messenger.AndroidUtilities
import org.telegram.ui.Cells.HeaderCell
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Cells.TextDetailSettingsCell
import org.telegram.ui.Cells.TextInfoPrivacyCell
import org.telegram.ui.Cells.TextSettingsCell
import org.telegram.ui.ForkLocale

/**
 * A Berrygram settings block that lives inside a Telegram settings screen.
 *
 * The whole application uses one settings system, so a Berrygram feature is
 * not a separate menu any more: the block is appended to the Telegram screen
 * that owns the feature and reuses the theme aware cells of that screen. The
 * block therefore stays readable in the dark and the light theme, follows the
 * system font scale and keeps the look of the host screen.
 */
class ForkSettingsSection(context: Context) : LinearLayout(context) {

    private val rows = ArrayList<View>()
    private var stateBlock: (() -> Unit)? = null

    init {
        orientation = VERTICAL
        setPadding(0, 0, 0, AndroidUtilities.dp(4f))
    }

    /**
     * Registers a block that reloads the values of the rows. The host screen
     * calls it when the screen becomes visible again, so a value that was
     * changed somewhere else is never shown stale.
     */
    fun refreshState(block: () -> Unit) {
        stateBlock = block
        block()
    }

    /** Reloads the values of the rows. */
    fun refresh() {
        stateBlock?.invoke()
    }

    /** Adds a grey section header, the same style the Telegram screens use. */
    fun addHeader(@StringRes titleRes: Int): HeaderCell {
        rows.clear()
        val header = HeaderCell(context)
        header.setText(ForkLocale.get(titleRes))
        addView(header)
        return header
    }

    /** Adds a switch row. The listener is called with the new state. */
    fun addToggle(@StringRes titleRes: Int, checked: Boolean, iconRes: Int = 0, onChanged: (Boolean) -> Unit): TextCheckCell {
        val cell = TextCheckCell(context)
        cell.setTextAndCheck(ForkLocale.get(titleRes), checked, false)
        cell.setMultilineTitle()
        if (iconRes != 0) {
            cell.setIcon(iconRes)
        }
        cell.setOnClickListener {
            val enabled = !cell.isChecked
            cell.isChecked = enabled
            onChanged(enabled)
        }
        addRowView(cell)
        return cell
    }

    /** Adds a row that opens another screen or shows a value. */
    fun addRow(
        @StringRes titleRes: Int,
        value: CharSequence? = null,
        iconRes: Int = 0,
        onClick: ((View) -> Unit)? = null
    ): TextSettingsCell {
        val cell = TextSettingsCell(context)
        if (value != null) {
            cell.setTextAndValue(ForkLocale.get(titleRes), value, false)
        } else {
            cell.setText(ForkLocale.get(titleRes), false)
        }
        if (iconRes != 0) {
            cell.setIcon(iconRes)
        }
        cell.setMultilineTitle()
        if (onClick != null) {
            cell.setOnClickListener(onClick)
        }
        addRowView(cell)
        return cell
    }

    /** A destination with an explanatory subtitle, rather than a selectable value. */
    fun addDetailRow(@StringRes titleRes: Int, @StringRes descriptionRes: Int, iconRes: Int, onClick: (View) -> Unit): TextDetailSettingsCell {
        val cell = TextDetailSettingsCell(context)
        cell.setMultilineDetail(true)
        cell.setTextAndValueAndIcon(ForkLocale.get(titleRes), ForkLocale.get(descriptionRes), iconRes, false)
        cell.setMultilineTitle()
        cell.setOnClickListener(onClick)
        addRowView(cell)
        return cell
    }

    /** Adds an explanatory text under a group of rows. */
    fun addInfo(@StringRes textRes: Int): TextInfoPrivacyCell {
        rows.clear()
        val info = TextInfoPrivacyCell(context)
        info.setText(ForkLocale.get(textRes))
        addView(info)
        return info
    }

    /** Adds a row that is not a Berrygram string but a plain text. */
    fun addToggleWithText(title: CharSequence, checked: Boolean, onChanged: (Boolean) -> Unit): TextCheckCell {
        val cell = TextCheckCell(context)
        cell.setTextAndCheck(title, checked, false)
        cell.setMultilineTitle()
        cell.setOnClickListener {
            val enabled = !cell.isChecked
            cell.isChecked = enabled
            onChanged(enabled)
        }
        addRowView(cell)
        return cell
    }

    private fun addRowView(view: View) {
        // A hairline divider between the rows of the block, the host screen
        // draws the same line between its own cells.
        if (rows.isNotEmpty()) {
            val divider = View(context)
            divider.setBackgroundColor(org.telegram.ui.ActionBar.Theme.getColor(org.telegram.ui.ActionBar.Theme.key_divider))
            val params = LayoutParams(LayoutParams.MATCH_PARENT, 1)
            params.setMargins(AndroidUtilities.dp(16f), 0, AndroidUtilities.dp(16f), 0)
            addView(divider, params)
        }
        addView(view)
        rows.add(view)
    }
}
