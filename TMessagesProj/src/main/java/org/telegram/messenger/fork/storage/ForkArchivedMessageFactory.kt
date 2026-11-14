package org.telegram.messenger.fork.storage

import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC

/**
 * Builds a display only message object for a locally preserved message that
 * no longer exists on the Telegram servers.
 *
 * The created object is never written back to Telegram's database and is never
 * sent to the server. It only exists so that a deleted message can keep its
 * place inside an open chat.
 */
object ForkArchivedMessageFactory {
    @JvmStatic
    fun create(account: Int, entry: ForkArchiveManager.ArchivedMessage): MessageObject? {
        if (entry.messageId <= 0 || entry.date <= 0) {
            return null
        }
        return try {
            val controller = MessagesController.getInstance(account)
            val selfId = UserConfig.getInstance(account).clientUserId
            val message = if (entry.serializedMessage != null) {
                val stream = org.telegram.tgnet.SerializedData(entry.serializedMessage)
                try {
                    val restored = TLRPC.Message.TLdeserialize(stream, stream.readInt32(true), true) ?: return null
                    if (restored.id != entry.messageId) return null
                    restored.readAttachPath(stream, selfId)
                    restored
                } finally { stream.cleanup() }
            } else TLRPC.TL_message().apply {
                id = entry.messageId
                date = entry.date
                this.message = entry.messageText
                out = entry.senderId != 0L && entry.senderId == selfId
                peer_id = controller.getPeer(entry.dialogId)
                val fromId = if (entry.senderId != 0L) entry.senderId else entry.dialogId
                from_id = controller.getPeer(fromId)
            }
            message.dialog_id = entry.dialogId
            message.date = entry.date
            if (message.edit_date == 0) message.edit_date = entry.editDate
            if (message.message.isNullOrBlank() && message.media == null) {
                message.message = org.telegram.ui.ForkLocale.get(org.telegram.messenger.R.string.ForkPreservedMessage)
            }
            val obj = MessageObject(account, message, true, false)
            obj.isForkDeleted = true
            obj.forkDeletionObservedAt = entry.observedAt
            obj
        } catch (error: Throwable) {
            null
        }
    }
}
