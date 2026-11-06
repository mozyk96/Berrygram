package org.telegram.messenger.fork.performance;

import java.util.Locale;

/**
 * Pure hardware evaluation and tier classification rules.
 * Does not depend on Android framework classes for reliable JVM testing.
 */
public final class DeviceAnalyzerPolicy {
    public static final String TIER_LOW = "low";
    public static final String TIER_LOW_MEDIUM = "low_medium";
    public static final String TIER_MEDIUM = "medium";
    public static final String TIER_HIGH = "high";

    private DeviceAnalyzerPolicy() { }

    public static final class DeviceSpecs {
        public final int cpuCores;
        public final int maxCpuFreqMhz;
        public final long totalRamBytes;
        public final int apiLevel;
        public final float refreshRate;
        public final int screenWidth;
        public final int screenHeight;
        public final String gpuRenderer;
        public final String socModel;

        public DeviceSpecs(
                int cpuCores,
                int maxCpuFreqMhz,
                long totalRamBytes,
                int apiLevel,
                float refreshRate,
                int screenWidth,
                int screenHeight,
                String gpuRenderer,
                String socModel) {
            this.cpuCores = cpuCores;
            this.maxCpuFreqMhz = maxCpuFreqMhz;
            this.totalRamBytes = totalRamBytes;
            this.apiLevel = apiLevel;
            this.refreshRate = refreshRate;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.gpuRenderer = gpuRenderer != null ? gpuRenderer : "";
            this.socModel = socModel != null ? socModel : "";
        }
    }

    public static final class Assessment {
        public final String tier;
        public final int score;
        public final String summary;
        public final String reason;

        public Assessment(String tier, int score, String summary, String reason) {
            this.tier = tier;
            this.score = score;
            this.summary = summary;
            this.reason = reason;
        }
    }

    public static Assessment evaluate(DeviceSpecs specs) {
        if (specs == null) {
            return new Assessment(TIER_LOW, 20, "Unknown Device", "Missing device specifications; safe fallback.");
        }

        int score = 0;
        StringBuilder reasons = new StringBuilder();

        // 1. RAM Score (up to 25 pts)
        long ramGb = specs.totalRamBytes / (1024L * 1024L * 1024L);
        if (ramGb >= 12) {
            score += 25;
        } else if (ramGb >= 8) {
            score += 22;
        } else if (ramGb >= 6) {
            score += 17;
        } else if (ramGb >= 4) {
            score += 12;
        } else {
            score += 5;
            reasons.append("Low system RAM (").append(ramGb).append(" GB). ");
        }

        // 2. CPU Score (up to 25 pts)
        if (specs.cpuCores >= 8) {
            if (specs.maxCpuFreqMhz >= 2800) {
                score += 25;
            } else if (specs.maxCpuFreqMhz >= 2200) {
                score += 19;
            } else {
                score += 14;
            }
        } else if (specs.cpuCores >= 6) {
            score += 14;
        } else {
            score += 6;
            reasons.append("Limited CPU cores (").append(specs.cpuCores).append("). ");
        }

        // 3. GPU Classification (up to 35 pts)
        int gpuScore = classifyGpu(specs.gpuRenderer, specs.socModel);
        score += gpuScore;
        if (gpuScore <= 15) {
            reasons.append("Entry-level GPU (").append(specs.gpuRenderer).append("). ");
        } else if (gpuScore <= 22) {
            reasons.append("Mid-range GPU (").append(specs.gpuRenderer).append("). ");
        }

        // 4. Android API version (up to 15 pts)
        if (specs.apiLevel >= 34) {
            score += 15;
        } else if (specs.apiLevel >= 31) {
            score += 12;
        } else if (specs.apiLevel >= 28) {
            score += 8;
        } else {
            score += 4;
            reasons.append("Legacy Android API level (").append(specs.apiLevel).append("). ");
        }

        // 5. Display Load Factor penalty (High refresh rate on mid-tier GPU)
        long totalPixels = (long) Math.max(1, specs.screenWidth) * Math.max(1, specs.screenHeight);
        boolean isHighRefresh = specs.refreshRate >= 90f;
        if (isHighRefresh && gpuScore <= 22) {
            // Mid-range GPU driving 90/120Hz display: penalty for fillrate stress
            int penalty = isHighRefresh && specs.refreshRate >= 115f ? 12 : 8;
            score = Math.max(20, score - penalty);
            reasons.append("High refresh rate (").append(Math.round(specs.refreshRate))
                    .append("Hz) on mid-tier GPU reduces heavy blur headroom. ");
        }

        // Final Tier classification
        String tier;
        String summary;
        if (score >= 75) {
            tier = TIER_HIGH;
            summary = "High Performance Profile";
        } else if (score >= 58) {
            tier = TIER_MEDIUM;
            summary = "Balanced Medium Profile";
        } else if (score >= 42) {
            tier = TIER_LOW_MEDIUM;
            summary = "Optimized Low-Medium Profile";
        } else {
            tier = TIER_LOW;
            summary = "Battery-Saver Low Profile";
        }

        if (reasons.length() == 0) {
            reasons.append("Capable hardware with sufficient graphical headroom.");
        }

        return new Assessment(tier, score, summary, reasons.toString().trim());
    }

    private static int classifyGpu(String renderer, String socModel) {
        String raw = (renderer + " " + socModel).toUpperCase(Locale.ENGLISH);
        String gpu = raw.replace("(TM)", "").replace("(R)", "").replaceAll("\\s+", " ").trim();

        // Flagship tier (32 - 35)
        if (gpu.contains("ADRENO 740") || gpu.contains("ADRENO 750") || gpu.contains("ADRENO 730")
                || gpu.contains("IMMORTALIS") || gpu.contains("MALI-G715") || gpu.contains("MALI-G720")
                || gpu.contains("XCLIPSE 940") || gpu.contains("XCLIPSE 920") || gpu.contains("APPLE")
                || gpu.contains("SM8550") || gpu.contains("SM8650") || gpu.contains("DIMENSITY 9")) {
            return 35;
        }
        if (gpu.contains("ADRENO 660") || gpu.contains("ADRENO 650") || gpu.contains("MALI-G78 MP14")
                || gpu.contains("MALI-G710")) {
            return 30;
        }

        // Upper mid-range (24 - 28)
        if (gpu.contains("ADRENO 642") || gpu.contains("ADRENO 640") || gpu.contains("MALI-G77")
                || gpu.contains("MALI-G78") || gpu.contains("ADRENO 710")) {
            return 25;
        }

        // Mid-range (18 - 22): e.g. Mali-G68 (Samsung A25 / Exynos 1280), Mali-G57, Adreno 618, 619
        if (gpu.contains("MALI-G68") || gpu.contains("MALI-G57") || gpu.contains("S5E8825") || gpu.contains("EXYNOS 1280")
                || gpu.contains("ADRENO 619") || gpu.contains("ADRENO 618") || gpu.contains("ADRENO 620")) {
            return 20;
        }

        // Budget / Entry (10 - 15): Mali-G52, Mali-G31, Adreno 610, 506
        if (gpu.contains("MALI-G52") || gpu.contains("MALI-G31") || gpu.contains("MALI-T")
                || gpu.contains("ADRENO 610") || gpu.contains("ADRENO 612") || gpu.contains("ADRENO 506")) {
            return 12;
        }

        // Generic fallback heuristic
        if (gpu.contains("MALI")) return 18;
        if (gpu.contains("ADRENO")) return 22;
        return 16;
    }
}
