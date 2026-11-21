package org.telegram.messenger.test

import android.content.Intent
import android.graphics.Rect
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.collection.LongSparseArray
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.R
import org.telegram.messenger.LocaleController
import org.telegram.messenger.UserConfig
import org.telegram.messenger.UserObject
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.ActionBarLayout
import org.telegram.ui.ActionBar.ActionBarMenuItem
import org.telegram.ui.ActionBar.DrawerLayoutContainer
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ChatActivity
import org.telegram.ui.ProfileActivity
import org.telegram.ui.Components.ChatActivityEnterView
import org.telegram.ui.Components.ChatAvatarContainer

/** Exercises the production chat view using a synthetic peer on a logged-out emulator. */
@RunWith(AndroidJUnit4::class)
class ForkSyntheticChatInstrumentationTest {
    @Test fun syntheticChatsDisplayBothHeaderStyles() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val oldHeaderStyle = org.telegram.messenger.fork.panels.ForkChatHeaderSettings.isIosStyle()
        val oldEffect = org.telegram.messenger.fork.panels.ForkPanelSettings.style()
        try {
            val cases = listOf("glass", "blur", "none").flatMap { effect ->
                listOf("private", "bot", "group", "channel", "saved", "verification").flatMap { kind ->
                    listOf(false, true).map { floating -> Triple(kind, floating, effect) }
                }
            }
            for ((kind, ios, effect) in cases) {
                var chat: ChatActivity? = null
                try {
                    instrumentation.runOnMainSync {
                        org.telegram.messenger.fork.panels.ForkChatHeaderSettings.setIosStyle(ios)
                        org.telegram.messenger.fork.panels.ForkPanelSettings.setStyle(effect)
                        org.telegram.utils.glass.GlassEngine.invalidateSettings()
                        for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                            assertFalse("This fixture requires a logged-out emulator",
                                UserConfig.getInstance(account).isClientActivated)
                        }
                        val connectivity = activity.getSystemService(ConnectivityManager::class.java)
                        val network = connectivity.activeNetwork
                        assertFalse("This fixture requires an offline emulator",
                            network != null && connectivity.getNetworkCapabilities(network)
                                ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
                        Theme.createCommonResources(activity)
                        AndroidUtilities.enableEdgeToEdge(activity)
                        val controller = MessagesController.getInstance(UserConfig.selectedAccount)
                        val args = Bundle()
                        if (kind != "group" && kind != "channel") {
                            val peer = TLRPC.TL_user().apply {
                                id = when (kind) {
                                    "bot" -> 987654322L
                                    "saved" -> 987654326L
                                    "verification" -> UserObject.VERIFY
                                    else -> 987654325L
                                }
                                first_name = if (kind == "bot") "Тестовий бот" else "Тестова розмова з довгою назвою для перевірки шапки"
                                bot = kind == "bot"
                                self = kind == "saved"
                            }
                            controller.putUser(peer, false)
                            if (kind == "private") {
                                @Suppress("UNCHECKED_CAST")
                                val fullUsers = MessagesController::class.java.getDeclaredField("fullUsers")
                                    .apply { isAccessible = true }.get(controller) as LongSparseArray<TLRPC.UserFull>
                                fullUsers.put(peer.id, TLRPC.TL_userFull().apply {
                                    id = peer.id
                                    phone_calls_available = true
                                    notify_settings = TLRPC.TL_peerNotifySettings()
                                })
                            }
                            args.putLong("user_id", peer.id)
                        } else {
                            val peer = if (kind == "group") TLRPC.TL_chat() else TLRPC.TL_channel()
                            peer.id = if (kind == "group") 987654323L else 987654324L
                            peer.title = if (kind == "group") "Тестова група" else "Тестовий канал з довгою назвою для перевірки шапки"
                            if (kind == "channel") peer.broadcast = true
                            controller.putChat(peer, false)
                            args.putLong("chat_id", peer.id)
                        }
                        val root = DrawerLayoutContainer(activity)
                        val navigation = ActionBarLayout(activity, false)
                        navigation.setFragmentStack(arrayListOf())
                        navigation.setDrawerLayoutContainer(root)
                        root.setParentActionBarLayout(navigation)
                        root.setActionBarLayout(navigation)
                        root.addView(navigation)
                        activity.setContentView(root)
                        val fragment = ChatActivity(args)
                        chat = fragment
                        assertTrue("Synthetic $kind chat could not be opened",
                            navigation.presentFragment(INavigationLayout.NavigationParams(fragment).setNoAnimation(true)))
                        val avatar = fragment.avatarContainer
                        assertTrue("Synthetic $kind avatar is not visible", avatar.avatarImageView.visibility == View.VISIBLE)
                        assertTrue("Synthetic $kind title is missing", avatar.titleTextView.text.isNotEmpty())
                    }
                    ForkWait.idle(instrumentation)
                    instrumentation.runOnMainSync {
                        val fragment = checkNotNull(chat)
                        val avatar = fragment.avatarContainer
                        val floating = ios && kind != "verification"
                        assertEquals("$kind should use its supported header layout", floating, fragment.actionBar.usesBerrygramChatHeader())
                        assertEquals(effect, org.telegram.messenger.fork.panels.ForkPanelSettings.style())
                        if (floating) {
                            assertTrue("Floating $kind avatar is not after the title", avatar.avatarImageView.left > avatar.titleTextView.right)
                            assertEquals("Floating $kind must not show an overflow button", 0, fragment.headerItem?.width)
                            val bar = fragment.actionBar
                            val call = findVisibleCall(bar)
                            if (kind == "private") {
                                assertTrue("An eligible private chat must have a usable call button", call?.isClickable == true)
                                val callRect = Rect(0, 0, call!!.width, call.height)
                                bar.offsetDescendantRectToMyCoords(call, callRect)
                                val avatarRect = Rect(0, 0, avatar.avatarImageView.width, avatar.avatarImageView.height)
                                bar.offsetDescendantRectToMyCoords(avatar.avatarImageView, avatarRect)
                                assertTrue("Call and avatar must have separate hit targets", callRect.right <= avatarRect.left)
                                val glass = org.telegram.ui.ActionBar.ActionBar::class.java.getDeclaredField("glassDrawableMenu")
                                    .apply { isAccessible = true }.get(bar) as android.graphics.drawable.Drawable
                                assertTrue("Call and avatar must share one surface",
                                    glass.bounds.contains(callRect) && glass.bounds.contains(avatarRect))
                            } else {
                                assertTrue("$kind must not show a user call action", call == null)
                            }
                        } else {
                            assertTrue("Classic $kind avatar is not before the title", avatar.avatarImageView.right < avatar.titleTextView.left)
                            assertTrue("Classic $kind must keep its overflow button", (fragment.headerItem?.width ?: 0) > 0)
                        }
                    }
                    val screenshot = ForkScreenshot.capture(activity)
                    try {
                        java.io.File(activity.getExternalFilesDir(null), "berrygram-203-synthetic-$kind-${if (ios) "floating" else "classic"}-$effect.png")
                            .outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                    } finally { screenshot.recycle() }
                    if (ios && kind == "private" && effect == "none") {
                        instrumentation.runOnMainSync { assertTrue(checkNotNull(chat).avatarContainer.performClick()) }
                        ForkWait.idle(instrumentation)
                        instrumentation.runOnMainSync {
                            val fragment = checkNotNull(chat)
                            val profile = fragment.parentLayout.lastFragment as ProfileActivity
                            val more = ProfileActivity::class.java.getDeclaredField("otherItem")
                                .apply { isAccessible = true }.get(profile) as ActionBarMenuItem
                            more.toggleSubMenu()
                        }
                        ForkWait.idle(instrumentation)
                        instrumentation.runOnMainSync {
                            val fragment = checkNotNull(chat)
                            val profile = fragment.parentLayout.lastFragment as ProfileActivity
                            val more = ProfileActivity::class.java.getDeclaredField("otherItem")
                                .apply { isAccessible = true }.get(profile) as ActionBarMenuItem
                            val items = more.popupLayout
                            val action = (0 until items.itemsCount).map(items::getItemAt)
                                .filterIsInstance<org.telegram.ui.ActionBar.ActionBarMenuSubItem>()
                                .single { it.textView.text.toString() == org.telegram.ui.ForkLocale.get(R.string.ForkConversationActions) }
                            assertTrue(action.performClick())
                        }
                        ForkWait.idle(instrumentation)
                        instrumentation.runOnMainSync {
                            val fragment = checkNotNull(chat)
                            assertEquals(fragment, fragment.parentLayout.lastFragment)
                            assertTrue("Profile More must reopen the native chat actions", fragment.headerItem.isSubMenuShowing)
                            fragment.headerItem.closeSubMenu()
                        }
                    }
                } finally {
                    instrumentation.runOnMainSync { chat?.onFragmentDestroy() }
                }
            }
        } finally {
            instrumentation.runOnMainSync {
                org.telegram.messenger.fork.panels.ForkChatHeaderSettings.setIosStyle(oldHeaderStyle)
                org.telegram.messenger.fork.panels.ForkPanelSettings.setStyle(oldEffect)
                org.telegram.utils.glass.GlassEngine.invalidateSettings()
                activity.finish()
            }
        }
    }

    private fun findVisibleCall(view: View): View? {
        if (view.visibility != View.VISIBLE) return null
        if (view is ActionBarMenuItem && view.width > 0 &&
            view.contentDescription == LocaleController.getString(R.string.Call)) return view
        if (view is ViewGroup) for (i in 0 until view.childCount) {
            findVisibleCall(view.getChildAt(i))?.let { return it }
        }
        return null
    }

    @Test fun syntheticPrivateChatDisplaysHeaderAndComposer() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var chat: ChatActivity? = null
        var input: ChatActivityEnterView? = null
        try {
            instrumentation.runOnMainSync {
                for (account in 0 until UserConfig.MAX_ACCOUNT_COUNT) {
                    assertFalse("This fixture requires a logged-out emulator",
                        UserConfig.getInstance(account).isClientActivated)
                }
                val connectivity = activity.getSystemService(ConnectivityManager::class.java)
                val network = connectivity.activeNetwork
                assertFalse("This fixture requires an offline emulator",
                    network != null && connectivity.getNetworkCapabilities(network)
                        ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)
                Theme.createCommonResources(activity)
                AndroidUtilities.enableEdgeToEdge(activity)
                val peer = TLRPC.TL_user().apply {
                    id = 987654321L
                    first_name = "Тестова розмова"
                    status = TLRPC.TL_userStatusRecently()
                }
                MessagesController.getInstance(UserConfig.selectedAccount).putUser(peer, false)
                val root = DrawerLayoutContainer(activity)
                val navigation = ActionBarLayout(activity, false)
                navigation.setFragmentStack(arrayListOf())
                navigation.setDrawerLayoutContainer(root)
                root.setParentActionBarLayout(navigation)
                root.setActionBarLayout(navigation)
                root.addView(navigation)
                activity.setContentView(root)
                val fragment = ChatActivity(Bundle().apply { putLong("user_id", peer.id) })
                chat = fragment
                assertTrue("Synthetic chat could not be opened",
                    navigation.presentFragment(INavigationLayout.NavigationParams(fragment).setNoAnimation(true)))
                val avatar = ChatActivity::class.java.getDeclaredField("avatarContainer")
                    .apply { isAccessible = true }.get(fragment) as ChatAvatarContainer
                val composer = ChatActivity::class.java.getDeclaredField("chatActivityEnterView")
                    .apply { isAccessible = true }.get(fragment) as ChatActivityEnterView
                input = composer
                assertTrue("Chat avatar is not visible", avatar.avatarImageView.visibility == View.VISIBLE)
                assertTrue("Synthetic title is missing", avatar.titleTextView.text.toString().contains("Тестова"))
                assertTrue("Message composer is not visible", composer.visibility == View.VISIBLE)
            }
            ForkWait.idle(instrumentation)
            val screenshot = ForkScreenshot.capture(activity)
            try {
                java.io.File(activity.getExternalFilesDir(null), "berrygram-151-synthetic-chat.png")
                    .outputStream().use { screenshot.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
            } finally { screenshot.recycle() }
            instrumentation.runOnMainSync {
                val composer = input!!
                val text = "Перший рядок\nДругий рядок"
                composer.setFieldText(text)
                composer.checkSendButton(false)
                assertEquals("Multiline draft changed", text, composer.editField!!.text.toString())
                assertTrue("A draft should expose the send control", composer.isSendButtonVisible)
                composer.setFieldText("")
                composer.checkSendButton(false)
                assertFalse("An empty draft should restore the recording control", composer.isSendButtonVisible)
            }
        } finally {
            instrumentation.runOnMainSync { chat?.onFragmentDestroy(); activity.finish() }
        }
    }
}
