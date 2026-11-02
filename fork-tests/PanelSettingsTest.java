import org.telegram.messenger.fork.panels.ForkPanelSettings;
public class PanelSettingsTest {
 public static void main(String[] a) throws Exception {
  check(!ForkPanelSettings.enabled()); ForkPanelSettings.setEnabled(true); check(ForkPanelSettings.enabled());
  check(ForkPanelSettings.radiusDp() == 26);
  ForkPanelSettings.setRadiusDp(18); check(ForkPanelSettings.radiusDp() == 18);
  java.lang.reflect.Field cache = ForkPanelSettings.class.getDeclaredField("cachedRadius");
  cache.setAccessible(true); cache.set(null, null);
  check(ForkPanelSettings.radiusDp() == 18);
  ForkPanelSettings.setRadiusDp(100); check(ForkPanelSettings.radiusDp() == 32);
  ForkPanelSettings.setRadiusDp(0); check(ForkPanelSettings.radiusDp() == 12);
  ForkPanelSettings.setStyle("liquid"); check(ForkPanelSettings.enabled() && "liquid".equals(ForkPanelSettings.style()));
  ForkPanelSettings.setStyle("material"); check(!ForkPanelSettings.enabled() && "material".equals(ForkPanelSettings.style()));
  ForkPanelSettings.setStyle("blur"); check(ForkPanelSettings.enabled() && "blur".equals(ForkPanelSettings.style()));
  boolean rejected=false; try { ForkPanelSettings.setStyle("invalid"); } catch (IllegalArgumentException expected) { rejected=true; }
  check(rejected && "blur".equals(ForkPanelSettings.style()));
  ForkPanelSettings.setEnabled(false); check(!ForkPanelSettings.enabled()); System.out.println("PanelSettingsTest passed");
 }
 static void check(boolean v){if(!v)throw new AssertionError();}
}
