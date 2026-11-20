import org.telegram.messenger.fork.updater.GithubUpdateClient;
import org.telegram.messenger.fork.updater.UpdatePolicy;

/**
 * Validates the unified updater against the real public GitHub repository.
 *
 * <p>This test needs network access. It verifies that only official releases
 * are discovered and that asset selection and checksums match the public
 * artifacts. It does not install or verify compatibility with a legacy app.</p>
 */
public class UpdaterLiveGithubTest {
    private static final String REPO = "mozyk96/Berrygram";

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    public static void main(String[] args) throws Exception {
        boolean expectNone = args.length > 0 && args[0].equals("--no-compatible-release");
        GithubUpdateClient.Release latest;
        try {
            latest = GithubUpdateClient.latest(REPO);
        } catch (GithubUpdateClient.NoReleaseException noRelease) {
            if (!expectNone) throw noRelease;
            System.out.println("No compatible stable asset found for org.berrygram.client");
            return;
        }
        check(!expectNone, "unexpected compatible release: " + latest.version);
        System.out.println("official release found: " + latest.version + " (" + latest.size + " bytes)");
        if (args.length > 0) {
            check(args[0].equals(latest.version), "expected release " + args[0] + ", got " + latest.version);
        }
        if (args.length > 1) {
            check(args[1].equalsIgnoreCase(latest.digest), "published digest differs from the verified artifact");
        }
        check(latest.version != null && !latest.version.isEmpty(), "the official stream returned no release");
        check(!UpdatePolicy.isPrerelease(latest.version), "the official stream returned a prerelease: " + latest.version);
        check(latest.digest != null && latest.digest.matches("[0-9a-f]{64}"), "the official release has no verifiable checksum");
        check(latest.url != null && latest.url.startsWith("https://github.com/" + REPO + "/releases/download/"),
                "the official release url is not a release asset: " + latest.url);
        check(UpdatePolicy.validAssetUrl(REPO, "v" + latest.version, latest.url.substring(latest.url.lastIndexOf('/') + 1), latest.url),
                "the official release url is rejected by the policy");
        check(UpdatePolicy.shouldShowRelease(latest.version, "1.5.1"),
                "an older generation-2 install must be offered the release");
        check(!UpdatePolicy.shouldShowRelease(latest.version, latest.version),
                "the installed version must not be offered to itself");
        System.out.println("UpdaterLiveGithubTest passed (unified official stream)");
    }
}
