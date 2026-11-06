import org.telegram.messenger.fork.performance.DeviceAnalyzerPolicy;

public class DeviceAnalyzerPolicyTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) {
        // 1. Samsung Galaxy A25 (SM-A256B) benchmark
        // Specs: 8 cores, 2400 MHz max freq, 6 GB RAM, API 34, 120Hz refresh, Mali-G68 MP4, Exynos 1280 (s5e8825)
        DeviceAnalyzerPolicy.DeviceSpecs a25Specs = new DeviceAnalyzerPolicy.DeviceSpecs(
                8,
                2400,
                6L * 1024L * 1024L * 1024L,
                34,
                120.0f,
                1080,
                2340,
                "Mali-G68",
                "s5e8825"
        );
        DeviceAnalyzerPolicy.Assessment a25Assessment = DeviceAnalyzerPolicy.evaluate(a25Specs);

        // MUST NOT be high! Must be medium or low-medium
        check(!DeviceAnalyzerPolicy.TIER_HIGH.equals(a25Assessment.tier),
                "Galaxy A25 must not be classified as High (actual: " + a25Assessment.tier + ", score: " + a25Assessment.score + ")");
        check(DeviceAnalyzerPolicy.TIER_MEDIUM.equals(a25Assessment.tier) || DeviceAnalyzerPolicy.TIER_LOW_MEDIUM.equals(a25Assessment.tier),
                "Galaxy A25 should be classified as Medium or Low-Medium (got: " + a25Assessment.tier + ")");
        check(a25Assessment.reason.contains("refresh rate") || a25Assessment.reason.contains("Mid-range"),
                "Assessment reason should explain refresh rate or mid-range GPU factor");

        // 2. High-end flagship: Snapdragon 8 Gen 2 / Adreno 740, 12GB RAM, 120Hz
        DeviceAnalyzerPolicy.DeviceSpecs flagshipSpecs = new DeviceAnalyzerPolicy.DeviceSpecs(
                8,
                3200,
                12L * 1024L * 1024L * 1024L,
                34,
                120.0f,
                1440,
                3120,
                "Adreno (TM) 740",
                "SM8550"
        );
        DeviceAnalyzerPolicy.Assessment flagshipAssessment = DeviceAnalyzerPolicy.evaluate(flagshipSpecs);
        check(DeviceAnalyzerPolicy.TIER_HIGH.equals(flagshipAssessment.tier),
                "Flagship specs must be classified as High (got: " + flagshipAssessment.tier + ")");

        // 3. Entry-level device: Mali-G52, 3GB RAM, 60Hz, API 29
        DeviceAnalyzerPolicy.DeviceSpecs lowEndSpecs = new DeviceAnalyzerPolicy.DeviceSpecs(
                4,
                1600,
                3L * 1024L * 1024L * 1024L,
                29,
                60.0f,
                720,
                1600,
                "Mali-G52",
                "Helio G80"
        );
        DeviceAnalyzerPolicy.Assessment lowEndAssessment = DeviceAnalyzerPolicy.evaluate(lowEndSpecs);
        check(DeviceAnalyzerPolicy.TIER_LOW.equals(lowEndAssessment.tier) || DeviceAnalyzerPolicy.TIER_LOW_MEDIUM.equals(lowEndAssessment.tier),
                "Entry level must be classified as Low or Low-Medium (got: " + lowEndAssessment.tier + ")");

        System.out.println("DeviceAnalyzerPolicyTest passed");
    }
}
