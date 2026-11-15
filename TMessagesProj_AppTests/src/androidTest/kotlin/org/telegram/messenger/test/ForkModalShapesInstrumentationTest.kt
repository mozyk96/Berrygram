package org.telegram.messenger.test

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.R
import org.telegram.messenger.fork.panels.ForkPanelSettings
import org.telegram.ui.ActionBar.AlertDialog
import org.telegram.ui.ActionBar.BottomSheet
import org.telegram.ui.ActionBar.Theme

@RunWith(AndroidJUnit4::class)
class ForkModalShapesInstrumentationTest {
    @Test fun modalCornersShareLargeRadiusAndPreserveContentInsets() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val originalRadius = ForkPanelSettings.radiusDp()
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                ForkPanelSettings.setRadiusDp(26)
                val dialog = AlertDialog(activity, 0)
                val sheet = BottomSheet(activity, false)
                for ((name, host, type, resource) in listOf(
                    arrayOf("dialog", dialog, AlertDialog::class.java, R.drawable.popup_fixed_alert4),
                    arrayOf("sheet", sheet, BottomSheet::class.java, R.drawable.sheet_shadow_round)
                )) {
                    val field = (type as Class<*>).getDeclaredField("shadowDrawable").apply { isAccessible = true }
                    val drawable = field.get(host) as Drawable
                    val padding = Rect()
                    val stockPadding = Rect()
                    drawable.getPadding(padding)
                    activity.resources.getDrawable(resource as Int).getPadding(stockPadding)
                    assertEquals("$name changed existing content insets", stockPadding, padding)
                    val bitmap = Bitmap.createBitmap(dp(320f), dp(200f), Bitmap.Config.ARGB_8888)
                    try {
                        drawable.setBounds(0, 0, bitmap.width, bitmap.height)
                        drawable.draw(Canvas(bitmap))
                        assertTrue("$name still has tight legacy corners", Color.alpha(bitmap.getPixel(
                            padding.left + dp(10f), padding.top + dp(4f))) < 128)
                        assertTrue("$name lost its readable fill", Color.alpha(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)) > 200)
                        java.io.File(activity.getExternalFilesDir(null), "berrygram-200-$name-shape.png")
                            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } finally { bitmap.recycle() }
                }
            }
        } finally { instrumentation.runOnMainSync { ForkPanelSettings.setRadiusDp(originalRadius); activity.finish() } }
    }
}
