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
  reject(valid.replace("\"prerelease\":false", "\"prerelease\":true"));
  reject(valid.replace("\"size\":1234", "\"size\":0"));
  reject(valid.replace("\"size\":1234", "\"size\":536870913"));
  reject(valid.replace("example/project", "example/other"));
  reject(valid.replace("sha256:"+digest, "sha256:"+digest.substring(1)));
  String asset=valid.substring(valid.indexOf("{\"name\""), valid.length()-2);
  reject(valid.substring(0,valid.length()-2)+","+asset+"]}");
  check(GithubUpdateClient.parseRelease("example/project", valid.replace("Changes", "x".repeat(13000))).notes.length()==12000);
  System.out.println("GithubReleaseFixtureTest passed");
 }
 static void reject(String json) throws Exception {boolean rejected=false;try {GithubUpdateClient.parseRelease("example/project",json);} catch(Exception expected){rejected=true;}check(rejected);}
 static void check(boolean v){if(!v) throw new AssertionError();}
}
