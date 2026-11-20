package org.telegram.messenger.test

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.R
import org.telegram.messenger.fork.panels.ForkPanelSettings
import org.telegram.ui.ActionBar.ActionBar
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.ChatAvatarContainer
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory
import org.telegram.ui.Components.blur3.drawable.color.BlurredBackgroundColorProviderThemed
import org.telegram.ui.ForkGraphicsSurface
import org.telegram.utils.glass.source.GlassSourceColor

@RunWith(AndroidJUnit4::class)
class ForkChatHeaderInstrumentationTest {
    @Test fun titleAndStatusStayInsideTheCapsuleBelowTheStatusBar() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val original = org.telegram.messenger.fork.panels.ForkChatHeaderSettings.isIosStyle()
        try {
            instrumentation.runOnMainSync {
                val previousInset = AndroidUtilities.statusBarHeight
                try {
                    AndroidUtilities.statusBarHeight = dp(32f)
                    org.telegram.messenger.fork.panels.ForkChatHeaderSettings.setIosStyle(true)
                    Theme.createCommonResources(activity)
                    val bar = ActionBar(activity).apply {
                        setOccupyStatusBar(true)
                        setBackButtonImage(R.drawable.ic_ab_back)
                        compactChatGlass = true
                    }
                    val avatar = ChatAvatarContainer(activity, null, false).apply {
                        setOccupyStatusBar(true)
                        setGlassMode()
                        setTitle("Тестова група")
                        setSubtitle("12 учасників")
                        setTitleColors(Color.MAGENTA, Color.GREEN)
                    }
                    bar.addView(avatar, FrameLayout.LayoutParams(-1, -1))
                    bar.setForkHeaderAvatar(avatar)
                    val menu = bar.createMenu().apply { addItem(1, R.drawable.ic_ab_other) }
                    bar.measure(View.MeasureSpec.makeMeasureSpec(dp(393f), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(dp(88f), View.MeasureSpec.EXACTLY))
                    bar.layout(0, 0, bar.measuredWidth, bar.measuredHeight)
                    assertTrue(bar.usesBerrygramChatHeader())
                    val bitmap = Bitmap.createBitmap(bar.width, bar.height, Bitmap.Config.ARGB_8888)
                    try {
                        bar.draw(Canvas(bitmap))
                        fun hasTextColor(color: Int): Boolean {
                            for (y in menu.top until menu.bottom) for (x in avatar.left until avatar.right) {
                                if (bitmap.getPixel(x, y) == color) return true
                            }
                            return false
                        }
                        assertTrue("Title must paint inside the row below the status bar", hasTextColor(Color.MAGENTA))
                        assertTrue("Status must paint inside the same row", hasTextColor(Color.GREEN))
                    } finally {
                        bitmap.recycle()
                    }
                } finally {
                    AndroidUtilities.statusBarHeight = previousInset
                }
            }
        } finally {
            instrumentation.runOnMainSync {
                org.telegram.messenger.fork.panels.ForkChatHeaderSettings.setIosStyle(original)
                activity.finish()
            }
        }
    }

