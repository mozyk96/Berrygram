package org.telegram.messenger.test

import android.content.Intent
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.R
import org.telegram.messenger.fork.performance.PerformancePolicy
import org.telegram.messenger.fork.performance.PerformanceSettings
import org.telegram.ui.ActionBar.ActionBarLayout
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.DrawerLayoutContainer
import org.telegram.ui.ActionBar.INavigationLayout
import org.telegram.ui.ForkLocale
import org.telegram.ui.ForkSettingsBlocks
import org.telegram.ui.fork.settings.ForkSettingsSection

@RunWith(AndroidJUnit4::class)
class ForkPerformanceModeInstrumentationTest {
    @Test fun manualMediumUsesSmallerBackdropThanHighWithoutChangingAppearancePreference() {
        org.junit.Assume.assumeTrue(android.os.Build.VERSION.SDK_INT >= 33)
        val originalMode = PerformanceSettings.configMode()
        val originalLevel = PerformanceSettings.manualLevel()
        val originalStyle = org.telegram.messenger.fork.panels.ForkPanelSettings.style()
        val originalAdaptive = org.telegram.messenger.fork.panels.ForkPanelSettings.adaptiveEffects()
        try {
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                org.telegram.messenger.fork.panels.ForkPanelSettings.setAdaptiveEffects(false)
                org.telegram.messenger.fork.panels.ForkPanelSettings.setStyle("glass")
                PerformanceSettings.setConfigMode("manual")
                PerformanceSettings.setManualLevel("medium")
                val mediumDownscale = org.telegram.ui.ForkGlassSurface.captureBlurAmount(15)
                PerformanceSettings.setManualLevel("high")
                val highDownscale = org.telegram.ui.ForkGlassSurface.captureBlurAmount(15)
                assertTrue("Medium and High must have different actual backdrop budgets", mediumDownscale > highDownscale)
                PerformanceSettings.setManualLevel("low")
                assertEquals("material", org.telegram.ui.ForkGraphicsSurface.effectiveStyle())
                assertEquals("glass", org.telegram.messenger.fork.panels.ForkPanelSettings.style())
            }
        } finally {
            PerformanceSettings.setConfigMode(originalMode)
            PerformanceSettings.setManualLevel(originalLevel)
            org.telegram.messenger.fork.panels.ForkPanelSettings.setStyle(originalStyle)
            org.telegram.messenger.fork.panels.ForkPanelSettings.setAdaptiveEffects(originalAdaptive)
        }
    }

    @Test fun unavailableGpuRendererIsReportedAsUnknownRatherThanBoardName() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            assertEquals(android.opengl.EGL14.EGL_NO_CONTEXT, android.opengl.EGL14.eglGetCurrentContext())
            assertEquals("", org.telegram.messenger.fork.performance.ForkDeviceAnalyzer.scanDevice().gpuRenderer)
        }
    }

    @Test fun choosingManualImmediatelyRevealsLevelWithoutReopeningScreen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val original = PerformanceSettings.configMode()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        lateinit var section: ForkSettingsSection
        try {
            instrumentation.runOnMainSync {
                PerformanceSettings.setConfigMode(PerformancePolicy.CONFIG_AUTO)
                val root = DrawerLayoutContainer(activity)
                val navigation = ActionBarLayout(activity, false)
                navigation.setFragmentStack(arrayListOf())
                navigation.setDrawerLayoutContainer(root)
                root.setParentActionBarLayout(navigation)
                root.setActionBarLayout(navigation)
                root.addView(navigation)
                activity.setContentView(root)
                val fragment = object : BaseFragment() {
                    override fun createView(context: android.content.Context): View {
                        section = ForkSettingsBlocks.performance(this)
                        fragmentView = section
                        return section
                    }
                }
                assertTrue(navigation.presentFragment(INavigationLayout.NavigationParams(fragment).setNoAnimation(true)))
                assertEquals(View.GONE, section.getChildAt(3).visibility)
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync { section.getChildAt(1).performClick() }
            instrumentation.waitForIdleSync()
            val label = ForkLocale.get(R.string.ForkPerformanceManualMode)
            instrumentation.runOnMainSync {
                fun find(view: View): View? {
                    if (view is org.telegram.ui.ActionBar.SimpleTextView && view.text.toString().contains(label)) return view
                    if (view is android.widget.TextView && view.text.toString().contains(label)) return view
                    if (view is android.view.ViewGroup) for (i in 0 until view.childCount) {
                        val match = find(view.getChildAt(i))
                        if (match != null) return match
                    }
                    return null
                }
                var choice = android.view.inspector.WindowInspector.getGlobalWindowViews().firstNotNullOfOrNull { find(it) }
                assertTrue("Manual choice is missing from the actual popup", choice != null)
                while (choice != null && !choice!!.isClickable) choice = choice!!.parent as? View
                assertTrue("Manual choice cannot be clicked", choice?.performClick() == true)
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                assertEquals(PerformancePolicy.CONFIG_MANUAL, PerformanceSettings.configMode())
                assertEquals("Manual level remains hidden after selecting Manual", View.VISIBLE, section.getChildAt(3).visibility)
            }
        } finally {
            instrumentation.runOnMainSync {
                PerformanceSettings.setConfigMode(original)
                activity.finish()
            }
        }
    }
}
