package org.telegram.ui;

import android.view.View;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.camera.RoundCameraSettings;
import org.telegram.ui.ActionBar.BaseFragment;

/** Anchored selections for the actual round-video camera and encoder preferences. */
public final class ForkRoundCameraShortcuts {
    private ForkRoundCameraShortcuts() { }

    public static void show(BaseFragment parent, View anchor, Runnable onChanged) {
        String[] modes = {"back", "front", "choose"};
        CharSequence[] names = {ForkLocale.get(R.string.ForkCameraBackShort), ForkLocale.get(R.string.ForkCameraFrontShort), ForkLocale.get(R.string.ForkCameraChooseDuring)};
        int[] icons = {R.drawable.camera, R.drawable.fork_camera_front, R.drawable.msg_photo_switch2};
        int selected = java.util.Arrays.asList(modes).indexOf(RoundCameraSettings.mode());
        ForkChoicePopup.show(parent, anchor, names, icons, selected, i -> {
            RoundCameraSettings.setMode(modes[i]);
            if (onChanged != null) onChanged.run();
        });
    }

    public static void showQuality(BaseFragment parent, View anchor, Runnable onChanged) {
        String[] modes = {"standard", "auto", "custom"};
        CharSequence[] names = {ForkLocale.get(R.string.ForkVideoQualityStandard), ForkLocale.get(R.string.ForkVideoQualityAuto), ForkLocale.get(R.string.ForkVideoQualityCustom)};
        int selected = java.util.Arrays.asList(modes).indexOf(RoundCameraSettings.qualityMode());
        ForkChoicePopup.show(parent, anchor, names, null, selected, i -> {
            if (i != 2) {
                RoundCameraSettings.setQuality(modes[i], 0);
                if (onChanged != null) onChanged.run();
                return;
            }
            AndroidUtilities.runOnUIThread(() -> {
                int[] rates = {500000, 1000000, 1500000, 2500000, 4000000};
                CharSequence[] labels = {"0.5 Mbps", "1 Mbps", "1.5 Mbps", "2.5 Mbps", "4 Mbps"};
                int current = -1;
                for (int j = 0; j < rates.length; j++) if (rates[j] == RoundCameraSettings.customBitrate()) current = j;
                ForkChoicePopup.show(parent, anchor, labels, null, current, j -> {
                    RoundCameraSettings.setQuality("custom", rates[j]);
                    if (onChanged != null) onChanged.run();
                });
            }, 150);
        });
    }
}
