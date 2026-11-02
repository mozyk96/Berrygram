import org.telegram.messenger.fork.panels.PanelPolicy;
public class PanelPolicyTest { public static void main(String[] args) {
 check(PanelPolicy.useGlass(true,false,33,true));
 check(!PanelPolicy.useGlass(false,false,33,true));
 check(!PanelPolicy.useGlass(true,true,33,true));
 check(!PanelPolicy.useGlass(true,false,32,true));
 check(!PanelPolicy.useGlass(true,false,33,false));
 check(PanelPolicy.popupRadiusDp(true) == 26);
 check(PanelPolicy.captureMenuGlass(true, false, 33, true, false, true));
 check(!PanelPolicy.captureMenuGlass(true, false, 33, true, true, true));
 check(!PanelPolicy.captureMenuGlass(true, false, 33, true, false, false));
 check(!PanelPolicy.captureMenuGlass(true, true, 33, true, false, true));
 check(PanelPolicy.popupRadiusDp(false) == 12);
 check(PanelPolicy.settingsRadiusDp() == 18);
 check(PanelPolicy.settingsGapDp() == 6);
 check(PanelPolicy.popupAlpha(255, true) == 240);
 check(PanelPolicy.popupAlpha(128, true) == 120);
 check(PanelPolicy.popupAlpha(255, false) == 255);
 check(PanelPolicy.clampPopupOffset(340, 0, 200, 0, 400, 8) == -148);
 check(PanelPolicy.clampPopupOffset(10, -30, 200, 0, 400, 8) == -2);
 check(PanelPolicy.clampPopupOffset(100, 0, 200, 0, 400, 8) == 0);
 check(PanelPolicy.clampPopupOffset(100, 0, 500, 0, 400, 8) == -92);
 System.out.println("PanelPolicyTest passed");
} static void check(boolean v){if(!v)throw new AssertionError();} }
