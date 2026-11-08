package org.telegram.ui.fork.settings

import android.content.Context
import org.telegram.messenger.R
import org.telegram.ui.Components.UItem
import org.telegram.ui.ForkLocale
import org.telegram.ui.SettingsActivity
import java.util.ArrayList

/**
 * Builds the Berrygram entries of the unified Telegram settings screen.
 *
 * The entries are placed directly after the Telegram row they belong to, so
 * privacy, appearance, language, notification, storage and performance
 * controls are reachable from the section the user expects them in.
 */
object ForkUnifiedSettings {
    private const val COLOR_TOP = 0xFF3AA0E8.toInt()
    private const val COLOR_BOTTOM = 0xFF2B7CB8.toInt()

    /** Inserts the entries after the given Telegram settings row. */
    @JvmStatic
    fun insertAfter(items: ArrayList<UItem>, anchorId: Int, entries: List<UItem>) {
        if (entries.isEmpty()) return
        var index = -1
        for (a in items.indices) {
            if (items[a].id == anchorId) {
                index = a
                break
            }
        }
        if (index < 0) return
        items.addAll(index + 1, entries)
    }

    /** Creates the Berrygram rows of the unified settings screen. */
    @JvmStatic
    fun buildEntries(context: Context): List<UItem> {
        val result = ArrayList<UItem>()
        for (entry in ForkUnifiedSettingsMapping.entries()) {
            val title = ForkLocale.get(resourceId(entry.titleResource))
            val subtitle = ForkLocale.get(resourceId(entry.subtitleResource))
            result.add(SettingsActivity.SettingCell.Factory.of(
                entry.id,
                COLOR_TOP,
                COLOR_BOTTOM,
                iconFor(entry.id),
                title,
                subtitle
            ))
        }
        return result
    }

    /**
     * Places every Berrygram entry directly after the Telegram row it belongs
     * to, so privacy, appearance, language, notification, storage and
     * performance controls are found in the expected section.
     */
    @JvmStatic
    fun applyTo(items: ArrayList<UItem>, context: Context) {
        for (entry in ForkUnifiedSettingsMapping.entries()) {
            val title = ForkLocale.get(resourceId(entry.titleResource))
            val subtitle = ForkLocale.get(resourceId(entry.subtitleResource))
            val item = SettingsActivity.SettingCell.Factory.of(
                entry.id,
                COLOR_TOP,
                COLOR_BOTTOM,
                iconFor(entry.id),
                title,
                subtitle
            )
            insertAfter(items, entry.anchorId, listOf(item))
        }
    }

    private fun iconFor(id: Int): Int = when (id) {
        ForkUnifiedSettingsMapping.ID_PRIVACY -> R.drawable.settings_privacy
        ForkUnifiedSettingsMapping.ID_EFFECTS -> R.drawable.msg_palette
        ForkUnifiedSettingsMapping.ID_MEDIA -> R.drawable.settings_data
        ForkUnifiedSettingsMapping.ID_DIAGNOSTICS -> R.drawable.settings_sounds
        ForkUnifiedSettingsMapping.ID_TRANSLATION -> R.drawable.msg_translate
        ForkUnifiedSettingsMapping.ID_ARCHIVE -> R.drawable.msg_archive
        ForkUnifiedSettingsMapping.ID_PERFORMANCE -> R.drawable.msg_speed
        else -> R.drawable.msg_settings
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
