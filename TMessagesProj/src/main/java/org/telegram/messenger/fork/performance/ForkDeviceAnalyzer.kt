package org.telegram.messenger.fork.performance

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import org.telegram.messenger.AndroidUtilities
import org.telegram.messenger.ApplicationLoader
import org.telegram.messenger.FileLog
import java.io.RandomAccessFile
import java.util.Locale

/**
 * Hardware diagnostics and device capability scanner for Berrygram.
 * Collects hardware telemetry (CPU, GPU, RAM, Display) and computes
 * realistic graphics ratings. Written in Kotlin.
 */
object ForkDeviceAnalyzer {
    @Volatile
    private var cachedAssessment: DeviceAnalyzerPolicy.Assessment? = null

    @JvmStatic
    fun getAssessment(forceRescan: Boolean): DeviceAnalyzerPolicy.Assessment {
        if (!forceRescan) {
            val cached = cachedAssessment
            if (cached != null) {
                return cached
            }
        }

        val specs = scanDevice()
        val assessment = DeviceAnalyzerPolicy.evaluate(specs)
        cachedAssessment = assessment
        return assessment
    }

    @JvmStatic
    fun scanDevice(): DeviceAnalyzerPolicy.DeviceSpecs {
        val context = ApplicationLoader.applicationContext
        val cpuCores = Runtime.getRuntime().availableProcessors()
        val maxFreq = readMaxCpuFreq(cpuCores)
        val totalRam = readTotalRam(context)
        val apiLevel = Build.VERSION.SDK_INT
        val refreshRate = if (AndroidUtilities.screenRefreshRate > 0) AndroidUtilities.screenRefreshRate else 60.0f
        val width = AndroidUtilities.displaySize.x
        val height = AndroidUtilities.displaySize.y
        val soc = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && Build.SOC_MODEL != null) {
            Build.SOC_MODEL
        } else {
            Build.HARDWARE ?: ""
        }
        val gpu = Build.HARDWARE ?: ""

        return DeviceAnalyzerPolicy.DeviceSpecs(
            cpuCores,
            maxFreq,
            totalRam,
            apiLevel,
            refreshRate,
            width,
            height,
            gpu,
            soc
        )
    }

    private fun readMaxCpuFreq(cores: Int): Int {
        var maxFreqKhz = 0
        for (i in 0 until cores) {
            try {
                RandomAccessFile("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq", "r").use { reader ->
                    val line = reader.readLine()
                    if (line != null) {
                        val freq = line.trim().toInt()
                        if (freq > maxFreqKhz) {
                            maxFreqKhz = freq
                        }
                    }
                }
            } catch (ignored: Throwable) {
            }
        }
        if (maxFreqKhz > 0) {
            return maxFreqKhz / 1000
        }
        return 2000
    }

    private fun readTotalRam(context: Context?): Long {
        if (context != null) {
            try {
                val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
                if (actManager != null) {
                    val memInfo = ActivityManager.MemoryInfo()
                    actManager.getMemoryInfo(memInfo)
                    return memInfo.totalMem
                }
            } catch (e: Throwable) {
                FileLog.e(e)
            }
        }
        return Runtime.getRuntime().maxMemory()
    }
}
