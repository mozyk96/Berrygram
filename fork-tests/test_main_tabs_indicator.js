const fs = require('fs');
const cp = require('child_process');
const path = require('path');
const root = path.resolve(__dirname, '..');
const javaRoot = path.join(root, 'TMessagesProj/src/main/java');

function read(file) {
    return fs.readFileSync(path.join(javaRoot, file), 'utf8');
}

function method(source, signature) {
    const start = source.indexOf(signature);
    if (start < 0) throw new Error('Missing production method ' + signature);
    let end = source.indexOf('{', start), depth = 0;
    do {
        if (source[end] === '{') depth++;
        if (source[end] === '}') depth--;
        end++;
    } while (depth);
    return source.slice(start, end);
}

const activity = read('org/telegram/ui/MainTabsActivity.java');
const statistic = read('org/telegram/ui/StatisticActivity.java');
const layout = read('org/telegram/ui/MainTabsLayout.java');
const pager = read('org/telegram/ui/Components/ViewPagerFixed.java');

const activitySelect = method(activity, 'public void selectTab(int position, boolean animated)');
const statisticSelect = method(statistic, 'public void selectTab(int position, boolean animated)');
const layoutSelect = method(layout, 'public void setTabSelected(View tab, boolean animated)');

if (!activitySelect.includes('tabsView.setTabSelected(')) {
    throw new Error('MainTabsActivity bypasses the shared bottom-tab indicator');
}
if (activitySelect.includes('tab.setSelected(')) {
    throw new Error('MainTabsActivity still updates GlassTabView directly');
}
if (!statisticSelect.includes('tabsView.setTabSelected(')) {
    throw new Error('StatisticActivity bypasses the shared bottom-tab indicator');
}
if (statisticSelect.includes('tab.setSelected(')) {
    throw new Error('StatisticActivity still updates GlassTabView directly');
}
if (layout.includes('setSkipDrawSelector(false)')) {
    throw new Error('Shared indicator can fall back to the obsolete per-tab selector');
}

// The pager owns the navigation position, the indicator only renders it.
if (!layout.includes('public void setIndicatorPosition(float position)')) {
    throw new Error('The indicator has no pager driven position API');
}
if (!activity.includes('viewPager.getPositionAnimated()')) {
    throw new Error('MainTabsActivity does not read the pager position for the indicator');
}
if (!activity.includes('tabsView.setIndicatorPosition(')) {
    throw new Error('MainTabsActivity does not forward the pager position to the indicator');
}
if (layout.includes('selectedTabPositionX') || layout.includes('elongationAnim')) {
    throw new Error('The indicator still owns an independent spring animation');
}

// An idle screen must not wake the main thread. The pager reports every frame
// of a drag or an animation, a second frame loop only fought with it.
if (activity.includes('postFrameCallback')) {
    throw new Error('MainTabsActivity still runs a separate indicator frame loop');
}
if (activity.includes('forkIndicatorIdleFrames') || activity.includes('forkIndicatorFrameScheduled')) {
    throw new Error('MainTabsActivity still keeps the idle frame state of the removed frame loop');
}

// The tab selection may only change when the page really changes, otherwise
// every drag frame restarted the selection animation.
const animationUpdate = method(activity, 'protected void onViewPagerTabAnimationUpdate(boolean manual)');
if (!animationUpdate.includes('Math.round(position)')) {
    throw new Error('The pager update does not detect the page change');
}
if (!animationUpdate.includes('forkLastSelectedPosition')) {
    throw new Error('The pager update does not remember the selected page');
}
if (animationUpdate.includes('selectTab(Math.round(position), true)')) {
    throw new Error('The pager update still re-selects the tab on every frame');
}

// The pager normalises the translation with its own width, the display width
// was wrong in split screen and on tablets.
const available = method(pager, 'protected float getAvailableTranslationX()');
if (!available.includes('getMeasuredWidth()')) {
    throw new Error('The pager still normalises the position with the display width');
}
if (available.includes('AndroidUtilities.displaySize.x')) {
    throw new Error('The pager normalises the position with the display width');
}
const position = method(pager, 'public float getPositionAnimated()');
if (!position.includes('currentPosition')) {
    throw new Error('The pager position has no fallback for an unmeasured pager');
}

// The anchor search has to stay inside the visible anchors, a stale entry
// moved the indicator after a tab was hidden.
const anchors = method(layout, 'private void refreshAnchors()');
if (!anchors.includes('anchorCount')) {
    throw new Error('The anchor list does not track the number of visible anchors');
}
if (!layout.includes('findAnchorIndex(anchorPositions, anchorCount, position)')) {
    throw new Error('The anchor search is not limited to the visible anchors');
}

const layoutSync = method(layout, 'private void syncSelectedTabIndicator()');
const tabsPosition = method(activity, 'private void checkUi_tabsPosition()');
if (layoutSync.includes('animatedLongSelectedViewCenterX = targetX')) {
    throw new Error('The indicator can still snap to a target instead of following the pager');
}
const findSelected = method(layout, 'private View findSelectedTab()');
if (!findSelected.includes('child.isSelected()')) {
    throw new Error('Indicator selection is ambiguous while the previous tab is still animating out');
}

