package org.telegram.messenger.fork.storage

import org.telegram.SQLite.SQLiteCursor
import org.telegram.SQLite.SQLiteDatabase
import org.telegram.SQLite.SQLiteException
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * SQLite local chat archive manager for Berrygram.
 * Preserves message history and dialogs independently of Telegram deletions.
 * Multi-account separated. Written in Kotlin.
 */
class ForkArchiveManager private constructor(private val currentAccount: Int) {
    private var database: SQLiteDatabase? = null

    class ArchivedMessage(
        @JvmField val dialogId: Long,
        @JvmField val messageId: Int,
        @JvmField val date: Int,
        @JvmField val senderId: Long,
        @JvmField val senderName: String?,
        @JvmField val messageText: String
    )

    class ArchivedDialogSummary(
        @JvmField val dialogId: Long,
        @JvmField val messageCount: Int,
        @JvmField val lastDate: Int,
        @JvmField val lastSnippet: String?
    )

    init {
        openDatabase()
    }

    private fun openDatabase() {
        val filesDir = ApplicationLoader.applicationContext.filesDir
        val dbFile = File(filesDir, "fork_archive_$currentAccount.db")
        try {
            database = SQLiteDatabase(dbFile.path)
            database?.executeFast(
                "CREATE TABLE IF NOT EXISTS archived_messages (" +
                    "dialog_id INTEGER NOT NULL, " +
                    "message_id INTEGER NOT NULL, " +
                    "date INTEGER NOT NULL, " +
                    "sender_id INTEGER NOT NULL, " +
                    "sender_name TEXT, " +
                    "message_text TEXT NOT NULL, " +
                    "PRIMARY KEY (dialog_id, message_id)" +
                    ");"
            )?.stepThis()?.dispose()
            database?.executeFast(
                "CREATE INDEX IF NOT EXISTS idx_archived_dialog ON archived_messages(dialog_id, date DESC);"
            )?.stepThis()?.dispose()
        } catch (e: SQLiteException) {
            FileLog.e(e)
        }
    }

    fun archiveMessage(
        dialogId: Long,
        messageId: Int,
        date: Int,
        senderId: Long,
        senderName: String?,
        messageText: String?
    ) {
        if (messageText.isNullOrEmpty()) return
        val db = database ?: return
        try {
            val state = db.executeFast("INSERT OR REPLACE INTO archived_messages VALUES (?, ?, ?, ?, ?, ?);")
            state.bindLong(1, dialogId)
            state.bindInteger(2, messageId)
            state.bindInteger(3, date)
            state.bindLong(4, senderId)
            if (senderName != null) {
                state.bindString(5, senderName)
            } else {
                state.bindNull(5)
            }
            state.bindString(6, messageText)
            state.step()
            state.dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    fun getArchivedDialogs(): List<ArchivedDialogSummary> {
        val list = mutableListOf<ArchivedDialogSummary>()
        val db = database ?: return list
        var cursor: SQLiteCursor? = null
        try {
            cursor = db.queryFinalized(
                "SELECT dialog_id, COUNT(*), MAX(date), message_text FROM archived_messages GROUP BY dialog_id ORDER BY MAX(date) DESC;"
            )
            while (cursor != null && cursor.next()) {
                val did = cursor.longValue(0)
                val count = cursor.intValue(1)
                val lastDate = cursor.intValue(2)
                val snippet = cursor.stringValue(3)
                list.add(ArchivedDialogSummary(did, count, lastDate, snippet))
            }
        } catch (e: Exception) {
            FileLog.e(e)
        } finally {
            cursor?.dispose()
        }
        return list
    }

    fun getArchivedMessages(dialogId: Long): List<ArchivedMessage> {
        val list = mutableListOf<ArchivedMessage>()
        val db = database ?: return list
        var cursor: SQLiteCursor? = null
        try {
            cursor = db.queryFinalized(
                "SELECT dialog_id, message_id, date, sender_id, sender_name, message_text FROM archived_messages WHERE dialog_id = $dialogId ORDER BY date ASC;"
            )
            while (cursor != null && cursor.next()) {
                val did = cursor.longValue(0)
                val mid = cursor.intValue(1)
                val date = cursor.intValue(2)
                val senderId = cursor.longValue(3)
                val senderName = cursor.stringValue(4)
                val text = cursor.stringValue(5) ?: ""
                list.add(ArchivedMessage(did, mid, date, senderId, senderName, text))
            }
        } catch (e: Exception) {
            FileLog.e(e)
        } finally {
            cursor?.dispose()
        }
        return list
    }

    fun search(query: String?): List<ArchivedMessage> {
        val list = mutableListOf<ArchivedMessage>()
        if (query.isNullOrBlank()) return list
        val db = database ?: return list
        var cursor: SQLiteCursor? = null
        val safeQuery = query.replace("'", "''")
        try {
            cursor = db.queryFinalized(
                "SELECT dialog_id, message_id, date, sender_id, sender_name, message_text FROM archived_messages WHERE message_text LIKE '%$safeQuery%' ORDER BY date DESC LIMIT 50;"
            )
            while (cursor != null && cursor.next()) {
                val did = cursor.longValue(0)
                val mid = cursor.intValue(1)
                val date = cursor.intValue(2)
                val senderId = cursor.longValue(3)
                val senderName = cursor.stringValue(4)
                val text = cursor.stringValue(5) ?: ""
                list.add(ArchivedMessage(did, mid, date, senderId, senderName, text))
            }
        } catch (e: Exception) {
            FileLog.e(e)
        } finally {
            cursor?.dispose()
        }
        return list
    }

    fun clearArchive() {
        val db = database ?: return
        try {
            db.executeFast("DELETE FROM archived_messages;").stepThis().dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    companion object {
        private val instances = ConcurrentHashMap<Int, ForkArchiveManager>()

        @JvmStatic
        fun getInstance(currentAccount: Int): ForkArchiveManager {
            return instances.getOrPut(currentAccount) {
                ForkArchiveManager(currentAccount)
            }
        }
    }
}
