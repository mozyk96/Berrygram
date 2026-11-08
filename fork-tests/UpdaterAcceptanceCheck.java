import org.telegram.messenger.fork.updater.UpdatePolicy;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks the published GitHub release against the rules the in-app updater
 * applies, so a release is only considered usable when the app can really
 * discover and install it.
 */
public class UpdaterAcceptanceCheck {
    private static final String REPO = "mozyk96/Berrygram";
    private static final String TAG = "v1.4.0-beta.2";

    public static void main(String[] args) throws Exception {
        String json = read("https://api.github.com/repos/" + REPO + "/releases?per_page=30");
        boolean found = false;
        List<String> accepted = new ArrayList<>();
        int index = json.indexOf("\"tag_name\":\"" + TAG + "\"");
        if (index < 0) {
            throw new AssertionError("Release " + TAG + " is not published");
        }
        int start = json.lastIndexOf("{", index);
        int end = json.indexOf("\"assets\"", index);
        if (end < 0) {
            throw new AssertionError("Release " + TAG + " has no assets section");
        }
        String prerelease = json.contains("\"prerelease\": true") || json.contains("\"prerelease\":true") ? "true" : "false";
        String assetName = "";
        String digest = "";
        java.util.regex.Matcher nameMatcher = java.util.regex.Pattern
                .compile("\"name\"\\s*:\\s*\"(BerryGram-1\\.4\\.0-beta\\.2-arm64-v8a\\.apk)\"").matcher(json);
        if (nameMatcher.find()) {
            assetName = nameMatcher.group(1);
            String tail = json.substring(nameMatcher.end(), Math.min(json.length(), nameMatcher.end() + 3000));
            java.util.regex.Matcher digestMatcher = java.util.regex.Pattern
                    .compile("\"digest\"\\s*:\\s*\"sha256:([0-9a-fA-F]{64})\"").matcher(tail);
            if (digestMatcher.find()) {
                digest = digestMatcher.group(1);
            }
        }
        boolean isPrerelease = "true".equals(prerelease);
        boolean arm64 = UpdatePolicy.isArm64Asset(TAG, assetName);
        boolean digestOk = UpdatePolicy.validDigest("sha256:" + digest);
        boolean versionOk = UpdatePolicy.compareVersions("1.4.0-beta.2", "1.4.0-beta.1") != null
                && UpdatePolicy.compareVersions("1.4.0-beta.2", "1.4.0-beta.1") > 0;
        System.out.println("tag=" + TAG + " prerelease=" + isPrerelease);
        System.out.println("asset=" + assetName + " arm64Accepted=" + arm64);
        System.out.println("digestPresent=" + digestOk + " versionGreaterThanBeta1=" + versionOk);
        if (!arm64) {
            throw new AssertionError("Updater would not accept the asset name");
        }
        if (!digestOk) {
            throw new AssertionError("Updater would not accept the digest");
        }
        if (!versionOk) {
            throw new AssertionError("Version comparison failed");
        }
        accepted.add(TAG);
        found = true;
        if (!found) {
            throw new AssertionError("Release not found");
        }
        System.out.println("UpdaterAcceptanceCheck passed for " + TAG);
    }

    private static String read(String url) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestProperty("User-Agent", "berrygram-audit");
        connection.setConnectTimeout(20000);
        connection.setReadTimeout(20000);
        try (InputStream input = connection.getInputStream()) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = input.read(buffer)) > 0) {
                output.write(buffer, 0, read);
            }
            return output.toString("UTF-8");
        }
    }
}
