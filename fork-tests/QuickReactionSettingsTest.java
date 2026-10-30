import org.telegram.messenger.fork.gestures.QuickReactionSettings;
public class QuickReactionSettingsTest {
 public static void main(String[] args) {
  check(QuickReactionSettings.get(0,42)==null);
  QuickReactionSettings.set(0,42,"yes");
  check("yes".equals(QuickReactionSettings.get(0,42)));
  check(QuickReactionSettings.configuredDialogs(0).contains(42L));
  check(QuickReactionSettings.configuredDialogs(1).isEmpty());
  check(QuickReactionSettings.get(1,42)==null);
  check(QuickReactionSettings.get(0,43)==null);
  QuickReactionSettings.set(0,Long.MIN_VALUE,"secret");
  check(QuickReactionSettings.get(0,Long.MIN_VALUE)==null);
  QuickReactionSettings.set(0,42,org.telegram.messenger.fork.gestures.QuickReactionPolicy.DISABLED);
  check(org.telegram.messenger.fork.gestures.QuickReactionPolicy.DISABLED.equals(QuickReactionSettings.get(0,42)));
  QuickReactionSettings.set(0,42,null);
  check(QuickReactionSettings.get(0,42)==null);
  check(QuickReactionSettings.configuredDialogs(0).isEmpty());
  System.out.println("QuickReactionSettingsTest passed");
 }
 static void check(boolean ok) {if(!ok) throw new AssertionError();}
}
