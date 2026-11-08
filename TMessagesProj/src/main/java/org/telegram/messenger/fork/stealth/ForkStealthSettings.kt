package org.telegram.messenger.fork.stealth

import android.content.Context
import android.content.SharedPreferences
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.UserConfig

/**
 * Thread-safe preference storage for Berrygram Ghost Mode, stealth reading,
 * message preservation and archive retention options.
 *
 * Preferences are isolated per account (`fork_stealth_<account>_<userId>`).
 * The legacy global file `fork_stealth` created by earlier 1.4.0 builds is read
 * once as a fallback and migrated on the first write, so no setting is lost.
 */
object ForkStealthSettings {
    private const val LEGACY_PREFS_NAME = "fork_stealth"

    private const val KEY_GHOST_MODE = "ghost_mode_enabled"
    private const val KEY_STEALTH_PRIVATE = "stealth_read_private"
    private const val KEY_STEALTH_GROUPS = "stealth_read_groups"
    private const val KEY_DELETED_MESSAGES = "deleted_messages_enabled"
    private const val KEY_EDIT_HISTORY = "edit_history_enabled"
    private const val KEY_SHOW_EDIT_IN_MENU = "show_edit_in_menu"
    private const val KEY_ARCHIVE_ENABLED = "archive_enabled"
    private const val KEY_RETENTION_DAYS = "retention_days"

    /** Retention in days; 0 keeps archived content forever. */
    const val RETENTION_FOREVER = 0
    val RETENTION_OPTIONS = intArrayOf(7, 30, 90, RETENTION_FOREVER)

    private val lock = Any()

    private class Cache(
        val ghostMode: Boolean,
        val stealthPrivate: Boolean,
        val stealthGroups: Boolean,
        val deletedMessages: Boolean,
        val editHistory: Boolean,
        val showEditInMenu: Boolean,
        val archiveEnabled: Boolean,
        val retentionDays: Int
    )

    private val caches = HashMap<Int, Cache>()
    private val legacyMigrated = HashSet<Int>()

    private fun validAccount(account: Int): Boolean = account in 0 until UserConfig.MAX_ACCOUNT_COUNT

    private fun prefsName(account: Int): String {
        val userId = UserConfig.getInstance(account).clientUserId
        return "fork_stealth_${account}_$userId"
    }

    private fun prefs(account: Int): SharedPreferences {
        return ApplicationLoader.applicationContext.getSharedPreferences(prefsName(account), Context.MODE_PRIVATE)
    }

