import org.telegram.ui.fork.settings.ForkUnifiedSettingsMapping;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Verifies the unified settings integration.
 *
 * <p>Berrygram has exactly one settings system: the root of the settings screen
 * keeps the Update Center only and every other Berrygram feature is a block
 * inside the Telegram screen that owns it. The test fails when the root grows a
 * duplicate row again or when a host screen loses its block.</p>
 */
public class ForkUnifiedSettingsMappingTest {
    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static String read(File file) throws Exception {
        return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
    }

    public static void main(String[] args) throws Exception {
        List<ForkUnifiedSettingsMapping.Host> hosts = ForkUnifiedSettingsMapping.hosts();
        List<ForkUnifiedSettingsMapping.Entry> entries = ForkUnifiedSettingsMapping.entries();

        check(!hosts.isEmpty(), "The unified settings map is empty");
        check(!entries.isEmpty(), "The unified settings map has no features");
        check(ForkUnifiedSettingsMapping.uniqueIdentifiers(), "The unified settings map has duplicate identifiers");
        check(ForkUnifiedSettingsMapping.knownHosts(), "A feature uses an unknown Telegram screen");
        check(hosts.size() >= 6, "Too few Telegram screens host a Berrygram block");
        check(ForkUnifiedSettingsMapping.rootRows() == 1,
                "The settings root must keep the Update Center only");

        Set<String> features = new HashSet<>();
        for (ForkUnifiedSettingsMapping.Entry entry : entries) {
            check(entry.feature != null && !entry.feature.isEmpty(),
                    "A feature of the unified settings has no name");
            check(features.add(entry.feature + "@" + entry.hostKey),
                    "The feature " + entry.feature + " is mapped twice");
        }

        String root = new File(".").getCanonicalPath();
        File settings = new File(root, "TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java");
        check(settings.isFile(), "SettingsActivity not found");
        String source = read(settings);

        // The root must not insert the Berrygram shortcut rows any more.
        check(!source.contains("ForkUnifiedSettings.applyTo"),
                "The settings root still inserts the Berrygram shortcut rows");
        check(!source.contains("ForkUnifiedSettings.buildAboutEntry"),
                "The settings root still inserts the Berrygram about row");
        check(source.contains("ForkUnifiedSettings.buildUpdateEntry"),
                "The settings root does not contain the Update Center row");
        check(!source.contains("presentSettingFragment(new ForkSettingsActivity())"),
                "The standalone Berrygram settings entry was not removed");

        // Every host screen carries its Berrygram block.
        File blocksFile = new File(root, "TMessagesProj/src/main/java/org/telegram/ui/ForkSettingsBlocks.kt");
        check(blocksFile.isFile(), "The shared Berrygram settings blocks are missing");
        String blocksSource = read(blocksFile);
        for (ForkUnifiedSettingsMapping.Host host : hosts) {
            check(blocksSource.contains("fun " + hostKeyToMethod(host.key) + "("),
                    "The shared blocks have no builder for " + host.key);
        }

        String[] hostFiles = {
                "TMessagesProj/src/main/java/org/telegram/ui/ThemeActivity.java",
                "TMessagesProj/src/main/java/org/telegram/ui/PrivacySettingsActivity.java",
                "TMessagesProj/src/main/java/org/telegram/ui/NotificationsSettingsActivity.java",
                "TMessagesProj/src/main/java/org/telegram/ui/DataSettingsActivity.java",
                "TMessagesProj/src/main/java/org/telegram/ui/LanguageSelectActivity.java",
                "TMessagesProj/src/main/java/org/telegram/ui/LiteModeSettingsActivity.java"
        };
        int withBlock = 0;
        for (String path : hostFiles) {
            File file = new File(root, path);
            check(file.isFile(), "The host screen " + path + " does not exist");
            String hostSource = read(file);
            if (hostSource.contains("ForkSettingsBlocks.")) {
                withBlock++;
            }
        }
        check(withBlock == hostFiles.length,
                "A Telegram settings screen lost its Berrygram block");

        // The legacy hub still exists and redirects to the unified settings.
        File hub = new File(root, "TMessagesProj/src/main/java/org/telegram/ui/ForkSettingsActivity.java");
        check(hub.isFile(), "The legacy Berrygram settings hub is missing, old links would break");
        String hubSource = read(hub);
        check(hubSource.contains("new SettingsActivity()"),
                "The legacy Berrygram settings hub does not redirect to the unified settings");

        System.out.println("ForkUnifiedSettingsMappingTest passed (" + entries.size()
                + " features in " + hosts.size() + " Telegram screens)");
    }

    private static String hostKeyToMethod(String key) {
        StringBuilder builder = new StringBuilder();
        boolean upper = false;
        for (int a = 0; a < key.length(); a++) {
            char c = key.charAt(a);
            if (c == '_') {
                upper = true;
                continue;
            }
            builder.append(upper ? Character.toUpperCase(c) : c);
            upper = false;
        }
        return builder.toString();
    }
}
