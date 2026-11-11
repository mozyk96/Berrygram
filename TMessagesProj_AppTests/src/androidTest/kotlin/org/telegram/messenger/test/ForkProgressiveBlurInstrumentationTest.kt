package org.telegram.messenger.test

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.LiteMode
import org.telegram.ui.Components.blur3.RenderNodeWithHash
import org.telegram.utils.glass.GlassEngine
import org.telegram.utils.glass.fade.GlassFadeBlurredDrawable
import org.telegram.utils.glass.fade.GlassFadeDirection
import org.telegram.utils.glass.fade.GlassFadeRangeParams
import org.telegram.utils.glass.source.GlassSourceColor
import org.telegram.utils.glass.source.GlassSourceRenderNode

/** Exercises the actual hardware-rendered fade on an isolated emulator. */
@RunWith(AndroidJUnit4::class)
class ForkProgressiveBlurInstrumentationTest {
    @Test fun backdropBlurIncreasesTowardBottom() {
        assumeTrue(Build.VERSION.SDK_INT >= 31)
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val hadBlur = LiteMode.isEnabled(LiteMode.FLAG_CHAT_BLUR)
        val source = GlassSourceRenderNode(null)
        val engine = GlassEngine()
        var root: FrameLayout? = null
        val paint = Paint()
        fun stripes(canvas: Canvas, width: Int, height: Int) {
            for (x in 0 until width step 8) {
                paint.color = if ((x / 8) % 2 == 0) Color.BLACK else Color.WHITE
                canvas.drawRect(x.toFloat(), 0f, (x + 8).toFloat(), height.toFloat(), paint)
            }
        }
        try {
            instrumentation.runOnMainSync {
                LiteMode.toggleFlag(LiteMode.FLAG_CHAT_BLUR, true)
                val frame = FrameLayout(activity)
                root = frame
                frame.addView(object : View(activity) {
                    override fun onDraw(canvas: Canvas) { stripes(canvas, width, height) }
                }, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                source.setupRenderer(object : RenderNodeWithHash.Renderer {
                    override fun renderNodeUpdateDisplayList(canvas: Canvas) { stripes(canvas, frame.width, frame.height) }
                })
                source.setBlur(18f)
                val fade = GlassFadeBlurredDrawable(source).apply {
                    setInefficientRenderPathsAllowed(true)
                    setDirection(GlassFadeDirection.BOTTOM)
                    setMaskRange(GlassFadeRangeParams.FILL)
                    setOverlayColorSource(GlassSourceColor().apply { setColor(Color.BLACK) })
                    setOverlayAlpha(0)
                    setOverlayOnlyAlpha(0)
                }
                val overlay = View(activity).apply { background = fade }
                frame.addView(overlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                activity.setContentView(frame)
                engine.registerDrawable(overlay, fade)
                engine.setRoot(frame)
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                val frame = root!!
                source.setSize(frame.width, frame.height)
                source.updateDisplayListIfNeeded()
                engine.invalidate()
                frame.invalidate()
            }
            Thread.sleep(1200)
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            val position = IntArray(2)
            instrumentation.runOnMainSync { root!!.getLocationOnScreen(position) }
            val frame = root!!
            fun contrast(y: Int): Int {
                var min = 255
                var max = 0
                for (x in 12 until frame.width - 12) {
                    val pixel = screenshot.getPixel(position[0] + x, position[1] + y)
                    val value = Color.red(pixel)
                    min = minOf(min, value); max = maxOf(max, value)
                }
                return max - min
            }
            val samples = listOf(1, 3, 5, 7, 9).map { contrast(frame.height * it / 10) }
            assertTrue("Expected increasing blur: $samples", samples.first() > 100 && samples.last() < samples.first() * 0.8)
            for (index in 1 until samples.size) {
                assertTrue("Blur progression is not smooth: $samples", samples[index] <= samples[index - 1] + 8 && samples[index - 1] - samples[index] < samples.first() * 0.6)
            }
        } finally {
            instrumentation.runOnMainSync {
                engine.setRoot(null)
                LiteMode.toggleFlag(LiteMode.FLAG_CHAT_BLUR, hadBlur)
                activity.finish()
            }
        }
    }
}
