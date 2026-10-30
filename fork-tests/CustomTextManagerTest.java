import org.telegram.messenger.fork.text.*;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.R;
import java.util.Collections;
public class CustomTextManagerTest {
 public static void main(String[] args) {
  ApplicationLoader.applicationContext.getSharedPreferences("fork_custom_text",0).edit().putString("Online","legacy").apply();
  check(CustomTextRegistry.find("SearchChats", 0) != null);
  check(CustomTextRegistry.find("SearchEmptyViewFilteredSubtitleMedia", 0) != null);
  check(CustomTextRegistry.find("Login", 0) == null);
  for (CustomTextRegistry.Entry entry : CustomTextRegistry.ENTRIES) {
   check(CustomTextRegistry.find(entry.key, 0) == entry);
   if (entry.resource != 0) {
    CustomTextRegistry.Entry first = null;
    for (CustomTextRegistry.Entry candidate : CustomTextRegistry.ENTRIES) if (candidate.resource == entry.resource) { first = candidate; break; }
    check(CustomTextRegistry.find(null, entry.resource) == first);
   }
  }
  check(CustomTextRegistry.find(null, 0) == null);
  check(CustomTextRegistry.find("unknown", R.string.Online) == null);
  check("legacy".equals(CustomTextManager.get("Online")));
  String pack=CustomTextManager.createPack("Work");
  check(CustomTextManager.get("Online")==null);
  CustomTextManager.set("Online","named");
  CustomTextManager.setPersonal(0,42,"Online","personal");
  CustomTextManager.setAlias(0,42,"Friend");
  check(CustomTextManager.getPersonal(1,42,"Online")==null);
  try(CustomTextContext.Scope scope=CustomTextContext.open(0,42)) {
   check("personal".equals(CustomTextManager.resolve("Online",R.string.Online,"online")));
   check("online".equals(scope.original()));
  }
  check("online".equals(CustomTextManager.resolve("Online",R.string.Online,"online")));
  CustomTextManager.replaceAll(Collections.singletonMap("Online","imported"));
  check("personal".equals(CustomTextManager.getPersonal(0,42,"Online")));
  check("Friend".equals(CustomTextManager.alias(0,42)));
  CustomTextManager.activatePack("custom");
  check("legacy".equals(CustomTextManager.get("Online")));
  CustomTextManager.activatePack(pack);
  check("imported".equals(CustomTextManager.get("Online")));
  CustomTextManager.deletePack(pack);
  check("custom".equals(CustomTextManager.activePack()));
  check("legacy".equals(CustomTextManager.get("Online")));
  System.out.println("CustomTextManagerTest passed");
 }
 static void check(boolean ok){ if(!ok) throw new AssertionError(); }
}
