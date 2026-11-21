package org.telegram.messenger.test

import android.content.Intent
import android.content.Context
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.inspector.WindowInspector
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.LiteMode
import org.telegram.messenger.R
import org.telegram.messenger.fork.panels.ForkPanelSettings
import org.telegram.messenger.fork.panels.ForkChatHeaderSettings
import org.telegram.messenger.fork.performance.PerformanceSettings
import org.telegram.ui.ActionBar.ActionBarLayout
import org.telegram.ui.ActionBar.DrawerLayoutContainer
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ActionBar.SimpleTextView
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Cells.TextSettingsCell
import org.telegram.ui.Cells.TextCheckCell
import org.telegram.ui.Components.RecyclerListView
import org.telegram.ui.ForkLocale
import org.telegram.ui.ForkPerformanceSettingsActivity
import org.telegram.ui.ThemeActivity

@RunWith(AndroidJUnit4::class)
class ForkAppearanceSelectorInstrumentationTest {
    @Test fun appearanceAndHeaderPersistAcrossProcessRestart() {
        val phase = InstrumentationRegistry.getArguments().getString("appearanceRestartPhase")
        assumeTrue(phase == "write" || phase == "read")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val fixture = instrumentation.targetContext.getSharedPreferences("fork_appearance_restart_test", Context.MODE_PRIVATE)
        instrumentation.runOnMainSync {
            if (phase == "write") {
                assertTrue(fixture.edit()
                    .putString("originalStyle", ForkPanelSettings.style())
                    .putInt("originalFlags", LiteMode.getValue(true))
                    .putBoolean("originalHeader", ForkChatHeaderSettings.isIosStyle())
                    .putInt("writerPid", android.os.Process.myPid()).commit())
                LiteMode.setForkVisualMode("none")
                ForkChatHeaderSettings.setIosStyle(true)
            } else {
                val originalStyle = checkNotNull(fixture.getString("originalStyle", null))
                try {
                    assertTrue("The read must run in a new app process",
                        fixture.getInt("writerPid", -1) != android.os.Process.myPid())
                    assertEquals("none", ForkPanelSettings.style())
                    assertTrue(ForkChatHeaderSettings.isIosStyle())
                } finally {
                    LiteMode.setAllFlags(fixture.getInt("originalFlags", 0))
                    LiteMode.setForkVisualMode(originalStyle)
                    ForkChatHeaderSettings.setIosStyle(fixture.getBoolean("originalHeader", false))
                    fixture.edit().clear().apply()
                }
            }
        }
        if (phase == "write") {
            // The shell kills this process as soon as instrumentation ends. Wait
            // for the earlier apply() writes without rewriting either setting.
            assertTrue(instrumentation.targetContext.getSharedPreferences("fork_panels", Context.MODE_PRIVATE)
                .edit().commit())
        }
    }

