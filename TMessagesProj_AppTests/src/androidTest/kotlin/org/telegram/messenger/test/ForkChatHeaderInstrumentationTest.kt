package org.telegram.messenger.test

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.R
import org.telegram.messenger.fork.panels.ForkPanelSettings
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed
import org.telegram.ui.ForkGraphicsSurface
import org.telegram.utils.glass.source.GlassSourceColor

@RunWith(AndroidJUnit4::class)
class ForkChatHeaderInstrumentationTest {
    @Test fun compactHeaderDrawsOneContinuousSurface() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                val bar = ActionBar(activity)
                bar.setOccupyStatusBar(false)
                bar.setBackButtonImage(R.drawable.ic_ab_back)
                bar.setTitle("Example")
                val source = GlassSourceColor().apply { setColor(Color.RED) }
                bar.setupGlass(BlurredBackgroundDrawableViewFactory(source),
                    BlurredBackgroundColorProviderThemed(null, Theme.key_actionBarDefault, 1f))
                bar.compactChatGlass = true
                for (width in listOf(360f, 393f, 600f)) {
                    bar.measure(View.MeasureSpec.makeMeasureSpec(dp(width), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(dp(56f), View.MeasureSpec.EXACTLY))
                    bar.layout(0, 0, bar.measuredWidth, bar.measuredHeight)
                    val bitmap = Bitmap.createBitmap(bar.width, bar.height, Bitmap.Config.ARGB_8888)
                    try {
                        bar.draw(Canvas(bitmap))
                        assertTrue("Gap between back and title should have a surface at $width dp",
                            Color.alpha(bitmap.getPixel(dp(55f), bar.height / 2)) > 0)
                    } finally { bitmap.recycle() }
                }
            }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }

    @Test fun noBackgroundChatHeaderDrawsNoPlate() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val oldStyle = ForkPanelSettings.style()
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                val bar = ActionBar(activity)
                bar.setOccupyStatusBar(false)
                bar.setBackButtonImage(R.drawable.ic_ab_back)
                bar.setTitle("Example")
                val source = GlassSourceColor().apply { setColor(Color.RED) }
                bar.setupGlass(BlurredBackgroundDrawableViewFactory(source),
                    BlurredBackgroundColorProviderThemed(null, Theme.key_actionBarDefault, 1f))
                bar.compactChatGlass = true

                fun render(): Bitmap {
                    bar.measure(View.MeasureSpec.makeMeasureSpec(dp(393f), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(dp(56f), View.MeasureSpec.EXACTLY))
                    bar.layout(0, 0, bar.measuredWidth, bar.measuredHeight)
                    val bitmap = Bitmap.createBitmap(bar.width, bar.height, Bitmap.Config.ARGB_8888)
                    bar.draw(Canvas(bitmap))
                    return bitmap
                }

                ForkPanelSettings.setStyle("none")
                org.telegram.utils.glass.GlassEngine.invalidateSettings()
                assertTrue("No Background must be active", ForkGraphicsSurface.backgroundless())
                val plain = render()
                try {
                    assertEquals("No Background header must not draw a glass plate", 0,
                        Color.alpha(plain.getPixel(dp(55f), bar.height / 2)))
                    assertEquals("No Background header gap must stay transparent", 0,
                        Color.alpha(plain.getPixel(dp(56f), bar.height / 2)))
                    java.io.File(activity.getExternalFilesDir(null), "berrygram-1.5-header-nobg.png")
                        .outputStream().use { plain.compress(Bitmap.CompressFormat.PNG, 100, it) }
                } finally { plain.recycle() }

                ForkPanelSettings.setStyle("glass")
                org.telegram.utils.glass.GlassEngine.invalidateSettings()
                val glass = render()
                try {
                    assertTrue("Liquid Glass header must draw its plate", Color.alpha(
                        glass.getPixel(dp(55f), bar.height / 2)) > 0)
                    java.io.File(activity.getExternalFilesDir(null), "berrygram-1.5-header-glass.png")
                        .outputStream().use { glass.compress(Bitmap.CompressFormat.PNG, 100, it) }
                } finally { glass.recycle() }
            }
        } finally {
            instrumentation.runOnMainSync {
                ForkPanelSettings.setStyle(oldStyle)
                org.telegram.utils.glass.GlassEngine.invalidateSettings()
                activity.finish()
            }
        }
    }
}
