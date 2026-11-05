import android.content.Context;
import android.os.PowerManager;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.fork.performance.PerformancePolicy;
import org.telegram.messenger.fork.performance.PerformanceSettings;

public class PerformanceSettingsTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }

    private static final class PowerContext extends Context {
        PowerManager power = new PowerManager();
        @Override public Object getSystemService(String name) {
            return POWER_SERVICE.equals(name) ? power : null;
        }
    }

    public static void main(String[] args) {
        PowerContext context = new PowerContext();
        ApplicationLoader.applicationContext = context;
        check(PerformancePolicy.AUTOMATIC.equals(PerformanceSettings.profile()), "Default profile");
        check(PerformanceSettings.reduceEffects() == (PerformanceSettings.systemPowerSaver() || org.telegram.messenger.SharedConfig.deviceIsLow()), "Automatic resolution");

        PerformanceSettings.setProfile(PerformancePolicy.BATTERY);
        check(PerformanceSettings.reduceEffects(), "Battery profile must reduce effects");
        check(PerformanceSettings.profile().equals(PerformancePolicy.BATTERY), "Battery persistence");

        PerformanceSettings.setProfile(PerformancePolicy.CUSTOM);
        PerformanceSettings.setCustomReduceEffects(false);
        check(!PerformanceSettings.reduceEffects(), "Custom off");
        PerformanceSettings.setCustomReduceEffects(true);
        check(PerformanceSettings.reduceEffects(), "Custom on");

        PerformanceSettings.setProfile(PerformancePolicy.SMOOTH);
        check(!PerformanceSettings.reduceEffects(), "Smooth profile");
        PowerManager.powerSave = true;
        context.power = new PowerManager();
        check(PerformanceSettings.reduceEffects(), "System power saver must win");
        PowerManager.powerSave = false;

        org.telegram.messenger.SharedConfig.low = true;
        PerformanceSettings.setProfile(PerformancePolicy.AUTOMATIC);
        check(PerformanceSettings.reduceEffects(), "Automatic low device");
        org.telegram.messenger.SharedConfig.low = false;

        PerformanceSettings.reset();
        check(PerformancePolicy.AUTOMATIC.equals(PerformanceSettings.profile()), "Reset profile");
        check(!PerformanceSettings.customReduceEffects(), "Reset custom");
        System.out.println("PerformanceSettingsTest passed (persistence, reset, automatic/battery/custom/power saver)");
    }
}
