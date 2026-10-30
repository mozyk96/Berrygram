import org.telegram.messenger.fork.panels.PanelPolicy;
public class PanelPolicyTest { public static void main(String[] args) {
 check(PanelPolicy.useGlass(true,false,33,true));
 check(!PanelPolicy.useGlass(false,false,33,true));
 check(!PanelPolicy.useGlass(true,true,33,true));
 check(!PanelPolicy.useGlass(true,false,32,true));
 check(!PanelPolicy.useGlass(true,false,33,false));
 System.out.println("PanelPolicyTest passed");
} static void check(boolean v){if(!v)throw new AssertionError();} }
