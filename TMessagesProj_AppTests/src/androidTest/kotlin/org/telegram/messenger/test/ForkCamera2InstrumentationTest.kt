package org.telegram.messenger.test

import android.util.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.camera.Camera2Session

@RunWith(AndroidJUnit4::class)
class ForkCamera2InstrumentationTest {
    @Test fun previewSizeSelectionHandlesUnavailableOrUnmeasuredSurfaces() {
        assertNull(Camera2Session.chooseOptimalSize(null, 640, 480, false))
        assertNull(Camera2Session.chooseOptimalSize(emptyArray(), 640, 480, false))
        assertNull(Camera2Session.chooseOptimalSize(arrayOf(Size(640, 480)), 0, 480, false))
        assertEquals(Size(640, 480), Camera2Session.chooseOptimalSize(arrayOf(Size(1280, 720), Size(640, 480)), 640, 480, false))
        assertNull(Camera2Session.create(false, 0, 480))
    }
}
