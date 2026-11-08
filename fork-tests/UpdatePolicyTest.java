import org.telegram.messenger.fork.updater.UpdatePolicy;
public class UpdatePolicyTest {
 public static void main(String[] args) {
  java.util.Set<String> signer=java.util.Collections.singleton("cert-a");
  check(UpdatePolicy.validIdentity("app",10,signer,"app",11,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"other",11,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"app",10,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"app",11,java.util.Collections.singleton("cert-b")));
  check(!UpdatePolicy.validIdentity("app",10,java.util.Collections.emptySet(),"app",11,java.util.Collections.emptySet()));
  check(UpdatePolicy.compatibleCode(100, 101, false));
  check(!UpdatePolicy.compatibleCode(100, 100, false));
  check(UpdatePolicy.compatibleCode(100, 100, true));
  check(!UpdatePolicy.compatibleCode(100, 99, true));
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
  check(UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.2.3-beta.1", url, digest, 100, false));
  check(UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.2.3-beta.1", url, digest, 100, true), "the stable release of the same line supersedes its beta build");
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.3.0-beta.1", url, digest, 100, false), "an older stable release is never a valid saved release");
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.3.0-beta.1", url, digest, 100, true));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.0.0", url, digest, 0));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.0.0", "https://evil.test/file", digest, 100));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.2.3", "1.0.0", url, "invalid", 100));
  check(UpdatePolicy.shouldShowRelease("1.2.0", "1.2.0-beta.9", false));
  check(UpdatePolicy.shouldShowRelease("1.2.0", "1.2.0-beta.9", true));
  check(UpdatePolicy.compareVersions("1.2.0", "1.2.0-beta.9") > 0);
  check(UpdatePolicy.compareVersions("1.3.0", "1.3.0-beta.5") > 0);
  check(UpdatePolicy.shouldShowRelease("1.3.0", "1.3.0-beta.5", false));
  check(UpdatePolicy.shouldShowRelease("1.3.0", "1.3.0-beta.5", true));
  check(UpdatePolicy.validAsset("v1.3.0", "BerryGram-1.3.0-arm64-v8a.apk"));
  check(UpdatePolicy.validSavedRelease("owner/project", "1.3.0", "1.3.0-beta.5",
      "https://github.com/owner/project/releases/download/v1.3.0/BerryGram-1.3.0-arm64-v8a.apk",
      "a".repeat(64), 100, false));
  check(UpdatePolicy.validIdentity("org.example.forkclient",72569,signer,"org.example.forkclient",72579,signer));
  check(UpdatePolicy.compareVersions("1.4.0-beta.2", "1.4.0-beta.1") > 0);
  check(UpdatePolicy.compareVersions("1.4.0", "1.4.0-beta.2") > 0);
  check(UpdatePolicy.shouldShowRelease("1.4.0-beta.2", "1.4.0-beta.1", true));
  // A stable release of an older line is never offered, that would be a downgrade.
  check(!UpdatePolicy.shouldShowRelease("1.3.0", "1.4.0-beta.2", false), "older stable release must not be offered to a beta build");
  check(!UpdatePolicy.shouldShowRelease("1.3.0", "1.4.0-rc.1", false), "older stable release must not be offered to a release candidate");
  check(!UpdatePolicy.shouldShowRelease("1.3.0", "1.4.0-beta.2", true), "older stable release must not be offered on the beta channel");
  // The stable release of the same line replaces an installed pre-release.
  check(UpdatePolicy.shouldShowRelease("1.4.0", "1.4.0-rc.1", false), "stable release of the same line is offered");
  check(UpdatePolicy.shouldShowRelease("1.4.0", "1.4.0-beta.2", false), "stable release of the same line is offered to a beta build");
  check(UpdatePolicy.isStableRecovery("1.4.0", "1.4.0-beta.2", false), "same line recovery detected");
  check(!UpdatePolicy.isStableRecovery("1.3.0", "1.4.0-beta.2", false), "different line is not a recovery");
  check(!UpdatePolicy.isStableRecovery("1.4.0", "1.4.0-beta.2", true), "the beta channel does not use the stable recovery");
  check(!UpdatePolicy.isStableRecovery("1.4.0", "1.4.0", false), "a stable build does not need a recovery");
  // A release without a checksum is only accepted when the release ships a checksum file.
  check(UpdatePolicy.validReleaseUrl("owner/project", "v1.4.0", "SHA256SUMS", "https://github.com/owner/project/releases/download/v1.4.0/SHA256SUMS"), "checksum file url accepted");
  check(!UpdatePolicy.validReleaseUrl("owner/project", "v1.4.0", "SHA256SUMS", "https://evil.test/SHA256SUMS"), "foreign checksum url rejected");
  check(UpdatePolicy.validAsset("v1.4.0-beta.2", "BerryGram-1.4.0-beta.2-arm64-v8a.apk"));
  check(UpdatePolicy.validIdentity("org.example.forkclient.beta", 7271, signer, "org.example.forkclient.beta", 7272, signer));
  check(UpdatePolicy.validAsset("v1.2.0","BerryGram-1.2.0-arm64-v8a.apk"));
  System.out.println("UpdatePolicyTest passed (including Beta9 to Stable promotion)");
 }
 static void check(boolean v){if(!v) throw new AssertionError();}
 static void check(boolean v,String message){if(!v) throw new AssertionError(message);}
}
