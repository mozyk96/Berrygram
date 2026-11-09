package org.telegram.ui;

import android.content.Context;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.Theme;

/** Shared navigation header for BerryGram pages without contextual actions. */
public final class ForkFloatingHeader {
    private ForkFloatingHeader() { }

    private static int surface(Context context) {
        int color = Theme.getColor(Theme.key_windowBackgroundWhite);
        return "material".equals(ForkGraphicsSurface.effectiveStyle()) ? ForkGraphicsSurface.materialColor(context, color) : color;
    }

    public static void refresh(View view) {
        if (!(view instanceof LinearLayout)) return;
        LinearLayout root = (LinearLayout) view;
        if (root.getChildCount() == 0 || !(root.getChildAt(0) instanceof LinearLayout)) return;
        LinearLayout header = (LinearLayout) root.getChildAt(0);
        int color = surface(view.getContext());
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        for (int i = 0; i < header.getChildCount(); i++) {
            View child = header.getChildAt(i);
            if (i == 0 || i > 1) {
                child.setBackground(Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(48), color, Theme.getColor(Theme.key_actionBarDefaultSelector)));
            }
            if (child instanceof ImageView) {
                ((ImageView) child).setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.MULTIPLY));
            } else if (child instanceof TextView) {
                ((TextView) child).setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }
        }
    }

    public static View wrap(Context context, ActionBar actionBar, View content, CharSequence title, Runnable onBack) {
        return wrap(context, actionBar, content, title, onBack, null);
    }

    public static View wrap(Context context, ActionBar actionBar, View content, CharSequence title, Runnable onBack, View action) {
        actionBar.setAddToContainer(false);
        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        root.setPadding(0, actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0, 0, 0);

        LinearLayout header = new LinearLayout(context);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setMinimumHeight(AndroidUtilities.dp(56));
        int side = AndroidUtilities.dp(12);
        header.setPadding(side, 0, side, 0);
        root.addView(header, new LinearLayout.LayoutParams(-1, LinearLayout.LayoutParams.WRAP_CONTENT));

        ImageView back = new ImageView(context);
        back.setImageResource(R.drawable.ic_ab_back);
        back.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), PorterDuff.Mode.MULTIPLY));
        back.setScaleType(ImageView.ScaleType.CENTER);
        back.setContentDescription(LocaleController.getString(R.string.AccDescrGoBack));
        int surface = surface(context);
        back.setBackground(Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(48), surface, Theme.getColor(Theme.key_actionBarDefaultSelector)));
        back.setOnClickListener(v -> onBack.run());
        header.addView(back, new LinearLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(48)));

        TextView label = new TextView(context);
        label.setText(title);
        label.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        label.setTextSize(20);
        label.setTypeface(AndroidUtilities.bold());
        label.setMaxLines(2);
        label.setEllipsize(TextUtils.TruncateAt.END);
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, -2, 1);
        titleParams.setMarginStart(AndroidUtilities.dp(14));
        header.addView(label, titleParams);
        if (action != null) {
            action.setBackground(Theme.createSimpleSelectorCircleDrawable(AndroidUtilities.dp(48), surface, Theme.getColor(Theme.key_actionBarDefaultSelector)));
            header.addView(action, new LinearLayout.LayoutParams(AndroidUtilities.dp(48), AndroidUtilities.dp(48)));
        }
        root.addView(content, new LinearLayout.LayoutParams(-1, 0, 1));
        return root;
    }
}
