import org.telegram.messenger.fork.menu.MessageMenuSettings;
public class MessageMenuSettingsTest {
 public static void main(String[] args) {
  MessageMenuSettings.reset();
  check(MessageMenuSettings.order().isEmpty());
  MessageMenuSettings.toggle("hidden","copy");
  MessageMenuSettings.toggle("pinned","reply");
  check(MessageMenuSettings.hidden().contains("copy") && MessageMenuSettings.pinned().contains("reply"));
  MessageMenuSettings.move("copy",-1);
  check(MessageMenuSettings.order().get(0).equals("copy"));
  for (String key : new String[]{"view_topic", "open_profile", "add_stickers", "copy_phone", "statistics", "add_contact"}) {
   MessageMenuSettings.toggle("hidden",key); check(MessageMenuSettings.hidden().contains(key));
  }
  boolean rejected=false; try {MessageMenuSettings.toggle("hidden","delete");} catch(IllegalArgumentException e){rejected=true;}
  check(rejected && !MessageMenuSettings.hidden().contains("delete"));
  MessageMenuSettings.reset();
  check(MessageMenuSettings.order().isEmpty() && MessageMenuSettings.hidden().isEmpty() && MessageMenuSettings.pinned().isEmpty());
  System.out.println("MessageMenuSettingsTest passed");
 }
 static void check(boolean v) {if(!v) throw new AssertionError();}
}
