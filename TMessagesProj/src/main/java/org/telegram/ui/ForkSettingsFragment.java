package org.telegram.ui;

import android.view.View;
import android.view.ViewGroup;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailSettingsCell;
import org.telegram.ui.Cells.TextSettingsCell;
import org.telegram.ui.fork.settings.ForkInsets;

/**
 * Base of the Berrygram screens.
 *
 * <p>Every Berrygram screen reserves the space of the floating bottom
 * navigation of the main tabs and of the system navigation bar, so the last
 * row of a screen can always be scrolled completely into view. The height is
 * taken from the real window insets, it is never a fixed value.</p>
 */
public abstract class ForkSettingsFragment extends BaseFragment {

    @Override
    protected void onViewCreated(View view) {
        super.onViewCreated(view);
        enableWrapping(view);
    }

    private void enableWrapping(View view) {
        if (view instanceof TextSettingsCell) {
            ((TextSettingsCell) view).setMultilineTitle();
        } else if (view instanceof TextCheckCell) {
            ((TextCheckCell) view).setMultilineTitle();
        } else if (view instanceof TextDetailSettingsCell) {
            ((TextDetailSettingsCell) view).setMultilineTitle();
        } else if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) enableWrapping(group.getChildAt(i));
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        WindowInsetsCompat insets = fragmentView == null ? null : ViewCompat.getRootWindowInsets(fragmentView);
        int bottom = insets == null ? getBottomInset() : insets.getInsets(WindowInsetsCompat.Type.navigationBars()).bottom;
        applyBottomInset(bottom);
    }

    @Override
    public void onInsets(int left, int top, int right, int bottom) {
        super.onInsets(left, top, right, bottom);
        applyBottomInset(bottom);
    }

    /** Reserves the bottom space of the screen. */
    protected void applyBottomInset(int systemBottom) {
        View view = fragmentView;
        if (view == null) {
            return;
        }
        int padding = ForkInsets.bottomPadding(systemBottom);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            int count = group.getChildCount();
            for (int a = count - 1; a >= 0; a--) {
                View child = group.getChildAt(a);
                if (child.getVisibility() != View.VISIBLE) {
                    continue;
                }
                ForkInsets.applyBottomPadding(child, padding);
                return;
            }
        }
        ForkInsets.applyBottomPadding(view, padding);
    }
}
