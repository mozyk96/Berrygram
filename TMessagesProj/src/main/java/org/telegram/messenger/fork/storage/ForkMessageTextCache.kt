package org.telegram.messenger.fork.storage

import java.util.LinkedHashMap

/**
 * Keeps the last known text of recently seen messages per account.
 *
 * Telegram reports an edited message without its previous content, so the
 * previous version has to be known before the edit arrives. Reading the old
 * value from the message database is racy, because the storage queue may
 * already have overwritten the row. This cache therefore keeps the previous
 * text deterministically and is only a fallback-free fast path: when a message
 * is missing here the database is queried as before.
 */
object ForkMessageTextCache {
    private const val MAX_ENTRIES_PER_ACCOUNT = 2000

    class Entry(
        @JvmField val date: Int,
        @JvmField val text: String
    )

    private val caches = LinkedHashMap<Int, LinkedHashMap<Long, Entry>>()
    private val lock = Any()

    private fun key(dialogId: Long, messageId: Int): Long {
        return dialogId * 1_000_000_007L + messageId
    }

    private fun mapFor(account: Int): LinkedHashMap<Long, Entry> {
        synchronized(lock) {
            var map = caches[account]
            if (map == null) {
                map = object : LinkedHashMap<Long, Entry>(64, 0.75f, true) {
                    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Entry>?): Boolean {
                        return size > MAX_ENTRIES_PER_ACCOUNT
                    }
                }
                caches[account] = map
            }
            return map
        }
    }

    /** Remembers the current text of a message. */
    @JvmStatic
    fun put(account: Int, dialogId: Long, messageId: Int, date: Int, text: String?) {
        if (messageId <= 0 || text == null) return
        synchronized(lock) {
            mapFor(account)[key(dialogId, messageId)] = Entry(date, text)
        }
    }

    /** Returns the remembered text of a message, or null when it is unknown. */
    @JvmStatic
    fun get(account: Int, dialogId: Long, messageId: Int): Entry? {
        if (messageId <= 0) return null
        synchronized(lock) {
            return mapFor(account)[key(dialogId, messageId)]
        }
    }

    @JvmStatic
    fun remove(account: Int, dialogId: Long, messageId: Int) {
        synchronized(lock) {
            mapFor(account).remove(key(dialogId, messageId))
        }
    }

    @JvmStatic
    fun clear(account: Int) {
        synchronized(lock) {
            caches.remove(account)
        }
    }

    @JvmStatic
    fun size(account: Int): Int {
        synchronized(lock) {
            return mapFor(account).size
        }
    }
}
