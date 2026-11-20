package org.telegram.messenger.test

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.ParcelFileDescriptor
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Renders the activity hierarchy without asking the system compositor.
 *
 * `UiAutomation.takeScreenshot()` asks the compositor for the last presented
 * frame and blocks until it arrives. On a software rendered emulator the
 * compositor never answers that request, so the test hangs in the graphics
 * backend instead of in the application. The hierarchy render still exercises
 * the widgets, but it cannot capture a separate window or GPU-only effects.
 */
object ForkScreenshot {
    fun captureDisplay(): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        return ParcelFileDescriptor.AutoCloseInputStream(
            instrumentation.uiAutomation.executeShellCommand("screencap -p")
        ).use { checkNotNull(BitmapFactory.decodeStream(it)) { "No PNG from display capture" } }
    }

    fun capture(activity: Activity): Bitmap {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val decor: View = activity.window.decorView
        var result: Bitmap? = null
        instrumentation.runOnMainSync {
            val width = if (decor.width > 0) decor.width else decor.measuredWidth
            val height = if (decor.height > 0) decor.height else decor.measuredHeight
            val bitmap = Bitmap.createBitmap(
                maxOf(1, width), maxOf(1, height), Bitmap.Config.ARGB_8888
            )
            decor.draw(Canvas(bitmap))
            result = bitmap
        }
        return result!!
    }
}
