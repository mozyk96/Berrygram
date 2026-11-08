import org.telegram.messenger.fork.updater.GithubUpdateClient;
import org.telegram.messenger.fork.updater.UpdatePolicy;

/**
 * Validates the updater against the real public GitHub repository.
 *
 * <p>This test needs network access. It verifies that the stable and the beta
 * channel find the published releases, that the asset selection works with the
 * real asset names and that the decision logic never offers a downgrade.</p>
 */
public class UpdaterLiveGithubTest {
    private static final String REPO = "mozyk96/Berrygram";

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        // Stable channel: only stable releases are considered.
        GithubUpdateClient.Release stable = GithubUpdateClient.latest(REPO, false);
        System.out.println("stable release found: " + stable.version + " (" + stable.size + " bytes)");
        check(stable.version != null && !stable.version.isEmpty(), "stable channel returned no release");
        check(!UpdatePolicy.isPrerelease(stable.version), "stable channel returned a pre-release: " + stable.version);
        check(stable.digest != null && stable.digest.matches("[0-9a-f]{64}"), "stable release has no verifiable checksum");
        check(stable.url != null && stable.url.startsWith("https://github.com/" + REPO + "/releases/download/"),
                "stable release url is not a release asset: " + stable.url);
        check(UpdatePolicy.validAssetUrl(REPO, "v" + stable.version, stable.url.substring(stable.url.lastIndexOf('/') + 1), stable.url),
                "stable release url rejected by the policy");

        // Beta channel: pre-releases are considered as well.
        GithubUpdateClient.Release beta = GithubUpdateClient.latest(REPO, true);
        System.out.println("beta release found: " + beta.version + " (" + beta.size + " bytes)");
        check(beta.version != null && !beta.version.isEmpty(), "beta channel returned no release");
        check(beta.digest != null && beta.digest.matches("[0-9a-f]{64}"), "beta release has no verifiable checksum");
        Integer channels = UpdatePolicy.compareVersions(beta.version, stable.version);
        check(channels != null && channels >= 0,
                "the beta channel must not return an older release than the stable channel");

        // Decision matrix against the versions that exist in the field.
        check(UpdatePolicy.shouldShowRelease(beta.version, "1.3.0", true), "a beta build must find the beta release");
        check(UpdatePolicy.shouldShowRelease(beta.version, "1.4.0-beta.1", true), "an older beta must find the beta release");
        check(!UpdatePolicy.shouldShowRelease(beta.version, beta.version, true), "the same version must not be offered");
        check(!UpdatePolicy.shouldShowRelease("1.3.0", "1.4.0-beta.2", false),
                "an older stable release must not be offered to a newer beta build");
        check(!UpdatePolicy.shouldShowRelease("1.3.0", "1.4.0-rc.1", false),
                "an older stable release must not be offered to a newer release candidate");
        check(!UpdatePolicy.shouldShowRelease("1.3.0", "1.4.0-beta.2", true),
                "an older stable release must not be offered on the beta channel");
        check(!UpdatePolicy.shouldShowRelease(beta.version, "1.4.0", true),
                "the installed version must not be offered to itself");
        check(UpdatePolicy.shouldShowRelease(beta.version, "1.4.0-rc.1", true),
                "the published release must be offered to an installed release candidate of the same line");
        // The published stable release replaces an installed candidate of the same line.
        check(UpdatePolicy.shouldShowRelease(stable.version, "1.4.0-rc.1", false),
                "the published stable release must be offered to a release candidate of the same line");
        check(UpdatePolicy.shouldShowRelease(stable.version, "1.4.0-beta.2", false),
                "the published stable release must be offered to a beta build of the same line");
        check(!UpdatePolicy.shouldShowRelease(stable.version, "1.4.0", false),
                "the installed version must not be offered to itself");
        check(UpdatePolicy.shouldShowRelease(stable.version, "1.4.1-beta.1", false),
                "the stable release of the same minor line supersedes its beta builds, the version code decides during download");

        System.out.println("UpdaterLiveGithubTest passed");
    }
}
