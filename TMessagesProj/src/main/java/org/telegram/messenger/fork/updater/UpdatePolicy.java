package org.telegram.messenger.fork.updater;
import java.net.URI;
import java.util.Locale;
/** Strict source and asset validation for stable, full APK releases. */
public final class UpdatePolicy {
    private UpdatePolicy() {}
    public static boolean validIdentity(String installedPackage, long installedVersion, java.util.Set<String> installedSigners,
                                        String candidatePackage, long candidateVersion, java.util.Set<String> candidateSigners) {
        return installedPackage != null && installedPackage.equals(candidatePackage) && candidateVersion > installedVersion
            && installedSigners != null && !installedSigners.isEmpty() && installedSigners.equals(candidateSigners);
    }
    public static Integer compareVersions(String a, String b) {
        if (a == null || b == null || !a.matches("[0-9]{1,9}\\.[0-9]{1,9}\\.[0-9]{1,9}") || !b.matches("[0-9]{1,9}\\.[0-9]{1,9}\\.[0-9]{1,9}")) return null;
        String[] left=a.split("\\."), right=b.split("\\.");
        for(int i=0;i<3;i++) { int comparison=Long.compare(Long.parseLong(left[i]),Long.parseLong(right[i])); if(comparison!=0) return comparison; }
        return 0;
    }
    public static boolean validRepository(String value) {
        if (value == null || value.length() > 140 || !value.matches("[A-Za-z0-9][A-Za-z0-9-]{0,38}/[A-Za-z0-9_.-]{1,100}")) return false;
        String repo=value.substring(value.indexOf('/')+1);
        return !repo.equals(".") && !repo.equals("..") && !repo.toLowerCase(Locale.ROOT).endsWith(".git");
    }
    public static boolean validAsset(String tag, String name) {
        if (tag == null || !tag.matches("v?[0-9]{1,9}\\.[0-9]{1,9}\\.[0-9]{1,9}")) return false;
        String version=tag.startsWith("v") ? tag.substring(1) : tag;
        return ("BerryGram-"+version+"-arm64-v8a.apk").equals(name);
    }
    public static boolean validDigest(String digest) { return digest != null && digest.matches("sha256:[0-9a-fA-F]{64}"); }
    public static boolean validSavedRelease(String repo, String version, String installed, String url, String digest, long size) {
        Integer comparison = compareVersions(version, installed);
        if (comparison == null || comparison <= 0 || size <= 0 || size > 512L * 1024 * 1024 || !validDigest("sha256:" + digest)) return false;
        String name = "BerryGram-" + version + "-arm64-v8a.apk";
        return validAssetUrl(repo, "v" + version, name, url) || validAssetUrl(repo, version, name, url);
    }
    public static boolean validAssetUrl(String repo, String tag, String name, String url) {
        if (!validRepository(repo) || !validAsset(tag,name) || url==null) return false;
        try {
            URI uri=new URI(url);
            return "https".equalsIgnoreCase(uri.getScheme()) && "github.com".equalsIgnoreCase(uri.getHost()) && uri.getPort()==-1
                && uri.getUserInfo()==null && uri.getQuery()==null && uri.getFragment()==null
                && ("/"+repo+"/releases/download/"+tag+"/"+name).equals(uri.getRawPath());
        } catch (Exception ignored) { return false; }
    }
}
