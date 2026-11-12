package org.telegram.messenger.fork.storage

import android.content.Context
import org.telegram.SQLite.SQLiteCursor
import org.telegram.SQLite.SQLiteDatabase
import org.telegram.SQLite.SQLiteException
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * SQLite storage and in-memory cache for deleted messages preserved by Berrygram.
 * Multi-account separated. Written in Kotlin.
 */
class ForkDeletedMessagesStorage private constructor(private val currentAccount: Int) {
    private var database: SQLiteDatabase? = null
    private val deletedCache: MutableMap<String, Boolean> = ConcurrentHashMap()

    init {
        openDatabase()
    }

    private fun openDatabase() {
        val filesDir = ApplicationLoader.applicationContext.filesDir
        val dbFile = File(filesDir, "fork_deleted_messages_$currentAccount.db")
        try {
            database = SQLiteDatabase(dbFile.path)
            database?.executeFast(
                "CREATE TABLE IF NOT EXISTS deleted_messages (" +
                    "dialog_id INTEGER NOT NULL, " +
                    "message_id INTEGER NOT NULL, " +
                    "date INTEGER NOT NULL, " +
                    "PRIMARY KEY (dialog_id, message_id)" +
                    ");"
            )?.stepThis()?.dispose()
        } catch (e: SQLiteException) {
            FileLog.e(e)
        }
    }

    fun recordDeletedMessage(dialogId: Long, messageId: Int, deleteDate: Int) {
        val key = "$dialogId:$messageId"
        deletedCache[key] = true
        val db = database ?: return
        try {
            val state = db.executeFast("INSERT OR REPLACE INTO deleted_messages VALUES (?, ?, ?);")
            state.bindLong(1, dialogId)
            state.bindInteger(2, messageId)
            state.bindInteger(3, deleteDate)
            state.step()
            state.dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    fun isDeleted(dialogId: Long, messageId: Int): Boolean {
        val key = "$dialogId:$messageId"
        val cached = deletedCache[key]
        if (cached != null) {
            return cached
        }
        val db = database ?: return false
        var result = false
        var cursor: SQLiteCursor? = null
        try {
            cursor = db.queryFinalized(
                "SELECT 1 FROM deleted_messages WHERE dialog_id = $dialogId AND message_id = $messageId LIMIT 1;"
            )
            if (cursor != null && cursor.next()) {
                result = true
            }
        } catch (e: Exception) {
            FileLog.e(e)
        } finally {
            cursor?.dispose()
        }
        deletedCache[key] = result
        return result
    }

    fun removeDeletedMessage(dialogId: Long, messageId: Int) {
        val key = "$dialogId:$messageId"
        deletedCache.remove(key)
        val db = database ?: return
        try {
            val state = db.executeFast("DELETE FROM deleted_messages WHERE dialog_id = ? AND message_id = ?;")
            state.bindLong(1, dialogId)
            state.bindInteger(2, messageId)
            state.step()
            state.dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    /** Removes stored markers older than the given timestamp. */
    fun cleanupOlderThan(cutoffDate: Int): Int {
        val db = database ?: return 0
        return try {
            var removed = 0
            var cursor: SQLiteCursor? = null
            try {
                cursor = db.queryFinalized("SELECT COUNT(*) FROM deleted_messages WHERE date < $cutoffDate;")
                if (cursor != null && cursor.next()) removed = cursor.intValue(0)
            } finally {
                cursor?.dispose()
            }
            db.executeFast("DELETE FROM deleted_messages WHERE date < $cutoffDate;").stepThis().dispose()
            deletedCache.clear()
            removed
        } catch (e: Exception) {
            FileLog.e(e)
            0
        }
    }

    fun clear() {
        deletedCache.clear()
        val db = database ?: return
        try {
            db.executeFast("DELETE FROM deleted_messages;").stepThis().dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    companion object {
        private val instances = ConcurrentHashMap<Int, ForkDeletedMessagesStorage>()

        @JvmStatic
        fun getInstance(currentAccount: Int): ForkDeletedMessagesStorage {
            return instances.getOrPut(currentAccount) {
                ForkDeletedMessagesStorage(currentAccount)
            }
        }
    }
}
