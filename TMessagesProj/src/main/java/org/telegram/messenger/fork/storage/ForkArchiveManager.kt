package org.telegram.messenger.fork.storage

import org.telegram.SQLite.SQLiteCursor
import org.telegram.SQLite.SQLiteDatabase
import org.telegram.SQLite.SQLiteException
import org.telegram.messenger.AndroidUtilities
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
 *
 * The archive is read and written from several threads: the message updates of
 * Telegram, the archive executor and the import executor. Every access to the
 * database is therefore serialized, the native SQLite handle is not safe for
 * concurrent use.
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
        @JvmField val deleted: Boolean = false,
        @JvmField val observedAt: Int = 0,
        @JvmField val editDate: Int = 0,
        @JvmField val serializedMessage: ByteArray? = null
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
            database?.executeFast("CREATE TABLE IF NOT EXISTS local_deletions (owner_id INTEGER NOT NULL, dialog_id INTEGER NOT NULL, message_id INTEGER NOT NULL, PRIMARY KEY(owner_id, dialog_id, message_id));")?.stepThis()?.dispose()
            database?.executeFast("CREATE TABLE IF NOT EXISTS archived_message_state (dialog_id INTEGER NOT NULL, message_id INTEGER NOT NULL, observed_at INTEGER NOT NULL DEFAULT 0, edit_date INTEGER NOT NULL DEFAULT 0, message_enc BLOB, PRIMARY KEY(dialog_id, message_id));")?.stepThis()?.dispose()
            var markers: SQLiteCursor? = null
            try {
                markers = database?.queryFinalized("SELECT owner_id, dialog_id, message_id FROM local_deletions;")
                while (markers != null && markers.next()) {
                    ForkDeletionPolicy.markLocal(currentAccount, markers.longValue(0), markers.longValue(1), listOf(markers.intValue(2)))
                }
            } finally { markers?.dispose() }
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
                    val buffer = java.nio.ByteBuffer.allocateDirect(encrypted.size).put(encrypted).apply { flip() }
                    val state = db.executeFast(
                        "UPDATE archived_messages SET message_text = '', message_text_enc = ? WHERE dialog_id = ? AND message_id = ?;"
                    )
                    state.bindByteBuffer(1, buffer)
                    state.bindLong(2, dialogId)
                    state.bindInteger(3, messageId)
                    state.step()
                    state.dispose()
                    buffer.clear() // Keep the direct memory alive until the statement is finalized.
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
    @Synchronized
    fun archiveMessage(
        dialogId: Long,
        messageId: Int,
        date: Int,
        senderId: Long,
        senderName: String?,
        messageText: String?,
        deleted: Boolean = false
    ): Boolean {
        if (messageText.isNullOrEmpty() || isLocallyDeleted(dialogId, messageId)) return false
        val db = database ?: return false
        try {
            val encrypted = ForkArchiveCrypto.encrypt(currentAccount, messageText)
                ?: throw IllegalStateException("Archive encryption is unavailable")
            val buffer = java.nio.ByteBuffer.allocateDirect(encrypted.size).put(encrypted).apply { flip() }
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
            state.bindString(6, "")
            state.bindByteBuffer(7, buffer)
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
            update.bindString(4, "")
            update.bindByteBuffer(5, buffer)
            update.bindInteger(6, if (deleted) 1 else 0)
            update.bindLong(7, dialogId)
            update.bindInteger(8, messageId)
            update.step()
            update.dispose()
            buffer.clear() // SQLite keeps the bound buffer until the statement is finalized.
            return true
        } catch (e: Exception) {
            FileLog.e(e)
            return false
        }
    }

    @Synchronized
    private fun isLocallyDeleted(dialogId: Long, messageId: Int): Boolean {
        if (ForkDeletionPolicy.isLocal(currentAccount, dialogId, messageId)) return true
        val db = database ?: return true
        var cursor: SQLiteCursor? = null
        return try {
            cursor = db.queryFinalized("SELECT 1 FROM local_deletions WHERE owner_id = ${org.telegram.messenger.UserConfig.getInstance(currentAccount).clientUserId} AND dialog_id = $dialogId AND message_id = $messageId;")
            cursor != null && cursor.next()
        } finally { cursor?.dispose() }
    }

    @Synchronized
    private fun suppressLocal(owner: Long, dialogId: Long, ids: List<Int>) {
        val db = database ?: throw IllegalStateException("Archive database unavailable")
        for (id in ids.filter { it > 0 }.distinct()) {
            db.executeFast("INSERT OR IGNORE INTO local_deletions VALUES ($owner, $dialogId, $id);").stepThis().dispose()
            if (org.telegram.messenger.UserConfig.getInstance(currentAccount).clientUserId != owner) continue
            deleteMessage(dialogId, id)
            ForkMessageTextCache.remove(currentAccount, dialogId, id)
            ForkDeletedMessagesStorage.getInstance(currentAccount).removeDeletedMessage(dialogId, id)
            ForkEditHistoryStorage.getInstance(currentAccount).deleteMessage(dialogId, id)
        }
    }

    @Synchronized
    private fun saveMessageState(dialogId: Long, id: Int, observedAt: Int, editDate: Int, snapshot: String?) {
        if (isLocallyDeleted(dialogId, id)) return
        val db = database ?: return
        db.executeFast("INSERT OR IGNORE INTO archived_message_state(dialog_id, message_id) VALUES ($dialogId, $id);").stepThis().dispose()
        val encrypted = snapshot?.let { ForkArchiveCrypto.encrypt(currentAccount, it) }
        if (snapshot != null && encrypted == null) throw IllegalStateException("Archive encryption is unavailable")
        val buffer = encrypted?.let { java.nio.ByteBuffer.allocateDirect(it.size).put(it).apply { flip() } }
        val state = db.executeFast("UPDATE archived_message_state SET observed_at = CASE WHEN observed_at = 0 THEN ? ELSE observed_at END, edit_date = MAX(edit_date, ?), message_enc = COALESCE(?, message_enc) WHERE dialog_id = ? AND message_id = ?;")
        try {
            state.bindInteger(1, observedAt)
            state.bindInteger(2, editDate)
            if (buffer == null) state.bindNull(3) else state.bindByteBuffer(3, buffer)
            state.bindLong(4, dialogId)
            state.bindInteger(5, id)
            state.step()
        } finally { state.dispose(); buffer?.clear() }
    }

    private fun readPreservedEntry(cursor: SQLiteCursor): ArchivedMessage {
        val entry = readEntry(cursor)
        val db = database ?: return entry
        var state: SQLiteCursor? = null
        return try {
            state = db.queryFinalized("SELECT observed_at, edit_date, message_enc FROM archived_message_state WHERE dialog_id = ${entry.dialogId} AND message_id = ${entry.messageId};")
            if (state != null && state.next()) {
                val plain = ForkArchiveCrypto.decrypt(currentAccount, state.byteArrayValue(2))
                val bytes = plain?.let { android.util.Base64.decode(it, android.util.Base64.NO_WRAP) }
                ArchivedMessage(entry.dialogId, entry.messageId, entry.date, entry.senderId, entry.senderName,
                        entry.messageText, entry.deleted, state.intValue(0), state.intValue(1), bytes)
            } else entry
        } catch (error: Exception) { FileLog.e(error); entry }
        finally { state?.dispose() }
    }

    /** Marks already archived messages of one chat as deleted. */
    @JvmOverloads
    @Synchronized
    fun markDeleted(dialogId: Long, messageIds: List<Int>, observedAt: Int = (System.currentTimeMillis() / 1000L).toInt()): Int {
        val eligible = messageIds.filter { !ForkDeletionPolicy.isLocal(currentAccount, dialogId, it) }
        val chunks = sqlIdChunks(eligible)
        if (chunks.isEmpty()) return 0
        val db = database ?: return 0
        return try {
            for (chunk in chunks) {
                db.executeFast("INSERT OR IGNORE INTO archived_message_state(dialog_id, message_id, observed_at) SELECT dialog_id, message_id, $observedAt FROM archived_messages WHERE dialog_id = $dialogId AND message_id IN ($chunk);").stepThis().dispose()
                db.executeFast("UPDATE archived_message_state SET observed_at = $observedAt WHERE observed_at = 0 AND dialog_id = $dialogId AND message_id IN ($chunk);").stepThis().dispose()
                db.executeFast("UPDATE archived_messages SET deleted = 1 WHERE dialog_id = $dialogId AND message_id IN ($chunk);").stepThis().dispose()
            }
            eligible.size
        } catch (e: Exception) { FileLog.e(e); 0 }
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
    @JvmOverloads
    @Synchronized
    fun markDeletedByIds(messageIds: List<Int>, observedAt: Int = (System.currentTimeMillis() / 1000L).toInt()): List<Long> {
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
            val commonDialogs = dialogs.filter { dialog ->
                if (dialog > 0) true else {
                    val chat = org.telegram.messenger.MessagesController.getInstance(currentAccount).getChat(-dialog)
                    chat != null && !org.telegram.messenger.ChatObject.isChannel(chat)
                }
            }
            for (dialog in commonDialogs) markDeleted(dialog, messageIds, observedAt)
            commonDialogs
        } catch (e: Exception) {
            FileLog.e(e)
            emptyList()
        }
    }

    /** Removes an archived message entry completely from local storage. */
    @Synchronized
    fun deleteMessage(dialogId: Long, messageId: Int): Boolean {
        val db = database ?: return false
        return try {
            val state = db.executeFast("DELETE FROM archived_messages WHERE dialog_id = ? AND message_id = ?;")
            state.bindLong(1, dialogId)
            state.bindInteger(2, messageId)
            state.step()
            state.dispose()
            db.executeFast("DELETE FROM archived_message_state WHERE dialog_id = $dialogId AND message_id = $messageId;").stepThis().dispose()
            true
        } catch (e: Exception) {
            FileLog.e(e)
            false
        }
    }

    /** Returns locally preserved messages that were deleted in Telegram. */
    @Synchronized
    fun getDeletedMessages(dialogId: Long, limit: Int): List<ArchivedMessage> {
        val list = mutableListOf<ArchivedMessage>()
        val db = database ?: return list
        var cursor: SQLiteCursor? = null
        try {
            cursor = db.queryFinalized(
                "SELECT dialog_id, message_id, date, sender_id, sender_name, message_text, message_text_enc, deleted" +
                    " FROM archived_messages WHERE dialog_id = $dialogId AND deleted = 1 AND NOT EXISTS (SELECT 1 FROM local_deletions l WHERE l.owner_id = ${org.telegram.messenger.UserConfig.getInstance(currentAccount).clientUserId} AND l.dialog_id = archived_messages.dialog_id AND l.message_id = archived_messages.message_id) ORDER BY date ASC LIMIT $limit;"
            )
            while (cursor != null && cursor.next()) {
                list.add(readPreservedEntry(cursor))
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

    @Synchronized
    fun getArchivedDialogs(): List<ArchivedDialogSummary> {
        val list = mutableListOf<ArchivedDialogSummary>()
        val db = database ?: throw IllegalStateException("Archive database unavailable")
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
            throw e
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

    @Synchronized
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
    @Synchronized
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
    @Synchronized
    fun cleanupOlderThan(cutoffDate: Int): Int {
        val db = database ?: return 0
        return try {
            val removed = countMessages("WHERE date < $cutoffDate")
            db.executeFast("DELETE FROM archived_messages WHERE date < $cutoffDate;").stepThis().dispose()
            db.executeFast("DELETE FROM archived_message_state WHERE NOT EXISTS (SELECT 1 FROM archived_messages m WHERE m.dialog_id = archived_message_state.dialog_id AND m.message_id = archived_message_state.message_id);").stepThis().dispose()
            removed
        } catch (e: Exception) {
            FileLog.e(e)
            0
        }
    }

    @Synchronized
    fun deleteDialog(dialogId: Long): Int {
        val db = database ?: return 0
        return try {
            val removed = countMessages("WHERE dialog_id = $dialogId")
            db.executeFast("DELETE FROM archived_messages WHERE dialog_id = $dialogId;").stepThis().dispose()
            db.executeFast("DELETE FROM archived_message_state WHERE dialog_id = $dialogId;").stepThis().dispose()
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
    @Synchronized
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

    @Synchronized
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

    @Synchronized
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
    @Synchronized
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
    @Synchronized
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

    @Synchronized
    fun clearArchive() {
        val db = database ?: return
        try {
            db.executeFast("DELETE FROM archived_messages;").stepThis().dispose()
            db.executeFast("DELETE FROM archived_message_state;").stepThis().dispose()
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
            // computeIfAbsent is atomic, two threads can never open the same
            // database file twice.
            return instances.computeIfAbsent(currentAccount) { ForkArchiveManager(currentAccount) }
        }

        /**
         * Stores a message without blocking the caller. The write runs on the
         * archive executor, so the update of an incoming message never touches
         * the database on the UI thread.
         */
        @JvmStatic
        fun archiveMessageAsync(
            account: Int,
            dialogId: Long,
            messageId: Int,
            date: Int,
            senderId: Long,
            senderName: String?,
            messageText: String?
        ) {
            if (messageText.isNullOrEmpty()) return
            executor.execute {
                try {
                    getInstance(account).archiveMessage(
                        dialogId, messageId, date, senderId, senderName, messageText, false
                    )
                } catch (error: Throwable) {
                    FileLog.e(error)
                }
            }
        }

        /**
         * Size of the local archive of an account in bytes. The file is only
         * measured, the database is not opened for it.
         */
        @JvmStatic
        fun archiveSizeBytes(account: Int): Long {
            return try {
                val file = File(ApplicationLoader.applicationContext.filesDir, "fork_archive_$account.db")
                if (file.exists()) file.length() else 0L
            } catch (error: Throwable) {
                0L
            }
        }

        /** Removes every archived row without blocking the caller. */
        @JvmStatic
        fun clearArchiveAsync(account: Int, callback: (() -> Unit)? = null) {            executor.execute {
                try {
                    getInstance(account).clearArchive()
                } catch (error: Throwable) {
                    FileLog.e(error)
                }
                if (callback != null) {
                    AndroidUtilities.runOnUIThread { callback() }
                }
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

        @JvmStatic
        fun loadPreservedMessage(account: Int, dialogId: Long, id: Int, callback: ForkArchiveCallback) {
            executor.execute {
                val result = try {
                    val manager = getInstance(account)
                    synchronized(manager) {
                        val cursor = manager.database?.queryFinalized("SELECT dialog_id, message_id, date, sender_id, sender_name, message_text, message_text_enc, deleted FROM archived_messages WHERE dialog_id = $dialogId AND message_id = $id;")
                        try { if (cursor != null && cursor.next()) listOf(manager.readPreservedEntry(cursor)) else emptyList() }
                        finally { cursor?.dispose() }
                    }
                } catch (error: Throwable) { FileLog.e(error); emptyList() }
                AndroidUtilities.runOnUIThread { callback.onLoaded(result) }
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

        @JvmStatic
        fun suppressLocalDeletes(account: Int, dialogId: Long, messageIds: List<Int>) {
            val ids = messageIds.toList()
            val owner = org.telegram.messenger.UserConfig.getInstance(account).clientUserId
            ForkDeletionPolicy.markLocal(account, owner, dialogId, ids)
            executor.execute {
                try { getInstance(account).suppressLocal(owner, dialogId, ids) }
                catch (error: Throwable) { FileLog.e(error) }
            }
        }

        @JvmStatic
        fun captureMessageAsync(account: Int, dialogId: Long, message: org.telegram.tgnet.TLRPC.Message, deleted: Boolean) {
            if (message.id <= 0 || ForkDeletionPolicy.isLocal(account, dialogId, message.id)) return
            val owner = org.telegram.messenger.UserConfig.getInstance(account).clientUserId
            val id = message.id
            val date = message.date
            val edit = message.edit_date
            val sender = org.telegram.messenger.DialogObject.getPeerDialogId(message.from_id)
            val text = message.message.orEmpty()
            val observed = if (deleted) (System.currentTimeMillis() / 1000L).toInt() else 0
            val stream = org.telegram.tgnet.SerializedData(message.objectSize)
            val snapshot = try {
                message.serializeToStream(stream)
                android.util.Base64.encodeToString(stream.toByteArray(), android.util.Base64.NO_WRAP)
            } finally { stream.cleanup() }
            executor.execute {
                try {
                    if (org.telegram.messenger.UserConfig.getInstance(account).clientUserId != owner) return@execute
                    val manager = getInstance(account)
                    // Empty text is valid for voice/video; the full TL message is encrypted separately.
                    if (manager.archiveMessage(dialogId, id, date, sender, null, text.ifEmpty { " " }, deleted)) {
                        manager.saveMessageState(dialogId, id, observed, edit, snapshot)
                    }
                } catch (error: Throwable) { FileLog.e(error) }
            }
        }

        /** Marks messages as deleted without blocking the caller. */
        @JvmStatic
        fun markDeletedAsync(account: Int, dialogId: Long, messageIds: List<Int>) {
            val observed = (System.currentTimeMillis() / 1000L).toInt()
            val ids = messageIds.toList()
            executor.execute {
                try {
                    getInstance(account).markDeleted(dialogId, ids, observed)
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
            val observed = (System.currentTimeMillis() / 1000L).toInt()
            val ids = messageIds.toList()
            executor.execute {
                try {
                    getInstance(account).markDeletedByIds(ids, observed)
                } catch (error: Throwable) {
                    FileLog.e(error)
                }
            }
        }

        /** Deletes an archived message asynchronously. */
        @JvmStatic
        fun deleteMessageAsync(account: Int, dialogId: Long, messageId: Int) {
            suppressLocalDeletes(account, dialogId, listOf(messageId))
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
        fun loadDialogsAsync(account: Int, callback: (List<ArchivedDialogSummary>?) -> Unit) {
            executor.execute {
                val result = try {
                    getInstance(account).getArchivedDialogs()
                } catch (error: Throwable) {
                    FileLog.e(error)
                    null
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