    private fun legacyPrefs(): SharedPreferences {
        return ApplicationLoader.applicationContext.getSharedPreferences(LEGACY_PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun readBoolean(prefs: SharedPreferences, key: String, fallback: Boolean): Boolean {
        return try {
            prefs.getBoolean(key, fallback)
        } catch (corrupt: ClassCastException) {
            prefs.edit().remove(key).apply()
            fallback
        }
    }

    private fun readInt(prefs: SharedPreferences, key: String, fallback: Int): Int {
        return try {
            prefs.getInt(key, fallback)
        } catch (corrupt: ClassCastException) {
            prefs.edit().remove(key).apply()
            fallback
        }
    }

    /** Returns the account cache, migrating legacy values on first use. */
    private fun cache(account: Int): Cache {
        if (!validAccount(account)) {
            return Cache(false, true, false, true, true, true, false, RETENTION_FOREVER)
        }
        synchronized(lock) {
            caches[account]?.let { return it }
            val current = prefs(account)
            val legacy = legacyPrefs()
            val migrate = !legacyMigrated.contains(account) && legacy.all.isEmpty().not()
            fun bool(key: String, fallback: Boolean): Boolean {
                val value = if (current.contains(key)) readBoolean(current, key, fallback) else null
                if (value != null) return value
                if (migrate) return readBoolean(legacy, key, fallback)
                return fallback
            }
            val resolved = Cache(
                ghostMode = bool(KEY_GHOST_MODE, false),
                stealthPrivate = bool(KEY_STEALTH_PRIVATE, true),
                stealthGroups = bool(KEY_STEALTH_GROUPS, false),
                deletedMessages = bool(KEY_DELETED_MESSAGES, true),
                editHistory = bool(KEY_EDIT_HISTORY, true),
                showEditInMenu = bool(KEY_SHOW_EDIT_IN_MENU, true),
                archiveEnabled = bool(KEY_ARCHIVE_ENABLED, false),
                retentionDays = normalizeRetention(
                    if (current.contains(KEY_RETENTION_DAYS)) readInt(current, KEY_RETENTION_DAYS, RETENTION_FOREVER)
                    else if (migrate) readInt(legacy, KEY_RETENTION_DAYS, RETENTION_FOREVER)
                    else RETENTION_FOREVER
                )
            )
            caches[account] = resolved
            if (migrate) {
                legacyMigrated.add(account)
            }
            return resolved
        }
    }

    private fun invalidate(account: Int) {
        synchronized(lock) { caches.remove(account) }
    }

    fun normalizeRetention(days: Int): Int = if (RETENTION_OPTIONS.contains(days)) days else RETENTION_FOREVER

    @JvmStatic
    fun ghostModeEnabled(account: Int): Boolean = cache(account).ghostMode

    @JvmStatic
    fun setGhostModeEnabled(account: Int, enabled: Boolean) {
        prefs(account).edit().putBoolean(KEY_GHOST_MODE, enabled).apply()
        invalidate(account)
    }

    @JvmStatic
    fun stealthReadPrivate(account: Int): Boolean = cache(account).stealthPrivate

    @JvmStatic
    fun setStealthReadPrivate(account: Int, enabled: Boolean) {
        prefs(account).edit().putBoolean(KEY_STEALTH_PRIVATE, enabled).apply()
        invalidate(account)
    }

    @JvmStatic
    fun stealthReadGroups(account: Int): Boolean = cache(account).stealthGroups

    @JvmStatic
    fun setStealthReadGroups(account: Int, enabled: Boolean) {
        prefs(account).edit().putBoolean(KEY_STEALTH_GROUPS, enabled).apply()
        invalidate(account)
    }

    @JvmStatic
    fun deletedMessagesEnabled(account: Int): Boolean = cache(account).deletedMessages

    @JvmStatic
    fun setDeletedMessagesEnabled(account: Int, enabled: Boolean) {
        prefs(account).edit().putBoolean(KEY_DELETED_MESSAGES, enabled).apply()
        invalidate(account)
    }

    @JvmStatic
    fun editHistoryEnabled(account: Int): Boolean = cache(account).editHistory

    @JvmStatic
    fun setEditHistoryEnabled(account: Int, enabled: Boolean) {
        prefs(account).edit().putBoolean(KEY_EDIT_HISTORY, enabled).apply()
        invalidate(account)
    }

    @JvmStatic
    fun showEditInMenu(account: Int): Boolean = cache(account).showEditInMenu

    @JvmStatic
    fun setShowEditInMenu(account: Int, enabled: Boolean) {
        prefs(account).edit().putBoolean(KEY_SHOW_EDIT_IN_MENU, enabled).apply()
        invalidate(account)
    }

    @JvmStatic
    fun archiveEnabled(account: Int): Boolean = cache(account).archiveEnabled

    @JvmStatic
    fun setArchiveEnabled(account: Int, enabled: Boolean) {
        prefs(account).edit().putBoolean(KEY_ARCHIVE_ENABLED, enabled).apply()
        invalidate(account)
    }

    @JvmStatic
    fun retentionDays(account: Int): Int = cache(account).retentionDays

    @JvmStatic
    fun setRetentionDays(account: Int, days: Int) {
        prefs(account).edit().putInt(KEY_RETENTION_DAYS, normalizeRetention(days)).apply()
        invalidate(account)
    }

    @JvmStatic
    fun shouldSuppressRead(dialogId: Long, isGroupOrChannel: Boolean, isSecretChat: Boolean): Boolean {
        return shouldSuppressRead(dialogId, isGroupOrChannel, isSecretChat, selectedAccount())
    }

    @JvmStatic
    fun shouldSuppressRead(dialogId: Long, isGroupOrChannel: Boolean, isSecretChat: Boolean, account: Int): Boolean {
        return shouldSuppressRead(dialogId, isGroupOrChannel, isSecretChat, account, false)
    }

    /** An explicit user "Mark as read" request is never suppressed. */
    @JvmStatic
    fun shouldSuppressRead(dialogId: Long, isGroupOrChannel: Boolean, isSecretChat: Boolean, account: Int, forced: Boolean): Boolean {
        return StealthPolicy.shouldSuppressRead(
            ghostModeEnabled(account),
            stealthReadPrivate(account),
            stealthReadGroups(account),
            isGroupOrChannel,
            isSecretChat,
            forced
        )
    }

    @JvmStatic
    fun reset(account: Int) {
        prefs(account).edit().clear().apply()
        invalidate(account)
    }

    private fun selectedAccount(): Int = try {
        UserConfig.selectedAccount
    } catch (error: Throwable) {
        0
    }
}