const fixture = `public class MainTabsIndicatorTest {
    static class View {
        static final int VISIBLE = 0, GONE = 8;
    }
    static class GlassTabView extends View {
        final int index;
        boolean visible = true;
        GlassTabView(int index) { this.index = index; }
    }
    static class MainTabsLayout {
        GlassTabView selected;
        float indicatorPosition = -1f;
        boolean enabled, clickable;
        int visibility;
        void setClickable(boolean value) { clickable = value; }
        void setEnabled(boolean value) { enabled = value; }
        void setAlpha(float value) { }
        void setVisibility(int value) { visibility = value; }
        boolean isViewVisible(GlassTabView tab) { return tab != null && tab.visible; }
        void setTabSelected(View tab, boolean animated) { selected = (GlassTabView) tab; }
        void setIndicatorPosition(float position) { indicatorPosition = position; }
    }
    static class ViewPagerStub {
        float position = 0f;
        float getPositionAnimated() { return position; }
    }
    GlassTabView[] tabs;
    MainTabsLayout tabsView = new MainTabsLayout();
    static class Wrapper { void setTranslationY(float value) { } }
    static class UpdateLayout { boolean isUpdateLayoutVisible() { return false; } static final int HEIGHT = 0; }
    static class Animator { float value; float getFloatValue() { return value; } }
    Wrapper tabsViewWrapper = new Wrapper();
    UpdateLayout updateLayoutWrapper = new UpdateLayout();
    Animator animatorTabsVisible = new Animator();
    static int dp(float value) { return (int) value; }
    static float lerp(float from, float to, float factor) { return from + (to - from) * factor; }
    ViewPagerStub viewPager = new ViewPagerStub();
    int forkLastSelectedPosition = -1;
    static int indexToPosition(int index) { return index > 2 ? index - 1 : index; }
    ${activitySelect.replace('public void selectTab', 'public void selectMainTab')}
    ${statisticSelect.replace('public void selectTab', 'public void selectStatisticTab')}
    ${tabsPosition.replace('UpdateLayoutWrapper.HEIGHT', 'UpdateLayout.HEIGHT')}
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        MainTabsIndicatorTest test = new MainTabsIndicatorTest();
        test.animatorTabsVisible.value = 1f;
        test.checkUi_tabsPosition();
        check(test.tabsView.enabled && test.tabsView.clickable && test.tabsView.visibility == View.VISIBLE,
            "Visible bottom navigation must be enabled and clickable");
        test.animatorTabsVisible.value = 0f;
        test.checkUi_tabsPosition();
        check(!test.tabsView.enabled && !test.tabsView.clickable && test.tabsView.visibility == View.GONE,
            "Hidden bottom navigation must not be enabled or clickable");
        test.tabs = new GlassTabView[] {
            new GlassTabView(0), new GlassTabView(1), new GlassTabView(2),
            new GlassTabView(3), new GlassTabView(4)
        };
        test.tabs[3].visible = false;
        test.selectMainTab(0, false);
        check(test.tabsView.selected == test.tabs[0], "Chats selection was not delegated");
        test.selectMainTab(1, true);
        check(test.tabsView.selected == test.tabs[1], "Contacts selection was not delegated");
        test.selectMainTab(2, true);
        check(test.tabsView.selected == test.tabs[2], "Settings selection was not delegated");
        test.tabs[2].visible = false;
        test.tabs[3].visible = true;
        test.selectMainTab(2, true);
        check(test.tabsView.selected == test.tabs[3], "Calls selection was not delegated");
        test.selectMainTab(3, false);
        check(test.tabsView.selected == test.tabs[4], "Profile selection was not delegated");

        // The indicator always follows the position of the pager.
        test.viewPager.position = 1.5f;
        test.selectMainTab(2, false);
        check(Math.abs(test.tabsView.indicatorPosition - 1.5f) < 0.0001f,
            "The indicator does not render the pager position");

        test.tabsView.selected = null;
        test.selectStatisticTab(1, true);
        check(test.tabsView.selected == test.tabs[1], "Statistics selection was not delegated");
        System.out.println("MainTabsIndicatorTest passed (shared indicator delegation and pager driven position)");
    }
}`;

const output = path.join(root, 'build/fork-smoke/main-tabs-indicator');
fs.mkdirSync(output, { recursive: true });
const fixturePath = path.join(output, 'MainTabsIndicatorTest.java');
fs.writeFileSync(fixturePath, fixture);
const javaHome = process.env.JAVA_HOME || path.resolve(root, '../jdk17/jdk-17.0.20.1+1');
for (const [exe, args] of [['javac', ['-d', output, fixturePath]], ['java', ['-cp', output, 'MainTabsIndicatorTest']]]) {
    const result = cp.spawnSync(path.join(javaHome, 'bin', exe + '.exe'), args, { stdio: 'inherit' });
    if (result.error) throw result.error;
    if (result.status !== 0) process.exit(result.status || 1);
}