    @Test fun chatActionsShareOneSurfaceAtNarrowAndWideWidths() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val original = org.telegram.messenger.fork.panels.ForkChatHeaderSettings.isIosStyle()
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                val bar = ActionBar(activity).apply {
                    setOccupyStatusBar(false)
                    setBackButtonImage(R.drawable.ic_ab_back)
                    compactChatGlass = true
                }
                val avatar = ChatAvatarContainer(activity, null, false).apply {
                    setOccupyStatusBar(false)
                    setGlassMode()
                    setTitle("Довга назва 👩🏽‍💻 Україна — 日本語")
                    setSubtitle("був(ла) нещодавно")
                }
                bar.addView(avatar, FrameLayout.LayoutParams(-1, -1))
                bar.setForkHeaderAvatar(avatar)
                val menu = bar.createMenu().apply {
                    addItem(1, R.drawable.msg_calls)
                    addItem(2, R.drawable.ic_ab_other)
                }
                val selection = bar.createActionMode()
                assertEquals(View.INVISIBLE, selection.visibility)
                assertTrue("A hidden selection bar must not disable the chat header", bar.usesBerrygramChatHeader())
                val source = GlassSourceColor().apply { setColor(Color.RED) }
                bar.setupGlass(BlurredBackgroundDrawableViewFactory(source),
                    BlurredBackgroundColorProviderThemed(null, Theme.key_actionBarDefault, 1f))
                for (width in listOf(320f, 393f, 600f)) {
                    org.telegram.messenger.fork.panels.ForkChatHeaderSettings.setIosStyle(true)
                    bar.measure(View.MeasureSpec.makeMeasureSpec(dp(width), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(dp(56f), View.MeasureSpec.EXACTLY))
                    bar.layout(0, 0, bar.measuredWidth, bar.measuredHeight)
                    assertTrue(bar.usesBerrygramChatHeader())
                    assertTrue("Header overlaps back control", avatar.left >= dp(56f))
                    assertTrue("Header overlaps actions", avatar.right <= menu.left - dp(6f))
                    val bitmap = Bitmap.createBitmap(bar.width, bar.height, Bitmap.Config.ARGB_8888)
                    try {
                        bar.draw(Canvas(bitmap))
                        for (x in avatar.right + dp(2f) until menu.left - dp(2f)) {
                            for (y in dp(10f) until dp(28f)) {
                                assertTrue("Title painted outside its island over the action gap",
                                    Color.alpha(bitmap.getPixel(x, y)) < 128)
                            }
                        }
                        val field = ActionBar::class.java.getDeclaredField("glassDrawableMenu").apply { isAccessible = true }
                        val drawable = field.get(bar) as org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable
                        val bounds = android.graphics.Rect()
                        drawable.getBoundsWithPaddingInset(bounds)
                        assertEquals("Both actions must share the same capsule",
                            menu.getItem(2).right - menu.getItem(1).left, bounds.width())
                        assertEquals(dp(44f), bounds.height())
                        val callRect = android.graphics.RectF()
                        assertTrue("Opening the popup must leave other actions visible",
                            !bar.getMenuItemGlassRect(menu.getItem(1), callRect))
                        assertEquals(menu.x + menu.getItem(1).x, callRect.left, 1f)
                        assertEquals(dp(44f).toFloat(), callRect.width(), 1f)
                        if (width == 393f) java.io.File(activity.getExternalFilesDir(null), "berrygram-200-ios-header.png")
                            .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    } finally { bitmap.recycle() }
                    org.telegram.messenger.fork.panels.ForkChatHeaderSettings.setIosStyle(false)
                    assertTrue("The retired preference must not switch the layout", bar.usesBerrygramChatHeader())
                }
                bar.setSearchFactor(1f)
                assertTrue("Search owns the header while active", !bar.usesBerrygramChatHeader())
                bar.setSearchFactor(0f)
                assertTrue(bar.usesBerrygramChatHeader())
                selection.alpha = 1f
                selection.visibility = View.VISIBLE
                assertTrue("Selection owns the header while active", !bar.usesBerrygramChatHeader())
                selection.visibility = View.INVISIBLE
                assertTrue(bar.usesBerrygramChatHeader())
            }
        } finally {
            instrumentation.runOnMainSync { org.telegram.messenger.fork.panels.ForkChatHeaderSettings.setIosStyle(original); activity.finish() }
        }
    }

    @Test fun chatAvatarAndTextFitBetweenBackAndActions() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                val bar = ActionBar(activity).apply {
                    setOccupyStatusBar(false)
                    setBackButtonImage(R.drawable.ic_ab_back)
                }
                val avatar = ChatAvatarContainer(activity, null, false).apply {
                    setOccupyStatusBar(false)
                    setGlassMode()
                    setTitle("Дуже довга назва розмови для перевірки ширини")
                    setSubtitle("був(ла) нещодавно")
                }
                bar.addView(avatar, FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT).apply {
                    leftMargin = dp(52f)
                    rightMargin = dp(100f)
                })
                val menu = bar.createMenu().apply {
                    addItem(1, R.drawable.msg_calls)
                    addItem(2, R.drawable.ic_ab_other)
                }
                for (width in listOf(320f, 360f, 393f)) {
                    bar.measure(View.MeasureSpec.makeMeasureSpec(dp(width), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(dp(56f), View.MeasureSpec.EXACTLY))
                    bar.layout(0, 0, bar.measuredWidth, bar.measuredHeight)
                    assertTrue("Avatar must be beside back control", avatar.left >= dp(48f))
                    assertTrue("Title must be after avatar",
                        avatar.getTitleTextView().left >= avatar.avatarImageView.right)
                    assertTrue("Title must leave room for actions",
                        avatar.left + avatar.getTitleTextView().right <= bar.width - dp(100f))
                    assertTrue("Title must not overlap the action menu",
                        avatar.left + avatar.getTitleTextView().right <= menu.left)
                    assertTrue("Status must be below title",
                        avatar.getSubtitleTextView().top > avatar.getTitleTextView().top)
                    if (width == 360f) {
                        val bitmap = Bitmap.createBitmap(bar.width, bar.height, Bitmap.Config.ARGB_8888)
                        try {
                            bar.draw(Canvas(bitmap))
                            java.io.File(activity.getExternalFilesDir(null), "berrygram-151-header-fixture.png")
                                .outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        } finally { bitmap.recycle() }
                    }
                }
            }
        } finally { instrumentation.runOnMainSync { activity.finish() } }
    }

    @Test fun telegramHeaderKeepsSeparatedSurfacesAfterDrawableRecreation() {
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
                        assertTrue("Circular back control must keep its rounded corner at $width dp",
                            Color.alpha(bitmap.getPixel(dp(8f), dp(5f))) < 128)
                        assertTrue("Back and title islands must remain separated at $width dp",
                            Color.alpha(bitmap.getPixel(dp(55f), bar.height / 2)) < 128)
                        assertTrue("Title island must retain a readable surface", Color.alpha(
                            bitmap.getPixel(dp(80f), bar.height / 2)) > 128)
                    } finally { bitmap.recycle() }
                }
                // Recreating a chat view may reuse its ActionBar and replace the drawable.
                bar.setupGlass(BlurredBackgroundDrawableViewFactory(source),
                    BlurredBackgroundColorProviderThemed(null, Theme.key_actionBarDefault, 1f))
                val recreated = Bitmap.createBitmap(bar.width, bar.height, Bitmap.Config.ARGB_8888)
                try {
                    bar.draw(Canvas(recreated))
                    assertTrue("Recreated chat back control must remain circular",
                        Color.alpha(recreated.getPixel(dp(8f), dp(5f))) < 128)
                } finally { recreated.recycle() }
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
                        Color.alpha(plain.getPixel(dp(80f), bar.height - dp(12f))))
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
                        glass.getPixel(dp(80f), bar.height - dp(12f))) > 0)
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
