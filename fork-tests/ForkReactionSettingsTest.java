import java.util.*;
import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.fork.actions.*;

public class ForkReactionSettingsTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }
    private static final class NamedContext extends Context {
        private final Map<String, SharedPreferences> stores = new HashMap<>();
        @Override public SharedPreferences getSharedPreferences(String name, int mode) {
            if (!stores.containsKey(name)) stores.put(name, new Context.Prefs());
            return stores.get(name);
        }
    }
    public static void main(String[] args) {
        ApplicationLoader.applicationContext = new NamedContext();
        check(ActionRegistry.contains(ForkReactionSettings.ACTION_ID));
        check(ForkReactionSettings.standardEmoji("😎"));
        check(ForkReactionSettings.standardEmoji("❤️‍🔥"));
        check(ForkReactionSettings.standardEmoji("👩🏽‍💻"));
        for (String invalid : Arrays.asList("animated_123", "hello", "😎,❤", "😎\n", "\u200D😎", "😎\u200D", "😎\u200D\u200D😎", "\uD800", " ", "\uFE0F")) {
            check(!ForkReactionSettings.standardEmoji(invalid));
        }
        ForkReactionSettings.setFavorites(0, Arrays.asList("😎", "❤️‍🔥"));
        check(ForkReactionSettings.favorites(0).equals(Arrays.asList("😎", "❤️‍🔥")));
        ForkReactionSettings.setDoubleTapReaction(0, "👩🏽‍💻");
        check("👩🏽‍💻".equals(ForkReactionSettings.doubleTapReaction(0, "fallback")));
        ForkReactionSettings.reset(0);
        check(!ForkReactionSettings.customized(0));
        check("native-value".equals(ForkReactionSettings.doubleTapReaction(0, "native-value")));
        check(ForkReactionSettings.normalize(Arrays.asList("bad", "👍", "👍", "❤", "🔥", "👏", "😁")).equals(Arrays.asList("👍", "❤", "🔥", "👏")));
        check(ForkReactionSettings.allowedFavorites(Arrays.asList("🔥", "👍", "❤"), Arrays.asList("❤", "🔥")).equals(Arrays.asList("🔥", "❤")));
        check(ForkReactionSettings.allowedFavorites(Arrays.asList("👍"), Collections.emptyList()).isEmpty());
        ForkReactionSettings.setFavorites(0, Arrays.asList("🔥", "👍", "❤"));
        check(ForkReactionSettings.customized(0));
        check(!ForkReactionSettings.customized(1));
        ForkReactionSettings.move(0, 2, 0);
        check(ForkReactionSettings.favorites(0).equals(Arrays.asList("❤", "🔥", "👍")));
        // Read directly from the shared persistent store; no process-local cache can satisfy this.
        ActionSettings.preferences(0).edit().putString("reaction_favorites", "👏,👍").apply();
        check(ForkReactionSettings.favorites(0).equals(Arrays.asList("👏", "👍")));
        ForkReactionSettings.setDoubleTapReaction(0, "🔥");
        check("🔥".equals(ForkReactionSettings.doubleTapReaction(0, "fallback")));
        check("fallback".equals(ForkReactionSettings.doubleTapReaction(1, "fallback")));
        org.telegram.messenger.UserConfig.setTestUserId(0, 999);
        check(!ForkReactionSettings.customized(0));
        check("fallback".equals(ForkReactionSettings.doubleTapReaction(0, "fallback")));
        org.telegram.messenger.UserConfig.setTestUserId(0, 100);
        check(ForkReactionSettings.customized(0));
        check("🔥".equals(ForkReactionSettings.doubleTapReaction(0, "fallback")));
        ForkReactionSettings.useNativeFavorites(0);
        check(!ForkReactionSettings.customized(0));
        check("🔥".equals(ForkReactionSettings.doubleTapReaction(0, "fallback")));
        ForkReactionSettings.setFavorites(0, Collections.singletonList("👍"));
        check(ForkReactionSettings.favorites(0).size() == 1);
        ActionSettings.preferences(0).edit().putString("reaction_double_tap", "animated_123").apply();
        check("fallback".equals(ForkReactionSettings.doubleTapReaction(0, "fallback")));
        ActionSettings.preferences(0).edit().putString("reaction_favorites", "bad,bad").apply();
        check(ForkReactionSettings.favorites(0).size() == 4);
        boolean rejected = false;
        try { ForkReactionSettings.setFavorites(0, Collections.emptyList()); } catch (IllegalArgumentException expected) { rejected = true; }
        check(rejected);
        ForkReactionSettings.reset(0);
        check(!ForkReactionSettings.customized(0));
        check("fallback".equals(ForkReactionSettings.doubleTapReaction(0, "fallback")));
        ActionSettings.setGesture(0, "double_tap", "copy");
        ActionSettings.setGesture(0, "tap", "reply");
        ActionSettings.setGesture(0, "swipe_right", "forward");
        ForkReactionSettings.setDoubleTapReaction(0, "😎");
        check(ActionRegistry.REACTION.equals(ActionSettings.gesture(0, "double_tap")));
        ForkReactionSettings.setDoubleTapReaction(0, null);
        check("native".equals(ActionSettings.gesture(0, "double_tap")));
        check("fallback".equals(ForkReactionSettings.doubleTapReaction(0, "fallback")));
        ForkReactionSettings.setDoubleTapReaction(0, "😎");
        ForkReactionSettings.reset(0);
        check("native".equals(ActionSettings.gesture(0, "double_tap")));
        ActionSettings.setGesture(0, "double_tap", "copy");
        ForkReactionSettings.reset(0);
        check("copy".equals(ActionSettings.gesture(0, "double_tap")));
        check("reply".equals(ActionSettings.gesture(0, "tap")));
        check("forward".equals(ActionSettings.gesture(0, "swipe_right")));
        check("native".equals(ActionSettings.gesture(1, "double_tap")));
        System.out.println("ForkReactionSettingsTest passed (policy, order, count, native intersection, persistence, isolation, reset)");
    }
}
