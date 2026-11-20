package org.telegram.messenger.test

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Captures the visible window of an activity.
 *
 * `UiAutomation.takeScreenshot()` asks the compositor for the last presented
 * frame and blocks until it arrives. On a software rendered emulator the
 * compositor never answers that request, so the test hangs in the graphics
 * backend instead of in the application. Drawing the real view hierarchy on
 * the main thread produces the same picture without the dependency on the
 * graphics backend, and it still renders the production widgets.
 */
object ForkScreenshot {
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
