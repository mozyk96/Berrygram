package org.telegram.messenger.fork.storage

/**
 * Java friendly callback for the asynchronous archive reads, so Java call
 * sites can use a lambda without returning a Kotlin unit value.
 */
fun interface ForkArchiveCallback {
    fun onLoaded(messages: List<ForkArchiveManager.ArchivedMessage>)
}
