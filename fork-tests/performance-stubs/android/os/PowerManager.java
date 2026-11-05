package android.os;
public class PowerManager {
 public static final int THERMAL_STATUS_MODERATE = 2;
 public static boolean powerSave;
 public boolean isPowerSaveMode(){ return powerSave; }
 public int getCurrentThermalStatus(){ return 0; }
}
