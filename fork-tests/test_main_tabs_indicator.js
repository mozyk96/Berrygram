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

const activitySelect = method(activity, 'public void selectTab(int position, boolean animated)');
const statisticSelect = method(statistic, 'public void selectTab(int position, boolean animated)');
const layoutSelect = method(layout, 'public void setTabSelected(View tab, boolean animated)');
const layoutLayout = method(layout, 'protected void onLayout(boolean changed, int l, int t, int r, int b)');

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
if (!layoutSelect.includes('selectedTabPositionX.cancel()') || !layoutSelect.includes('elongationAnim.cancel()')) {
    throw new Error('Shared indicator does not cancel stale spring animations');
}
if (layout.includes('setSkipDrawSelector(false)')) {
    throw new Error('Shared indicator can fall back to the obsolete per-tab selector');
}
const layoutDetach = method(layout, 'protected void onDetachedFromWindow()');
if (!layoutLayout.includes('syncSelectedTabIndicator()')) {
    throw new Error('Shared indicator is not resynchronized after layout');
}
if (!layoutDetach.includes('selectedTabPositionX.cancel()') || !layoutDetach.includes('elongationAnim.cancel()')) {
    throw new Error('Shared indicator springs survive detach');
}
const findSelected = method(layout, 'private View findSelectedTab()');
if (!findSelected.includes('child.isSelected()')) {
    throw new Error('Indicator selection is ambiguous while the previous tab is still animating out');
}

const fixture = `public class MainTabsIndicatorTest {
    static class View {}
    static class GlassTabView extends View {
        final int index;
        boolean visible = true;
        GlassTabView(int index) { this.index = index; }
    }
    static class MainTabsLayout {
        GlassTabView selected;
        boolean isViewVisible(GlassTabView tab) { return tab != null && tab.visible; }
        void setTabSelected(View tab, boolean animated) { selected = (GlassTabView) tab; }
    }
    GlassTabView[] tabs;
    MainTabsLayout tabsView = new MainTabsLayout();
    static int indexToPosition(int index) { return index > 2 ? index - 1 : index; }
    ${activitySelect.replace('public void selectTab', 'public void selectMainTab')}
    ${statisticSelect.replace('public void selectTab', 'public void selectStatisticTab')}
    static void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    public static void main(String[] args) {
        MainTabsIndicatorTest test = new MainTabsIndicatorTest();
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

        test.tabsView.selected = null;
        test.selectStatisticTab(1, true);
        check(test.tabsView.selected == test.tabs[1], "Statistics selection was not delegated");
        System.out.println("MainTabsIndicatorTest passed (shared indicator delegation and lifecycle policy)");
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
