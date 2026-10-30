import org.telegram.messenger.fork.gestures.MessageLongPressSettings;
public class MessageLongPressSettingsTest {
 public static void main(String[] args) {
  check("standard".equals(MessageLongPressSettings.mode(0,42)));
  MessageLongPressSettings.setMode("copy"); check("copy".equals(MessageLongPressSettings.mode(0,42)));
  MessageLongPressSettings.setPersonal(0,42,"forward");
  check("forward".equals(MessageLongPressSettings.mode(0,42)) && "copy".equals(MessageLongPressSettings.mode(1,42)));
  MessageLongPressSettings.setPersonal(0,42,null); MessageLongPressSettings.setMode("standard");
  check("standard".equals(MessageLongPressSettings.mode(0,42)));
  System.out.println("MessageLongPressSettingsTest passed");
 }
 static void check(boolean v) {if(!v) throw new AssertionError();}
}
