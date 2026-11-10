package org.telegram.messenger.test

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNull
import org.telegram.messenger.R
import org.telegram.ui.ActionBar.BaseFragment
import org.telegram.ui.ActionBar.Theme
import org.telegram.ui.ForkArchiveActivity
import org.telegram.ui.ForkLocale
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.fork.storage.ForkArchiveManager
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Exercises a failed archive read without modifying the database file. */
@RunWith(AndroidJUnit4::class)
class ForkArchiveFailureInstrumentationTest {
    @Test fun unavailableDatabaseShowsRetryInsteadOfEmptyState() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(Intent(instrumentation.targetContext, SettingsTestHostActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val manager = ForkArchiveManager.getInstance(0)
        val field = ForkArchiveManager::class.java.getDeclaredField("database")
        field.isAccessible = true
        val original = synchronized(manager) { field.get(manager).also { field.set(manager, null) } }
        val fragment = ForkArchiveActivity()
        try {
            instrumentation.runOnMainSync {
                Theme.createCommonResources(activity)
                fragment.onFragmentCreate()
                val actionBar = fragment.createActionBar(activity)
                val actionBarField = BaseFragment::class.java.getDeclaredField("actionBar")
                actionBarField.isAccessible = true
                actionBarField.set(fragment, actionBar)
                val page = fragment.performCreateView(activity)
                val host = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                if (actionBar.shouldAddToContainer()) host.addView(actionBar)
                host.addView(page, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
                activity.setContentView(host)
                fragment.onResume()
            }
            var found = false
            val deadline = android.os.SystemClock.elapsedRealtime() + 10_000
            while (!found && android.os.SystemClock.elapsedRealtime() < deadline) {
                instrumentation.runOnMainSync {
                    fun hasRetry(view: View): Boolean {
                        if (view is TextView && view.isShown && view.isClickable && view.text.toString() == ForkLocale.get(R.string.ForkArchiveLoadError)) return true
                        if (view is ViewGroup) for (i in 0 until view.childCount) if (hasRetry(view.getChildAt(i))) return true
                        return false
                    }
                    found = hasRetry(activity.window.decorView)
                }
                if (!found) Thread.sleep(100)
            }
            assertTrue("Archive error with retry was not shown", found)
        } finally {
            synchronized(manager) { field.set(manager, original) }
            instrumentation.runOnMainSync { fragment.onPause(); fragment.onFragmentDestroy(); activity.finish() }
        }
    }

    @Test fun unavailableDatabaseIsNotReportedAsAnEmptyArchive() {
        val manager = ForkArchiveManager.getInstance(0)
        val field = ForkArchiveManager::class.java.getDeclaredField("database")
        field.isAccessible = true
        val original = synchronized(manager) { field.get(manager).also { field.set(manager, null) } }
        try {
            val latch = CountDownLatch(1)
            var result: List<ForkArchiveManager.ArchivedDialogSummary>? = null
            ForkArchiveManager.loadDialogsAsync(0) { value -> result = value; latch.countDown() }
            assertTrue("Archive callback timed out", latch.await(10, TimeUnit.SECONDS))
            assertNull("A database failure was reported as an empty archive", result)
        } finally {
            synchronized(manager) { field.set(manager, original) }
        }
    }
}
