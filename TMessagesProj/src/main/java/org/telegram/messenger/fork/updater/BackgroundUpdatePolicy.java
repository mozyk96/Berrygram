package org.telegram.messenger.fork.updater;

/** Restricts automatic downloads to the user's chosen network transports. */
public final class BackgroundUpdatePolicy {
    public static final int OFF = 0, WIFI = 1, MOBILE = 2, ANY = 3;
    private BackgroundUpdatePolicy() { }
    public static boolean allowed(int mode, boolean wifi, boolean mobile) {
        switch (mode) {
            case WIFI: return wifi && !mobile;
            case MOBILE: return mobile && !wifi;
            case ANY: return wifi || mobile;
            default: return false;
        }
    }
}
