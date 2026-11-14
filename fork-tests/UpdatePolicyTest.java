import org.telegram.messenger.fork.updater.UpdatePolicy;
public class UpdatePolicyTest {
 public static void main(String[] args) {
  java.util.Set<String> signer=java.util.Collections.singleton("cert-a");
  check(UpdatePolicy.validIdentity("app",10,signer,"app",11,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"other",11,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"app",10,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"app",11,java.util.Collections.singleton("cert-b")));
  check(!UpdatePolicy.validIdentity("app",10,java.util.Collections.emptySet(),"app",11,java.util.Collections.emptySet()));
  check(UpdatePolicy.compatibleCode(100, 101));
  check(UpdatePolicy.compatibleCode(72949, 72959), "1.5.0 ARM64 must upgrade 1.4.6");
  check(UpdatePolicy.shouldShowRelease("1.5.0", "1.4.6"), "1.5.0 must be offered to 1.4.6");
  check(UpdatePolicy.compatibleCode(72959, 72969), "1.5.1 ARM64 must upgrade 1.5.0");
  check(UpdatePolicy.shouldShowRelease("1.5.1", "1.5.0"), "1.5.1 must be offered to 1.5.0");
  check(!UpdatePolicy.compatibleCode(100, 100));
  check(!UpdatePolicy.compatibleCode(100, 99));
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
  check(UpdatePolicy.compareVersions("1.2.0+build.1","1.2.0+build.2") == 0);
  check(UpdatePolicy.compareVersions("1.2.0-beta.01","1.2.0") == null);
  // One official stream: only a strictly newer official version is offered.
  check(UpdatePolicy.shouldShowRelease("1.4.1", "1.4.0-beta.1"), "an official release supersedes an older beta");
  check(UpdatePolicy.shouldShowRelease("1.4.1", "1.4.0-beta.2"), "an official release supersedes an older beta");
  check(UpdatePolicy.shouldShowRelease("1.4.1", "1.4.0-rc.1"), "an official release supersedes an older RC");
  check(UpdatePolicy.shouldShowRelease("1.4.1", "1.4.1-rc.1"), "the final release supersedes its RC");
  check(UpdatePolicy.shouldShowRelease("1.4.0", "1.4.0-beta.2"), "a final release supersedes its beta");
  check(UpdatePolicy.shouldShowRelease("1.4.1", "1.4.0"));
  check(!UpdatePolicy.shouldShowRelease("1.4.1", "1.4.1"), "the same version must not be offered");
  check(!UpdatePolicy.shouldShowRelease("1.3.0", "1.4.0-beta.2"), "an older official release must not be offered");
  check(!UpdatePolicy.shouldShowRelease("1.3.0", "1.4.0-rc.1"), "an older official release must not replace an RC");
  check(!UpdatePolicy.shouldShowRelease("1.4.1-rc.1", "1.4.0"), "a prerelease is never shown by the official updater");
  check(!UpdatePolicy.shouldShowRelease("1.4.1-beta.1", "1.4.0-beta.2"), "a prerelease candidate is never offered");
  check(!UpdatePolicy.shouldShowRelease("bad", "1.4.0"), "an invalid candidate is rejected");
  check(!UpdatePolicy.shouldShowRelease("1.4.1", "bad"), "an invalid installed version is rejected");
  check(UpdatePolicy.validAsset("v1.2.0-beta.1","BerryGram-1.2.0-beta.1-arm64-v8a.apk"));
  check(UpdatePolicy.validIdentity("app",72489,signer,"app",72499,signer));
  check(UpdatePolicy.validAsset("v1.0.0","BerryGram-1.0.0-arm64-v8a.apk"));
  check(!UpdatePolicy.validAsset("v1.0.0-beta.01","BerryGram-1.0.0-beta.01-arm64-v8a.apk"));
  check(!UpdatePolicy.validAsset("v1.0.0","BerryGram-1.0.1-arm64-v8a.apk"));
  check(UpdatePolicy.validDigest("sha256:"+"a".repeat(64)));
  check(!UpdatePolicy.validDigest("sha256:"+"z".repeat(64)));
  check(UpdatePolicy.validRepository("owner/project"));
  check(!UpdatePolicy.validRepository("owner/../other"));
  check(!UpdatePolicy.validRepository("https://github.com/owner/project"));
  check(UpdatePolicy.validAssetUrl("owner/project","v1.0.0","BerryGram-1.0.0-arm64-v8a.apk","https://github.com/owner/project/releases/download/v1.0.0/BerryGram-1.0.0-arm64-v8a.apk"));
  check(!UpdatePolicy.validAssetUrl("owner/project","v1.0.0","BerryGram-1.0.0-arm64-v8a.apk","https://github.com.evil.test/a"));
  String url="https://github.com/owner/project/releases/download/v1.4.1/BerryGram-1.4.1-arm64-v8a.apk";
  String digest="a".repeat(64);
  check(UpdatePolicy.validSavedRelease("owner/project", "1.4.1", "1.4.0", url, digest, 100));
  check(UpdatePolicy.validSavedRelease("owner/project", "1.4.1", "1.4.0-beta.2", url, digest, 100), "a saved official release is offered to an older beta install");
  check(UpdatePolicy.validSavedRelease("owner/project", "1.4.1", "1.4.1-rc.1", url, digest, 100), "a saved final release replaces its RC");
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.4.1", "1.4.1", url, digest, 100));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.4.1-rc.1", "1.4.0", url, digest, 100), "a saved prerelease is never restored");
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.3.0", "1.4.0-beta.2", url, digest, 100), "an older saved release must not be restored");
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.4.1", "1.4.0", url, digest, 0));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.4.1", "1.4.0", "https://evil.test/file", digest, 100));
  check(!UpdatePolicy.validSavedRelease("owner/project", "1.4.1", "1.4.0", url, "invalid", 100));
  // A release without a checksum is only accepted when the release ships a checksum file.
  check(UpdatePolicy.validReleaseUrl("owner/project", "v1.4.0", "SHA256SUMS", "https://github.com/owner/project/releases/download/v1.4.0/SHA256SUMS"), "checksum file url accepted");
  check(!UpdatePolicy.validReleaseUrl("owner/project", "v1.4.0", "SHA256SUMS", "https://evil.test/SHA256SUMS"), "foreign checksum url rejected");
  System.out.println("UpdatePolicyTest passed (official stream, no downgrades)");
 }
 static void check(boolean v){if(!v) throw new AssertionError();}
 static void check(boolean v,String message){if(!v) throw new AssertionError(message);}
}
