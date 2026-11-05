import java.util.*;
import org.telegram.messenger.fork.actions.ReactionFilterPolicy;

public class ReactionFilterPolicyTest {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private static final String LIKE = "\uD83D\uDC4D";
    private static final String LOVE = "\u2764";
    private static final String FIRE = "\uD83D\uDD25";
    private static final String CLAP = "\uD83D\uDC4F";
    private static final String PARTY = "\uD83C\uDF89";

    public static void main(String[] args) {
        List<String> nativeList = Arrays.asList(CLAP, LOVE, FIRE, LIKE);
        check(ReactionFilterPolicy.indices(Collections.emptyList(), nativeList).equals(Arrays.asList(0, 1, 2, 3)),
                "Empty favorites must fall back to the native list");
        check(ReactionFilterPolicy.indices(null, nativeList).equals(Arrays.asList(0, 1, 2, 3)),
                "Null favorites must fall back to the native list");
        check(ReactionFilterPolicy.indices(Arrays.asList(LIKE), nativeList).equals(Collections.singletonList(3)),
                "One favorite must map to its native index");
        check(ReactionFilterPolicy.indices(Arrays.asList(FIRE, LIKE, LOVE, CLAP), nativeList).equals(Arrays.asList(2, 3, 1, 0)),
                "Four favorites must preserve configured order");
        check(ReactionFilterPolicy.indices(Arrays.asList(PARTY, LIKE), nativeList).equals(Collections.singletonList(3)),
                "Disallowed favorites must be filtered");
        check(ReactionFilterPolicy.indices(Arrays.asList(PARTY), nativeList).equals(Arrays.asList(0, 1, 2, 3)),
                "All-disallowed favorites must not produce an empty panel");
        check(ReactionFilterPolicy.indices(Arrays.asList(LIKE, LIKE, LOVE), nativeList).equals(Arrays.asList(3, 1)),
                "Duplicate favorites must be used once");
        check(ReactionFilterPolicy.indices(Arrays.asList(LIKE), Collections.emptyList()).isEmpty(),
                "Empty native list stays empty");
        check(ReactionFilterPolicy.narrowed(Arrays.asList(3), 4), "Narrowed subset");
        check(!ReactionFilterPolicy.narrowed(Arrays.asList(0, 1, 2, 3), 4), "Full list is not narrowed");
        check(!ReactionFilterPolicy.narrowed(Collections.emptyList(), 0), "Empty native list is not narrowed");
        System.out.println("ReactionFilterPolicyTest passed (0..4 favorites, order, filtering, fallback, duplicates)");
    }
}
