package org.telegram.messenger.fork.stealth

import android.content.Context
import android.content.SharedPreferences
import org.telegram.messenger.ApplicationLoader

/**
 * Thread-safe preference storage for Berrygram Ghost Mode, stealth reading,
 * and message preservation options. Written in Kotlin.
 */
object ForkStealthSettings {
    private const val PREFS_NAME = "fork_stealth"

    private const val KEY_GHOST_MODE = "ghost_mode_enabled"
    private const val KEY_STEALTH_PRIVATE = "stealth_read_private"
    private const val KEY_STEALTH_GROUPS = "stealth_read_groups"
    private const val KEY_DELETED_MESSAGES = "deleted_messages_enabled"
    private const val KEY_EDIT_HISTORY = "edit_history_enabled"
    private const val KEY_SHOW_EDIT_IN_MENU = "show_edit_in_menu"
    private const val KEY_ARCHIVE_ENABLED = "archive_enabled"

    @Volatile private var cachedGhostMode: Boolean? = null
    @Volatile private var cachedStealthPrivate: Boolean? = null
    @Volatile private var cachedStealthGroups: Boolean? = null
    @Volatile private var cachedDeletedMessages: Boolean? = null
    @Volatile private var cachedEditHistory: Boolean? = null
    @Volatile private var cachedShowEditInMenu: Boolean? = null
    @Volatile private var cachedArchiveEnabled: Boolean? = null

    private fun prefs(): SharedPreferences {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @JvmStatic
    fun ghostModeEnabled(): Boolean {
        var value = cachedGhostMode
        if (value == null) {
            value = prefs().getBoolean(KEY_GHOST_MODE, false)
            cachedGhostMode = value
        }
        return value
    }

    @JvmStatic
    @Synchronized
    fun setGhostModeEnabled(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_GHOST_MODE, enabled).apply()
        cachedGhostMode = enabled
    }

    @JvmStatic
    fun stealthReadPrivate(): Boolean {
        var value = cachedStealthPrivate
        if (value == null) {
            value = prefs().getBoolean(KEY_STEALTH_PRIVATE, true)
            cachedStealthPrivate = value
        }
        return value
    }

    @JvmStatic
    @Synchronized
    fun setStealthReadPrivate(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_STEALTH_PRIVATE, enabled).apply()
        cachedStealthPrivate = enabled
    }

    @JvmStatic
    fun stealthReadGroups(): Boolean {
        var value = cachedStealthGroups
        if (value == null) {
            value = prefs().getBoolean(KEY_STEALTH_GROUPS, false)
            cachedStealthGroups = value
        }
        return value
    }

    @JvmStatic
    @Synchronized
    fun setStealthReadGroups(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_STEALTH_GROUPS, enabled).apply()
        cachedStealthGroups = enabled
    }

    @JvmStatic
    fun deletedMessagesEnabled(): Boolean {
        var value = cachedDeletedMessages
        if (value == null) {
            value = prefs().getBoolean(KEY_DELETED_MESSAGES, true)
            cachedDeletedMessages = value
        }
        return value
    }

    @JvmStatic
    @Synchronized
    fun setDeletedMessagesEnabled(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_DELETED_MESSAGES, enabled).apply()
        cachedDeletedMessages = enabled
    }

    @JvmStatic
    fun editHistoryEnabled(): Boolean {
        var value = cachedEditHistory
        if (value == null) {
            value = prefs().getBoolean(KEY_EDIT_HISTORY, true)
            cachedEditHistory = value
        }
        return value
    }

    @JvmStatic
    @Synchronized
    fun setEditHistoryEnabled(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_EDIT_HISTORY, enabled).apply()
        cachedEditHistory = enabled
    }

    @JvmStatic
    fun showEditInMenu(): Boolean {
        var value = cachedShowEditInMenu
        if (value == null) {
            value = prefs().getBoolean(KEY_SHOW_EDIT_IN_MENU, true)
            cachedShowEditInMenu = value
        }
        return value
    }

    @JvmStatic
    @Synchronized
    fun setShowEditInMenu(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_SHOW_EDIT_IN_MENU, enabled).apply()
        cachedShowEditInMenu = enabled
    }

    @JvmStatic
    fun archiveEnabled(): Boolean {
        var value = cachedArchiveEnabled
        if (value == null) {
            value = prefs().getBoolean(KEY_ARCHIVE_ENABLED, false)
            cachedArchiveEnabled = value
        }
        return value
    }

    @JvmStatic
    @Synchronized
    fun setArchiveEnabled(enabled: Boolean) {
        prefs().edit().putBoolean(KEY_ARCHIVE_ENABLED, enabled).apply()
        cachedArchiveEnabled = enabled
    }

    @JvmStatic
    fun shouldSuppressRead(dialogId: Long, isGroupOrChannel: Boolean, isSecretChat: Boolean): Boolean {
        return StealthPolicy.shouldSuppressRead(
            ghostModeEnabled(),
            stealthReadPrivate(),
            stealthReadGroups(),
            isGroupOrChannel,
            isSecretChat
        )
    }

    @JvmStatic
    @Synchronized
    fun reset() {
        prefs().edit().clear().apply()
        cachedGhostMode = null
        cachedStealthPrivate = null
        cachedStealthGroups = null
        cachedDeletedMessages = null
        cachedEditHistory = null
        cachedShowEditInMenu = null
        cachedArchiveEnabled = null
    }
}
