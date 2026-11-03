import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.graphics.Rect;
import android.view.View;
import org.telegram.ui.ForkSurfaceAnimation;
import org.telegram.ui.ForkGraphicsSurface;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.fork.panels.ForkAnimationSettings;

/** Exercises production animation geometry/lifecycle with mock Android rendering. */
public class SurfaceAnimationLifecycleTest {
    private static void near(float actual, float expected) {
        if (Math.abs(actual - expected) > .0001f) throw new AssertionError(actual + " != " + expected);
    }
    public static void main(String[] args) {
        for (String style : new String[]{"liquid", "blur"}) {
            ForkGraphicsSurface.style = style;
            for (boolean opening : new boolean[]{true, false}) {
                for (boolean cancel : new boolean[]{true, false}) {
                    View view = new View();
                    view.setTranslationX(7); view.setTranslationY(9);
                    view.setPivotX(4); view.setPivotY(5);
                    AnimatorSet animation = ForkSurfaceAnimation.create(view, new Rect(40, 100, 88, 148), opening, false);
                    if (animation == null) throw new AssertionError("No animation");
                    animation.start();
                    if (opening) {
                        near(view.visibleCenterX(), 64 + 7);
                        near(view.visibleCenterY(), 124 + 9);
                    }
                    ValueAnimator.last.fraction(.5f);
                    if (!(view.getScaleX() > 0 && view.getScaleX() < 1)) throw new AssertionError("No surface morph");
                    if (view.getTranslationY() >= 9) throw new AssertionError("Source displacement lost");
                    if (cancel) animation.cancel(); else { ValueAnimator.last.fraction(1); animation.end(); }
                    near(view.getScaleX(), 1); near(view.getScaleY(), 1);
                    near(view.getTranslationX(), 7); near(view.getTranslationY(), 9);
                    near(view.getPivotX(), 4); near(view.getPivotY(), 5);
                    if (view.getClipToOutline() || view.getOutlineProvider() != null) throw new AssertionError("Clip leaked");
                }
            }
        }
        View view = new View();
        AndroidUtilities.scale = 0;
        if (ForkSurfaceAnimation.create(view, null, true, false) != null) throw new AssertionError("System disable ignored");
        AndroidUtilities.scale = 1; ForkAnimationSettings.enabled = false;
        if (ForkSurfaceAnimation.enabled()) throw new AssertionError("Preference ignored");
        ForkAnimationSettings.enabled = true; ForkGraphicsSurface.suppressed = true;
        if (ForkSurfaceAnimation.enabled()) throw new AssertionError("Fallback ignored");
        System.out.println("SurfaceAnimationLifecycleTest passed (mock Android rendering; no device claims)");
    }
}
