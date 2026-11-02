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
  check(ForkPanelSettings.effectStrength() == 50);
  ForkPanelSettings.setEffectStrength(200); check(ForkPanelSettings.effectStrength() == 100);
  java.lang.reflect.Field strengthCache = ForkPanelSettings.class.getDeclaredField("cachedStrength");
  strengthCache.setAccessible(true); strengthCache.set(null, null);
  check(ForkPanelSettings.effectStrength() == 100);
  ForkPanelSettings.setStyle("liquid"); check(ForkPanelSettings.enabled() && "liquid".equals(ForkPanelSettings.style()));
  java.lang.reflect.Field styleCache = ForkPanelSettings.class.getDeclaredField("cachedStyle");
  java.lang.reflect.Field enabledCache = ForkPanelSettings.class.getDeclaredField("cached");
  styleCache.setAccessible(true); enabledCache.setAccessible(true);
  styleCache.set(null, null); enabledCache.set(null, null);
  check(ForkPanelSettings.enabled() && "liquid".equals(ForkPanelSettings.style()));
  ForkPanelSettings.setStyle("material"); check(!ForkPanelSettings.enabled() && "material".equals(ForkPanelSettings.style()));
  ForkPanelSettings.setStyle("blur"); check(ForkPanelSettings.enabled() && "blur".equals(ForkPanelSettings.style()));
  boolean rejected=false; try { ForkPanelSettings.setStyle("invalid"); } catch (IllegalArgumentException expected) { rejected=true; }
  check(rejected && "blur".equals(ForkPanelSettings.style()));
  check(ForkPanelSettings.customAccent() == null);
  ForkPanelSettings.setCustomAccent("#15aBcd");
  check("#15ABCD".equals(ForkPanelSettings.customAccent()));
  rejected=false; try { ForkPanelSettings.setCustomAccent("#zzzzzz"); } catch (IllegalArgumentException expected) { rejected=true; }
  check(rejected && "#15ABCD".equals(ForkPanelSettings.customAccent()));
  ForkPanelSettings.setCustomAccent(null); check(ForkPanelSettings.customAccent() == null);
  ForkPanelSettings.setEnabled(false); check(!ForkPanelSettings.enabled()); System.out.println("PanelSettingsTest passed");
 }
 static void check(boolean v){if(!v)throw new AssertionError();}
}
