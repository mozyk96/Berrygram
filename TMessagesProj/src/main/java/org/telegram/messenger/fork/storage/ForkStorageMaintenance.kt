package org.telegram.messenger.fork.storage

import org.telegram.messenger.UserConfig

/**
 * Applies the configured archive retention for every account.
 *
 * Retention is enforced both here and while reading, because background
 * scheduling is not exact and the app may stay in memory for a long time.
 */
object ForkStorageMaintenance {
    private const val CLEANUP_INTERVAL_SECONDS = 6 * 3600L
    private const val STAMP_KEY = "fork_last_cleanup"

    @JvmStatic
    fun runScheduledCleanup() {
        for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
            if (!UserConfig.isValidAccount(account)) {
                continue
            }
            try {
                cleanupAccount(account, force = false)
            } catch (error: Throwable) {
                // Cleanup must never break application start.
            }
        }
    }

    /** Applies retention immediately; returns the number of removed rows. */
    @JvmStatic
    fun cleanupAccount(account: Int, force: Boolean): Int {
        if (!UserConfig.isValidAccount(account)) {
            return 0
        }
        val days = org.telegram.messenger.fork.stealth.ForkStealthSettings.retentionDays(account)
        if (days == org.telegram.messenger.fork.stealth.ForkStealthSettings.RETENTION_FOREVER) {
            return 0
        }
        val now = System.currentTimeMillis() / 1000L
        if (!force) {
            val last = lastCleanup(account)
            if (now - last < CLEANUP_INTERVAL_SECONDS) {
                return 0
            }
        }
        val cutoff = (now - days * 86400L).toInt()
        var removed = 0
        try {
            removed += ForkArchiveManager.getInstance(account).cleanupOlderThan(cutoff)
        } catch (error: Throwable) {
            // keep going
        }
        try {
            removed += ForkEditHistoryStorage.getInstance(account).cleanupOlderThan(cutoff)
        } catch (error: Throwable) {
            // keep going
        }
        try {
            removed += ForkDeletedMessagesStorage.getInstance(account).cleanupOlderThan(cutoff)
        } catch (error: Throwable) {
            // keep going
        }
        setLastCleanup(account, now)
        return removed
    }

    private fun lastCleanup(account: Int): Long {
        return try {
            org.telegram.messenger.ApplicationLoader.applicationContext
                .getSharedPreferences("fork_storage_${account}", android.content.Context.MODE_PRIVATE)
                .getLong(STAMP_KEY, 0L)
        } catch (error: Throwable) {
            0L
        }
    }

    private fun setLastCleanup(account: Int, value: Long) {
        try {
            org.telegram.messenger.ApplicationLoader.applicationContext
                .getSharedPreferences("fork_storage_${account}", android.content.Context.MODE_PRIVATE)
                .edit().putLong(STAMP_KEY, value).apply()
        } catch (error: Throwable) {
            // ignore
        }
    }
}
