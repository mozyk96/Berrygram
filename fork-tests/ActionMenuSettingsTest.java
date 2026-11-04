import java.util.*;
import android.content.*;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.fork.actions.*;
public class ActionMenuSettingsTest {
 static void check(boolean value) { if (!value) throw new AssertionError(); }
 public static void main(String[] args) {
  ApplicationLoader.applicationContext = new Context() {
   private final Map<String, SharedPreferences> named = new HashMap<>();
   @Override public SharedPreferences getSharedPreferences(String name,int mode) {
    return named.computeIfAbsent(name, key -> new Context.Prefs());
   }
  };
  List<Integer> nativeOptions=Arrays.asList(99,3,8,13,4,1);
  ActionMenuSettings.reset(0);
  check(ActionMenuSettings.plan(0,nativeOptions).mainIndices.equals(Arrays.asList(0,1,2,3,4,5)));
  check(!ActionMenuSettings.editableIds().contains("reaction"));
  ActionMenuSettings.setVisible(0,"copy",false);
  ActionMenuSettings.setPinned(0,"reply",true);
  check(!ActionMenuSettings.visible(0,"copy") && ActionMenuSettings.pinned(0,"reply"));
  check(ActionMenuSettings.visible(1,"copy") && !ActionMenuSettings.pinned(1,"reply"));
  ActionMenuSettings.move(0,ActionMenuSettings.order(0).indexOf("delete"),0);
  check(ActionMenuSettings.order(0).get(0).equals("delete"));
  List<String> dragged = new ArrayList<>(ActionMenuSettings.order(0));
  for (int to=1; to<dragged.size(); to++) {
   ActionMenuSettings.move(0,to-1,to); dragged.add(to,dragged.remove(to-1));
   check(ActionMenuSettings.order(0).equals(dragged));
  }
  check(!ActionMenuSettings.order(1).equals(dragged));
  check(ActionMenuSettings.plan(0,nativeOptions).moreIndices.equals(Arrays.asList(1)));
  check(ActionMenuSettings.plan(0,nativeOptions).mainIndices.get(0)==2);
  UserConfig.setTestUserId(0,999);
  check(ActionMenuSettings.visible(0,"copy"));
  UserConfig.setTestUserId(0,100);
  check(!ActionMenuSettings.visible(0,"copy"));
  ActionSettings.preferences(0).edit().putString("menu_order","bogus,copy,copy,reply").apply();
  check(ActionMenuSettings.order(0).get(0).equals("copy") && ActionMenuSettings.order(0).size()==ActionMenuSettings.editableIds().size());
  ActionSettings.preferences(0).edit().putBoolean("menu_order",true).apply();
  check(ActionMenuSettings.order(0).equals(ActionMenuSettings.editableIds()));
  ActionSettings.setGesture(0,"tap","reply");
  ActionMenuSettings.reset(0);
  check(ActionSettings.gesture(0,"tap").equals("reply"));
  check(ActionMenuSettings.visible(0,"copy") && !ActionMenuSettings.pinned(0,"reply"));
  boolean rejected=false; try { ActionMenuSettings.setVisible(0,"reaction",false); } catch(IllegalArgumentException e){rejected=true;}
  check(rejected);
  System.out.println("ActionMenuSettingsTest passed");
 }
}
