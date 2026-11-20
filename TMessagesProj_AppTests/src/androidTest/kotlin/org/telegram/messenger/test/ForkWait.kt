package org.telegram.messenger.test

import android.app.Instrumentation
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Synchronizes a fixture with the application main thread.
 *
 * `Instrumentation.waitForIdleSync()` waits until the whole process is
 * completely idle. The production chat screen keeps its animations and its
 * loading indicators alive, so that wait never ends on a real screen and the
 * fixture hangs instead of failing. Synchronizing with the main thread
 * directly gives the same guarantee for the work the fixture scheduled and
 * cannot block forever.
 */
object ForkWait {
    fun idle(instrumentation: Instrumentation = InstrumentationRegistry.getInstrumentation()) {
        instrumentation.runOnMainSync { }
        Thread.sleep(150)
    }
}
