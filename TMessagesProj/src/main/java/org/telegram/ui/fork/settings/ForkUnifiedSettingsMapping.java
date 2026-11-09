package org.telegram.ui.fork.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maps every Berrygram feature to the Telegram settings screen that owns it.
 *
 * The map is the single source of truth of the settings integration. The
 * Berrygram root of the settings screen only keeps the Update Center, every
 * other feature is a block inside the screen that owns it, so the application
 * has exactly one settings system.
 */
public final class ForkUnifiedSettingsMapping {

    /** Identifier of the Update Center row, the only Berrygram row of the root. */
    public static final int ID_UPDATES = 108;

    /**
     * Legacy identifiers of the 1.4.1 shortcut rows. The rows are gone, the
     * identifiers stay so that old links keep opening the same screen.
     */
    public static final int LEGACY_ID_PRIVACY = 101;
    public static final int LEGACY_ID_EFFECTS = 102;
    public static final int LEGACY_ID_MEDIA = 103;
    public static final int LEGACY_ID_DIAGNOSTICS = 104;
    public static final int LEGACY_ID_TRANSLATION = 105;
    public static final int LEGACY_ID_ARCHIVE = 106;
    public static final int LEGACY_ID_PERFORMANCE = 107;
    public static final int LEGACY_ID_ABOUT = 108;

    /** Identifier of the removed standalone Berrygram settings entry. */
    public static final int REMOVED_HUB_ID = 27;

    /** The Telegram screen that hosts a Berrygram block. */
    public static final class Host {
        public final String key;
        public final String titleResource;
        public final String features;

        Host(String key, String titleResource, String features) {
            this.key = key;
            this.titleResource = titleResource;
            this.features = features;
        }
    }

    /** One Berrygram block and the Telegram screen it belongs to. */
    public static final class Entry {
        public final String hostKey;
        public final String feature;

        Entry(String hostKey, String feature) {
            this.hostKey = hostKey;
            this.feature = feature;
        }
    }

    public static final Host HOST_CHAT_SETTINGS = new Host("chat_settings", "SettingsChat", "effects, reactions, message menu, gestures, custom text, media and input");
    public static final Host HOST_PRIVACY = new Host("privacy", "SettingsPrivacy", "ghost mode, deleted messages, edit history, archive privacy, retention");
    public static final Host HOST_NOTIFICATIONS = new Host("notifications", "SettingsNotifications", "notification diagnostics");
    public static final Host HOST_DATA = new Host("data", "SettingsData", "archive browser, archive import, storage usage");
    public static final Host HOST_LANGUAGE = new Host("language", "SettingsLanguage", "translation");
    public static final Host HOST_POWER_SAVING = new Host("power_saving", "SettingsPowerSaving", "device analyzer, performance profile");

    private static final List<Host> HOSTS;
    private static final List<Entry> ENTRIES;

    static {
        List<Host> hosts = new ArrayList<>();
        hosts.add(HOST_CHAT_SETTINGS);
        hosts.add(HOST_PRIVACY);
        hosts.add(HOST_NOTIFICATIONS);
        hosts.add(HOST_DATA);
        hosts.add(HOST_LANGUAGE);
        hosts.add(HOST_POWER_SAVING);
        HOSTS = Collections.unmodifiableList(hosts);

        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(HOST_CHAT_SETTINGS.key, "effects"));
        entries.add(new Entry(HOST_CHAT_SETTINGS.key, "reactions"));
        entries.add(new Entry(HOST_CHAT_SETTINGS.key, "message_menu"));
        entries.add(new Entry(HOST_CHAT_SETTINGS.key, "gestures"));
        entries.add(new Entry(HOST_CHAT_SETTINGS.key, "custom_text"));
        entries.add(new Entry(HOST_CHAT_SETTINGS.key, "media_and_input"));
        entries.add(new Entry(HOST_PRIVACY.key, "ghost_mode"));
        entries.add(new Entry(HOST_PRIVACY.key, "deleted_messages"));
        entries.add(new Entry(HOST_PRIVACY.key, "edit_history"));
        entries.add(new Entry(HOST_PRIVACY.key, "archive_privacy"));
        entries.add(new Entry(HOST_PRIVACY.key, "retention"));
        entries.add(new Entry(HOST_NOTIFICATIONS.key, "diagnostics"));
        entries.add(new Entry(HOST_DATA.key, "archive"));
        entries.add(new Entry(HOST_LANGUAGE.key, "translation"));
        entries.add(new Entry(HOST_POWER_SAVING.key, "performance"));
        ENTRIES = Collections.unmodifiableList(entries);
    }

    private ForkUnifiedSettingsMapping() { }

    /** Every Telegram screen that hosts a Berrygram block. */
    public static List<Host> hosts() {
        return HOSTS;
    }

    /** Every Berrygram block of the unified settings. */
    public static List<Entry> entries() {
        return ENTRIES;
    }

    /** Verifies that the map itself has no duplicate identifiers. */
    public static boolean uniqueIdentifiers() {
        Set<String> keys = new HashSet<>();
        for (Host host : HOSTS) {
            if (!keys.add(host.key)) {
                return false;
            }
        }
        return true;
    }

    /** Verifies that every block is attached to a known Telegram screen. */
    public static boolean knownHosts() {
        Set<String> hosts = new HashSet<>();
        for (Host host : HOSTS) {
            hosts.add(host.key);
        }
        for (Entry entry : ENTRIES) {
            if (!hosts.contains(entry.hostKey)) {
                return false;
            }
        }
        return true;
    }

    /**
     * The root of the settings screen keeps the Update Center only, every other
     * Berrygram feature lives inside the Telegram screen that owns it.
     */
    public static int rootRows() {
        return 1;
    }
}
