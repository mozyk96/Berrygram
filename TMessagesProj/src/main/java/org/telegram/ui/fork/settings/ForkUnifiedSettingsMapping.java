package org.telegram.ui.fork.settings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Maps every Berrygram feature to its place inside the unified Telegram
 * settings. The map is the single source of truth for the settings
 * integration and is verified by a regression test.
 */
public final class ForkUnifiedSettingsMapping {
    /** Identifier of the removed standalone Berrygram settings entry. */
    public static final int REMOVED_HUB_ID = 27;

    public static final int ID_PRIVACY = 101;
    public static final int ID_EFFECTS = 102;
    public static final int ID_MEDIA = 103;
    public static final int ID_DIAGNOSTICS = 104;
    public static final int ID_TRANSLATION = 105;
    public static final int ID_ARCHIVE = 106;
    public static final int ID_PERFORMANCE = 107;
    public static final int ID_ABOUT = 108;

    /** Telegram settings row the Berrygram entry is placed after. */
    public static final int ANCHOR_PRIVACY = 3;
    public static final int ANCHOR_APPEARANCE = 2;
    public static final int ANCHOR_NOTIFICATIONS = 5;
    public static final int ANCHOR_LANGUAGE = 10;
    public static final int ANCHOR_DATA = 6;
    public static final int ANCHOR_POWER = 9;

    /** One relocated Berrygram feature. */
    public static final class Entry {
        public final int id;
        public final int anchorId;
        public final String titleResource;
        public final String subtitleResource;
        public final String destination;

        Entry(int id, int anchorId, String titleResource, String subtitleResource, String destination) {
            this.id = id;
            this.anchorId = anchorId;
            this.titleResource = titleResource;
            this.subtitleResource = subtitleResource;
            this.destination = destination;
        }
    }

    private ForkUnifiedSettingsMapping() { }

    /** Every Berrygram entry of the unified settings screen. */
    public static List<Entry> entries() {
        List<Entry> entries = new ArrayList<>();
        entries.add(new Entry(ID_PRIVACY, ANCHOR_PRIVACY, "ForkUnifiedPrivacyTitle", "ForkUnifiedPrivacyInfo",
                "org.telegram.ui.ForkStealthSettingsActivity"));
        entries.add(new Entry(ID_EFFECTS, ANCHOR_APPEARANCE, "ForkUnifiedEffectsTitle", "ForkUnifiedEffectsInfo",
                "org.telegram.ui.ForkAppearanceSettingsActivity"));
        entries.add(new Entry(ID_MEDIA, ANCHOR_APPEARANCE, "ForkUnifiedMediaTitle", "ForkUnifiedMediaInfo",
                "org.telegram.ui.ForkMediaSettingsActivity"));
        entries.add(new Entry(ID_DIAGNOSTICS, ANCHOR_NOTIFICATIONS, "ForkUnifiedDiagnosticsTitle", "ForkUnifiedDiagnosticsInfo",
                "org.telegram.ui.ForkNotificationDiagnosticsActivity"));
        entries.add(new Entry(ID_TRANSLATION, ANCHOR_LANGUAGE, "ForkUnifiedTranslationTitle", "ForkUnifiedTranslationInfo",
                "org.telegram.ui.ForkTranslationSettingsActivity"));
        entries.add(new Entry(ID_ARCHIVE, ANCHOR_DATA, "ForkUnifiedArchiveTitle", "ForkUnifiedArchiveInfo",
                "org.telegram.ui.ForkArchiveActivity"));
        entries.add(new Entry(ID_PERFORMANCE, ANCHOR_POWER, "ForkUnifiedPerformanceTitle", "ForkUnifiedPerformanceInfo",
                "org.telegram.ui.ForkPerformanceSettingsActivity"));
        return Collections.unmodifiableList(entries);
    }

    /** Verifies that the map itself has no duplicate identifiers. */
    public static boolean uniqueIdentifiers() {
        Set<Integer> ids = new HashSet<>();
        for (Entry entry : entries()) {
            if (!ids.add(entry.id)) {
                return false;
            }
        }
        return true;
    }

    /** Verifies that every entry is attached to a Telegram settings row. */
    public static boolean knownAnchors() {
        Set<Integer> anchors = new HashSet<>();
        anchors.add(ANCHOR_PRIVACY);
        anchors.add(ANCHOR_APPEARANCE);
        anchors.add(ANCHOR_NOTIFICATIONS);
        anchors.add(ANCHOR_LANGUAGE);
        anchors.add(ANCHOR_DATA);
        anchors.add(ANCHOR_POWER);
        for (Entry entry : entries()) {
            if (!anchors.contains(entry.anchorId)) {
                return false;
            }
        }
        return true;
    }
}
