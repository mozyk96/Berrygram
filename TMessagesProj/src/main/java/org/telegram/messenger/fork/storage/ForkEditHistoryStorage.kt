package org.telegram.messenger.fork.storage

import org.telegram.SQLite.SQLiteCursor
import org.telegram.SQLite.SQLiteDatabase
import org.telegram.SQLite.SQLiteException
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * SQLite storage for prior versions of edited messages in Berrygram.
 *
 * Stored versions are AES-GCM encrypted with an account bound Keystore key.
 * Databases created by earlier 1.4.0 builds are migrated on first open.
 */
class ForkEditHistoryStorage private constructor(private val currentAccount: Int) {
    private var database: SQLiteDatabase? = null

    class EditRevision(
        @JvmField val editDate: Int,
        @JvmField val text: String
    )

    init {
        openDatabase()
    }

    private fun openDatabase() {
        val filesDir = ApplicationLoader.applicationContext.filesDir
        val dbFile = File(filesDir, "fork_edit_history_$currentAccount.db")
        try {
            database = SQLiteDatabase(dbFile.path)
            database?.executeFast(
                "CREATE TABLE IF NOT EXISTS edit_history (" +
                    "dialog_id INTEGER NOT NULL, " +
                    "message_id INTEGER NOT NULL, " +
                    "edit_date INTEGER NOT NULL, " +
                    "text TEXT NOT NULL DEFAULT '', " +
                    "text_enc BLOB" +
                    ");"
            )?.stepThis()?.dispose()
            database?.executeFast(
                "CREATE INDEX IF NOT EXISTS idx_edit_history ON edit_history(dialog_id, message_id);"
            )?.stepThis()?.dispose()
            migrateLegacyRows()
        } catch (e: SQLiteException) {
            FileLog.e(e)
        }
    }

    private fun migrateLegacyRows() {
        val db = database ?: return
        try {
            var hasColumn = false
            var cursor: SQLiteCursor? = null
            try {
                cursor = db.queryFinalized("PRAGMA table_info(edit_history);")
                while (cursor != null && cursor.next()) {
                    if (cursor.stringValue(1) == "text_enc") {
                        hasColumn = true
                        break
                    }
                }
            } finally {
                cursor?.dispose()
            }
            if (!hasColumn) {
                db.executeFast("ALTER TABLE edit_history ADD COLUMN text_enc BLOB;").stepThis().dispose()
            }
            if (!ForkArchiveCrypto.isAvailable(currentAccount)) {
                return
            }
            val legacy = mutableListOf<Triple<Long, Int, String>>()
            cursor = null
            try {
                cursor = db.queryFinalized(
                    "SELECT dialog_id, message_id, text FROM edit_history WHERE text IS NOT NULL AND text != '';"
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
                        "UPDATE edit_history SET text = '', text_enc = ? WHERE dialog_id = ? AND message_id = ?;"
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

    fun recordPriorVersion(dialogId: Long, messageId: Int, editDate: Int, text: String?) {
        if (text.isNullOrEmpty()) return
        val db = database ?: return
        try {
            // Never store the same version twice, for example when the same edit
            // update is processed more than once.
            if (lastVersionText(dialogId, messageId) == text) {
                return
            }
            val encrypted = ForkArchiveCrypto.encrypt(currentAccount, text)
            val state = db.executeFast(
                "INSERT INTO edit_history (dialog_id, message_id, edit_date, text, text_enc) VALUES (?, ?, ?, ?, ?);"
            )
            state.bindLong(1, dialogId)
            state.bindInteger(2, messageId)
            state.bindInteger(3, editDate)
            if (encrypted != null) {
                state.bindString(4, "")
                state.bindByteBuffer(5, java.nio.ByteBuffer.wrap(encrypted))
            } else {
                state.bindString(4, text)
                state.bindNull(5)
            }
            state.step()
            state.dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    /** Returns the text of the most recently stored version of a message. */
    private fun lastVersionText(dialogId: Long, messageId: Int): String? {
        val db = database ?: return null
        var cursor: SQLiteCursor? = null
        return try {
            cursor = db.queryFinalized(
                "SELECT text, text_enc FROM edit_history WHERE dialog_id = $dialogId AND message_id = $messageId ORDER BY edit_date DESC LIMIT 1;"
            )
            if (cursor != null && cursor.next()) {
                val encrypted = cursor.byteArrayValue(1)
                if (encrypted != null && encrypted.isNotEmpty()) {
                    ForkArchiveCrypto.decrypt(currentAccount, encrypted) ?: cursor.stringValue(0)
                } else {
                    cursor.stringValue(0)
                }
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

    fun getRevisions(dialogId: Long, messageId: Int): List<EditRevision> {
        val list = mutableListOf<EditRevision>()
        val db = database ?: return list
        var cursor: SQLiteCursor? = null
        try {
            cursor = db.queryFinalized(
                "SELECT edit_date, text, text_enc FROM edit_history WHERE dialog_id = $dialogId AND message_id = $messageId ORDER BY edit_date ASC;"
            )
            while (cursor != null && cursor.next()) {
                val date = cursor.intValue(0)
                val encrypted = cursor.byteArrayValue(2)
                val text = if (encrypted != null && encrypted.isNotEmpty()) {
                    ForkArchiveCrypto.decrypt(currentAccount, encrypted) ?: cursor.stringValue(1) ?: ""
                } else {
                    cursor.stringValue(1) ?: ""
                }
                list.add(EditRevision(date, text))
            }
        } catch (e: Exception) {
            FileLog.e(e)
        } finally {
            cursor?.dispose()
        }
        return list
    }

    fun hasEdits(dialogId: Long, messageId: Int): Boolean {
        val db = database ?: return false
        var cursor: SQLiteCursor? = null
        try {
            cursor = db.queryFinalized(
                "SELECT 1 FROM edit_history WHERE dialog_id = $dialogId AND message_id = $messageId LIMIT 1;"
            )
            return cursor != null && cursor.next()
        } catch (e: Exception) {
            FileLog.e(e)
            return false
        } finally {
            cursor?.dispose()
        }
    }

    /** Removes stored versions older than the given timestamp. */
    fun cleanupOlderThan(cutoffDate: Int): Int {
        val db = database ?: return 0
        return try {
            var removed = 0
            var cursor: SQLiteCursor? = null
            try {
                cursor = db.queryFinalized("SELECT COUNT(*) FROM edit_history WHERE edit_date < $cutoffDate;")
                if (cursor != null && cursor.next()) removed = cursor.intValue(0)
            } finally {
                cursor?.dispose()
            }
            db.executeFast("DELETE FROM edit_history WHERE edit_date < $cutoffDate;").stepThis().dispose()
            removed
        } catch (e: Exception) {
            FileLog.e(e)
            0
        }
    }

    fun clear() {
        val db = database ?: return
        try {
            db.executeFast("DELETE FROM edit_history;").stepThis().dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    companion object {
        private val instances = ConcurrentHashMap<Int, ForkEditHistoryStorage>()

        @JvmStatic
        fun getInstance(currentAccount: Int): ForkEditHistoryStorage {
            return instances.getOrPut(currentAccount) {
                ForkEditHistoryStorage(currentAccount)
            }
        }
    }
}
