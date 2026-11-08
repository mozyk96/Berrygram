import org.telegram.messenger.fork.updater.GithubUpdateClient;
public class GithubReleaseFixtureTest {
 static String digest="a".repeat(64);
 static String fixture(String state, boolean draft, String digestValue, String url) {
  return "{\"draft\":"+draft+",\"prerelease\":false,\"tag_name\":\"v1.2.3\",\"body\":\"Changes\",\"assets\":[{\"name\":\"BerryGram-1.2.3-arm64-v8a.apk\",\"state\":\""+state+"\",\"size\":1234,\"digest\":\""+digestValue+"\",\"browser_download_url\":\""+url+"\"}]}";
 }
 public static void main(String[] args) throws Exception {
  String url="https://github.com/example/project/releases/download/v1.2.3/BerryGram-1.2.3-arm64-v8a.apk";
  GithubUpdateClient.Release release=GithubUpdateClient.parseRelease("example/project",fixture("uploaded",false,"sha256:"+digest,url));
  check(release.version.equals("1.2.3") && release.size==1234 && release.digest.equals(digest) && release.notes.equals("Changes"));
  reject(fixture("uploaded",true,"sha256:"+digest,url));
  reject(fixture("uploaded",false,"",url));
  reject(fixture("uploaded",false,"sha256:"+digest,"https://evil.test/a"));
  reject(fixture("pending",false,"sha256:"+digest,url));
  String valid=fixture("uploaded",false,"sha256:"+digest,url);
  String prerelease=valid.replace("\"prerelease\":false", "\"prerelease\":true");
  reject(prerelease);
  // The official stream ignores prerelease entries and never selects them.
  expectNoRelease("["+prerelease+"]");
  String beta=prerelease.replace("1.2.3", "1.3.0-beta.1");
  String beta2=beta.replace("beta.1", "beta.2");
  String stable=valid.replace("1.2.3", "1.3.0");
  check(GithubUpdateClient.parseReleaseList("example/project", "["+valid+","+beta+","+beta2+"]").version.equals("1.2.3"),
      "prereleases are ignored when an official release exists");
  check(GithubUpdateClient.parseReleaseList("example/project", "["+beta2+","+stable+"]").version.equals("1.3.0"),
      "the official release wins over a newer prerelease");
  // A prerelease version must be ignored even when the API labels it as a normal release.
  String mislabeledBeta=beta.replace("\"prerelease\":true", "\"prerelease\":false");
  check(GithubUpdateClient.parseReleaseList("example/project", "["+valid+","+mislabeledBeta+"]").version.equals("1.2.3"),
      "a prerelease version string is ignored regardless of the prerelease flag");
  String newer=prerelease.replace("1.2.3", "1.10.0").replace("\"prerelease\":true", "\"prerelease\":false");
  check(GithubUpdateClient.parseReleaseList("example/project", "["+valid+","+newer+"]").version.equals("1.10.0"));
  // Historical 1.4.1 RC metadata must not hide the final official 1.4.1.
  String rc=prerelease.replace("1.2.3", "1.4.1-rc.1");
  String finalRelease=newer.replace("1.10.0", "1.4.1");
  check(GithubUpdateClient.parseReleaseList("example/project", "["+rc+","+finalRelease+"]").version.equals("1.4.1"),
      "the official 1.4.1 release is selected over its historical RC");
  String futureBeta=prerelease.replace("1.2.3", "1.4.2-beta.1");
  check(GithubUpdateClient.parseReleaseList("example/project", "["+finalRelease+","+futureBeta+"]").version.equals("1.4.1"),
      "a newer prerelease does not replace the newest official release");
  String draft=newer.replace("\"draft\":false", "\"draft\":true");
  check(GithubUpdateClient.parseReleaseList("example/project", "["+draft+","+valid+"]").version.equals("1.2.3"));
  String unsafe=newer.replace("example/project", "example/other");
  check(GithubUpdateClient.parseReleaseList("example/project", "["+unsafe+","+valid+"]").version.equals("1.2.3"));
  expectNoRelease("[]");
  reject(valid.replace("\"size\":1234", "\"size\":0"));
  reject(valid.replace("\"size\":1234", "\"size\":536870913"));
  reject(valid.replace("example/project", "example/other"));
  reject(valid.replace("sha256:"+digest, "sha256:"+digest.substring(1)));
  String asset=valid.substring(valid.indexOf("{\"name\""), valid.length()-2);
  reject(valid.substring(0,valid.length()-2)+","+asset+"]}");
  // A release without an API digest is only accepted when it ships a checksum file.
  String sumsAsset="{\"name\":\"SHA256SUMS\",\"state\":\"uploaded\",\"size\":104,\"digest\":\"sha256:"+digest+"\",\"browser_download_url\":\"https://github.com/example/project/releases/download/v1.2.3/SHA256SUMS\"}";
  String apkAsset="{\"name\":\"BerryGram-1.2.3-arm64-v8a.apk\",\"state\":\"uploaded\",\"size\":1234,\"digest\":\"\",\"browser_download_url\":\"https://github.com/example/project/releases/download/v1.2.3/BerryGram-1.2.3-arm64-v8a.apk\"}";
  String withSums="{\"draft\":false,\"prerelease\":false,\"tag_name\":\"v1.2.3\",\"body\":\"Changes\",\"assets\":["+sumsAsset+","+apkAsset+"]}";
  GithubUpdateClient.Release fallback=GithubUpdateClient.parseRelease("example/project",withSums);
  check(fallback.digest.isEmpty() && fallback.sumsUrl!=null,"a release with a checksum file is accepted without an API digest");
  String withoutSums="{\"draft\":false,\"prerelease\":false,\"tag_name\":\"v1.2.3\",\"body\":\"Changes\",\"assets\":["+apkAsset+"]}";
  reject(withoutSums);
  check(GithubUpdateClient.parseRelease("example/project", valid.replace("Changes", "x".repeat(13000))).notes.length()==12000);
  System.out.println("GithubReleaseFixtureTest passed (official stream only)");
 }
 static void reject(String json) throws Exception {boolean rejected=false;try {GithubUpdateClient.parseRelease("example/project",json);} catch(Exception expected){rejected=true;}check(rejected);}
 static void expectNoRelease(String json) throws Exception {
  boolean rejected=false;
  try {GithubUpdateClient.parseReleaseList("example/project",json);} catch(GithubUpdateClient.NoReleaseException expected){rejected=true;}
  check(rejected,"expected no official release");
 }
 static void check(boolean v){if(!v) throw new AssertionError();}
 static void check(boolean v,String message){if(!v) throw new AssertionError(message);}
}
