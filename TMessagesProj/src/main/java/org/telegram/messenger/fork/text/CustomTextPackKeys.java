package org.telegram.messenger.fork.text;
/** Keeps legacy custom-pack keys stable while isolating named packs. */
public final class CustomTextPackKeys {
    private CustomTextPackKeys() {}
    public static boolean validId(String id) { return "custom".equals(id) || id != null && id.matches("pack-[0-9a-f-]{6,36}"); }
    public static String key(String packId, String resourceKey) {
        if (!validId(packId) || resourceKey == null || !resourceKey.matches("[A-Za-z][A-Za-z0-9_]*")) throw new IllegalArgumentException("Invalid pack key");
        return "custom".equals(packId) ? resourceKey : "pack:" + packId + ":" + resourceKey;
    }
}
