package org.telegram.ui.fork.settings;

import android.view.View;
import android.view.ViewGroup;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.DialogsActivity;
import org.telegram.ui.MainTabsActivity;

/**
 * Insets of the Berrygram screens.
 *
 * The application uses a floating bottom navigation that is drawn above the
 * content of the main tabs. Every screen that is opened from the settings has
 * to reserve that space, otherwise the last row stays hidden behind the
 * navigation. The height is taken from the navigation itself, so a different
 * screen size, a gesture area or a different navigation bar is handled
 * automatically.
 */
public final class ForkInsets {
    private ForkInsets() { }

    /** Height of the floating bottom navigation of the main tabs. */
    public static int mainTabsBottomOffset() {
        MainTabsActivity activity = MainTabsActivity.getInstance();
        if (activity == null || !activity.isMainTabsBarVisible()) {
            return 0;
        }
        return AndroidUtilities.dp(DialogsActivity.MAIN_TABS_HEIGHT_WITH_MARGINS);
    }

    /**
     * Bottom padding of a scrollable Berrygram screen: the system navigation
     * bar plus the floating bottom navigation of the main tabs.
     */
    public static int bottomPadding(BaseFragment fragment, int systemBottom) {
        return bottomPadding(systemBottom);
    }

    /**
     * Bottom padding of a scrollable Berrygram screen: the system navigation
     * bar plus the floating bottom navigation of the main tabs.
     */
    public static int bottomPadding(int systemBottom) {
        int bottom = systemBottom > 0 ? systemBottom : AndroidUtilities.navigationBarHeight;
        return bottom + mainTabsBottomOffset();
    }

    /**
     * Applies the bottom padding to a scroll container. The padding is added
     * to the container and the content is allowed to scroll into it, so the
     * last row can always be scrolled completely into view.
     */
    public static void applyBottomPadding(View container, int padding) {
        if (container == null) {
            return;
        }
        int left = container.getPaddingLeft();
        int top = container.getPaddingTop();
        int right = container.getPaddingRight();
        container.setPadding(left, top, right, padding);
        if (container instanceof ViewGroup) {
            ((ViewGroup) container).setClipToPadding(false);
        }
        container.requestLayout();
    }
}
