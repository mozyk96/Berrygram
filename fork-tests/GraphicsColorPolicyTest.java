import org.telegram.messenger.fork.panels.GraphicsColorPolicy;
public final class GraphicsColorPolicyTest {
 public static void main(String[] args) {
  check(GraphicsColorPolicy.parseHex("#12aBcD") == 0xff12abcd);
  check(GraphicsColorPolicy.parseHex("12ABCD") == 0xff12abcd);
  check(GraphicsColorPolicy.parseHex("#123") == null);
  check(GraphicsColorPolicy.parseHex("#gg0000") == null);
  check("#12ABCD".equals(GraphicsColorPolicy.formatHex(0xff12abcd)));
  check(GraphicsColorPolicy.contrastText(0xffffffff) == 0xff000000);
  check(GraphicsColorPolicy.contrastText(0xff000000) == 0xffffffff);
  check(GraphicsColorPolicy.contrastRatio(GraphicsColorPolicy.readableAccent(0xffffff00, 0xffffffff), 0xffffffff) >= 4.5);
  check(GraphicsColorPolicy.contrastRatio(GraphicsColorPolicy.readableAccent(0xff000040, 0xff000000), 0xff000000) >= 4.5);
  System.out.println("GraphicsColorPolicyTest passed");
 }
 static void check(boolean value) { if (!value) throw new AssertionError(); }
}
