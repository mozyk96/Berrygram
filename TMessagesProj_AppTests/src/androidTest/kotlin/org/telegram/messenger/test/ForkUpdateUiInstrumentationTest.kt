package org.telegram.messenger.test

import android.content.Intent
import android.graphics.Bitmap
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import org.telegram.messenger.R
import org.telegram.ui.ForkLocale
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.ui.ActionBar.ActionBarLayout
import org.telegram.ui.ActionBar.DrawerLayoutContainer
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ActionBar.Theme
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.LocaleController
import org.telegram.messenger.UserConfig
import org.telegram.ui.ForkUpdateActivity
import java.io.File

/** Renders the actual Update Center in a logged-out emulator; no account is used. */
@RunWith(AndroidJUnit4::class)
class ForkUpdateUiInstrumentationTest {
    @Test fun updateCenterRendersOnNarrowScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val activity = instrumentation.startActivitySync(Intent(context, SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val fragment = ForkUpdateActivity()
        val controller = LocaleController.getInstance()
        val originalLanguage = controller.currentLocaleInfo
        instrumentation.runOnMainSync {
            val language = InstrumentationRegistry.getArguments().getString("berrygram_language")
            if (!language.isNullOrEmpty()) {
                val selected = controller.getLanguageFromDict(language)
                assertTrue("Requested language missing", selected != null)
                controller.applyLanguage(selected, false, true, UserConfig.selectedAccount)
            }
            Theme.createCommonResources(activity)
            // Host the screen exactly like the application does: the navigation container wires
            // onInsetsInternal to the fragment view, which is what reserves the system navigation
            // and the floating bottom navigation. targetSdk 36 enforces edge-to-edge, so a bare
            // LinearLayout host would draw the update action under the system bar.
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
        instrumentation.runOnMainSync { fragment.onResume() }
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            fun findButton(view: android.view.View): Button? {
                if (view is Button) return view
                if (view is ViewGroup) for (i in 0 until view.childCount) {
                    val found = findButton(view.getChildAt(i))
                    if (found != null) return found
                }
                return null
            }
            fun findLabel(view: android.view.View, text: String): TextView? {
                if (view is TextView && view.text.toString() == text) return view
                if (view is ViewGroup) for (i in 0 until view.childCount) {
                    val found = findLabel(view.getChildAt(i), text)
                    if (found != null) return found
                }
                return null
            }
            val label = findLabel(activity.window.decorView, ForkLocale.get(R.string.ForkUpdateAutomatic))
            assertTrue("Automatic update label is missing", label != null && label.isShown)
            val layout = label!!.layout
            assertTrue("Automatic update label is not laid out", layout != null && layout.lineCount > 0)
            for (line in 0 until layout.lineCount) {
                assertTrue("Automatic update label is truncated", layout.getEllipsisCount(line) == 0)
                if (line + 1 < layout.lineCount) {
                    val boundary = layout.getLineEnd(line)
                    val text = label.text
                    assertTrue("Automatic update label breaks inside a word", boundary == 0 || boundary >= text.length ||
                        text[boundary - 1].isWhitespace() || text[boundary].isWhitespace())
                }
            }
            val button = findButton(activity.window.decorView)
            assertTrue("Update action is not visible", button != null && button.isShown && button.width > 0 && button.height > 0)
            val location = IntArray(2)
            button!!.getLocationOnScreen(location)
            val navigation = activity.window.decorView.rootWindowInsets.systemWindowInsetBottom
            val screenHeight = activity.resources.displayMetrics.heightPixels
            assertTrue("Update action overlaps system navigation", location[1] + button.height <= screenHeight - navigation)
        }
        instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/Berrygram-update-test.png").use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use { input -> input.readBytes() }
        }
        val screenshot = instrumentation.uiAutomation.takeScreenshot()
        assertTrue("Screenshot is empty", screenshot != null && screenshot.width > 0 && screenshot.height > 0)
        val file = File(context.getExternalFilesDir(null), "update-center-instrumentation.png")
        file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        instrumentation.runOnMainSync {
            fragment.onFragmentDestroy()
            if (originalLanguage != null) controller.applyLanguage(originalLanguage, false, true, UserConfig.selectedAccount)
            activity.finish()
        }
    }
}
