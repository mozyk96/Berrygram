package org.telegram.messenger.test

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.MediaController
import org.telegram.ui.PhotoViewer
import java.io.File
import java.io.FileOutputStream

/** Opens a local synthetic image without logging in or using user media. */
@RunWith(AndroidJUnit4::class)
class ForkPhotoViewerInstrumentationTest {
    @Test fun localPhotoViewerOpensAndCloses() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val file = File(activity.cacheDir, "berrygram-viewer-test.png")
        val bitmap = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.rgb(52, 105, 155))
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val photos = arrayListOf<Any>(MediaController.PhotoEntry(0, 1, 0, file.absolutePath, 0, false, 256, 256, file.length()))
        val viewer = PhotoViewer.getInstance()
        val originalStatusHeight = AndroidUtilities.statusBarHeight
        try {
            var opened = false
            instrumentation.runOnMainSync {
                // The normal LaunchActivity initializes this through DrawerLayoutContainer.
                val statusId = activity.resources.getIdentifier("status_bar_height", "dimen", "android")
                if (AndroidUtilities.statusBarHeight == 0 && statusId != 0) AndroidUtilities.statusBarHeight = activity.resources.getDimensionPixelSize(statusId)
                viewer.setParentActivity(activity)
                opened = viewer.openPhotoForSelect(photos, 0, 0, false, PhotoViewer.EmptyPhotoViewerProvider(), null)
            }
            instrumentation.waitForIdleSync()
            Thread.sleep(1500) // Allow the viewer transition and image loading to finish.
            assertTrue("Local photo viewer did not open", opened && viewer.isVisible)
            val screenshot = instrumentation.uiAutomation.takeScreenshot()
            var imageSeen = false
            for (y in 0 until screenshot.height step 8) for (x in 0 until screenshot.width step 8) {
                val pixel = screenshot.getPixel(x, y)
                if (Color.blue(pixel) > Color.red(pixel) + 30 && Color.blue(pixel) > Color.green(pixel) + 20) imageSeen = true
            }
            assertTrue("The local image was not rendered in the viewer", imageSeen)
            instrumentation.uiAutomation.executeShellCommand("screencap -p /sdcard/Download/Berrygram-photo-viewer.png").use {
                ParcelFileDescriptor.AutoCloseInputStream(it).use { input -> input.readBytes() }
            }
        } finally {
            instrumentation.runOnMainSync { viewer.destroyPhotoViewer(); AndroidUtilities.statusBarHeight = originalStatusHeight; activity.finish() }
            bitmap.recycle()
        }
    }
}
