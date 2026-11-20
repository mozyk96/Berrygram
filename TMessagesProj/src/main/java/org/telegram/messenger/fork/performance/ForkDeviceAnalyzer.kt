package org.telegram.messenger.fork.performance

import android.app.ActivityManager
import android.content.Context
import android.opengl.EGL14
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

    /** Result of the one-off offscreen probe, empty when the probe is unavailable. */
    @Volatile
    private var probedGpuRenderer: String? = null

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
        // GL_RENDERER requires a current context. A board/SoC name is not a GPU
        // renderer, so only a real renderer string or a completed probe is used.
        val gpu = currentGlRenderer().ifEmpty { probedGpuRenderer.orEmpty() }

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

    /** The renderer of the current GL context, empty when there is none. */
    private fun currentGlRenderer(): String = try {
        if (EGL14.eglGetCurrentContext() != EGL14.EGL_NO_CONTEXT) {
            android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER).orEmpty()
        } else {
            ""
        }
    } catch (error: Throwable) {
        ""
    }

    /**
     * Reads the real GPU renderer through a temporary offscreen EGL context.
     * The probe never touches the visible surface, so it is safe to run from a
     * background thread while the chat is rendered. The result is cached.
     */
    @JvmStatic
    fun probeGpuRenderer(): String {
        probedGpuRenderer?.let { return it }
        val renderer = runOffscreenProbe()
        probedGpuRenderer = renderer
        return renderer
    }

    private fun runOffscreenProbe(): String = try {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (display == EGL14.EGL_NO_DISPLAY) {
            ""
        } else {
            val version = IntArray(2)
            if (!EGL14.eglInitialize(display, version, 0, version, 1)) {
                ""
            } else {
                try {
                    val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
                    val configCount = IntArray(1)
                    val attributes = intArrayOf(
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                        EGL14.EGL_NONE
                    )
                    if (!EGL14.eglChooseConfig(display, attributes, 0, configs, 0, 1, configCount, 0)
                        || configCount[0] <= 0 || configs[0] == null) {
                        ""
                    } else {
                        val contextAttributes = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
                        val context = EGL14.eglCreateContext(display, configs[0], EGL14.EGL_NO_CONTEXT, contextAttributes, 0)
                        if (context == null || context == EGL14.EGL_NO_CONTEXT) {
                            ""
                        } else {
                            try {
                                val surfaceAttributes = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
                                val surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttributes, 0)
                                if (surface == null || surface == EGL14.EGL_NO_SURFACE) {
                                    ""
                                } else {
                                    try {
                                        if (!EGL14.eglMakeCurrent(display, surface, surface, context)) {
                                            ""
                                        } else {
                                            try {
                                                android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER).orEmpty()
                                            } finally {
                                                EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE,
                                                    EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                                            }
                                        }
                                    } finally {
                                        EGL14.eglDestroySurface(display, surface)
                                    }
                                }
                            } finally {
                                EGL14.eglDestroyContext(display, context)
                            }
                        }
                    }
                } finally {
                    EGL14.eglTerminate(display)
                }
            }
        }
    } catch (error: Throwable) {
        FileLog.e(error)
        ""
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
