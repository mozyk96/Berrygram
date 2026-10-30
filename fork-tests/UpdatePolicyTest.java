import org.telegram.messenger.fork.updater.UpdatePolicy;
public class UpdatePolicyTest {
 public static void main(String[] args) {
  java.util.Set<String> signer=java.util.Collections.singleton("cert-a");
  check(UpdatePolicy.validIdentity("app",10,signer,"app",11,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"other",11,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"app",10,signer));
  check(!UpdatePolicy.validIdentity("app",10,signer,"app",11,java.util.Collections.singleton("cert-b")));
  check(!UpdatePolicy.validIdentity("app",10,java.util.Collections.emptySet(),"app",11,java.util.Collections.emptySet()));
  check(UpdatePolicy.compareVersions("1.0.0","1.0.0")==0);
  check(UpdatePolicy.compareVersions("1.10.0","1.9.9")>0);
  check(UpdatePolicy.compareVersions("2.0.0","3.0.0")<0);
  check(UpdatePolicy.compareVersions("unknown","1.0.0")==null);
  check(UpdatePolicy.validRepository("owner/project"));
  check(!UpdatePolicy.validRepository("owner/../other"));
  check(!UpdatePolicy.validRepository("https://github.com/owner/project"));
  check(UpdatePolicy.validAsset("v1.0.0","BerryGram-1.0.0-arm64-v8a.apk"));
  check(!UpdatePolicy.validAsset("v1.0.0-beta","BerryGram-1.0.0-beta-arm64-v8a.apk"));
  check(!UpdatePolicy.validAsset("v1.0.0","BerryGram-1.0.1-arm64-v8a.apk"));
  check(UpdatePolicy.validDigest("sha256:"+"a".repeat(64)));
  check(!UpdatePolicy.validDigest("sha256:"+"z".repeat(64)));
  check(UpdatePolicy.validAssetUrl("owner/project","v1.0.0","BerryGram-1.0.0-arm64-v8a.apk","https://github.com/owner/project/releases/download/v1.0.0/BerryGram-1.0.0-arm64-v8a.apk"));
  check(!UpdatePolicy.validAssetUrl("owner/project","v1.0.0","BerryGram-1.0.0-arm64-v8a.apk","https://github.com.evil.test/a"));
  System.out.println("UpdatePolicyTest passed");
 }
 static void check(boolean v){if(!v) throw new AssertionError();}
}
