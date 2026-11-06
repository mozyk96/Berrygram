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
 * Multi-account separated. Written in Kotlin.
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
                    "text TEXT NOT NULL" +
                    ");"
            )?.stepThis()?.dispose()
            database?.executeFast(
                "CREATE INDEX IF NOT EXISTS idx_edit_history ON edit_history(dialog_id, message_id);"
            )?.stepThis()?.dispose()
        } catch (e: SQLiteException) {
            FileLog.e(e)
        }
    }

    fun recordPriorVersion(dialogId: Long, messageId: Int, editDate: Int, text: String?) {
        if (text.isNullOrEmpty()) return
        val db = database ?: return
        try {
            val state = db.executeFast("INSERT INTO edit_history VALUES (?, ?, ?, ?);")
            state.bindLong(1, dialogId)
            state.bindInteger(2, messageId)
            state.bindInteger(3, editDate)
            state.bindString(4, text)
            state.step()
            state.dispose()
        } catch (e: Exception) {
            FileLog.e(e)
        }
    }

    fun getRevisions(dialogId: Long, messageId: Int): List<EditRevision> {
        val list = mutableListOf<EditRevision>()
        val db = database ?: return list
        var cursor: SQLiteCursor? = null
        try {
            cursor = db.queryFinalized(
                "SELECT edit_date, text FROM edit_history WHERE dialog_id = $dialogId AND message_id = $messageId ORDER BY edit_date ASC;"
            )
            while (cursor != null && cursor.next()) {
                val date = cursor.intValue(0)
                val text = cursor.stringValue(1) ?: ""
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
