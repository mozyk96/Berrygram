package org.telegram.messenger.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.SQLite.SQLiteDatabase
import org.telegram.messenger.fork.storage.ForkArchiveCrypto
import org.telegram.messenger.fork.storage.ForkArchiveImporter
import org.telegram.messenger.fork.storage.ForkArchiveManager
import org.telegram.tgnet.TLRPC
import java.lang.reflect.InvocationTargetException

/** Uses an isolated test account and leaves any existing archive untouched. */
@RunWith(AndroidJUnit4::class)
class ForkArchiveEncryptionInstrumentationTest {
    @Test fun keystoreEncryptionRoundTripsAndFailedWritesPreserveCiphertext() {
        val account = 987654
        val text = "archive test message"
        val encrypted = ForkArchiveCrypto.encrypt(account, text)
        assertNotNull("Android Keystore must encrypt archive text", encrypted)
        assertEquals(text, ForkArchiveCrypto.decrypt(account, encrypted))

        val dialogId = System.nanoTime().and(Long.MAX_VALUE)
        val manager = ForkArchiveManager.getInstance(account)
        assertTrue(manager.archiveMessage(dialogId, 1, 1, 1, null, text, false))
        val dbField = ForkArchiveManager::class.java.getDeclaredField("database").apply { isAccessible = true }
        val db = dbField.get(manager) as SQLiteDatabase
        fun assertStoredOriginal() {
            val cursor = db.queryFinalized("SELECT message_text, message_text_enc FROM archived_messages WHERE dialog_id = $dialogId AND message_id = 1")
            try {
                assertTrue("Expected archived message", cursor.next())
                assertEquals("Archive must not store plaintext", "", cursor.stringValue(0))
                assertEquals(text, ForkArchiveCrypto.decrypt(account, cursor.byteArrayValue(1)))
            } finally { cursor.dispose() }
        }
        assertStoredOriginal()
        val unavailableField = ForkArchiveCrypto::class.java.getDeclaredField("unavailable").apply { isAccessible = true }
        @Suppress("UNCHECKED_CAST") val unavailable = unavailableField.get(null) as MutableSet<Int>
        try {
            unavailable.add(account)
            assertFalse("Unavailable encryption must fail the write", manager.archiveMessage(dialogId, 1, 2, 1, null, "replacement", false))
        } finally { unavailable.remove(account) }
        assertStoredOriginal()
    }

    @Test fun failedArchiveWriteStopsImport() {
        val account = 987654
        val manager = ForkArchiveManager.getInstance(account)
        val dbField = ForkArchiveManager::class.java.getDeclaredField("database").apply { isAccessible = true }
        val db = synchronized(manager) { dbField.get(manager).also { dbField.set(manager, null) } }
        try {
            val message = TLRPC.TL_message().apply {
                id = 1
                date = 1
                this.message = "import test"
                from_id = TLRPC.TL_peerUser().apply { user_id = 1 }
            }
            val method = ForkArchiveImporter::class.java.getDeclaredMethod("storeArchivedMessage", ForkArchiveManager::class.java, Long::class.javaPrimitiveType, TLRPC.Message::class.java).apply { isAccessible = true }
            try {
                method.invoke(ForkArchiveImporter(account), manager, 1L, message)
                fail("A failed archive write must stop the import")
            } catch (error: InvocationTargetException) {
                assertTrue(error.cause is IllegalStateException)
            }
        } finally { synchronized(manager) { dbField.set(manager, db) } }
    }

    @Test fun legacyPlaintextIsMigratedWithoutLosingContent() {
        val account = 987654
        val dialogId = System.nanoTime().and(Long.MAX_VALUE)
        val manager = ForkArchiveManager.getInstance(account)
        val dbField = ForkArchiveManager::class.java.getDeclaredField("database").apply { isAccessible = true }
        val db = dbField.get(manager) as SQLiteDatabase
        val statement = db.executeFast("INSERT INTO archived_messages (dialog_id, message_id, date, sender_id, message_text) VALUES (?, 1, 1, 1, ?)")
        try {
            statement.bindLong(1, dialogId)
            statement.bindString(2, "legacy archive text")
            statement.step()
        } finally { statement.dispose() }
        val migrate = ForkArchiveManager::class.java.getDeclaredMethod("migrateLegacyRows").apply { isAccessible = true }
        migrate.invoke(manager)
        val cursor = db.queryFinalized("SELECT message_text, message_text_enc FROM archived_messages WHERE dialog_id = $dialogId AND message_id = 1")
        try {
            assertTrue(cursor.next())
            assertEquals("", cursor.stringValue(0))
            assertEquals("legacy archive text", ForkArchiveCrypto.decrypt(account, cursor.byteArrayValue(1)))
        } finally { cursor.dispose() }
    }
}
