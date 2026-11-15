package org.telegram.messenger.test

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.UserConfig
import org.telegram.messenger.fork.storage.ForkArchiveManager
import org.telegram.tgnet.TLRPC
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Synthetic data only; requires a logged-out test package. */
@RunWith(AndroidJUnit4::class)
class ForkPreservedMessageInstrumentationTest {
    @Test fun preservedMessageCannotQueueVoteOrTodoRequests() {
        val account = 3
        assertFalse(UserConfig.getInstance(account).isClientActivated)
        val raw = TLRPC.TL_message().apply {
            id = 800099
            date = 12345
            message = "synthetic read-only historical record"
            peer_id = TLRPC.TL_peerUser().apply { user_id = 987654399L }
            from_id = peer_id
        }
        val message = org.telegram.messenger.MessageObject(account, raw, false, false).apply { isForkDeleted = true }
        val helper = org.telegram.messenger.SendMessagesHelper.getInstance(account)
        assertEquals("Preserved poll queued a server request", 0, helper.sendVote(message, arrayListOf(), null))
        assertEquals("Preserved task queued a server request", 0,
            helper.toggleTodo(0, message, TLRPC.TodoItem().apply { id = 1 }, true, null))
    }

    @Test fun captureRejectsProtectedAndDisappearingContent() {
        val account = 3
        assertFalse(UserConfig.getInstance(account).isClientActivated)
        val dialog = 987654399L
        val manager = ForkArchiveManager.getInstance(account)
        for ((index, configure) in listOf<(TLRPC.TL_message) -> Unit>(
            { it.ttl_period = 60 },
            { it.ttl = 60 },
            { it.noforwards = true },
            { it.media = TLRPC.TL_messageMediaPhoto().apply { ttl_seconds = 60; photo = TLRPC.TL_photoEmpty() } }
        ).withIndex()) {
            val id = 800001 + index
            manager.deleteMessage(dialog, id)
            val message = TLRPC.TL_message().apply {
                this.id = id
                date = 12345
                this.message = "synthetic content that must not be retained"
                peer_id = TLRPC.TL_peerUser().apply { user_id = dialog }
                from_id = peer_id
                configure(this)
            }
            ForkArchiveManager.captureMessageAsync(account, dialog, message, true)
            val latch = CountDownLatch(1)
            var entries: List<ForkArchiveManager.ArchivedMessage>? = null
            ForkArchiveManager.loadPreservedMessage(account, dialog, id) { entries = it; latch.countDown() }
            assertTrue("Archive callback timed out", latch.await(10, TimeUnit.SECONDS))
            try { assertTrue("Excluded fixture $index was archived", entries!!.isEmpty()) }
            finally { manager.deleteMessage(dialog, id) }
        }
    }
}
