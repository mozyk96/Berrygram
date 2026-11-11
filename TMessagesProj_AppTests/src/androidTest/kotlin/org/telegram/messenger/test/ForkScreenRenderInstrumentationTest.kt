package org.telegram.messenger.test

import android.content.Context
import android.content.Intent
import android.os.ParcelFileDescriptor
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.LocaleController
import org.telegram.messenger.UserConfig
import org.telegram.ui.ActionBar.ActionBarLayout
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme

/** Renders fork-owned settings on an isolated, logged-out emulator. */
@RunWith(AndroidJUnit4::class)
class ForkScreenRenderInstrumentationTest {
    @Test fun renderSettingsAndReportClippedLabels() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val names = listOf("ForkAboutActivity", "ForkAppearanceSettingsActivity", "ForkArchiveActivity", "ForkCustomTextActivity",
            "ForkExperimentalGlassActivity", "ForkGestureSettingsActivity", "ForkMediaSettingsActivity", "ForkMessageMenuSettingsActivity",
            "ForkNotificationDiagnosticsActivity", "ForkReactionSettingsActivity", "ForkPerformanceSettingsActivity", "ForkSettingsActivity",
            "ForkStealthSettingsActivity", "ForkTranslationSettingsActivity", "ForkVideoSettingsActivity", "ForkVoiceSettingsActivity", "ForkUpdateActivity", "LiteModeSettingsActivity")
        val problems = mutableListOf<String>()
        val controller = LocaleController.getInstance()
        val original = controller.currentLocaleInfo
        instrumentation.runOnMainSync {
            Theme.createCommonResources(activity)
            val selected = controller.getLanguageFromDict("uk")
            if (selected != null) controller.applyLanguage(selected, false, true, UserConfig.selectedAccount)
        }
        for (name in names) {
            var fragment: BaseFragment? = null
            try {
                instrumentation.runOnMainSync {
                    val instance = Class.forName("org.telegram.ui.$name").getConstructor().newInstance() as BaseFragment
                    fragment = instance
                    if (name == "LiteModeSettingsActivity") {
                        val navigation = ActionBarLayout(activity, false)
                        navigation.setFragmentStack(arrayListOf())
                        instance.setParentLayout(navigation)
                    }
                    instance.onFragmentCreate()
                    val actionBar = if (name == "LiteModeSettingsActivity") null else instance.createActionBar(activity)
                    if (actionBar != null) {
                        val field = BaseFragment::class.java.getDeclaredField("actionBar")
                        field.isAccessible = true
                        field.set(instance, actionBar)
                    }
                    val page = instance.performCreateView(activity)
                    val host = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                    if (actionBar != null && actionBar.shouldAddToContainer()) host.addView(actionBar, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
                    instance.simpleActionBar?.let { if (it.parent == null) host.addView(it, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)) }
                    host.addView(page, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                    activity.setContentView(host)
                    instance.onResume()
                }
                Thread.sleep(400)
                fun inspect(view: View) {
                        if (!view.isShown || view.width == 0 || view.height == 0) return
                        if (view is TextView && view.layout != null) {
                            for (line in 0 until view.layout.lineCount) {
                                if (view.layout.getEllipsisCount(line) > 0) problems.add("$name: ellipsis: ${view.text}")
                                if (line + 1 < view.layout.lineCount) {
                                    val end = view.layout.getLineEnd(line)
                                    val text = view.text
                                    if (end > 0 && end < text.length && Character.isLetterOrDigit(text[end - 1]) && Character.isLetterOrDigit(text[end]))
                                        problems.add("$name: word break: $text")
                                }
                            }
                        }
                        if (view is ViewGroup) for (i in 0 until view.childCount) inspect(view.getChildAt(i))
                    }
                instrumentation.runOnMainSync {
                    inspect(activity.window.decorView)
                    if (name == "ForkArchiveActivity") {
                        fun hasVisibleText(view: View, expected: String): Boolean {
                            if (view is TextView && view.isShown && view.text.toString() == expected) return true
                            if (view is ViewGroup) for (i in 0 until view.childCount) if (hasVisibleText(view.getChildAt(i), expected)) return true
                            return false
                        }
                        if (!hasVisibleText(activity.window.decorView, org.telegram.ui.ForkLocale.get(org.telegram.messenger.R.string.ForkArchiveEmpty)))
                            problems.add("$name: empty archive state is missing")
                    }
                    if (name == "ForkSettingsActivity") {
                        fun header(view: View): TextView? {
                            if (view is TextView && view.text.toString() == org.telegram.ui.ForkLocale.get(org.telegram.messenger.R.string.ForkSettingsTitle)) return view
                            if (view is ViewGroup) for (i in 0 until view.childCount) {
                                val found = header(view.getChildAt(i))
                                if (found != null) return found
                            }
                            return null
                        }
                        val title = header(activity.window.decorView)
                        if (title != null) {
                            val point = IntArray(2); title.getLocationOnScreen(point)
                            val statusTop = activity.window.decorView.rootWindowInsets.systemWindowInsetTop
                            if (point[1] < statusTop) problems.add("$name: header overlaps status bar")
                        }
                    }
                }
                fun findLists(view: View, lists: MutableList<View>) {
                    if (view.javaClass.name.contains("RecyclerListView") && view.isShown) lists.add(view)
                    if (view is ViewGroup) for (i in 0 until view.childCount) findLists(view.getChildAt(i), lists)
                }
                val lists = mutableListOf<View>()
                instrumentation.runOnMainSync { findLists(activity.window.decorView, lists) }
                for (list in if (name == "LiteModeSettingsActivity") emptyList() else lists) {
                    var steps = 0
                    while (steps++ < 40) {
                        var canScroll = false
                        instrumentation.runOnMainSync {
                            canScroll = list.canScrollVertically(1)
                            if (canScroll) list.scrollBy(0, (list.height * 3 / 4).coerceAtLeast(1))
                        }
                        if (!canScroll) break
                        instrumentation.waitForIdleSync()
                        instrumentation.runOnMainSync { inspect(list) }
                    }
                }
                instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/Berrygram-$name.png").use {
                    ParcelFileDescriptor.AutoCloseInputStream(it).use { input -> input.readBytes() }
                }
            } catch (error: Throwable) {
                problems.add("$name: ${error.cause ?: error}")
            } finally {
                instrumentation.runOnMainSync {
                    fragment?.onPause()
                    fragment?.onFragmentDestroy()
                    activity.setContentView(View(activity))
                }
            }
        }
        instrumentation.runOnMainSync {
            if (original != null) controller.applyLanguage(original, false, true, UserConfig.selectedAccount)
            activity.finish()
        }
        assertTrue("Settings rendering problems:\n" + problems.joinToString("\n"), problems.isEmpty())
    }
}
