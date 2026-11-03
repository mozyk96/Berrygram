import org.telegram.messenger.fork.updater.UpdatePolicy;
public class UpdatePolicyTest {
 public static void main(String[] args) {
  java.util.Set<String> signer=java.util.Collections.singleton("cert-a");
  check(UpdatePolicy.validIdentity("app",10,signer,"app",11,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"other",11,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"app",10,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"app",11,java.util.Collections.singleton("cert-b")));
  check(!UpdatePolicy.validIdentity("app",10,java.util.Collections.emptySet(),"app",11,java.util.Collections.emptySet()));
  check(UpdatePolicy.enoughSpace(100, 100, 0));
  check(UpdatePolicy.enoughSpace(40, 100, 60));
  check(!UpdatePolicy.enoughSpace(39, 100, 60));
  check(!UpdatePolicy.enoughSpace(100, 100, 101));
  check(!UpdatePolicy.enoughSpace(-1, 100, 0));
  check(UpdatePolicy.compareVersions("1.0.0","1.0.0")==0);
  check(UpdatePolicy.compareVersions("1.10.0","1.9.9")>0);
  check(UpdatePolicy.compareVersions("2.0.0","3.0.0")<0);
  check(UpdatePolicy.compareVersions("unknown","1.0.0")==null);
  check(UpdatePolicy.compareVersions("1.2.0-beta.1","1.2.0-beta.2") < 0);
  check(UpdatePolicy.compareVersions("1.2.0-beta.10","1.2.0-beta.2") > 0);
  check(UpdatePolicy.compareVersions("1.2.0-beta.3","1.2.0") < 0);
  check(UpdatePolicy.shouldShowRelease("1.2.0", "1.2.0-rc.2", true));
  check(UpdatePolicy.shouldShowRelease("1.2.0", "1.2.1-beta.1", false));
  check(!UpdatePolicy.shouldShowRelease("1.2.0", "1.2.1-beta.1", true));
  check(!UpdatePolicy.shouldShowRelease("1.2.0", "1.2.0", false));
  check(!UpdatePolicy.shouldShowRelease("bad", "1.2.0", false));
  check(UpdatePolicy.compareVersions("1.2.0+build.1","1.2.0+build.2") == 0);
  check(UpdatePolicy.compareVersions("1.2.0-beta.01","1.2.0") == null);
  check(UpdatePolicy.validAsset("v1.2.0-beta.1","BerryGram-1.2.0-beta.1-arm64-v8a.apk"));
  check(UpdatePolicy.validIdentity("app",72489,signer,"app",72499,signer));
  check(UpdatePolicy.validSavedRelease("owner/project", "1.2.0-beta.2", "1.2.0-beta.1", "https://github.com/owner/project/releases/download/v1.2.0-beta.2/BerryGram-1.2.0-beta.2-arm64-v8a.apk", "a".repeat(64), 100));
  check(UpdatePolicy.validRepository("owner/project"));
  check(!UpdatePolicy.validRepository("owner/../other"));
  check(!UpdatePolicy.validRepository("https://github.com/owner/project"));
  check(UpdatePolicy.validAsset("v1.0.0","BerryGram-1.0.0-arm64-v8a.apk"));
  check(!UpdatePolicy.validAsset("v1.0.0-beta.01","BerryGram-1.0.0-beta.01-arm64-v8a.apk"));
  check(!UpdatePolicy.validAsset("v1.0.0","BerryGram-1.0.1-arm64-v8a.apk"));
  check(UpdatePolicy.validDigest("sha256:"+"a".repeat(64)));
  check(!UpdatePolicy.validDigest("sha256:"+"z".repeat(64)));
  check(UpdatePolicy.validAssetUrl("owner/project","v1.0.0","BerryGram-1.0.0-arm64-v8a.apk","https://github.com/owner/project/releases/download/v1.0.0/BerryGram-1.0.0-arm64-v8a.apk"));
  check(!UpdatePolicy.validAssetUrl("owner/project","v1.0.0","BerryGram-1.0.0-arm64-v8a.apk","https://github.com.evil.test/a"));
  String url="https://github.com/owner/project/releases/download/v1.2.3/BerryGram-1.2.3-arm64-v8a.apk";
  String digest="a".repeat(64);
  check(UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.0.0", url, digest, 100));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.2.3", url, digest, 100));
  check(UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.3.0-beta.1", url, digest, 100, false));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.3.0-beta.1", url, digest, 100, true));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.0.0", url, digest, 0));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.0.0", "https://evil.test/file", digest, 100));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.0.0", url, "invalid", 100));
  System.out.println("UpdatePolicyTest passed");
 }
 static void check(boolean v){if(!v) throw new AssertionError();}
}
