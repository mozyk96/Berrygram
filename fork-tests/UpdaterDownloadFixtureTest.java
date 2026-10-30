import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Locale;
import org.telegram.messenger.fork.updater.GithubUpdateClient;

/** Runs updater network/file logic using an in-process URL handler and mock PackageManager. */
public final class UpdaterDownloadFixtureTest {
 static byte[] body; static String metadata; static boolean redirect;
 static final String ASSET="https://github.com/example/project/releases/download/v1.0.1/BerryGram-1.0.1-arm64-v8a.apk";
 static final class Handler extends URLStreamHandler {
  protected URLConnection openConnection(URL url) { return new HttpURLConnection(url) {
   public void disconnect(){} public boolean usingProxy(){return false;} public void connect(){}
   public int getResponseCode(){return redirect && url.toString().equals(ASSET)?302:200;}
   public String getHeaderField(String name){return "Location".equals(name) && redirect ? "https://evil.invalid/payload.apk" : null;}
   public InputStream getInputStream(){return new ByteArrayInputStream(url.getHost().equals("api.github.com") ? metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8) : body);}
  }; }
 }
 static String sha(byte[] b) throws Exception { StringBuilder s=new StringBuilder();for(byte v:MessageDigest.getInstance("SHA-256").digest(b))s.append(String.format(Locale.ROOT,"%02x",v & 255));return s.toString(); }
 static void expectFailure(UpdaterPackageFixtureTest.Ctx c,GithubUpdateClient.Release release) throws Exception {
  boolean failed=false;try{GithubUpdateClient.downloadAndVerify(c,release,null);}catch(Exception expected){failed=true;}
  if(!failed)throw new AssertionError("Invalid download accepted");
  File dir=new File(c.cache,"berrygram-updates");String[] files=dir.list();if(files!=null && files.length!=0)throw new AssertionError("Partial file remained");
 }
 public static void main(String[] args) throws Exception {
  URL.setURLStreamHandlerFactory(protocol -> protocol.equals("https") ? new Handler() : null);
  byte[] valid=Files.readAllBytes(UpdaterPackageFixtureTest.apk(true).toPath());body=valid;
  metadata="{\"draft\":false,\"prerelease\":false,\"tag_name\":\"v1.0.1\",\"body\":\"Test\",\"assets\":[{\"name\":\"BerryGram-1.0.1-arm64-v8a.apk\",\"state\":\"uploaded\",\"size\":"+valid.length+",\"digest\":\"sha256:"+sha(valid)+"\",\"browser_download_url\":\""+ASSET+"\"}]}";
  GithubUpdateClient.Release release=GithubUpdateClient.latest("example/project");
  UpdaterPackageFixtureTest.Ctx c=new UpdaterPackageFixtureTest.Ctx();
  c.pm.installed=UpdaterPackageFixtureTest.info(UpdaterPackageFixtureTest.NAME,"1.0.0",100,0,1);
  c.pm.candidate=UpdaterPackageFixtureTest.info(UpdaterPackageFixtureTest.NAME,"1.0.1",101,0,1);
  final long[] progress={0};File result=GithubUpdateClient.downloadAndVerify(c,release,(n,total)->progress[0]=n);
  if(!java.util.Arrays.equals(valid,Files.readAllBytes(result.toPath())) || progress[0]!=valid.length)throw new AssertionError("Download/progress mismatch");
  if(!result.delete())throw new AssertionError("Could not clean own fixture file");
  body=valid.clone();body[0]^=1;expectFailure(c,release);
  body=java.util.Arrays.copyOf(valid,valid.length-1);expectFailure(c,release);
  body=java.util.Arrays.copyOf(valid,valid.length+1);expectFailure(c,release);
  body=valid;c.pm.candidate=UpdaterPackageFixtureTest.info(UpdaterPackageFixtureTest.NAME,"1.0.1",101,0,2);expectFailure(c,release);
  c.pm.candidate=UpdaterPackageFixtureTest.info(UpdaterPackageFixtureTest.NAME,"1.0.1",101,0,1);
  Thread.currentThread().interrupt();try { expectFailure(c,release); } finally { Thread.interrupted(); }
  redirect=true;expectFailure(c,release);
  System.out.println("UpdaterDownloadFixtureTest passed (mock HTTPS and PackageManager; no real signature verification)");
 }
}
