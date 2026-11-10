package org.telegram.ui;

import android.content.Context;
import android.view.View;
import android.widget.FrameLayout;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;

/** Dedicated screen for the official Berrygram release feed. */
public final class ForkUpdateActivity extends ForkSettingsFragment {
    private ForkUpdaterPanel updater;
    @Override public View createView(Context context) {
        actionBar.setTitle(ForkLocale.get(R.string.ForkUpdatePanel));
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override public void onItemClick(int id) { if (id == -1) finishFragment(); }
        });
        updater = new ForkUpdaterPanel(context, this);
        FrameLayout container = new FrameLayout(context);
        container.addView(updater, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        fragmentView = container;
        return fragmentView;
    }
    @Override public void onPause() { if (updater != null) updater.onParentPaused(); super.onPause(); }
    @Override public void onResume() { super.onResume(); if (updater != null) updater.onParentResumed(); }
    @Override public void onFragmentDestroy() { if (updater != null) updater.dispose(); updater = null; super.onFragmentDestroy(); }
}
