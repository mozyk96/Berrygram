import org.telegram.messenger.fork.panels.PanelPolicy;
public class PanelPolicyTest { public static void main(String[] args) {
 check(PanelPolicy.useGlass(true,false,33,true));
 check(!PanelPolicy.useGlass(false,false,33,true));
 check(!PanelPolicy.useGlass(true,true,33,true));
 check(!PanelPolicy.useGlass(true,false,32,true));
 check(!PanelPolicy.useGlass(true,false,33,false));
 check(PanelPolicy.popupAlpha(255, true) == 240);
 check(PanelPolicy.popupAlpha(128, true) == 120);
 check(PanelPolicy.popupAlpha(255, false) == 255);
 System.out.println("PanelPolicyTest passed");
} static void check(boolean v){if(!v)throw new AssertionError();} }