    @Test fun chatSettingsSelectorReopensAcrossAllModeTransitions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val oldStyle = ForkPanelSettings.style()
        val oldFlags = LiteMode.getValue(true)
        val oldHeader = ForkChatHeaderSettings.isIosStyle()
        val oldAdaptive = ForkPanelSettings.adaptiveEffects()
        val oldConfigMode = PerformanceSettings.configMode()
        val oldManualLevel = PerformanceSettings.manualLevel()
        val oldPowerSaverLevel = LiteMode.getPowerSaverLevel()
        val oldTheme = Theme.getCurrentTheme()
        val settings = ThemeActivity(ThemeActivity.THEME_TYPE_BASIC)
        val modes = mapOf("glass" to R.string.ForkGraphicsGlass, "blur" to R.string.ForkGraphicsBlur,
            "none" to R.string.ForkGraphicsNone)
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                Theme.createChatResources(activity, false)
                Theme.createDialogsResources(activity)
                ForkPanelSettings.setAdaptiveEffects(false)
                PerformanceSettings.setConfigMode("manual")
                PerformanceSettings.setManualLevel("high")
                LiteMode.setPowerSaverLevel(0)
                LiteMode.setForkVisualMode("glass")
                ForkChatHeaderSettings.setIosStyle(true)
                val root = DrawerLayoutContainer(activity)
                val navigation = ActionBarLayout(activity, false)
                navigation.setFragmentStack(arrayListOf())
                navigation.setDrawerLayoutContainer(root)
                root.setParentActionBarLayout(navigation)
                root.setActionBarLayout(navigation)
                root.addView(navigation)
                activity.setContentView(root)
                assertTrue(navigation.presentFragment(INavigationLayout.NavigationParams(settings).setNoAnimation(true)))
            }
            for ((step, next) in listOf("none", "glass", "blur", "none", "blur", "glass").withIndex()) {
                ForkWait.idle(instrumentation)
                instrumentation.runOnMainSync {
                    val list = find(settings.fragmentView) { it is RecyclerListView } as RecyclerListView
                    val graphics = ThemeActivity::class.java.getDeclaredField("forkGraphicsRow")
                        .apply { isAccessible = true }.getInt(settings)
                    list.scrollToPosition(graphics)
                }
                ForkWait.idle(instrumentation)
                instrumentation.runOnMainSync {
                    val row = find(settings.fragmentView) {
                        it is TextSettingsCell && it.textView.text.toString() == ForkLocale.get(R.string.ForkGraphicsStyle)
                    }
                    assertTrue("Appearance row is not clickable", row?.performClick() == true)
                }
                ForkWait.idle(instrumentation)
                instrumentation.runOnMainSync {
                    val label = ForkLocale.get(modes.getValue(next))
                    val choice = WindowInspector.getGlobalWindowViews().firstNotNullOfOrNull { root ->
                        find(root) { view ->
                            when (view) {
                                is TextView -> view.text?.toString() == label
                                is SimpleTextView -> view.text?.toString() == label
                                else -> false
                            }
                        }
                    }
                    val item = generateSequence(choice) { it.parent as? View }.firstOrNull { it.isClickable }
                    assertTrue("Cannot select $next from ${ForkPanelSettings.style()}", item?.performClick() == true)
                    assertEquals(next, ForkPanelSettings.style())
                }
                if (step == 3) {
                    ForkWait.idle(instrumentation)
                    instrumentation.runOnMainSync {
                        val theme = checkNotNull(Theme.getTheme("Night"))
                        Theme.applyTheme(theme, false, false)
                        assertTrue(Theme.isCurrentThemeDark())
                        assertEquals("none", ForkPanelSettings.style())
                        assertTrue(settings.parentLayout.presentFragment(
                            INavigationLayout.NavigationParams(ForkPerformanceSettingsActivity()).setNoAnimation(true)))
                    }
                    ForkWait.idle(instrumentation)
                    instrumentation.runOnMainSync {
                        assertTrue(settings.parentLayout.lastFragment.finishFragment(false))
                    }
                }
            }
            ForkWait.idle(instrumentation)
            repeat(3) {
                instrumentation.runOnMainSync {
                    assertTrue(find(settings.fragmentView) {
                        it is TextSettingsCell && it.textView.text.toString() == ForkLocale.get(R.string.ForkGraphicsStyle)
                    }?.performClick() == true)
                }
                ForkWait.idle(instrumentation)
                instrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
                ForkWait.idle(instrumentation)
                assertEquals("glass", ForkPanelSettings.style())
            }
            instrumentation.runOnMainSync {
                Theme.applyTheme(oldTheme, false, false)
                val label = ForkLocale.get(R.string.ForkChatHeaderStyle)
                val toggle = find(settings.fragmentView) { candidate ->
                    candidate is TextCheckCell && find(candidate) { it is TextView && it.text.toString() == label } != null
                } as TextCheckCell
                assertTrue("The new switch must read the old Floating preference", toggle.isChecked)
                assertTrue(toggle.performClick())
                assertEquals(false, ForkChatHeaderSettings.isIosStyle())
                assertTrue(toggle.performClick())
                assertTrue(ForkChatHeaderSettings.isIosStyle())
                assertEquals("glass", ForkPanelSettings.style())
            }
        } finally {
            instrumentation.runOnMainSync {
                LiteMode.setAllFlags(oldFlags)
                LiteMode.setForkVisualMode(oldStyle)
                ForkPanelSettings.setAdaptiveEffects(oldAdaptive)
                PerformanceSettings.setManualLevel(oldManualLevel)
                PerformanceSettings.setConfigMode(oldConfigMode)
                LiteMode.setPowerSaverLevel(oldPowerSaverLevel)
                ForkChatHeaderSettings.setIosStyle(oldHeader)
                activity.finish()
            }
        }
    }

    private fun find(view: View?, predicate: (View) -> Boolean): View? {
        if (view == null || view.visibility != View.VISIBLE) return null
        if (predicate(view)) return view
        if (view is ViewGroup) for (index in 0 until view.childCount) {
            find(view.getChildAt(index), predicate)?.let { return it }
        }
        return null
    }
}
