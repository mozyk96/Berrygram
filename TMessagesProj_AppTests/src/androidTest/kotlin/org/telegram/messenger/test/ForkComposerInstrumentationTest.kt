package org.telegram.messenger.test

import android.content.Intent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.view.Gravity
import android.view.View
import android.view.MotionEvent
import android.view.WindowInsets
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.FrameLayout
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.AndroidUtilities.dp
import org.telegram.messenger.MessagesController
import org.telegram.messenger.fork.panels.ForkComposerSettings
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.Components.ChatActivityEnterView
import org.telegram.ui.Components.SizeNotifierFrameLayout
import org.telegram.ui.Components.chat.ChatInputViewsContainer
import org.telegram.ui.Components.inset.WindowInsetsProvider
import org.telegram.ui.Components.blur3.drawable.BlurredBackgroundDrawable
import org.telegram.utils.glass.source.GlassSourceColor
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import org.telegram.ui.Components.inset.WindowInsetsStateHolder

@RunWith(AndroidJUnit4::class)
class ForkComposerInstrumentationTest {
    @Test fun focusedComposerRemainsAboveRealIme() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val original = ForkComposerSettings.isExperimentalComposerEnabled()
        var composer: ChatActivityEnterView? = null
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                ForkComposerSettings.setExperimentalComposerEnabled(true)
                activity.window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
                val root = SizeNotifierFrameLayout(activity)
                val container = ChatInputViewsContainer(activity)
                val holder = WindowInsetsStateHolder { container.checkInsets() }
                container.setWindowInsetsProvider(holder)
                val source = GlassSourceColor().apply { setColor(Color.RED) }
                container.setInputIslandBubbleDrawable(BlurredBackgroundDrawable(source))
                container.setUnderKeyboardBackgroundDrawable(BlurredBackgroundDrawable(source))
                val input = ChatActivityEnterView(activity, root, null, true).apply { shouldDrawBackground = false }
                composer = input
                container.inputIslandBubbleContainer.addView(input, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
                root.addView(container, FrameLayout.LayoutParams(-1, -1))
                ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
                    holder.setInsets(insets)
                    insets
                }
                activity.setContentView(root)
                input.editField!!.requestFocus()
            }
            instrumentation.waitForIdleSync()
            instrumentation.runOnMainSync {
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .showSoftInput(composer!!.editField, InputMethodManager.SHOW_IMPLICIT)
            }
            var visible = false
            for (attempt in 0 until 30) {
                Thread.sleep(100)
                instrumentation.runOnMainSync {
                    visible = activity.window.decorView.rootWindowInsets?.isVisible(WindowInsets.Type.ime()) == true
                }
                if (visible) break
            }
            Thread.sleep(600)
            instrumentation.runOnMainSync {
                assertTrue("The emulator IME must actually open", visible)
                val insets = activity.window.decorView.rootWindowInsets
                val screenHeight = activity.windowManager.currentWindowMetrics.bounds.height()
                val pos = IntArray(2)
                composer!!.getLocationOnScreen(pos)
                assertTrue("Composer overlaps the visible keyboard",
                    pos[1] + composer!!.height <= screenHeight - insets.getInsets(WindowInsets.Type.ime()).bottom + dp(2f))
            }
        } finally {
            instrumentation.runOnMainSync {
                (activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                    .hideSoftInputFromWindow(activity.window.decorView.windowToken, 0)
                composer?.onDestroy()
                ForkComposerSettings.setExperimentalComposerEnabled(original)
                activity.finish()
            }
        }
    }

    @Test fun nonChatInputKeepsItsExistingBackgroundWhenExperimentIsEnabled() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val original = ForkComposerSettings.isExperimentalComposerEnabled()
        var composer: ChatActivityEnterView? = null
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                ForkComposerSettings.setExperimentalComposerEnabled(true)
                val root = SizeNotifierFrameLayout(activity)
                val input = ChatActivityEnterView(activity, root, null, false)
                composer = input
                assertNotNull("Non-chat input owns its background", input.messageEditTextContainer.background)
            }
        } finally {
            instrumentation.runOnMainSync {
                composer?.onDestroy()
                ForkComposerSettings.setExperimentalComposerEnabled(original)
                activity.finish()
            }
        }
    }

    @Test fun cancelledTapDoesNotSwitchRecordingMode() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val preferences = MessagesController.getGlobalMainSettings()
        val originalMode = preferences.getBoolean("currentModeVideo", false)
        var composer: ChatActivityEnterView? = null
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                val root = SizeNotifierFrameLayout(activity)
                val input = ChatActivityEnterView(activity, root, null, true).apply { shouldDrawBackground = false }
                composer = input
                var switches = 0
                var audioCallbacks = 0
                input.setDelegate(java.lang.reflect.Proxy.newProxyInstance(
                    ChatActivityEnterView.ChatActivityEnterViewDelegate::class.java.classLoader,
                    arrayOf(ChatActivityEnterView.ChatActivityEnterViewDelegate::class.java)
                ) { _, method, _ ->
                    if (method.name == "onSwitchRecordMode") switches++
                    if (method.name == "needStartRecordAudio") audioCallbacks++
                    when (method.returnType) {
                        java.lang.Boolean.TYPE -> false
                        java.lang.Integer.TYPE -> 0
                        java.lang.Long.TYPE -> 0L
                        else -> null
                    }
                } as ChatActivityEnterView.ChatActivityEnterViewDelegate)
                root.addView(input, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
                activity.setContentView(root)
                root.measure(View.MeasureSpec.makeMeasureSpec(dp(393f), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dp(720f), View.MeasureSpec.EXACTLY))
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                ChatActivityEnterView::class.java.getDeclaredField("hasRecordVideo")
                    .apply { isAccessible = true }.setBoolean(input, true)
                val button = ChatActivityEnterView::class.java.getDeclaredField("audioVideoButtonContainer")
                    .apply { isAccessible = true }.get(input) as View
                val down = MotionEvent.obtain(0, 0, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
                val cancel = MotionEvent.obtain(0, 1, MotionEvent.ACTION_CANCEL, 10f, 10f, 0)
                try {
                    button.onTouchEvent(down)
                    button.onTouchEvent(cancel)
                    assertEquals("A cancelled tap cannot switch recording mode", 0, switches)
                } finally { down.recycle(); cancel.recycle() }
                val nextDown = MotionEvent.obtain(2, 2, MotionEvent.ACTION_DOWN, 10f, 10f, 0)
                val up = MotionEvent.obtain(2, 3, MotionEvent.ACTION_UP, 10f, 10f, 0)
                try {
                    button.onTouchEvent(nextDown)
                    button.onTouchEvent(up)
                    assertEquals("A completed tap switches recording mode once", 1, switches)
                } finally { nextDown.recycle(); up.recycle() }
                ChatActivityEnterView::class.java.getDeclaredField("hasRecordVideo")
                    .apply { isAccessible = true }.setBoolean(input, false)
                ChatActivityEnterView::class.java.getDeclaredField("calledRecordRunnable")
                    .apply { isAccessible = true }.setBoolean(input, true)
                val inactiveCancel = MotionEvent.obtain(4, 4, MotionEvent.ACTION_CANCEL, 10f, 10f, 0)
                try {
                    button.onTouchEvent(inactiveCancel)
                    assertEquals("Cancelling without an active recording must not finish audio", 0, audioCallbacks)
                } finally { inactiveCancel.recycle() }
            }
        } finally { instrumentation.runOnMainSync {
            composer?.onDestroy()
            preferences.edit().putBoolean("currentModeVideo", originalMode).commit()
            activity.finish()
        } }
    }
    @Test fun separateComposerBackdropLeavesWallpaperVisibleInTheGap() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val original = ForkComposerSettings.isExperimentalComposerEnabled()
        var composer: ChatActivityEnterView? = null
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                ForkComposerSettings.setExperimentalComposerEnabled(true)
                val root = SizeNotifierFrameLayout(activity)
                val container = ChatInputViewsContainer(activity)
                container.setWindowInsetsProvider(object : WindowInsetsProvider {
                    override fun getAnimatedMaxBottomInset() = 0f
                    override fun getAnimatedImeBottomInset() = 0f
                    override fun getAnimatedKeyboardVisibility() = 0f
                    override fun getCurrentMaxBottomInset() = 0
                    override fun getInsets(type: Int) = Insets.NONE
                    override fun inAppViewIsVisible() = false
                    override fun getInAppKeyboardRecommendedViewHeight() = 0
                })
                val source = GlassSourceColor().apply { setColor(Color.RED) }
                container.setInputIslandBubbleDrawable(BlurredBackgroundDrawable(source))
                container.setUnderKeyboardBackgroundDrawable(BlurredBackgroundDrawable(source))
                val input = ChatActivityEnterView(activity, root, null, true).apply { shouldDrawBackground = false }
                input.setDelegate(java.lang.reflect.Proxy.newProxyInstance(
                    ChatActivityEnterView.ChatActivityEnterViewDelegate::class.java.classLoader,
                    arrayOf(ChatActivityEnterView.ChatActivityEnterViewDelegate::class.java)
                ) { _, method, _ ->
                    when (method.returnType) {
                        java.lang.Boolean.TYPE -> false
                        java.lang.Integer.TYPE -> 0
                        java.lang.Long.TYPE -> 0L
                        else -> null
                    }
                } as ChatActivityEnterView.ChatActivityEnterViewDelegate)
                composer = input
                container.inputIslandBubbleContainer.addView(input,
                    FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { leftMargin = dp(7f); rightMargin = dp(7f) })
                root.addView(container, FrameLayout.LayoutParams(-1, -1))
                activity.setContentView(root)
                root.measure(View.MeasureSpec.makeMeasureSpec(dp(393f), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dp(720f), View.MeasureSpec.EXACTLY))
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                container.setInputBubbleHeight(input.height.toFloat())
                container.checkInsets()
                val field = Rect()
                input.messageEditTextContainer.getDrawingRect(field)
                container.offsetDescendantRectToMyCoords(input.messageEditTextContainer, field)
                val bitmap = Bitmap.createBitmap(container.width, container.height, Bitmap.Config.ARGB_8888)
                try {
                    container.draw(Canvas(bitmap))
                    assertEquals("Shared backdrop must not bridge the independent controls", 0,
                        Color.alpha(bitmap.getPixel(field.right + dp(2f), field.bottom - dp(25f))))
                    input.visibility = View.INVISIBLE
                    bitmap.eraseColor(Color.TRANSPARENT)
                    container.draw(Canvas(bitmap))
                    assertTrue("Hidden composer must not narrow the channel-action surface",
                        Color.alpha(bitmap.getPixel(container.width - dp(20f), field.bottom - dp(25f))) > 0)
                } finally { bitmap.recycle() }
            }
        } finally {
            instrumentation.runOnMainSync {
                composer?.onDestroy()
                ForkComposerSettings.setExperimentalComposerEnabled(original)
                activity.finish()
            }
        }
    }

    @Test fun separateRecorderFitsMeasuredComposerAndCentersItsIcon() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val original = ForkComposerSettings.isExperimentalComposerEnabled()
        var composer: ChatActivityEnterView? = null
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                ForkComposerSettings.setExperimentalComposerEnabled(true)
                val root = SizeNotifierFrameLayout(activity)
                val input = ChatActivityEnterView(activity, root, null, true).apply { shouldDrawBackground = false }
                input.setDelegate(java.lang.reflect.Proxy.newProxyInstance(
                    ChatActivityEnterView.ChatActivityEnterViewDelegate::class.java.classLoader,
                    arrayOf(ChatActivityEnterView.ChatActivityEnterViewDelegate::class.java)
                ) { _, method, _ -> when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    java.lang.Long.TYPE -> 0L
                    else -> null
                } } as ChatActivityEnterView.ChatActivityEnterViewDelegate)
                composer = input
                root.addView(input, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
                activity.setContentView(root)
                fun field(name: String) = ChatActivityEnterView::class.java.getDeclaredField(name)
                    .apply { isAccessible = true }.get(input) as View
                for (width in listOf(360, 393, 412, 600)) {
                    root.layoutDirection = if (width == 600) View.LAYOUT_DIRECTION_RTL else View.LAYOUT_DIRECTION_LTR
                    for (text in listOf("", "First line\nSecond line\nThird line",
                        "Українська 🇺🇦\nРусский\nEnglish\nالعربية\n".repeat(8))) {
                        input.setFieldText(text)
                        input.checkSendButton(false)
                        assertEquals("Send state must follow the actual draft", text.isNotBlank(), input.isSendButtonVisible)
                        root.measure(View.MeasureSpec.makeMeasureSpec(dp(width.toFloat()), View.MeasureSpec.EXACTLY),
                            View.MeasureSpec.makeMeasureSpec(dp(720f), View.MeasureSpec.EXACTLY))
                        root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                        val action = field("audioVideoButtonContainer")
                        val icon = field("audioVideoSendButton")
                        val actions = field("sendButtonContainer")
                        val actionLocation = IntArray(2)
                        val rootLocation = IntArray(2)
                        action.getLocationInWindow(actionLocation)
                        root.getLocationInWindow(rootLocation)
                        val anchorMethod = ChatActivityEnterView::class.java
                            .getDeclaredMethod("getRecordingAnchorX", View::class.java)
                            .apply { isAccessible = true }
                        val anchor = anchorMethod.invoke(input, root) as Int
                        assertEquals("Recording overlay must follow the actual control",
                            actionLocation[0] + action.width / 2 - rootLocation[0], anchor)
                        assertEquals("Recorder must be circular", action.width, action.height)
                        assertTrue("Recorder touch target must be at least 48dp", action.width >= dp(48f))
                        assertTrue("Recorder must have an accessibility label", !action.contentDescription.isNullOrBlank())
                        assertTrue("Measured input must contain recorder and margins at ${width}dp",
                            input.measuredHeight >= actions.height + (actions.layoutParams as FrameLayout.LayoutParams).bottomMargin)
                        assertEquals("Recorder icon horizontal center", action.width / 2f,
                            icon.left + icon.width / 2f, 1f)
                        assertEquals("Recorder icon vertical center", action.height / 2f,
                            icon.top + icon.height / 2f, 1f)
                        assertTrue("Text input and recorder must be separated",
                            input.messageEditTextContainer.right + dp(4f) <= actions.left + action.left)
                        assertEquals("Draft text must survive layout", text, input.editField!!.text.toString())
                    }
                }
                ForkComposerSettings.setExperimentalComposerEnabled(false)
                input.updateExperimentalComposerStyle()
                assertEquals("Classic recorder width must restore", dp(ChatActivityEnterView.DEFAULT_HEIGHT.toFloat()),
                    field("audioVideoButtonContainer").layoutParams.width)
            }
        } finally {
            instrumentation.runOnMainSync {
                composer?.onDestroy()
                ForkComposerSettings.setExperimentalComposerEnabled(original)
                activity.finish()
            }
        }
    }

    @Test fun renderComposerSnapshotForVisualAudit() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext,
            SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val original = ForkComposerSettings.isExperimentalComposerEnabled()
        var composer: ChatActivityEnterView? = null
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                ForkComposerSettings.setExperimentalComposerEnabled(true)
                val root = SizeNotifierFrameLayout(activity)
                val container = ChatInputViewsContainer(activity)
                container.setWindowInsetsProvider(object : WindowInsetsProvider {
                    override fun getAnimatedMaxBottomInset() = 0f
                    override fun getAnimatedImeBottomInset() = 0f
                    override fun getAnimatedKeyboardVisibility() = 0f
                    override fun getCurrentMaxBottomInset() = 0
                    override fun getInsets(type: Int) = Insets.NONE
                    override fun inAppViewIsVisible() = false
                    override fun getInAppKeyboardRecommendedViewHeight() = 0
                })
                val plate = androidx.core.graphics.ColorUtils.setAlphaComponent(
                    Theme.getColor(Theme.key_windowBackgroundWhite), 235)
                val source = GlassSourceColor().apply { setColor(plate) }
                container.setInputIslandBubbleDrawable(BlurredBackgroundDrawable(source))
                container.setUnderKeyboardBackgroundDrawable(BlurredBackgroundDrawable(source))
                val input = ChatActivityEnterView(activity, root, null, true).apply { shouldDrawBackground = false }
                input.setDelegate(java.lang.reflect.Proxy.newProxyInstance(
                    ChatActivityEnterView.ChatActivityEnterViewDelegate::class.java.classLoader,
                    arrayOf(ChatActivityEnterView.ChatActivityEnterViewDelegate::class.java)
                ) { _, method, _ -> when (method.returnType) {
                    java.lang.Boolean.TYPE -> false
                    java.lang.Integer.TYPE -> 0
                    java.lang.Long.TYPE -> 0L
                    else -> null
                } } as ChatActivityEnterView.ChatActivityEnterViewDelegate)
                composer = input
                container.inputIslandBubbleContainer.addView(input,
                    FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM).apply { leftMargin = dp(7f); rightMargin = dp(7f) })
                root.addView(container, FrameLayout.LayoutParams(-1, -1))
                activity.setContentView(root)
                input.setFieldText("Hello Berrygram\nSecond line of a real draft")
                root.measure(View.MeasureSpec.makeMeasureSpec(dp(393f), View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(dp(720f), View.MeasureSpec.EXACTLY))
                root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                container.setInputBubbleHeight(input.height.toFloat())
                container.checkInsets()
                val bitmap = Bitmap.createBitmap(container.width, container.height, Bitmap.Config.ARGB_8888)
                try {
                    container.draw(Canvas(bitmap))
                    val file = java.io.File(activity.getExternalFilesDir(null), "berrygram-1.5-composer.png")
                    file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    assertTrue("Composer snapshot must be written", file.length() > 0)

                    input.setFieldText("")
                    input.checkSendButton(false)
                    root.measure(View.MeasureSpec.makeMeasureSpec(dp(393f), View.MeasureSpec.EXACTLY),
                        View.MeasureSpec.makeMeasureSpec(dp(720f), View.MeasureSpec.EXACTLY))
                    root.layout(0, 0, root.measuredWidth, root.measuredHeight)
                    container.setInputBubbleHeight(input.height.toFloat())
                    container.checkInsets()
                    bitmap.eraseColor(Color.TRANSPARENT)
                    container.draw(Canvas(bitmap))
                    val empty = java.io.File(activity.getExternalFilesDir(null), "berrygram-1.5-composer-empty.png")
                    empty.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    assertTrue("Empty composer snapshot must be written", empty.length() > 0)
                } finally { bitmap.recycle() }
            }
        } finally {
            instrumentation.runOnMainSync {
                composer?.onDestroy()
                ForkComposerSettings.setExperimentalComposerEnabled(original)
                activity.finish()
            }
        }
    }
}
