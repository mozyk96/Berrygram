import org.telegram.messenger.fork.panels.ForkPanelSettings;
public class PanelSettingsTest {
 public static void main(String[] a) {
  check(!ForkPanelSettings.enabled()); ForkPanelSettings.setEnabled(true); check(ForkPanelSettings.enabled());
  ForkPanelSettings.setEnabled(false); check(!ForkPanelSettings.enabled()); System.out.println("PanelSettingsTest passed");
 }
 static void check(boolean v){if(!v)throw new AssertionError();}
}
