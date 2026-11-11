package org.telegram.messenger.fork.storage

import android.text.TextUtils
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.DialogObject
import org.telegram.messenger.FileLog
import org.telegram.messenger.MessagesController
import org.telegram.messenger.Utilities
import org.telegram.tgnet.TLRPC
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Imports the accessible message history of a chat into the local archive.
 *
 * The import uses Telegram's own `messages.getHistory` request, paginates
 * through the history and stores every message in the encrypted archive. Two
 * cursors are kept per chat: one for the historical backfill and one for the
 * forward update, so an interrupted import continues where it stopped and a
 * finished import is never repeated.
 */
class ForkArchiveImporter(private val account: Int) {

    companion object {
        /** Messages requested per page. */
        const val PAGE_SIZE = 100

        /** Upper bound of pages per chat in one run, keeps a single chat bounded. */
        const val MAX_PAGES = 50

        /** Upper bound of messages imported per chat in one run. */
        const val MAX_MESSAGES = 2000

        private val instances = ConcurrentHashMap<Int, ForkArchiveImporter>()

        @JvmStatic
        fun getInstance(account: Int): ForkArchiveImporter {
            return instances.getOrPut(account) { ForkArchiveImporter(account) }
        }
    }

    enum class Mode {
        /** Import the history that is not archived yet. */
        CREATE,

        /** Synchronise the newest messages and fill the known gaps. */
        UPDATE
    }

    enum class Phase {
        IDLE,
        RUNNING,
        FINISHED,
        CANCELLED,
        FAILED
    }

    class Progress(
        @JvmField val dialogId: Long,
        @JvmField val processed: Int,
        @JvmField val total: Int,
        @JvmField val phase: Phase,
        @JvmField val imported: Int,
        @JvmField val failed: Int
    )

    interface Listener {
        fun onProgress(progress: Progress)
    }

