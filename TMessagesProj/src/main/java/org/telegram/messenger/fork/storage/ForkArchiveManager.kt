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
        @JvmField val messageText: String,
        @JvmField val deleted: Boolean = false
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
                    "deleted INTEGER NOT NULL DEFAULT 0, " +
                    "PRIMARY KEY (dialog_id, message_id)" +
                    ");"
            )?.stepThis()?.dispose()
            database?.executeFast(
                "CREATE INDEX IF NOT EXISTS idx_archived_dialog ON archived_messages(dialog_id, date DESC);"
            )?.stepThis()?.dispose()
            database?.executeFast(
                "CREATE TABLE IF NOT EXISTS archive_import_state (" +
                    "dialog_id INTEGER PRIMARY KEY, " +
                    "backfill_id INTEGER NOT NULL DEFAULT 0, " +
                    "update_id INTEGER NOT NULL DEFAULT 0, " +
                    "updated_at INTEGER NOT NULL DEFAULT 0" +
                    ");"
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
            var hasDeletedColumn = false
            cursor = null
            try {
                cursor = db.queryFinalized("PRAGMA table_info(archived_messages);")
                while (cursor != null && cursor.next()) {
                    if (cursor.stringValue(1) == "deleted") {
                        hasDeletedColumn = true
                        break
                    }
                }
            } finally {
                cursor?.dispose()
            }
            if (!hasDeletedColumn) {
                db.executeFast("ALTER TABLE archived_messages ADD COLUMN deleted INTEGER NOT NULL DEFAULT 0;")
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

    /**
     * Stores a message locally. The deletion flag of an already stored row is
     * never cleared by a later capture, so a deleted message stays a deleted
     * message even when the same message is seen again.
     */
    @JvmOverloads
    fun archiveMessage(
        dialogId: Long,
        messageId: Int,
        date: Int,
        senderId: Long,
        senderName: String?,
        messageText: String?,
        deleted: Boolean = false
    ) {
        if (messageText.isNullOrEmpty()) return
        val db = database ?: return
        try {
            val encrypted = ForkArchiveCrypto.encrypt(currentAccount, messageText)
            val state = db.executeFast(
                "INSERT OR IGNORE INTO archived_messages (dialog_id, message_id, date, sender_id, sender_name, message_text, message_text_enc, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?);"
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
            state.bindInteger(8, if (deleted) 1 else 0)
            state.step()
            state.dispose()
            val update = db.executeFast(
                "UPDATE archived_messages SET date = ?, sender_id = ?, sender_name = ?, message_text = ?, message_text_enc = ?," +
                    " deleted = CASE WHEN deleted = 1 THEN 1 ELSE ? END WHERE dialog_id = ? AND message_id = ?;"
            )
            update.bindInteger(1, date)
            update.bindLong(2, senderId)
            if (senderName != null) {
                update.bindString(3, senderName)
            } else {
                update.bindNull(3)
            }
            if (encrypted != null) {
                update.bindString(4, "")
                update.bindByteBuffer(5, java.nio.ByteBuffer.wrap(encrypted))
            } else {
                update.bindString(4, messageText)
                update.bindNull(5)
            }
            update.bindInteger(6, if (deleted) 1 else 0)
            update.bindLong(7, dialogId)
            update.bindInteger(8, messageId)
            update.step()
            update.dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    /** Marks already archived messages of one chat as deleted. */
    fun markDeleted(dialogId: Long, messageIds: List<Int>): Int {
        val chunks = sqlIdChunks(messageIds)
        if (chunks.isEmpty()) return 0
        val db = database ?: return 0
        return try {
            for (chunk in chunks) {
                val state = db.executeFast(
                    "UPDATE archived_messages SET deleted = 1 WHERE dialog_id = $dialogId AND message_id IN ($chunk);"
                )
                state.step()
                state.dispose()
            }
            messageIds.size
        } catch (e: Exception) {
            FileLog.e(e)
            0
        }
    }

    /**
     * Splits an id list into SQL safe IN (...) chunks. Telegram deletion
     * updates can carry thousands of ids, and one giant expression would hit
     * the SQLite expression limits.
     */
    private fun sqlIdChunks(messageIds: List<Int>): List<String> {
        val valid = messageIds.filter { it > 0 }
        if (valid.isEmpty()) return emptyList()
        return valid.chunked(MAX_SQL_IDS) { chunk -> chunk.joinToString(",") }
    }

    /**
     * Marks messages as deleted for updates that only carry message ids.
     * Common chat message ids are account wide unique, so the affected chats
     * are resolved from the archive itself.
     */
    fun markDeletedByIds(messageIds: List<Int>): List<Long> {
        val chunks = sqlIdChunks(messageIds)
        if (chunks.isEmpty()) return emptyList()
        val db = database ?: return emptyList()
        val dialogs = LinkedHashSet<Long>()
        return try {
            for (chunk in chunks) {
                var cursor: SQLiteCursor? = null
                try {
                    cursor = db.queryFinalized("SELECT DISTINCT dialog_id FROM archived_messages WHERE message_id IN ($chunk);")
                    while (cursor != null && cursor.next()) {
                        dialogs.add(cursor.longValue(0))
                    }
                } finally {
                    cursor?.dispose()
                }
            }
            if (dialogs.isNotEmpty()) {
                for (chunk in chunks) {
                    val state = db.executeFast(
                        "UPDATE archived_messages SET deleted = 1 WHERE message_id IN ($chunk);"
                    )
                    state.step()
                    state.dispose()
                }
            }
            dialogs.toList()
        } catch (e: Exception) {
            FileLog.e(e)
            emptyList()
        }
    }

    /** Returns locally preserved messages that were deleted in Telegram. */
    fun getDeletedMessages(dialogId: Long, limit: Int): List<ArchivedMessage> {
        val list = mutableListOf<ArchivedMessage>()
        val db = database ?: return list
        var cursor: SQLiteCursor? = null
        try {
            cursor = db.queryFinalized(
                "SELECT dialog_id, message_id, date, sender_id, sender_name, message_text, message_text_enc, deleted" +
                    " FROM archived_messages WHERE dialog_id = $dialogId AND deleted = 1 ORDER BY date ASC LIMIT $limit;"
            )
            while (cursor != null && cursor.next()) {
                list.add(readEntry(cursor))
            }
        } catch (e: Exception) {
            FileLog.e(e)
        } finally {
            cursor?.dispose()
        }
        return list
    }

    private fun readEntry(cursor: SQLiteCursor): ArchivedMessage {
        return ArchivedMessage(
            cursor.longValue(0),
            cursor.intValue(1),
            cursor.intValue(2),
            cursor.longValue(3),
            cursor.stringValue(4),
            readText(cursor, 5, 6),
            cursor.intValue(7) != 0
        )
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
                "SELECT dialog_id, message_id, date, sender_id, sender_name, message_text, message_text_enc, deleted FROM archived_messages WHERE dialog_id = $dialogId ORDER BY date ASC;"
            )
            while (cursor != null && cursor.next()) {
                list.add(readEntry(cursor))
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
                "SELECT dialog_id, message_id, date, sender_id, sender_name, message_text, message_text_enc, deleted FROM archived_messages ORDER BY date DESC LIMIT 2000;"
            )
            while (cursor != null && cursor.next()) {
                val entry = readEntry(cursor)
                if (entry.messageText.lowercase().contains(needle)) {
                    list.add(entry)
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
            // The import cursors of a removed chat must not survive, otherwise a
            // later re-import would skip the range that was just deleted.
            db.executeFast("DELETE FROM archive_import_state WHERE dialog_id = $dialogId;").stepThis().dispose()
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

    /** True when a message is already stored for the chat. */
    fun exists(dialogId: Long, messageId: Int): Boolean {
        val db = database ?: return false
        var cursor: SQLiteCursor? = null
        return try {
            cursor = db.queryFinalized(
                "SELECT 1 FROM archived_messages WHERE dialog_id = $dialogId AND message_id = $messageId LIMIT 1;"
            )
            cursor != null && cursor.next()
        } catch (e: Exception) {
            FileLog.e(e)
            false
        } finally {
            cursor?.dispose()
        }
    }

    /**
     * Persistent import cursors of one chat.
     *
     * @param backfillId oldest message id that was imported, 0 when nothing was imported
     * @param updateId   newest message id that was imported, 0 when nothing was imported
     */
    class ImportState(
        @JvmField val dialogId: Long,
        @JvmField val backfillId: Int,
        @JvmField val updateId: Int,
        @JvmField val updatedAt: Long
    )

    fun importState(dialogId: Long): ImportState? {
        val db = database ?: return null
        var cursor: SQLiteCursor? = null
        return try {
            cursor = db.queryFinalized(
                "SELECT backfill_id, update_id, updated_at FROM archive_import_state WHERE dialog_id = $dialogId;"
            )
            if (cursor != null && cursor.next()) {
                ImportState(dialogId, cursor.intValue(0), cursor.intValue(1), cursor.longValue(2))
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

    fun saveImportState(state: ImportState) {
        val db = database ?: return
        try {
            val statement = db.executeFast(
                "INSERT OR REPLACE INTO archive_import_state (dialog_id, backfill_id, update_id, updated_at) VALUES (?, ?, ?, ?);"
            )
            statement.bindLong(1, state.dialogId)
            statement.bindInteger(2, state.backfillId)
            statement.bindInteger(3, state.updateId)
            statement.bindLong(4, state.updatedAt)
            statement.step()
            statement.dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    /** The newest archived message id of a chat, 0 when the chat is empty. */
    fun newestArchivedMessageId(dialogId: Long): Int {
        val db = database ?: return 0
        var cursor: SQLiteCursor? = null
        return try {
            cursor = db.queryFinalized(
                "SELECT MAX(message_id) FROM archived_messages WHERE dialog_id = $dialogId;"
            )
            if (cursor != null && cursor.next()) cursor.intValue(0) else 0
        } catch (e: Exception) {
            FileLog.e(e)
            0
        } finally {
            cursor?.dispose()
        }
    }

    /** The oldest archived message id of a chat, 0 when the chat is empty. */
    fun oldestArchivedMessageId(dialogId: Long): Int {
        val db = database ?: return 0
        var cursor: SQLiteCursor? = null
        return try {
            cursor = db.queryFinalized(
                "SELECT MIN(message_id) FROM archived_messages WHERE dialog_id = $dialogId;"
            )
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
            // Import cursors describe content that no longer exists. Keeping
            // them would make a fresh archive look complete and skip history.
            db.executeFast("DELETE FROM archive_import_state;").stepThis().dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    companion object {
        /** Upper bound of ids in one SQL IN (...) expression. */
        private const val MAX_SQL_IDS = 500

        private val instances = ConcurrentHashMap<Int, ForkArchiveManager>()

        private val executor: java.util.concurrent.ExecutorService =
            java.util.concurrent.Executors.newSingleThreadExecutor { runnable ->
                Thread(runnable, "berrygram-archive").apply {
                    isDaemon = true
                    priority = Thread.MIN_PRIORITY
                }
            }

        @JvmStatic
        fun getInstance(currentAccount: Int): ForkArchiveManager {
            return instances.getOrPut(currentAccount) {
                ForkArchiveManager(currentAccount)
            }
        }

        /**
         * Reads locally preserved deleted messages without blocking the caller.
         * The callback is invoked on the UI thread.
         */
        @JvmStatic
        fun loadDeletedMessages(
            account: Int,
            dialogId: Long,
            limit: Int,
            callback: ForkArchiveCallback
        ) {
            executor.execute {
                val result = try {
                    getInstance(account).getDeletedMessages(dialogId, limit)
                } catch (error: Throwable) {
                    emptyList()
                }
                org.telegram.messenger.AndroidUtilities.runOnUIThread { callback.onLoaded(result) }
            }
        }

        /** Stores a message without blocking the caller. */
        @JvmStatic
        fun archiveMessageAsync(            account: Int,
            dialogId: Long,
            messageId: Int,
            date: Int,
            senderId: Long,
            senderName: String?,
            messageText: String?,
            deleted: Boolean
        ) {
            executor.execute {
                try {
                    getInstance(account).archiveMessage(
                        dialogId, messageId, date, senderId, senderName, messageText, deleted
                    )
                } catch (error: Throwable) {
                    FileLog.e(error)
                }
            }
        }

        /** Marks messages as deleted without blocking the caller. */
        @JvmStatic
        fun markDeletedAsync(account: Int, dialogId: Long, messageIds: List<Int>) {            executor.execute {
                try {
                    getInstance(account).markDeleted(dialogId, messageIds)
                } catch (error: Throwable) {
                    FileLog.e(error)
                }
            }
        }

        /**
         * Marks messages as deleted for updates that carry only message ids.
         * The affected chats are resolved from the archive itself.
         */
        @JvmStatic
        fun markDeletedByIdsAsync(account: Int, messageIds: List<Int>) {
            executor.execute {
                try {
                    getInstance(account).markDeletedByIds(messageIds)
                } catch (error: Throwable) {
                    FileLog.e(error)
                }
            }
        }

        /**
         * Searches the archive without blocking the caller. Decrypting the
         * stored content is done on the archive thread, the result is delivered
         * on the UI thread.
         */
        fun searchAsync(account: Int, query: String?, callback: (List<ArchivedMessage>) -> Unit) {
            executor.execute {
                val result = try {
                    getInstance(account).search(query)
                } catch (error: Throwable) {
                    emptyList()
                }
                org.telegram.messenger.AndroidUtilities.runOnUIThread { callback(result) }
            }
        }

        /** Reads the archived chats without blocking the caller. */
        fun loadDialogsAsync(account: Int, callback: (List<ArchivedDialogSummary>) -> Unit) {
            executor.execute {
                val result = try {
                    getInstance(account).getArchivedDialogs()
                } catch (error: Throwable) {
                    emptyList()
                }
                org.telegram.messenger.AndroidUtilities.runOnUIThread { callback(result) }
            }
        }

        /** Reads the archived messages of one chat without blocking the caller. */
        fun loadMessagesAsync(account: Int, dialogId: Long, callback: (List<ArchivedMessage>) -> Unit) {
            executor.execute {
                val result = try {
                    getInstance(account).getArchivedMessages(dialogId)
                } catch (error: Throwable) {
                    emptyList()
                }
                org.telegram.messenger.AndroidUtilities.runOnUIThread { callback(result) }
            }
        }
    }
}
