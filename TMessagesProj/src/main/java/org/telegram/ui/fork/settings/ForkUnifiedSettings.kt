package org.telegram.ui.fork.settings

import android.content.Context
import org.telegram.messenger.R
import org.telegram.ui.Components.UItem
import org.telegram.ui.ForkLocale
import org.telegram.ui.SettingsActivity

/**
 * Builds the Berrygram rows of the unified Telegram settings screen.
 *
 * Berrygram uses one settings system. Every feature is part of the Telegram
 * screen that owns it, so the root screen only keeps the Update Center, which
 * has no other home. The mapping of the features to their screens lives in
 * [ForkUnifiedSettingsMapping] and is verified by a regression test.
 */
object ForkUnifiedSettings {
    private const val COLOR_TOP = 0xFF3AA0E8.toInt()
    private const val COLOR_BOTTOM = 0xFF2B7CB8.toInt()

    /** The Update Center row of the help section. */
    @JvmStatic
    fun buildUpdateEntry(context: Context): UItem {
        return SettingsActivity.SettingCell.Factory.of(
            ForkUnifiedSettingsMapping.ID_UPDATES,
            COLOR_TOP,
            COLOR_BOTTOM,
            R.drawable.msg_download,
            ForkLocale.get(R.string.ForkUnifiedUpdateTitle),
            ForkLocale.get(R.string.ForkUnifiedUpdateInfo)
        )
    }

    private fun resourceId(name: String): Int {
        return try {
            val field = R.string::class.java.getField(name)
            field.getInt(null)
        } catch (error: Throwable) {
            R.string.ForkSettingsTitle
        }
    }
}
