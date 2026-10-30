package org.telegram.messenger.fork.panels;
/** Existing Telegram glass is requested only when rendering capability and power state allow it. */
public final class PanelPolicy {
    private PanelPolicy() { }
    public static boolean useGlass(boolean enabled, boolean savingPower, int sdk, boolean capable) {
        return enabled && !savingPower && sdk >= 33 && capable;
    }
}
