import java.util.*;
import org.telegram.messenger.fork.gestures.FavoriteReactionSettings;
public class FavoriteReactionSettingsTest {
 public static void main(String[] args) {
  FavoriteReactionSettings.clear(0); FavoriteReactionSettings.clear(1);
  FavoriteReactionSettings.add(0,"👍"); FavoriteReactionSettings.add(0,"❤"); FavoriteReactionSettings.add(0,"👍");
  check(FavoriteReactionSettings.get(0).equals(Arrays.asList("👍","❤")));
  check(FavoriteReactionSettings.get(1).isEmpty());
  FavoriteReactionSettings.move(0,"❤",-1); check(FavoriteReactionSettings.get(0).equals(Arrays.asList("❤","👍")));
  FavoriteReactionSettings.remove(0,"❤"); check(FavoriteReactionSettings.get(0).equals(Collections.singletonList("👍")));
  FavoriteReactionSettings.clear(0); check(FavoriteReactionSettings.get(0).isEmpty());
  System.out.println("FavoriteReactionSettingsTest passed");
 }
 static void check(boolean v){if(!v) throw new AssertionError();}
}
