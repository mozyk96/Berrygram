package org.telegram.messenger.test

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.graphics.Insets
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.ActionBarLayout
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.DrawerLayoutContainer
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.IntroActivity
import org.telegram.ui.ForkSettingsFragment
import org.telegram.ui.LiteModeSettingsActivity
import org.telegram.messenger.AndroidUtilities

@RunWith(AndroidJUnit4::class)
class ForkSettingsInsetsInstrumentationTest {
    @Test fun powerSavingRendersThroughRealNavigationAtBothEnds() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val fragment = LiteModeSettingsActivity()
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                AndroidUtilities.enableEdgeToEdge(activity)
                val root = DrawerLayoutContainer(activity)
                val navigation = ActionBarLayout(activity, false)
                navigation.setFragmentStack(arrayListOf())
                navigation.setDrawerLayoutContainer(root)
                root.setParentActionBarLayout(navigation)
                root.setActionBarLayout(navigation)
                root.addView(navigation)
                activity.setContentView(root)
                assertTrue(navigation.presentFragment(INavigationLayout.NavigationParams(fragment).setNoAnimation(true)))
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(1000)
            lateinit var list: RecyclerListView
            instrumentation.runOnMainSync {
                list = LiteModeSettingsActivity::class.java.getDeclaredField("listView").apply { isAccessible = true }.get(fragment) as RecyclerListView
                val first = list.findViewHolderForAdapterPosition(0)
                assertTrue("Power Saving first row is missing", first != null && first.itemView.isShown)
                val firstPos = IntArray(2); first!!.itemView.getLocationOnScreen(firstPos)
                val header = requireNotNull(fragment.simpleActionBar)
                val headerPos = IntArray(2); header.getLocationOnScreen(headerPos)
                assertTrue("Power Saving first row overlaps the toolbar: first=" + firstPos[1] + " header=" + headerPos[1] + " height=" + header.height + " padTop=" + list.paddingTop + " listTop=" + list.top + " scrollOffset=" + list.computeVerticalScrollOffset(), firstPos[1] >= headerPos[1] + header.height)
            }
            instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/Berrygram-145-power-top.png").use { ParcelFileDescriptor.AutoCloseInputStream(it).use { input -> input.readBytes() } }
            instrumentation.runOnMainSync { list.scrollToPosition(list.adapter!!.itemCount - 1) }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync { assertTrue("Last setting cannot be reached", list.findViewHolderForAdapterPosition(list.adapter!!.itemCount - 1)?.itemView?.isShown == true) }
            instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/Berrygram-145-power-bottom.png").use { ParcelFileDescriptor.AutoCloseInputStream(it).use { input -> input.readBytes() } }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
    @Test fun berrygramScreensUseInsetsOnceAndKeepControlsAboveKeyboard() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                for (name in listOf("ForkAppearanceSettingsActivity", "ForkUpdateActivity")) {
                    val fragment = Class.forName("org.telegram.ui.$name").getConstructor().newInstance() as ForkSettingsFragment
                    try {
                        val navigation = ActionBarLayout(activity, false)
                        navigation.setFragmentStack(arrayListOf())
                        fragment.setParentLayout(navigation)
                        fragment.onFragmentCreate()
                        val page = fragment.performCreateView(activity)
                        assertTrue("$name does not draw behind the system bar", fragment.isSupportEdgeToEdge)
                        val base = page.paddingBottom
                        fun insets(ime: Int) = WindowInsetsCompat.Builder()
                            .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 24))
                            .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 24))
                            .setInsets(WindowInsetsCompat.Type.ime(), Insets.of(0, 0, 0, ime))
                            .setVisible(WindowInsetsCompat.Type.ime(), ime > 0)
                            .build()
                        fragment.onInsetsInternal(page, insets(0))
                        val navPadding = page.paddingBottom
                        assertTrue("$name does not reserve the navigation inset", navPadding >= base + 24)
                        fragment.onInsetsInternal(page, insets(260))
                        assertEquals("$name does not reserve the keyboard inset", navPadding - 24 + 260, page.paddingBottom)
                        fragment.onInsetsInternal(page, insets(0))
                        assertEquals("$name accumulated or retained insets", navPadding, page.paddingBottom)
                    } finally { fragment.onFragmentDestroy() }
                }
            }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
    @Test fun edgeToEdgeWindowAndInitialNavigationBackgroundFollowTheme() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                AndroidUtilities.enableEdgeToEdge(activity)
                assertEquals("Navigation bar must be transparent", Color.TRANSPARENT, activity.window.navigationBarColor)
                if (Build.VERSION.SDK_INT >= 29) assertTrue("System contrast scrim must not obscure the app", !activity.window.isNavigationBarContrastEnforced)
                val root = DrawerLayoutContainer(activity)
                assertEquals("Initial navigation background must follow the active theme", Theme.getColor(Theme.key_windowBackgroundGray), root.internalNavbarPaint.color)
                assertEquals("Onboarding navigation background must match its content", Theme.getColor(Theme.key_windowBackgroundWhite), IntroActivity().navigationBarColor)
            }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }
    @Test fun settingsKeepTheirFirstRowBelowTheGlassToolbarAfterInsets() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                val insets = WindowInsetsCompat.Builder()
                    .setInsets(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 24))
                    .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.systemBars(), Insets.of(0, 24, 0, 24))
                    .build()
                for (name in listOf("LiteModeSettingsActivity", "DataSettingsActivity", "LanguageSelectActivity", "NotificationsSettingsActivity", "PrivacySettingsActivity", "ThemeActivity")) {
                    val type = Class.forName("org.telegram.ui.$name")
                    val fragment = (if (name == "ThemeActivity") type.getConstructor(Int::class.javaPrimitiveType).newInstance(0) else type.getConstructor().newInstance()) as BaseFragment
                    try {
                        val navigation = ActionBarLayout(activity, false)
                        navigation.setFragmentStack(arrayListOf())
                        fragment.setParentLayout(navigation)
                        fragment.onFragmentCreate()
                        val page = fragment.performCreateView(activity)
                        val list = type.getDeclaredField("listView").apply { isAccessible = true }.get(fragment) as RecyclerListView
                        fragment.onInsetsInternal(page, insets)
                        assertTrue("$name: first setting is behind the toolbar", list.paddingTop >= ActionBar.getCurrentActionBarHeight() + 24)
                        val top = list.paddingTop
                        val bottom = list.paddingBottom
                        fragment.onInsetsInternal(page, insets)
                        assertEquals("$name: top insets changed on repeat dispatch", top, list.paddingTop)
                        assertEquals("$name: bottom insets were applied twice", bottom, list.paddingBottom)
                    } finally { fragment.onFragmentDestroy() }
                }
            }
        } finally {
            instrumentation.runOnMainSync { activity.finish() }
        }
    }
}
