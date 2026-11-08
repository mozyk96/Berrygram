import org.telegram.messenger.fork.updater.GithubUpdateClient;
import org.telegram.messenger.fork.updater.UpdatePolicy;

/**
 * Validates the unified updater against the real public GitHub repository.
 *
 * <p>This test needs network access. It verifies that only official releases
 * are discovered, that the asset selection works with the real asset names and
 * that historical Beta/RC builds still receive official updates without any
 * downgrade path.</p>
 */
public class UpdaterLiveGithubTest {
    private static final String REPO = "mozyk96/Berrygram";

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        GithubUpdateClient.Release latest = GithubUpdateClient.latest(REPO);
        System.out.println("official release found: " + latest.version + " (" + latest.size + " bytes)");
        check(latest.version != null && !latest.version.isEmpty(), "the official stream returned no release");
        check(!UpdatePolicy.isPrerelease(latest.version), "the official stream returned a prerelease: " + latest.version);
        check(latest.digest != null && latest.digest.matches("[0-9a-f]{64}"), "the official release has no verifiable checksum");
        check(latest.url != null && latest.url.startsWith("https://github.com/" + REPO + "/releases/download/"),
                "the official release url is not a release asset: " + latest.url);
        check(UpdatePolicy.validAssetUrl(REPO, "v" + latest.version, latest.url.substring(latest.url.lastIndexOf('/') + 1), latest.url),
                "the official release url is rejected by the policy");
        check(UpdatePolicy.compareVersions(latest.version, "1.4.0") >= 0,
                "the official stream must include at least the published 1.4.0 release");

        // Historical prerelease installs discover the newest official release.
        check(UpdatePolicy.shouldShowRelease(latest.version, "1.4.0-beta.1"),
                "a 1.4.0 beta install must discover the official release");
        check(UpdatePolicy.shouldShowRelease(latest.version, "1.4.0-beta.2"),
                "a 1.4.0 beta.2 install must discover the official release");
        check(UpdatePolicy.shouldShowRelease(latest.version, "1.4.0-rc.1"),
                "a 1.4.0 RC install must discover the official release");
        if (UpdatePolicy.compareVersions(latest.version, "1.4.0") > 0) {
            check(UpdatePolicy.shouldShowRelease(latest.version, "1.4.0"),
                    "a 1.4.0 install must discover a newer official release");
        }
        check(!UpdatePolicy.shouldShowRelease(latest.version, latest.version),
                "the installed version must not be offered to itself");
        check(!UpdatePolicy.shouldShowRelease("1.3.0", "1.4.0-beta.2"),
                "an older official release must not be offered to a newer beta build");
        check(!UpdatePolicy.shouldShowRelease("1.3.0", "1.4.0-rc.1"),
                "an older official release must not be offered to a newer RC build");
        check(!UpdatePolicy.shouldShowRelease(latest.version + "-beta.1", "1.4.0"),
                "a prerelease must never be offered by the official updater");

        System.out.println("UpdaterLiveGithubTest passed (unified official stream)");
    }
}
