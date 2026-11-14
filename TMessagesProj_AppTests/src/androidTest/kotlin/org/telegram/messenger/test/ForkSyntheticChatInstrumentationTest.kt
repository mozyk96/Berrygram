package org.telegram.messenger.test

import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MessagesController
import org.telegram.messenger.UserConfig
import org.telegram.tgnet.TLRPC
import org.telegram.ui.ActionBar.ActionBarLayout
import org.telegram.ui.ActionBar.DrawerLayoutContainer
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ChatActivity
import org.telegram.ui.Components.ChatActivityEnterView
import org.telegram.ui.Components.ChatAvatarContainer

/** Exercises the production chat view using a synthetic peer on a logged-out emulator. */
@RunWith(AndroidJUnit4::class)
class ForkSyntheticChatInstrumentationTest {
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
            instrumentation.waitForIdleSync()
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
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
