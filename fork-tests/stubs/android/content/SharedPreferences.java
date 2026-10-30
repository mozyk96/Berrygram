package android.content;
import java.util.Map;
public interface SharedPreferences {
 String getString(String key,String def); boolean getBoolean(String key,boolean def); boolean contains(String key); Map<String,?> getAll(); Editor edit();
 interface Editor { Editor putString(String key,String value); Editor putBoolean(String key,boolean value); Editor remove(String key); void apply(); }
}