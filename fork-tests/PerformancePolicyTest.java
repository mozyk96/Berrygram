import org.telegram.messenger.fork.performance.PerformancePolicy;

public class PerformancePolicyTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }

    public static void main(String[] args) {
        check(PerformancePolicy.AUTOMATIC.equals(PerformancePolicy.normalize(null)), "Null profile");
        check(PerformancePolicy.AUTOMATIC.equals(PerformancePolicy.normalize("bad")), "Corrupt profile");
        check(PerformancePolicy.SMOOTH.equals(PerformancePolicy.normalize("smooth")), "Smooth profile");
        check(PerformancePolicy.reduceEffects(PerformancePolicy.BATTERY, false, false, false), "Battery must reduce");
        check(PerformancePolicy.reduceEffects(PerformancePolicy.AUTOMATIC, false, true, false), "Automatic low device must reduce");
        check(!PerformancePolicy.reduceEffects(PerformancePolicy.AUTOMATIC, false, false, false), "Automatic capable device");
        check(!PerformancePolicy.reduceEffects(PerformancePolicy.SMOOTH, false, true, false), "Smooth must not reduce");
        check(PerformancePolicy.reduceEffects(PerformancePolicy.SMOOTH, true, false, false), "System power saver wins");
        check(PerformancePolicy.reduceEffects(PerformancePolicy.CUSTOM, false, false, true), "Custom reduce toggle");
        check(!PerformancePolicy.reduceEffects(PerformancePolicy.CUSTOM, false, true, false), "Custom keeps device capable");
        System.out.println("PerformancePolicyTest passed (automatic, smooth, battery, custom, power saver)");
    }
}
