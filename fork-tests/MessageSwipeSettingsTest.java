import org.telegram.messenger.fork.gestures.MessageSwipeSettings;
public class MessageSwipeSettingsTest {
 public static void main(String[] args) {
  check("reply".equals(MessageSwipeSettings.mode()));
  MessageSwipeSettings.setMode("menu"); check("menu".equals(MessageSwipeSettings.mode()));
  MessageSwipeSettings.setMode("off"); check("off".equals(MessageSwipeSettings.mode()));
  MessageSwipeSettings.setMode("copy"); check("copy".equals(MessageSwipeSettings.mode()));
  MessageSwipeSettings.setMode("forward"); check("forward".equals(MessageSwipeSettings.mode()));
  MessageSwipeSettings.setMode("off");
  boolean rejected=false; try {MessageSwipeSettings.setMode("bad");} catch(IllegalArgumentException e){rejected=true;}
  check(rejected && "off".equals(MessageSwipeSettings.mode()));
  MessageSwipeSettings.setMode("reply"); check("reply".equals(MessageSwipeSettings.mode()));
  MessageSwipeSettings.setPersonal(0,42,"menu");
  check("menu".equals(MessageSwipeSettings.mode(0,42)));
  check("reply".equals(MessageSwipeSettings.mode(1,42)));
  check("reply".equals(MessageSwipeSettings.mode(0,43)));
  MessageSwipeSettings.setPersonal(0,42,null); check("reply".equals(MessageSwipeSettings.mode(0,42)));
  check("off".equals(MessageSwipeSettings.mode(true)));
  MessageSwipeSettings.setMode(true,"copy");
  check("copy".equals(MessageSwipeSettings.mode(0,42,true)));
  MessageSwipeSettings.setPersonal(0,42,true,"forward");
  check("forward".equals(MessageSwipeSettings.mode(0,42,true)) && "copy".equals(MessageSwipeSettings.mode(1,42,true)));
  MessageSwipeSettings.setPersonal(0,42,true,null);
  MessageSwipeSettings.setMode(true,"off");
  System.out.println("MessageSwipeSettingsTest passed");
 }
 static void check(boolean v){if(!v) throw new AssertionError();}
}