    private val executor = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "berrygram-archive-import").apply {
            isDaemon = true
            priority = Thread.MIN_PRIORITY
        }
    }

    private val cancelled = AtomicBoolean(false)
    private val running = AtomicBoolean(false)

    @Volatile
    private var listener: Listener? = null

    /** The listener of the running import, used to restart it with the same callback. */
    fun currentListener(): Listener? = listener

    @Volatile
    private var currentPhase: Phase = Phase.IDLE

    fun setListener(listener: Listener?) {
        this.listener = listener
    }

    fun isRunning(): Boolean = running.get()

    fun phase(): Phase = currentPhase

    /** Starts the import for the given chats, one after the other. */
    fun start(dialogIds: List<Long>, mode: Mode, listener: Listener?) {
        if (dialogIds.isEmpty() || !running.compareAndSet(false, true)) {
            return
        }
        this.listener = listener
        this.cancelled.set(false)
        currentPhase = Phase.RUNNING
        executor.execute {
            var imported = 0
            var failed = 0
            try {
                for (dialogId in dialogIds) {
                    if (cancelled.get()) {
                        break
                    }
                    try {
                        imported += importChat(dialogId, mode)
                    } catch (error: Throwable) {
                        failed++
                        FileLog.e(error)
                    }
                }
            } finally {
                running.set(false)
                currentPhase = if (cancelled.get()) Phase.CANCELLED else if (failed > 0) Phase.FAILED else Phase.FINISHED
                notifyProgress(0L, 0, 0, currentPhase, imported, failed)
            }
        }
    }

    fun cancel() {
        cancelled.set(true)
    }

    private fun importChat(dialogId: Long, mode: Mode): Int {
        val controller = MessagesController.getInstance(account)
        val peer = controller.getInputPeer(dialogId) ?: return 0
        if (DialogObject.isEncryptedDialog(dialogId)) {
            // Secret chats are never imported.
            return 0
        }
        val store = ForkArchiveManager.getInstance(account)
        val state = store.importState(dialogId)
        var backfillId = state?.backfillId ?: 0
        var updateId = state?.updateId ?: 0
        val oldest = store.oldestArchivedMessageId(dialogId)
        val newest = store.newestArchivedMessageId(dialogId)
        if (backfillId == 0 && oldest > 0) {
            backfillId = oldest
        }
        if (updateId == 0 && newest > 0) {
            updateId = newest
        }

        var imported = 0
        var processed = 0
        var pages = 0

        // Forward update: start at the newest message and walk into the past
        // until a page reaches the stored cursor. This also fills the gap when
        // more than one page of messages arrived between two imports.
        if (mode == Mode.UPDATE || updateId > 0) {
            val forwardCursor = updateId
            val hadCursor = forwardCursor > 0
            var forwardOffset = 0
            var forwardPages = 0
            while (!cancelled.get() && forwardPages < MAX_PAGES && processed < MAX_MESSAGES) {
                val request = TLRPC.TL_messages_getHistory()
                request.peer = peer
                request.limit = PAGE_SIZE
                request.offset_id = forwardOffset
                request.add_offset = 0
                val messages = requestHistory(request) ?: throw IllegalStateException("Archive history request failed or was cancelled")
                if (messages.isEmpty()) {
                    break
                }
                var smallest = Int.MAX_VALUE
                var reachedKnown = false
                for (message in messages) {
                    if (cancelled.get()) {
                        break
                    }
                    processed++
                    if (message.id < smallest) {
                        smallest = message.id
                    }
                    if (org.telegram.messenger.fork.storage.ForkArchiveImportPolicy
                            .isNewer(message.id, forwardCursor)) {
                        if (storeArchivedMessage(store, dialogId, message)) {
                            imported++
                        }
                    } else {
                        reachedKnown = true
                    }
                    // The cursor remembers everything that was already seen, so
                    // a filtered out message (service, media only, self
                    // destructing) does not make the next run scan it again.
                    updateId = org.telegram.messenger.fork.storage.ForkArchiveImportPolicy
                        .nextUpdateCursor(updateId, message.id)
                }
                forwardPages++
                notifyProgress(dialogId, processed, 0, Phase.RUNNING, imported, 0)
                if (!hadCursor || reachedKnown || messages.size < PAGE_SIZE
                    || smallest == Int.MAX_VALUE || smallest == forwardOffset) {
                    break
                }
                forwardOffset = smallest
            }
            pages += forwardPages
            store.saveImportState(
                ForkArchiveManager.ImportState(
                    dialogId,
                    backfillId,
                    updateId,
                    System.currentTimeMillis()
                )
            )
        }

        // Historical backfill: paginate from the oldest known message into the past.
        var offsetId = backfillId
        while (!cancelled.get() && pages < MAX_PAGES && imported + processed < MAX_MESSAGES) {
            val request = TLRPC.TL_messages_getHistory()
            request.peer = peer
            request.limit = PAGE_SIZE
            request.offset_id = offsetId
            request.add_offset = 0
            val messages = requestHistory(request) ?: throw IllegalStateException("Archive history request failed or was cancelled")
            if (messages.isEmpty()) {
                break
            }
            var smallest = Int.MAX_VALUE
            for (message in messages) {
                processed++
                if (message.id < smallest) {
                    smallest = message.id
                }
                if (org.telegram.messenger.fork.storage.ForkArchiveImportPolicy
                        .shouldBackfill(message.id, backfillId)) {
                    if (storeArchivedMessage(store, dialogId, message)) {
                        imported++
                    }
                }
            }
            pages++
            notifyProgress(dialogId, processed, 0, Phase.RUNNING, imported, 0)
            // A fully processed page becomes the durable cursor even when it is
            // the oldest page, so a resumed import does not request it again.
            if (smallest != Int.MAX_VALUE) {
                backfillId = org.telegram.messenger.fork.storage.ForkArchiveImportPolicy
                    .nextBackfillCursor(backfillId, smallest)
            }
            store.saveImportState(
                ForkArchiveManager.ImportState(
                    dialogId,
                    backfillId,
                    updateId,
                    System.currentTimeMillis()
                )
            )
            if (smallest == Int.MAX_VALUE) {
                break
            }
            if (org.telegram.messenger.fork.storage.ForkArchiveImportPolicy
                    .reachedEnd(messages.size, PAGE_SIZE, smallest, offsetId)) {
                break
            }
            offsetId = smallest
        }

        store.saveImportState(
            ForkArchiveManager.ImportState(
                dialogId,
                if (backfillId == 0) 0 else backfillId,
                updateId,
                System.currentTimeMillis()
            )
        )
        notifyProgress(dialogId, processed, 0, Phase.RUNNING, imported, 0)
        return imported
    }

    /** Stores one message, returns true when it was new. */
    private fun storeArchivedMessage(store: ForkArchiveManager, dialogId: Long, message: TLRPC.Message): Boolean {
        try {
            if (message.id <= 0) {
                return false
            }
            if (!org.telegram.messenger.fork.storage.ForkArchiveImportPolicy.isEligible(
                    message.action != null,
                    message.ttl,
                    if (message.media != null) message.media.ttl_seconds else 0
                )) {
                // Service messages and self destructing content are never archived.
                return false
            }
            val text = message.message ?: ""
            if (text.isEmpty()) {
                return false
            }
            val senderId = DialogObject.getPeerDialogId(message.from_id)
            val existed = store.exists(dialogId, message.id)
            val stored = store.archiveMessage(
                dialogId,
                message.id,
                message.date,
                senderId,
                null,
                text,
                false
            )
            check(stored) { "Unable to write encrypted archive message" }
            return !existed
        } catch (error: Throwable) {
            FileLog.e(error)
            throw error
        }
    }

    /** Sends one history request and waits for the answer. */
    private fun requestHistory(request: TLRPC.TL_messages_getHistory): ArrayList<TLRPC.Message>? {
        val controller = MessagesController.getInstance(account)
        var attempt = 0
        while (attempt < 3) {
            if (cancelled.get()) {
                return null
            }
            val result = arrayOfNulls<TLRPC.messages_Messages>(1)
            val error = arrayOfNulls<TLRPC.TL_error>(1)
            val latch = Object()
            org.telegram.tgnet.ConnectionsManager.getInstance(account).sendRequest(request) { response, requestError ->
                // A channel answers with messages.channelMessages, a chat with
                // messages.messages. Both derive from messages.Messages.
                result[0] = response as? TLRPC.messages_Messages
                error[0] = requestError
                synchronized(latch) { latch.notifyAll() }
            }
            synchronized(latch) {
                var waited = 0
                while (result[0] == null && error[0] == null && waited < 60000) {
                    latch.wait(500)
                    waited += 500
                }
            }
            if (result[0] != null) {
                return ArrayList(result[0]!!.messages)
            }
            val requestError = error[0]
            if (requestError == null) {
                return null
            }
            if (requestError.code == 420 && (requestError.text ?: "").startsWith("FLOOD_WAIT")) {
                // The server asks to wait, the import continues afterwards. The
                // wait is sliced so a Cancel press is honoured within 500 ms.
                val text = requestError.text ?: ""
                val seconds = text.substringAfter("FLOOD_WAIT_", "5").toIntOrNull() ?: 5
                var remaining = (seconds + 1).coerceAtMost(60) * 1000L
                while (remaining > 0 && !cancelled.get()) {
                    val slice = minOf(remaining, 500L)
                    try {
                        Thread.sleep(slice)
                    } catch (interrupted: InterruptedException) {
                        Thread.currentThread().interrupt()
                        return null
                    }
                    remaining -= slice
                }
                if (cancelled.get()) {
                    return null
                }
                attempt++
                continue
            }
            FileLog.e("Archive import failed: " + (requestError.text ?: "unknown error"))
            return null
        }
        return null
    }

    private fun notifyProgress(dialogId: Long, processed: Int, total: Int, phase: Phase, imported: Int, failed: Int) {
        val callback = listener ?: return
        AndroidUtilities.runOnUIThread {
            callback.onProgress(Progress(dialogId, processed, total, phase, imported, failed))
        }
    }
}
