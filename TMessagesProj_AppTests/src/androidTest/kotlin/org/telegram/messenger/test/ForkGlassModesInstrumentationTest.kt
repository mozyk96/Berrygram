package org.telegram.messenger.test

import android.content.Intent
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.fork.panels.ForkPanelSettings
import org.telegram.messenger.fork.performance.PerformanceSettings
import org.telegram.messenger.LiteMode
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ForkGlassSurface
import org.telegram.ui.ForkGraphicsSurface
import org.telegram.ui.ForkPerformanceSettingsActivity
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundProviderBuilder

@RunWith(AndroidJUnit4::class)
class ForkGlassModesInstrumentationTest {
    @Test fun modeTransitionsKeepPreferenceAndFlagsInSync() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val oldStyle = ForkPanelSettings.style()
        val oldFlags = LiteMode.getValue(true)
        try {
            instrumentation.runOnMainSync {
                val apply = LiteMode::class.java.getMethod("setForkVisualMode", String::class.java)
                val controlled = LiteMode.FLAG_LIQUID_GLASS or LiteMode.FLAG_CHAT_BLUR
                for ((mode, bits) in listOf(
                    "glass" to controlled,
                    "blur" to LiteMode.FLAG_CHAT_BLUR,
                    "none" to 0,
                    "glass" to controlled
                )) {
                    apply.invoke(null, mode)
                    assertEquals("$mode preference", mode, ForkPanelSettings.style())
                    ForkPanelSettings::class.java.getDeclaredField("cachedStyle")
                        .apply { isAccessible = true }.set(null, null)
                    assertEquals("$mode persists in preferences", mode, ForkPanelSettings.style())
                    assertEquals("$mode flags", bits, LiteMode.getValue(true) and controlled)
                    assertEquals("$mode must preserve unrelated flags", oldFlags and controlled.inv(),
                        LiteMode.getValue(true) and controlled.inv())
                }
                LiteMode.toggleFlag(LiteMode.FLAG_LIQUID_GLASS, true)
                assertEquals("Re-enabling the native flag must not replace the selected style",
                    "glass", ForkPanelSettings.style())
            }
        } finally {
            instrumentation.runOnMainSync {
                LiteMode.setAllFlags(oldFlags)
                ForkPanelSettings.setStyle(oldStyle)
                org.telegram.utils.glass.GlassEngine.invalidateSettings()
            }
        }
    }

    @Test fun noBackgroundSuppressesPlatesButKeepsMaterialFallback() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val oldStyle = ForkPanelSettings.style()
        val oldAdaptive = ForkPanelSettings.adaptiveEffects()
        val oldConfigMode = PerformanceSettings.configMode()
        val oldManualLevel = PerformanceSettings.manualLevel()
        try {
            instrumentation.runOnMainSync {
                ForkPanelSettings.setAdaptiveEffects(false)
                PerformanceSettings.setConfigMode("manual")
                PerformanceSettings.setManualLevel("low")
                LiteMode.setForkVisualMode("none")
                assertEquals("No Background must survive power saving", "none",
                    ForkGraphicsSurface.effectiveStyle())
                assertTrue("No Background must suppress chrome plates",
                    ForkGraphicsSurface.backgroundless())
                assertTrue("No Background must not draw blur surfaces",
                    !ForkGraphicsSurface.blurSurfacesEnabled())
                assertEquals("No Background menus must not capture a blur",
                    0, ForkGlassSurface.captureBlurAmount(15))
                assertEquals("none", ForkGraphicsSurface.effectiveStyle())

                // The opaque material fallback is a plate, never "No Background".
                ForkPanelSettings.setStyle("glass")
                assertEquals("material", ForkGraphicsSurface.effectiveStyle())
                assertTrue("Material fallback must keep its plate",
                    !ForkGraphicsSurface.backgroundless())
                assertTrue("Material fallback must not enable blur surfaces",
                    !ForkGraphicsSurface.blurSurfacesEnabled())

                LiteMode.setForkVisualMode("none")
                assertEquals("none", ForkGraphicsSurface.effectiveStyle())
                assertTrue(ForkGraphicsSurface.backgroundless())
            }
        } finally {
            instrumentation.runOnMainSync {
                ForkPanelSettings.setStyle(oldStyle)
                ForkPanelSettings.setAdaptiveEffects(oldAdaptive)
                PerformanceSettings.setManualLevel(oldManualLevel)
                PerformanceSettings.setConfigMode(oldConfigMode)
                org.telegram.utils.glass.GlassEngine.invalidateSettings()
            }
        }
    }

    @Test fun enablingGlassKeepsTheExplicitlySelectedStyle() {
        val oldStyle = ForkPanelSettings.style()
        val oldFlags = LiteMode.getValue(true)
        try {
            ForkPanelSettings.setStyle("blur")
            val blurFlags = (oldFlags or LiteMode.FLAG_CHAT_BLUR) and LiteMode.FLAG_LIQUID_GLASS.inv()
            LiteMode.setAllFlags(blurFlags)
            ForkPanelSettings.setStyle("glass")
            LiteMode.setAllFlags(blurFlags or LiteMode.FLAG_LIQUID_GLASS)
            assertEquals("Enabling the glass flag must preserve the selected style",
                "glass", ForkPanelSettings.style())
        } finally {
            LiteMode.setAllFlags(oldFlags)
            ForkPanelSettings.setStyle(oldStyle)
            org.telegram.utils.glass.GlassEngine.invalidateSettings()
        }
    }

    @Test fun legacyLiquidAndExperimentalStylesUseTheSameGlassSettings() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val oldStyle = ForkPanelSettings.style()
        val oldAdaptive = ForkPanelSettings.adaptiveEffects()
        val oldConfigMode = PerformanceSettings.configMode()
        val oldManualLevel = PerformanceSettings.manualLevel()
        val oldPowerSaverLevel = LiteMode.getPowerSaverLevel()
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                ForkPanelSettings.setAdaptiveEffects(false)
                PerformanceSettings.setConfigMode("manual")
                PerformanceSettings.setManualLevel("high")
                LiteMode.setPowerSaverLevel(0)
                ForkPanelSettings.setStyle("glass")
                assertEquals("glass", ForkGraphicsSurface.effectiveStyle())
                val delegate = BlurredBackgroundProviderBuilder(null)
                    .setBackgroundColor { _, _ -> Color.WHITE }.build()
                val baseline = ForkGlassSurface.provider(null)
                val baselineColor = baseline.backgroundColor
                val baselineStroke = baseline.strokeColorTop
                val wrappedColor = ForkGlassSurface.wrap(delegate).backgroundColor
                val blur = ForkGlassSurface.captureBlurAmount(18)
                val saturation = ForkGlassSurface.backdropSaturation(1f)
                val labelMethod = ForkPerformanceSettingsActivity::class.java.getDeclaredMethod("styleLabel")
                    .apply { isAccessible = true }
                val glassLabel = labelMethod.invoke(null)
                for (legacy in listOf("liquid", "experimental")) {
                    ForkPanelSettings.setStyle(legacy)
                    assertEquals(legacy, ForkGraphicsSurface.effectiveStyle())
                    assertEquals("$legacy background", baselineColor, ForkGlassSurface.provider(null).backgroundColor)
                    assertEquals("$legacy edge", baselineStroke, ForkGlassSurface.provider(null).strokeColorTop)
                    assertEquals("$legacy wrapped surface", wrappedColor, ForkGlassSurface.wrap(delegate).backgroundColor)
                    assertEquals("$legacy blur", blur, ForkGlassSurface.captureBlurAmount(18))
                    assertEquals("$legacy saturation", saturation, ForkGlassSurface.backdropSaturation(1f), 0.001f)
                    assertEquals("$legacy performance label", glassLabel, labelMethod.invoke(null))
                }
                ForkPanelSettings.setStyle("blur")
                ForkPanelSettings.setStyle("glass")
                assertEquals("Glass settings must restore after Blur", baselineColor,
                    ForkGlassSurface.provider(null).backgroundColor)
            }
        } finally {
            instrumentation.runOnMainSync {
                ForkPanelSettings.setStyle(oldStyle)
                ForkPanelSettings.setAdaptiveEffects(oldAdaptive)
                PerformanceSettings.setManualLevel(oldManualLevel)
                PerformanceSettings.setConfigMode(oldConfigMode)
                LiteMode.setPowerSaverLevel(oldPowerSaverLevel)
                org.telegram.utils.glass.GlassEngine.invalidateSettings()
                activity.finish()
            }
        }
    }
}
