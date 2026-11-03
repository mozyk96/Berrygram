const fs = require('fs');
const cp = require('child_process');
const path = require('path');
const root = path.resolve(__dirname, '..');
const source = fs.readFileSync(path.join(root, 'TMessagesProj/src/main/java/org/telegram/ui/ActionBar/ActionBarPopupWindow.java'), 'utf8');
function method(name) {
    const start = source.indexOf('    private void ' + name + '(');
    if (start < 0) throw new Error('Missing production method ' + name);
    let depth = 0, brace = source.indexOf('{', start), end = brace;
    do { if (source[end] === '{') depth++; if (source[end] === '}') depth--; end++; } while (depth);
    return source.slice(start, end).replaceAll('org.telegram.ui.ForkSurfaceAnimation', 'SurfaceAnimation');
}
const methods = ['clearForkPreDraw', 'prepareForkFluidAnimation', 'postForkFluidAnimation'].map(method).join('\n');
const fixture = `public class PopupAnimationLifecycleTest {
    static class AnimatorSet { Runnable cancellation; void cancel() { cancellation.run(); } }
    static class ViewTreeObserver {
        interface OnPreDrawListener { boolean onPreDraw(); }
        java.util.List<OnPreDrawListener> listeners = new java.util.ArrayList<>();
        boolean isAlive() { return true; }
        void addOnPreDrawListener(OnPreDrawListener l) { listeners.add(l); }
        void removeOnPreDrawListener(OnPreDrawListener l) { listeners.remove(l); }
        void draw() { for (OnPreDrawListener l : new java.util.ArrayList<>(listeners)) l.onPreDraw(); }
    }
    static class View { ViewTreeObserver observer = new ViewTreeObserver(); ViewTreeObserver getViewTreeObserver() { return observer; } }
    static class SurfaceAnimation { static boolean enabled = true; static boolean enabled() { return enabled; } }
    static class Locker { int unlocks; void unlock() { unlocks++; } }
    View root = new View(); ViewTreeObserver forkPreDrawObserver;
    ViewTreeObserver.OnPreDrawListener forkPreDrawListener;
    AnimatorSet windowAnimatorSet; Locker notificationsLocker = new Locker();
    boolean animationEnabled = true, isClosingAnimated, pauseNotifications = true, forkFluidShown, showing = true;
    Integer forkOriginalAnimationStyle; int style = 7, starts;
    View getContentView() { return root; } View forkPopupLayout() { return root; }
    int getAnimationStyle() { return style; } void setAnimationStyle(int s) { style = s; }
    boolean isShowing() { return showing; } void startForkFluidAnimation() { starts++; }
    ${methods}
    static void check(boolean result, String message) { if (!result) throw new AssertionError(message); }
    public static void main(String[] args) {
        PopupAnimationLifecycleTest p = new PopupAnimationLifecycleTest();
        p.postForkFluidAnimation(); p.postForkFluidAnimation();
        check(p.root.observer.listeners.size() == 1, "Duplicate pending pre-draw");
        p.clearForkPreDraw(); p.root.observer.draw();
        check(p.starts == 0, "Dismissed pending animation restarted");
        p.postForkFluidAnimation(); p.prepareForkFluidAnimation();
        check(p.root.observer.listeners.isEmpty(), "Reopen retained stale pre-draw");
        check(p.style == 0 && p.forkOriginalAnimationStyle == 7, "System animation style not saved");
        AnimatorSet previous = new AnimatorSet(); p.windowAnimatorSet = previous; p.isClosingAnimated = true;
        previous.cancellation = () -> check(p.windowAnimatorSet == null, "Canceled close still owns popup");
        p.prepareForkFluidAnimation();
        check(!p.isClosingAnimated && p.notificationsLocker.unlocks == 1, "Reopen retained closing state/notification lock");
        p.postForkFluidAnimation(); p.root.observer.draw(); p.root.observer.draw();
        check(p.starts == 1 && p.root.observer.listeners.isEmpty(), "Pre-draw did not run exactly once");
        SurfaceAnimation.enabled = false; p.prepareForkFluidAnimation(); p.postForkFluidAnimation();
        check(p.style == 7 && p.forkOriginalAnimationStyle == null, "Disabled animation style not restored");
        check(p.root.observer.listeners.isEmpty(), "Disabled animation registered a callback");
        System.out.println("PopupAnimationLifecycleTest passed (production scheduling methods, mock Android window)");
    }
}`;
const output = path.join(root, 'build/fork-smoke/popup-lifecycle');
fs.mkdirSync(output, { recursive: true });
const fixturePath = path.join(output, 'PopupAnimationLifecycleTest.java');
fs.writeFileSync(fixturePath, fixture);
const javaHome = process.env.JAVA_HOME || path.resolve(root, '../jdk17/jdk-17.0.20.1+1');
for (const [exe, args] of [['javac', ['-d', output, fixturePath]], ['java', ['-cp', output, 'PopupAnimationLifecycleTest']]]) {
    const result = cp.spawnSync(path.join(javaHome, 'bin', exe + '.exe'), args, { stdio: 'inherit' });
    if (result.error) throw result.error;
    if (result.status !== 0) process.exit(result.status || 1);
}
