package android.content;
import java.util.HashMap; import java.util.Map;
public class Context {
 public static final int MODE_PRIVATE=0; private final Prefs prefs=new Prefs();
 public SharedPreferences getSharedPreferences(String name,int mode) { return prefs; }
 public static class Prefs implements SharedPreferences {
  final Map<String,Object> values=new HashMap<>();
  public String getString(String k,String d) { Object v=values.get(k); return v instanceof String ? (String)v:d; }
  public boolean getBoolean(String k,boolean d) { Object v=values.get(k); return v instanceof Boolean ? (Boolean)v:d; }
  public boolean contains(String k) { return values.containsKey(k); }
  public Map<String,?> getAll() { return new HashMap<>(values); }
  public Editor edit() { return new Editor() {
   final Map<String,Object> puts=new HashMap<>(); final java.util.Set<String> removes=new java.util.HashSet<>();
   public Editor putString(String k,String v) { puts.put(k,v);return this; }
   public Editor putBoolean(String k,boolean v) { puts.put(k,v);return this; }
   public Editor remove(String k) { removes.add(k);return this; }
   public void apply() { for(String k:removes) values.remove(k); values.putAll(puts); }
  }; }
 }
}