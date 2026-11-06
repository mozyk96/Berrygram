package org.telegram.messenger.fork.updater;
import java.net.URI;
import java.util.Locale;
/** Strict source and asset validation for stable, full APK releases. */
public final class UpdatePolicy {
    private UpdatePolicy() {}
    public static boolean enoughSpace(long available, long total, long existing) {
        return available >= 0 && total > 0 && existing >= 0 && existing <= total && available >= total - existing;
    }
    public static boolean validIdentity(String installedPackage, long installedVersion, java.util.Set<String> installedSigners,
                                        String candidatePackage, long candidateVersion, java.util.Set<String> candidateSigners) {
        return installedPackage != null && installedPackage.equals(candidatePackage) && candidateVersion > installedVersion
            && installedSigners != null && !installedSigners.isEmpty() && installedSigners.equals(candidateSigners);
    }
    /** Equal codes are allowed only for an explicit Beta to Stable recovery. */
    public static boolean compatibleCode(long installed, long candidate, boolean stableRecovery) {
        return candidate > installed || (stableRecovery && candidate == installed);
    }
    private static final String VERSION = "(0|[1-9][0-9]{0,8})\\.(0|[1-9][0-9]{0,8})\\.(0|[1-9][0-9]{0,8})(?:-([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+([0-9A-Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?";
    private static final java.util.regex.Pattern VERSION_PATTERN = java.util.regex.Pattern.compile(VERSION);
    private static java.util.regex.Matcher version(String value) {
        if (value == null || value.length() > 128) return null;
        java.util.regex.Matcher m = VERSION_PATTERN.matcher(value);
        if (!m.matches()) return null;
        if (m.group(4) != null) for (String id : m.group(4).split("\\.")) {
            if (id.matches("[0-9]+") && id.length() > 1 && id.charAt(0) == '0') return null;
        }
        return m;
    }
    public static Integer compareVersions(String a, String b) {
        java.util.regex.Matcher left = version(a), right = version(b);
        if (left == null || right == null) return null;
        for (int i = 1; i <= 3; i++) {
            int c = Long.compare(Long.parseLong(left.group(i)), Long.parseLong(right.group(i)));
            if (c != 0) return c;
        }
        String l = left.group(4), r = right.group(4);
        if (l == null || r == null) return l == r ? 0 : l == null ? 1 : -1;
        String[] ls = l.split("\\."), rs = r.split("\\.");
        for (int i = 0; i < Math.min(ls.length, rs.length); i++) {
            boolean ln = ls[i].matches("[0-9]+"), rn = rs[i].matches("[0-9]+");
            int c;
            if (ln && rn) { c = Integer.compare(ls[i].length(), rs[i].length()); if (c == 0) c = ls[i].compareTo(rs[i]); }
            else if (ln != rn) c = ln ? -1 : 1;
            else c = ls[i].compareTo(rs[i]);
            if (c != 0) return c;
        }
        return Integer.compare(ls.length, rs.length);
    }
    public static boolean shouldShowRelease(String candidate, String installed, boolean betaChannel) {
        Integer comparison = compareVersions(candidate, installed);
        return comparison != null && (comparison > 0 || (!betaChannel && isPrerelease(installed) && comparison < 0));
    }
    public static boolean isPrerelease(String value) { java.util.regex.Matcher m = version(value); return m != null && m.group(4) != null; }
    public static boolean validRepository(String value) {
        if (value == null || value.length() > 140 || !value.matches("[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9_.-]{1,100}")) return false;
        String repo=value.substring(value.indexOf('/')+1);
        return !repo.equals(".") && !repo.equals("..") && !repo.toLowerCase(Locale.ROOT).endsWith(".git");
    }
    public static boolean isArm64Asset(String tag, String name) {
        if (tag == null || name == null) return false;
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        if (version(version) == null) return false;
        return ("BerryGram-" + version + "-arm64-v8a.apk").equalsIgnoreCase(name);
    }

    public static boolean isUniversalAsset(String tag, String name) {
        if (tag == null || name == null) return false;
        String version = tag.startsWith("v") ? tag.substring(1) : tag;
        if (version(version) == null) return false;
        return ("BerryGram-" + version + "-universal.apk").equalsIgnoreCase(name);
    }

    public static boolean validAsset(String tag, String name) {
        return isArm64Asset(tag, name) || isUniversalAsset(tag, name);
    }
    public static boolean validDigest(String digest) { return digest != null && digest.matches("sha256:[0-9a-fA-F]{64}"); }
    public static boolean validSavedRelease(String repo, String version, String installed, String url, String digest, long size) {
        return validSavedRelease(repo, version, installed, url, digest, size, true);
    }
    public static boolean validSavedRelease(String repo, String version, String installed, String url, String digest, long size, boolean betaChannel) {
        if (!shouldShowRelease(version, installed, betaChannel) || size <= 0 || size > 512L * 1024 * 1024 || !validDigest("sha256:" + digest)) return false;
        String arm64 = "BerryGram-" + version + "-arm64-v8a.apk";
        String universal = "BerryGram-" + version + "-universal.apk";
        return validAssetUrl(repo, "v" + version, arm64, url) || validAssetUrl(repo, version, arm64, url)
            || validAssetUrl(repo, "v" + version, universal, url) || validAssetUrl(repo, version, universal, url);
    }
    public static boolean validAssetUrl(String repo, String tag, String name, String url) {
        if (!validRepository(repo) || !validAsset(tag, name) || url == null) return false;
        try {
            URI uri = new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && "github.com".equalsIgnoreCase(uri.getHost()) && uri.getPort() == -1
                && uri.getUserInfo() == null && uri.getQuery() == null && uri.getFragment() == null
                && ("/" + repo + "/releases/download/" + tag + "/" + name).equalsIgnoreCase(uri.getRawPath());
        } catch (Exception ignored) { return false; }
    }
}
