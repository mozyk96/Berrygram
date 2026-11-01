package org.telegram.ui;

import android.content.Context;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.camera.RoundCameraSettings;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;

/** Selects the initial camera used by Telegram video messages. */
public final class ForkRoundCameraShortcuts {
    private ForkRoundCameraShortcuts() {}
    public static void show(BaseFragment parent) { show(parent, null); }
    public static void show(BaseFragment parent, Runnable onChanged) {
        Context context = parent.getParentActivity();
        if (context == null) return;
        String[] modes = {"standard", "front", "back", "last", "gesture"};
        int[] names = {R.string.ForkCameraStandard, R.string.ForkCameraFront, R.string.ForkCameraBack, R.string.ForkCameraLast, R.string.ForkCameraGesture};
        String current = RoundCameraSettings.mode();
        CharSequence[] labels = new CharSequence[modes.length];
        for (int i = 0; i < modes.length; i++) labels[i] = (modes[i].equals(current) ? "✓ " : "") + context.getString(names[i]);
        parent.showDialog(new AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.ForkRoundCameraTitle))
            .setMessage(context.getString(R.string.ForkCameraFallback))
            .setItems(labels, (dialog, index) -> { RoundCameraSettings.setMode(modes[index]); if (onChanged != null) onChanged.run(); })
            .setNegativeButton(LocaleController.getString(R.string.Cancel), null).create());
    }
    public static void showQuality(BaseFragment parent) { showQuality(parent, null); }
    public static void showQuality(BaseFragment parent, Runnable onChanged) {
        Context context = parent.getParentActivity();
        if (context == null) return;
        String mode = RoundCameraSettings.qualityMode();
        String[] modes = {"standard", "auto", "custom"};
        int[] labels = {R.string.ForkVideoQualityStandard, R.string.ForkVideoQualityAuto, R.string.ForkVideoQualityCustom};
        CharSequence[] choices = new CharSequence[modes.length];
        for (int i = 0; i < modes.length; i++) choices[i] = (modes[i].equals(mode) ? "✓ " : "") + context.getString(labels[i])
                + ("custom".equals(modes[i]) && "custom".equals(mode) ? " (" + (RoundCameraSettings.customBitrate() / 1000) + " kbps)" : "");
        parent.showDialog(new AlertDialog.Builder(context).setTitle(context.getString(R.string.ForkVideoQualityTitle))
            .setMessage(context.getString(R.string.ForkVideoQualityDescription))
            .setItems(choices, (dialog, which) -> {
                if (which != 2) { RoundCameraSettings.setQuality(modes[which], 0); if (onChanged != null) onChanged.run(); return; }
                int[] rates = {500000, 1000000, 1500000, 2500000, 4000000};
                CharSequence[] items = {"0.5 Mbps", "1 Mbps", "1.5 Mbps", "2.5 Mbps", "4 Mbps"};
                parent.showDialog(new AlertDialog.Builder(context).setTitle(context.getString(R.string.ForkVideoQualityBitrate))
                    .setItems(items, (d, selected) -> { RoundCameraSettings.setQuality("custom", rates[selected]); if (onChanged != null) onChanged.run(); })
                    .setNegativeButton(LocaleController.getString(R.string.Cancel), null).create());
            }).setNegativeButton(LocaleController.getString(R.string.Cancel), null).create());
    }
}
