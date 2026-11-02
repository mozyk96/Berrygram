import org.telegram.messenger.fork.panels.ForkPanelSettings;
public class PanelSettingsTest {
 public static void main(String[] a) {
  check(!ForkPanelSettings.enabled()); ForkPanelSettings.setEnabled(true); check(ForkPanelSettings.enabled());
  check(ForkPanelSettings.radiusDp() == 26);
  ForkPanelSettings.setRadiusDp(18); check(ForkPanelSettings.radiusDp() == 18);
  ForkPanelSettings.setRadiusDp(100); check(ForkPanelSettings.radiusDp() == 32);
  ForkPanelSettings.setRadiusDp(0); check(ForkPanelSettings.radiusDp() == 12);
  ForkPanelSettings.setEnabled(false); check(!ForkPanelSettings.enabled()); System.out.println("PanelSettingsTest passed");
 }
 static void check(boolean v){if(!v)throw new AssertionError();}
}
