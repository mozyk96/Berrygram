package org.telegram.messenger.test

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.SparseArray
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.MessageObject
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.messenger.fork.stealth.ForkStealthSettings
import org.telegram.messenger.fork.storage.ForkArchiveManager
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.ActionBarLayout
import org.telegram.ui.ActionBar.DrawerLayoutContainer
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ChatActivity
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Artificial in-memory identity, no auth key/login, offline emulator only. */
@RunWith(AndroidJUnit4::class)
class ForkDeletionUiInstrumentationTest {
    @Test fun localVoiceDeletionRemovesSelectionWhileExternalDeletionKeepsReadOnlyBubble() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val account = UserConfig.selectedAccount
        val config = UserConfig.getInstance(account)
        assertFalse("Requires a logged-out fixture package", config.isClientActivated)
        val identityField = UserConfig::class.java.getDeclaredField("currentUser").apply { isAccessible = true }
        val idField = UserConfig::class.java.getDeclaredField("clientUserId").apply { isAccessible = true }
        val previousId = idField.getLong(config)
        val previousPreservation = ForkStealthSettings.deletedMessagesEnabled(account)
        val dialog = 987654388L
        val owner = 987654389L
        var chat: ChatActivity? = null
        try {
            instrumentation.runOnMainSync {
                val connectivity = activity.getSystemService(ConnectivityManager::class.java)
                assertFalse("Fixture must be offline", connectivity.activeNetwork?.let {
                    connectivity.getNetworkCapabilities(it)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                } == true)
                org.telegram.ui.ActionBar.Theme.createCommonResources(activity)
                ForkStealthSettings.setDeletedMessagesEnabled(account, true)
                MessagesController.getInstance(account).putUser(TLRPC.TL_user().apply {
                    id = dialog; first_name = "Тест видалення"
                }, false)
                val root = DrawerLayoutContainer(activity)
                val navigation = ActionBarLayout(activity, false)
                navigation.setFragmentStack(arrayListOf())
                navigation.setDrawerLayoutContainer(root)
                root.setParentActionBarLayout(navigation)
                root.setActionBarLayout(navigation)
                root.addView(navigation)
                activity.setContentView(root)
                chat = ChatActivity(Bundle().apply { putLong("user_id", dialog) })
                assertTrue(navigation.presentFragment(INavigationLayout.NavigationParams(chat!!).setNoAnimation(true)))
            }
            ForkWait.idle(instrumentation)
            val manager = ForkArchiveManager.getInstance(account)
            assertTrue(manager.archiveMessage(dialog, 800201, 12345, owner, null, "synthetic voice caption", false))
            instrumentation.runOnMainSync {
                config.setCurrentUser(TLRPC.TL_user().apply { id = owner; first_name = "Synthetic owner" })
                val fragment = chat!!
                @Suppress("UNCHECKED_CAST")
                val dictionaries = ChatActivity::class.java.getDeclaredField("messagesDict").apply { isAccessible = true }
                    .get(fragment) as Array<SparseArray<MessageObject>>
                @Suppress("UNCHECKED_CAST")
                val selection = ChatActivity::class.java.getDeclaredField("selectedMessagesIds").apply { isAccessible = true }
                    .get(fragment) as Array<SparseArray<MessageObject>>
                val select = ChatActivity::class.java.getDeclaredMethod("addToSelectedMessages", MessageObject::class.java,
                    Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType).apply { isAccessible = true }
                val deleted = ChatActivity::class.java.getDeclaredMethod("processDeletedMessages", ArrayList::class.java,
                    Long::class.javaPrimitiveType, Boolean::class.javaPrimitiveType, Boolean::class.javaPrimitiveType).apply { isAccessible = true }
                fun voice(id: Int): MessageObject {
                    val raw = TLRPC.TL_message().apply {
                        this.id = id; date = 12345; out = true; message = ""; flags = 512
                        peer_id = TLRPC.TL_peerUser().apply { user_id = dialog }
                        from_id = TLRPC.TL_peerUser().apply { user_id = owner }
                        media = TLRPC.TL_messageMediaDocument().apply {
                            flags = 1
                            document = TLRPC.TL_document().apply {
                                this.id = id.toLong(); date = 12345; dc_id = 2; mime_type = "audio/ogg"
                                size = 1024; file_reference = byteArrayOf()
                                attributes.add(TLRPC.TL_documentAttributeAudio().apply { voice = true; duration = 1.0 })
                            }
                        }
                    }
                    return MessageObject(account, raw, false, false).also {
                        assertTrue("Fixture must be voice media", it.isVoice)
                        fragment.messages.add(it)
                        dictionaries[0].put(id, it)
                    }
                }
                val local = voice(800201)
                select.invoke(fragment, local, false, true)
                assertEquals(1, selection[0].size())
                ForkArchiveManager.suppressLocalDeletes(account, dialog, listOf(local.id))
                deleted.invoke(fragment, arrayListOf(local.id), 0L, false, false)
                assertTrue("Locally deleted voice reappeared", fragment.messages.none { it.id == local.id })
                assertFalse(local.isForkDeleted)
                assertEquals(0, selection[0].size())
                assertFalse("Selection toolbar remained active", fragment.actionBar.isActionModeShowed)
                val remote = voice(800202)
                deleted.invoke(fragment, arrayListOf(remote.id), 0L, false, false)
                assertTrue("External deletion must retain available local content", fragment.messages.contains(remote))
                assertTrue(remote.isForkDeleted)
                assertFalse(remote.canForwardMessage())
                assertFalse(remote.canSetReaction())
            }
            val latch = CountDownLatch(1)
            var result: List<ForkArchiveManager.ArchivedMessage>? = null
            ForkArchiveManager.loadPreservedMessage(account, dialog, 800201) { result = it; latch.countDown() }
            assertTrue(latch.await(10, TimeUnit.SECONDS))
            assertTrue("Asynchronous archive work resurrected the local deletion", result!!.isEmpty())
            manager.deleteMessage(dialog, 800202)
            org.telegram.messenger.fork.storage.ForkDeletedMessagesStorage.getInstance(account).removeDeletedMessage(dialog, 800202)
        } finally {
            instrumentation.runOnMainSync {
                // Do not save the artificial identity to preferences or create a real session.
                identityField.set(config, null)
                idField.setLong(config, previousId)
                ForkStealthSettings.setDeletedMessagesEnabled(account, previousPreservation)
                chat?.onFragmentDestroy()
                activity.finish()
            }
        }
    }
}
