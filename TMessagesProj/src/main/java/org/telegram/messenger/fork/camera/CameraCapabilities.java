package org.telegram.messenger.fork.camera;

import android.content.Context;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.util.Range;
import android.util.Size;

import org.telegram.messenger.ApplicationLoader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Read-only Camera2 capability discovery used by the Adaptive Camera Engine.
 *
 * <p>It never opens a camera and caches immutable descriptors. Callers must
 * still validate a requested stream combination before creating a session.</p>
 */
public final class CameraCapabilities {
    private static volatile List<Descriptor> cached;
    private static volatile long cachedAt;

    private CameraCapabilities() { }

    public static final class Descriptor {
        public final String id;
        public final int facing;
        public final int hardwareLevel;
        public final Size[] previewSizes;
        public final Range<Integer>[] fpsRanges;
        public final int[] focusModes;
        public final int[] videoStabilizationModes;
        public final int[] opticalStabilizationModes;
        public final float maxZoom;
        public final boolean flash;
        public final int sensorOrientation;
        public final Range<Integer> exposureCompensation;

        Descriptor(String id, int facing, int hardwareLevel, Size[] previewSizes,
                   Range<Integer>[] fpsRanges, int[] focusModes,
                   int[] videoStabilizationModes, int[] opticalStabilizationModes,
                   float maxZoom, boolean flash, int sensorOrientation,
                   Range<Integer> exposureCompensation) {
            this.id = id;
            this.facing = facing;
            this.hardwareLevel = hardwareLevel;
            this.previewSizes = previewSizes == null ? new Size[0] : previewSizes;
            this.fpsRanges = fpsRanges == null ? new Range[0] : fpsRanges;
            this.focusModes = focusModes == null ? new int[0] : focusModes;
            this.videoStabilizationModes = videoStabilizationModes == null
                    ? new int[0] : videoStabilizationModes;
            this.opticalStabilizationModes = opticalStabilizationModes == null
                    ? new int[0] : opticalStabilizationModes;
            this.maxZoom = maxZoom;
            this.flash = flash;
            this.sensorOrientation = sensorOrientation;
            this.exposureCompensation = exposureCompensation;
        }

        public boolean front() {
            return facing == CameraCharacteristics.LENS_FACING_FRONT;
        }

        public boolean supportsFps(int fps) {
            for (Range<Integer> range : fpsRanges) {
                if (range != null && range.contains(fps)) return true;
            }
            return false;
        }

        public boolean supports60Fps() {
            return supportsFps(60);
        }

        public boolean hasOpticalStabilization() {
            for (int mode : opticalStabilizationModes) {
                if (mode == CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) return true;
            }
            return false;
        }

        public boolean hasVideoStabilization() {
            for (int mode : videoStabilizationModes) {
                if (mode == CameraCharacteristics.CONTROL_VIDEO_STABILIZATION_MODE_ON) return true;
            }
            return false;
        }

        public String hardwareLevelName() {
            switch (hardwareLevel) {
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY:
                    return "LEGACY";
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED:
                    return "LIMITED";
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL:
                    return "FULL";
                case CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3:
                    return "LEVEL_3";
                default:
                    return "UNKNOWN";
            }
        }

        /** Compact, human-readable capability statement for settings. */
        public String summary() {
            StringBuilder builder = new StringBuilder();
            builder.append(front() ? "Front" : "Rear");
            builder.append(" · ").append(hardwareLevelName());
            builder.append(" · ").append(supports60Fps() ? "up to 60 fps" : "up to 30 fps");
            if (hasOpticalStabilization()) builder.append(" · OIS");
            else if (hasVideoStabilization()) builder.append(" · EIS");
            if (maxZoom > 1f) builder.append(" · zoom ").append(String.format(java.util.Locale.US,
                    "%.1fx", maxZoom));
            if (flash) builder.append(" · flash");
            return builder.toString();
        }
    }

    public static List<Descriptor> probe(Context context) {
        List<Descriptor> value = cached;
        if (value != null) return value;
        synchronized (CameraCapabilities.class) {
            value = cached;
            if (value != null) return value;
            value = Collections.unmodifiableList(discover(context));
            cached = value;
            cachedAt = android.os.SystemClock.elapsedRealtime();
            return value;
        }
    }

    public static List<Descriptor> probe() {
        return probe(ApplicationLoader.applicationContext);
    }

    public static void invalidate() {
        synchronized (CameraCapabilities.class) {
            cached = null;
            cachedAt = 0;
        }
    }

    public static long cachedAt() {
        return cachedAt;
    }

    private static List<Descriptor> discover(Context context) {
        ArrayList<Descriptor> result = new ArrayList<>();
        if (context == null) return result;
        CameraManager manager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        if (manager == null) return result;
        try {
            for (String id : manager.getCameraIdList()) {
                try {
                    CameraCharacteristics characteristics =
                            manager.getCameraCharacteristics(id);
                    Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                    Integer level = characteristics.get(
                            CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
                    StreamConfigurationMap map = characteristics.get(
                            CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                    Size[] sizes = map == null ? null
                            : map.getOutputSizes(android.graphics.SurfaceTexture.class);
                    Range<Integer>[] fps = characteristics.get(
                            CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
                    int[] focus = characteristics.get(
                            CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
                    int[] videoStab = characteristics.get(
                            CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES);
                    int[] opticalStab = characteristics.get(
                            CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION);
                    Float zoom = characteristics.get(
                            CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
                    Boolean flash = characteristics.get(
                            CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer orientation = characteristics.get(
                            CameraCharacteristics.SENSOR_ORIENTATION);
                    Range<Integer> compensation = characteristics.get(
                            CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
                    result.add(new Descriptor(id,
                            facing == null ? CameraCharacteristics.LENS_FACING_BACK : facing,
                            level == null ? CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY : level,
                            sizes, fps, focus, videoStab, opticalStab,
                            zoom == null ? 1f : zoom,
                            Boolean.TRUE.equals(flash),
                            orientation == null ? 0 : orientation,
                            compensation));
                } catch (RuntimeException ignored) {
                    // A vendor may advertise an id that it cannot describe.
                }
            }
        } catch (CameraAccessException | RuntimeException ignored) {
            // No camera service or permission; the caller keeps Telegram's fallback.
        }
        return result;
    }

    public static String describeAll() {
        List<Descriptor> descriptors = probe();
        if (descriptors.isEmpty()) return "No Camera2 characteristics available";
        StringBuilder builder = new StringBuilder();
        for (Descriptor descriptor : descriptors) {
            if (builder.length() > 0) builder.append('\n');
            builder.append("#").append(descriptor.id).append(": ").append(descriptor.summary());
        }
        return builder.toString();
    }
}
