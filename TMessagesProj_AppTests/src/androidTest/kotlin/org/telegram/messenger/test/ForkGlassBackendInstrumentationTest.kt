package org.telegram.messenger.test

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable
import org.telegram.utils.glass.GlassSettings
import org.telegram.utils.glass.source.GlassSourceBitmap
import org.telegram.utils.glass.source.LiquidGlassBitmapSource

@RunWith(AndroidJUnit4::class)
class ForkGlassBackendInstrumentationTest {
    @Test fun changingWrappedBitmapDoesNotKeepAStaleBackdrop() {
        org.junit.Assume.assumeTrue(Build.VERSION.SDK_INT >= 33)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val first = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
            val second = android.graphics.Bitmap.createBitmap(2, 2, android.graphics.Bitmap.Config.ARGB_8888)
            try {
                val wrapper = org.telegram.utils.glass.source.GlassSourceWrapped().apply {
                    setSource(GlassSourceBitmap().apply { setBitmap(first) })
                }
                val factory = BlurredBackgroundDrawableViewFactory(wrapper).apply { setLiquidGlassEffectAllowed(true) }
                val drawable = factory.create().apply { setGlassSettings(GlassSettings(true, true)) }
                val resolve = BlurredBackgroundDrawable::class.java.getDeclaredMethod("resolveImpl").apply { isAccessible = true }
                resolve.invoke(drawable)
                assertSame(first, (drawable.unwrappedSource as GlassSourceBitmap).bitmap)
                wrapper.setSource(GlassSourceBitmap().apply { setBitmap(second) })
                resolve.invoke(drawable)
                assertSame("Wrapped source replacement kept the previous captured bitmap", second,
                    (drawable.unwrappedSource as GlassSourceBitmap).bitmap)
            } finally { first.recycle(); second.recycle() }
        }
    }

    @Test fun eligibleBitmapSurfacesUseCurrentRefractionAndRetainBlurFallback() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            for (source in listOf(GlassSourceBitmap(), org.telegram.utils.glass.source.GlassSourceWrapped().apply {
                setSource(GlassSourceBitmap())
            })) {
            val factory = BlurredBackgroundDrawableViewFactory(source)
            factory.setLiquidGlassEffectAllowed(true)
            val drawable = factory.create()
            val pick = BlurredBackgroundDrawable::class.java.getDeclaredMethod("pickSource").apply { isAccessible = true }
            drawable.setGlassSettings(GlassSettings(true, true))
            assertEquals("All eligible bitmap surfaces must use the current Berrygram refraction backend",
                Build.VERSION.SDK_INT >= 33, pick.invoke(drawable) is LiquidGlassBitmapSource)
            drawable.setGlassSettings(GlassSettings(true, false))
            assertSame("Blur must keep its original source", source, pick.invoke(drawable))
            drawable.setGlassSettings(GlassSettings(false, false))
            assertSame("No capture must keep its static source", source, pick.invoke(drawable))
            }
        }
    }
}
