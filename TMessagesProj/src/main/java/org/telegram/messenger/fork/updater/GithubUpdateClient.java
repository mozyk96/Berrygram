package org.telegram.messenger.fork.updater;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/** Network and signature verification for explicit, user-initiated stable releases. */
public final class GithubUpdateClient {
    private static final long MAX_APK = 512L * 1024 * 1024;
    private static final int MAX_METADATA = 1024 * 1024;
    public interface Progress { void update(long received, long total); }
    public static final class NoStableReleaseException extends java.io.IOException {
        public NoStableReleaseException() { super("No stable release found or repository unavailable"); }
    }
    public static final class Release {
        public String version, notes, url, digest;
        public long size;
    }
    private GithubUpdateClient() {}
    private static boolean allowedHost(String host) {
        return "api.github.com".equals(host) || "github.com".equals(host) || "release-assets.githubusercontent.com".equals(host) || "objects.githubusercontent.com".equals(host) || "github-releases.githubusercontent.com".equals(host);
    }
    private static HttpURLConnection open(String value) throws Exception { return open(value, 0); }
    private static HttpURLConnection open(String value, long offset) throws Exception {
        URL url=new URL(value);
        for (int redirect=0; redirect<=5; redirect++) {
            if (!"https".equalsIgnoreCase(url.getProtocol()) || url.getUserInfo()!=null || (url.getPort()!=-1 && url.getPort()!=443) || !allowedHost(url.getHost().toLowerCase(Locale.ROOT))) throw new SecurityException("Untrusted update URL");
            HttpURLConnection connection=(HttpURLConnection)url.openConnection();
            connection.setInstanceFollowRedirects(false); connection.setConnectTimeout(15000); connection.setReadTimeout(30000);
            connection.setRequestProperty("Accept","application/vnd.github+json"); connection.setRequestProperty("User-Agent","BerryGram-Updater");
            if (offset > 0) connection.setRequestProperty("Range", "bytes=" + offset + "-");
            int code=connection.getResponseCode();
            if (code>=300 && code<400) {
                String next=connection.getHeaderField("Location"); connection.disconnect();
                if (next==null) throw new java.io.IOException("Missing redirect");
                url=new URL(url,next); continue;
            }
            if (code!=200 && !(offset > 0 && code == 206)) {
                connection.disconnect();
                if (code == 404 && "api.github.com".equalsIgnoreCase(url.getHost()) && url.getPath().endsWith("/releases/latest")) throw new NoStableReleaseException();
                throw new java.io.IOException("Update server HTTP " + code);
            }
            return connection;
        }
        throw new java.io.IOException("Too many redirects");
    }
    public static Release latest(String repo) throws Exception {
        if (!UpdatePolicy.validRepository(repo)) throw new IllegalArgumentException("Invalid repository");
        HttpURLConnection connection=open("https://api.github.com/repos/"+repo+"/releases/latest");
        try (InputStream in=connection.getInputStream(); ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[8192]; int count,total=0;
            while ((count=in.read(buffer))!=-1) { total+=count; if(total>MAX_METADATA) throw new java.io.IOException("Release metadata too large"); out.write(buffer,0,count); }
            return parseRelease(repo,out.toString("UTF-8"));
        } finally { connection.disconnect(); }
    }
    public static Release parseRelease(String repo, String metadata) throws Exception {
        if (!UpdatePolicy.validRepository(repo) || metadata == null || metadata.length() > MAX_METADATA) throw new IllegalArgumentException("Invalid release metadata");
        JSONObject json=new JSONObject(metadata);
        if (json.optBoolean("draft",true) || json.optBoolean("prerelease",true)) throw new SecurityException("Not a stable release");
        String tag=json.optString("tag_name",""); JSONArray assets=json.optJSONArray("assets");
        if (assets==null) throw new java.io.IOException("Release has no assets");
        Release result=null;
        for(int i=0;i<assets.length();i++) {
            JSONObject asset=assets.optJSONObject(i); if(asset==null) continue;
            String name=asset.optString("name", ""), url=asset.optString("browser_download_url", ""), digest=asset.optString("digest", ""); long size=asset.optLong("size",0);
            if (!UpdatePolicy.validAsset(tag,name)) continue;
            if(result!=null || !UpdatePolicy.validAssetUrl(repo,tag,name,url) || !UpdatePolicy.validDigest(digest) || size<=0 || size>MAX_APK || !"uploaded".equals(asset.optString("state"))) throw new SecurityException("Invalid release asset");
            result=new Release(); result.version=tag.startsWith("v") ? tag.substring(1) : tag; result.url=url; result.digest=digest.substring(7).toLowerCase(Locale.ROOT); result.size=size;
            String notes=json.optString("body", ""); result.notes=notes.length()>12000 ? notes.substring(0,12000) : notes;
        }
        if(result==null) throw new java.io.IOException("No verified ARM64 APK asset");
        return result;
    }
    public static synchronized File downloadAndVerify(Context context, Release release, Progress progress) throws Exception {
        boolean arm64=false; for(String abi:Build.SUPPORTED_ABIS) if("arm64-v8a".equals(abi)) arm64=true;
        if(!arm64) throw new SecurityException("Unsupported device architecture");
        if (release == null || release.digest == null || !release.digest.matches("[0-9a-f]{64}") || release.size <= 0 || release.size > MAX_APK) throw new SecurityException("Invalid release metadata");
        File dir=new File(context.getCacheDir(),"berrygram-updates");
        if(!dir.isDirectory() && !dir.mkdirs()) throw new java.io.IOException("Cannot create update directory");
        File partial=new File(dir,"berrygram-" + release.digest + ".apk");
        boolean complete=false, networkPhase=true, retainPartial=false;
        HttpURLConnection connection=null;
        try {
            long offset=partial.isFile() ? partial.length() : 0;
            if (offset >= release.size) offset=0;
            connection=open(release.url,offset);
            boolean append=offset>0 && connection.getResponseCode()==206;
            if (append && !("bytes " + offset + "-" + (release.size - 1) + "/" + release.size).equals(connection.getHeaderField("Content-Range"))) throw new SecurityException("Invalid update range");
            if (!append) offset=0;
            MessageDigest hash=MessageDigest.getInstance("SHA-256"); long received=offset, lastProgress=0;
            if (append) try (InputStream existing=new java.io.FileInputStream(partial)) { byte[] buffer=new byte[32768]; int n; while((n=existing.read(buffer))!=-1) hash.update(buffer,0,n); }
            try(InputStream in=connection.getInputStream(); FileOutputStream out=new FileOutputStream(partial,append)) {
                byte[] buffer=new byte[32768]; int count;
                while((count=in.read(buffer))!=-1) {
                    if(Thread.currentThread().isInterrupted()) throw new InterruptedException();
                    received+=count; if(received>release.size || received>MAX_APK) throw new SecurityException("Update exceeds expected size");
                    hash.update(buffer,0,count); out.write(buffer,0,count);
                    long now=android.os.SystemClock.elapsedRealtime();
                    if(progress!=null && (now-lastProgress>=250 || received==release.size)) { progress.update(received,release.size); lastProgress=now; }
                }
                out.getFD().sync();
            }
            networkPhase=false;
            if(received!=release.size) throw new SecurityException("Update size mismatch");
            StringBuilder actual=new StringBuilder(); for(byte b:hash.digest()) actual.append(String.format(Locale.ROOT,"%02x",b & 255));
            if(!MessageDigest.isEqual(actual.toString().getBytes("US-ASCII"),release.digest.getBytes("US-ASCII"))) throw new SecurityException("Update digest mismatch");
            verifyPackage(context,partial,release.version);
            complete=true;
            return partial;
        } catch (java.io.IOException | InterruptedException e) {
            retainPartial=networkPhase && partial.isFile() && partial.length()>0 && partial.length()<release.size;
            throw e;
        } finally { if(connection!=null) connection.disconnect(); if(!complete && !retainPartial && partial.exists()) partial.delete(); }
    }
    private static Set<String> signers(PackageInfo info) throws Exception {
        Signature[] signatures=Build.VERSION.SDK_INT>=28 && info.signingInfo!=null ? info.signingInfo.getApkContentsSigners() : info.signatures;
        Set<String> result=new HashSet<>(); if(signatures==null) return result;
        for(Signature signature:signatures) result.add(Arrays.toString(MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())));
        return result;
    }
    private static void verifyPackage(Context context, File apk, String version) throws Exception {
        PackageManager pm=context.getPackageManager(); int flags=Build.VERSION.SDK_INT>=28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
        PackageInfo candidate=pm.getPackageArchiveInfo(apk.getAbsolutePath(),flags), installed=pm.getPackageInfo(context.getPackageName(),flags);
        if(candidate==null || !context.getPackageName().equals(candidate.packageName) || !version.equals(candidate.versionName)) throw new SecurityException("Update package or version mismatch");
        if(candidate.applicationInfo == null || (candidate.applicationInfo.flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) throw new SecurityException("Debug update rejected");
        boolean hasArm64=false;
        try (java.util.zip.ZipFile zip=new java.util.zip.ZipFile(apk)) {
            java.util.Enumeration<? extends java.util.zip.ZipEntry> entries=zip.entries();
            while(entries.hasMoreElements()) { String name=entries.nextElement().getName(); if(name.startsWith("lib/arm64-v8a/") && name.endsWith(".so")) { hasArm64=true; break; } }
        }
        if(!hasArm64) throw new SecurityException("Update has no ARM64 native libraries");
        long candidateCode=Build.VERSION.SDK_INT>=28 ? candidate.getLongVersionCode() : candidate.versionCode;
        long installedCode=Build.VERSION.SDK_INT>=28 ? installed.getLongVersionCode() : installed.versionCode;
        Set<String> expected=signers(installed), actual=signers(candidate);
        if(!UpdatePolicy.validIdentity(context.getPackageName(),installedCode,expected,candidate.packageName,candidateCode,actual)) throw new SecurityException("Update version or signing certificate mismatch");
    }
}
