package org.telegram.messenger.test

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.telegram.messenger.fork.updater.BackgroundUpdateJob
import org.telegram.messenger.fork.updater.BackgroundUpdatePolicy

/** Explicit live-network test: a debug test package must reject the official release APK. */
@RunWith(AndroidJUnit4::class)
class ForkBackgroundUpdateInstrumentationTest {
    @Test fun mobileOnlyDoesNotTransferOnWifi() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("Live background test is opt-in", InstrumentationRegistry.getArguments().getString("live_background") == "true")
        val context = instrumentation.targetContext
        val connectivity = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        assumeTrue("Requires Wi-Fi as the active network", capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true)
        val prefs = context.getSharedPreferences("fork_updater_panel", Context.MODE_PRIVATE)
        val oldMode = BackgroundUpdateJob.mode(context)
        try {
            prefs.edit().putLong("background_last_attempt_at", 0).putLong("background_received", 0).commit()
            assertTrue("Could not schedule mobile-only job", BackgroundUpdateJob.configure(context, BackgroundUpdatePolicy.MOBILE))
            val result = instrumentation.uiAutomation.executeShellCommand("cmd jobscheduler run -f ${context.packageName} ${0x424752}").use {
                ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText()
            }
            assertTrue("Could not start scheduled job: $result", result.contains("Running job"))
            val deadline = SystemClock.elapsedRealtime() + 15_000
            while (prefs.getLong("background_last_attempt_at", 0) == 0L && SystemClock.elapsedRealtime() < deadline) Thread.sleep(100)
            assertTrue("Scheduled job did not start", prefs.getLong("background_last_attempt_at", 0) > 0)
            assertEquals("Mobile-only job transferred data on Wi-Fi", 0L, prefs.getLong("background_received", 0))
            assertTrue("Mobile-only job should not remain active on Wi-Fi", !BackgroundUpdateJob.isRunning())
        } finally {
            BackgroundUpdateJob.configure(context, oldMode)
        }
    }

    @Test fun officialApkIsDownloadedButIncompatiblePackageIsRejected() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        assumeTrue("Live background test is opt-in", InstrumentationRegistry.getArguments().getString("live_background") == "true")
        val context = instrumentation.targetContext
        assumeTrue("Requires the deliberately older test build", context.packageManager.getPackageInfo(context.packageName, 0).versionName == "1.4.1")
        val prefs = context.getSharedPreferences("fork_updater_panel", Context.MODE_PRIVATE)
        val oldMode = BackgroundUpdateJob.mode(context)
        val oldVerified = prefs.getLong("background_verified_at", 0)
        try {
            prefs.edit().putLong("background_error_at", 0).putLong("background_received", 0).putLong("background_total", 0).commit()
            assertTrue("Could not schedule background job", BackgroundUpdateJob.configure(context, BackgroundUpdatePolicy.ANY))
            val command = "cmd jobscheduler run -f ${context.packageName} ${0x424752}"
            val result = instrumentation.uiAutomation.executeShellCommand(command).use {
                ParcelFileDescriptor.AutoCloseInputStream(it).bufferedReader().readText()
            }
            assertTrue("Could not start scheduled job: $result", result.contains("Running job"))
            val deadline = SystemClock.elapsedRealtime() + 240_000
            while (prefs.getLong("background_error_at", 0) == 0L && SystemClock.elapsedRealtime() < deadline) Thread.sleep(250)
            assertTrue("Background job did not report the expected verification failure", prefs.getLong("background_error_at", 0) > 0)
            val received = prefs.getLong("background_received", 0)
            val total = prefs.getLong("background_total", 0)
            assertTrue("The full update was not downloaded", total > 0 && received == total)
            assertEquals("The update must fail package or signature verification", 2, prefs.getInt("background_error_kind", 0))
            val files = java.io.File(context.cacheDir, "berrygram-updates").listFiles() ?: emptyArray()
            assertTrue("Rejected APK remained cached", files.none { it.name.endsWith(".apk") })
            assertEquals("Incompatible APK must never become verified", oldVerified, prefs.getLong("background_verified_at", 0))
        } finally {
            BackgroundUpdateJob.configure(context, oldMode)
        }
    }
}
