import org.telegram.ui.fork.settings.ForkUnifiedSettingsMapping;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Verifies the unified settings integration: every Berrygram feature has a
 * destination, the identifiers are unique, the removed standalone entry is
 * gone and the referenced screens exist.
 */
public class ForkUnifiedSettingsMappingTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        List<ForkUnifiedSettingsMapping.Entry> entries = ForkUnifiedSettingsMapping.entries();

        check(!entries.isEmpty(), "The unified settings map is empty");
        check(ForkUnifiedSettingsMapping.uniqueIdentifiers(), "The unified settings map has duplicate identifiers");
        check(ForkUnifiedSettingsMapping.knownAnchors(), "An entry uses an unknown Telegram settings row");

        // Every relocated feature has its own destination screen.
        Set<String> destinations = new HashSet<>();
        for (ForkUnifiedSettingsMapping.Entry entry : entries) {
            check(entry.titleResource != null && !entry.titleResource.isEmpty(),
                    "Entry " + entry.id + " has no title resource");
            check(entry.subtitleResource != null && !entry.subtitleResource.isEmpty(),
                    "Entry " + entry.id + " has no subtitle resource");
            check(entry.destination != null && entry.destination.startsWith("org.telegram.ui."),
                    "Entry " + entry.id + " has no valid destination");
            destinations.add(entry.destination);
        }
        check(destinations.size() >= 5, "Too few distinct destinations, features would be lost");

        // The removed standalone entry must not be created any more.
        String root = new File(".").getCanonicalPath();
        File settings = new File(root, "TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java");
        check(settings.isFile(), "SettingsActivity not found");
        String source = new String(Files.readAllBytes(settings.toPath()), StandardCharsets.UTF_8);
        check(!source.contains("SettingCell.Factory.of(27,"),
                "The standalone Berrygram settings entry was not removed");
        check(!source.contains("ForkLocale.get(R.string.ForkSettingsTitle), null"),
                "The standalone Berrygram settings row is still present");

        // The unified entries are created and every destination is opened.
        for (ForkUnifiedSettingsMapping.Entry entry : entries) {
            check(source.contains("case org.telegram.ui.fork.settings.ForkUnifiedSettingsMapping." + "ID_"),
                    "The unified settings switch does not handle every entry");
            check(source.contains(entry.destination.substring(entry.destination.lastIndexOf('.') + 1) + "("),
                    "The destination of entry " + entry.id + " is not opened");
            String simple = entry.destination.substring(entry.destination.lastIndexOf('.') + 1);
            File screen = new File(root, "TMessagesProj/src/main/java/" + entry.destination.replace('.', '/') + ".kt");
            if (!screen.isFile()) {
                screen = new File(root, "TMessagesProj/src/main/java/" + entry.destination.replace('.', '/') + ".java");
            }
            check(screen.isFile(), "The destination screen " + entry.destination + " does not exist");
            check(simple.length() > 0, "Invalid destination name");
        }

        // The old hub still exists and redirects to the unified settings.
        File hub = new File(root, "TMessagesProj/src/main/java/org/telegram/ui/ForkSettingsActivity.java");
        check(hub.isFile(), "The legacy Berrygram settings hub is missing, old links would break");
        String hubSource = new String(Files.readAllBytes(hub.toPath()), StandardCharsets.UTF_8);
        check(hubSource.contains("new SettingsActivity()"),
                "The legacy Berrygram settings hub does not redirect to the unified settings");

        System.out.println("ForkUnifiedSettingsMappingTest passed (" + entries.size() + " relocated features)");
    }
}
