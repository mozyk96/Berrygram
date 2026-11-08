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
 *
 * Message text is stored AES-GCM encrypted with an account bound Android
 * Keystore key. Databases created by earlier 1.4.0 builds are migrated on first
 * open: legacy plaintext rows are encrypted and the plaintext column is
 * cleared, so existing archives keep working without data loss.
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
                    "message_text TEXT NOT NULL DEFAULT '', " +
                    "message_text_enc BLOB, " +
                    "PRIMARY KEY (dialog_id, message_id)" +
                    ");"
            )?.stepThis()?.dispose()
            database?.executeFast(
                "CREATE INDEX IF NOT EXISTS idx_archived_dialog ON archived_messages(dialog_id, date DESC);"
            )?.stepThis()?.dispose()
            migrateLegacyRows()
        } catch (e: SQLiteException) {
            FileLog.e(e)
        }
    }

    /** Adds the encryption column to databases created before 1.4.0 Beta 2. */
    private fun migrateLegacyRows() {
        val db = database ?: return
        try {
            var hasColumn = false
            var cursor: SQLiteCursor? = null
            try {
                cursor = db.queryFinalized("PRAGMA table_info(archived_messages);")
                while (cursor != null && cursor.next()) {
                    if (cursor.stringValue(1) == "message_text_enc") {
                        hasColumn = true
                        break
                    }
                }
            } finally {
                cursor?.dispose()
            }
            if (!hasColumn) {
                db.executeFast("ALTER TABLE archived_messages ADD COLUMN message_text_enc BLOB;")
                    .stepThis().dispose()
            }
            if (!ForkArchiveCrypto.isAvailable(currentAccount)) {
                return
            }
            val legacy = mutableListOf<Triple<Long, Int, String>>()
            cursor = null
            try {
                cursor = db.queryFinalized(
                    "SELECT dialog_id, message_id, message_text FROM archived_messages WHERE message_text IS NOT NULL AND message_text != '';"
                )
                while (cursor != null && cursor.next()) {
                    legacy.add(Triple(cursor.longValue(0), cursor.intValue(1), cursor.stringValue(2) ?: ""))
                }
            } finally {
                cursor?.dispose()
            }
            for ((dialogId, messageId, text) in legacy) {
                val encrypted = ForkArchiveCrypto.encrypt(currentAccount, text) ?: continue
                try {
                    val state = db.executeFast(
                        "UPDATE archived_messages SET message_text = '', message_text_enc = ? WHERE dialog_id = ? AND message_id = ?;"
                    )
                    state.bindByteBuffer(1, java.nio.ByteBuffer.wrap(encrypted))
                    state.bindLong(2, dialogId)
                    state.bindInteger(3, messageId)
                    state.step()
                    state.dispose()
                } catch (error: Exception) {
                    FileLog.e(error)
                }
            }
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    private fun readText(cursor: SQLiteCursor, textIndex: Int, encryptedIndex: Int): String {
        val encrypted = if (encryptedIndex >= 0) cursor.byteArrayValue(encryptedIndex) else null
        if (encrypted != null && encrypted.isNotEmpty()) {
            ForkArchiveCrypto.decrypt(currentAccount, encrypted)?.let { return it }
        }
        return cursor.stringValue(textIndex) ?: ""
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
            val encrypted = ForkArchiveCrypto.encrypt(currentAccount, messageText)
            val state = db.executeFast(
                "INSERT OR REPLACE INTO archived_messages (dialog_id, message_id, date, sender_id, sender_name, message_text, message_text_enc) VALUES (?, ?, ?, ?, ?, ?, ?);"
            )
            state.bindLong(1, dialogId)
            state.bindInteger(2, messageId)
            state.bindInteger(3, date)
            state.bindLong(4, senderId)
            if (senderName != null) {
                state.bindString(5, senderName)
            } else {
                state.bindNull(5)
            }
            if (encrypted != null) {
                state.bindString(6, "")
                state.bindByteBuffer(7, java.nio.ByteBuffer.wrap(encrypted))
            } else {
                state.bindString(6, messageText)
                state.bindNull(7)
            }
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
                "SELECT dialog_id, COUNT(*), MAX(date) FROM archived_messages GROUP BY dialog_id ORDER BY MAX(date) DESC;"
            )
            while (cursor != null && cursor.next()) {
                val did = cursor.longValue(0)
                val count = cursor.intValue(1)
                val lastDate = cursor.intValue(2)
                list.add(ArchivedDialogSummary(did, count, lastDate, lastSnippet(did)))
            }
        } catch (e: Exception) {
            FileLog.e(e)
        } finally {
            cursor?.dispose()
        }
        return list
    }

    private fun lastSnippet(dialogId: Long): String? {
        val db = database ?: return null
        var cursor: SQLiteCursor? = null
        return try {
            cursor = db.queryFinalized(
                "SELECT message_text, message_text_enc FROM archived_messages WHERE dialog_id = $dialogId ORDER BY date DESC LIMIT 1;"
            )
            if (cursor != null && cursor.next()) {
                readText(cursor, 0, 1)
            } else {
                null
            }
        } catch (e: Exception) {
            FileLog.e(e)
            null
        } finally {
            cursor?.dispose()
        }
    }

    fun getArchivedMessages(dialogId: Long): List<ArchivedMessage> {
        val list = mutableListOf<ArchivedMessage>()
        val db = database ?: return list
        var cursor: SQLiteCursor? = null
        try {
            cursor = db.queryFinalized(
                "SELECT dialog_id, message_id, date, sender_id, sender_name, message_text, message_text_enc FROM archived_messages WHERE dialog_id = $dialogId ORDER BY date ASC;"
            )
            while (cursor != null && cursor.next()) {
                val did = cursor.longValue(0)
                val mid = cursor.intValue(1)
                val date = cursor.intValue(2)
                val senderId = cursor.longValue(3)
                val senderName = cursor.stringValue(4)
                val text = readText(cursor, 5, 6)
                list.add(ArchivedMessage(did, mid, date, senderId, senderName, text))
            }
        } catch (e: Exception) {
            FileLog.e(e)
        } finally {
            cursor?.dispose()
        }
        return list
    }

    /**
     * Local search. Encrypted content cannot be matched by SQL LIKE, so the
     * bounded result set is decrypted and filtered in memory.
     */
    fun search(query: String?): List<ArchivedMessage> {
        val list = mutableListOf<ArchivedMessage>()
        if (query.isNullOrBlank()) return list
        val needle = query.trim().lowercase()
        val db = database ?: return list
        var cursor: SQLiteCursor? = null
        try {
            cursor = db.queryFinalized(
                "SELECT dialog_id, message_id, date, sender_id, sender_name, message_text, message_text_enc FROM archived_messages ORDER BY date DESC LIMIT 2000;"
            )
            while (cursor != null && cursor.next()) {
                val did = cursor.longValue(0)
                val mid = cursor.intValue(1)
                val date = cursor.intValue(2)
                val senderId = cursor.longValue(3)
                val senderName = cursor.stringValue(4)
                val text = readText(cursor, 5, 6)
                if (text.lowercase().contains(needle)) {
                    list.add(ArchivedMessage(did, mid, date, senderId, senderName, text))
                    if (list.size >= 50) break
                }
            }
        } catch (e: Exception) {
            FileLog.e(e)
        } finally {
            cursor?.dispose()
        }
        return list
    }

    /** Removes archived rows older than the given timestamp. */
    fun cleanupOlderThan(cutoffDate: Int): Int {
        val db = database ?: return 0
        return try {
            val removed = countMessages("WHERE date < $cutoffDate")
            db.executeFast("DELETE FROM archived_messages WHERE date < $cutoffDate;").stepThis().dispose()
            removed
        } catch (e: Exception) {
            FileLog.e(e)
            0
        }
    }

    fun deleteDialog(dialogId: Long): Int {
        val db = database ?: return 0
        return try {
            val removed = countMessages("WHERE dialog_id = $dialogId")
            db.executeFast("DELETE FROM archived_messages WHERE dialog_id = $dialogId;").stepThis().dispose()
            removed
        } catch (e: Exception) {
            FileLog.e(e)
            0
        }
    }

    private fun countMessages(condition: String): Int {
        val db = database ?: return 0
        var cursor: SQLiteCursor? = null
        return try {
            cursor = db.queryFinalized("SELECT COUNT(*) FROM archived_messages $condition;")
            if (cursor != null && cursor.next()) cursor.intValue(0) else 0
        } catch (e: Exception) {
            FileLog.e(e)
            0
        } finally {
            cursor?.dispose()
        }
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
