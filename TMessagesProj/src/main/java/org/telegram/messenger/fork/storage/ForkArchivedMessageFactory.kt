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
            val message = TLRPC.TL_message()
            message.id = entry.messageId
            message.date = entry.date
            message.message = entry.messageText
            message.out = entry.senderId != 0L && entry.senderId == selfId
            message.dialog_id = entry.dialogId
            message.peer_id = controller.getPeer(entry.dialogId)
            val fromId = if (entry.senderId != 0L) entry.senderId else entry.dialogId
            message.from_id = controller.getPeer(fromId)
            val obj = MessageObject(account, message, true, false)
            obj.isForkDeleted = true
            obj
        } catch (error: Throwable) {
            null
        }
    }
}
