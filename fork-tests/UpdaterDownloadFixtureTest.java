import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Locale;
import org.telegram.messenger.fork.updater.GithubUpdateClient;

/** Runs updater network/file logic using an in-process URL handler and mock PackageManager. */
public final class UpdaterDownloadFixtureTest {
 static byte[] body; static String metadata; static boolean redirect, missingRelease, breakStream, sawRange, ignoreRange, badRange; static int assetRequests;
 static final String ASSET="https://github.com/example/project/releases/download/v1.0.1/BerryGram-1.0.1-arm64-v8a.apk";
 static final class Handler extends URLStreamHandler {
  protected URLConnection openConnection(URL url) { if(url.toString().equals(ASSET))assetRequests++; return new HttpURLConnection(url) {
   long offset;
   public void setRequestProperty(String key,String value){if ("Range".equals(key)){offset=Long.parseLong(value.substring(6,value.length()-1));sawRange=true;}}
   public void disconnect(){} public boolean usingProxy(){return false;} public void connect(){}
   public int getResponseCode(){return missingRelease && url.getHost().equals("api.github.com") ? 404 : redirect && url.toString().equals(ASSET)?302:offset>0 && !ignoreRange?206:200;}
   public String getHeaderField(String name){return "Location".equals(name) && redirect ? "https://evil.invalid/payload.apk" : "Content-Range".equals(name) && offset>0 ? "bytes "+(offset+(badRange?1:0))+"-"+(body.length-1)+"/"+body.length : null;}
   public InputStream getInputStream(){
    if(url.getHost().equals("api.github.com"))return new ByteArrayInputStream(metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    if(breakStream)return new InputStream(){int pos; public int read() throws IOException { if(pos>=body.length/2)throw new IOException("simulated disconnect");return body[pos++]&255; }};
    int start=ignoreRange ? 0 : (int)offset; return new ByteArrayInputStream(body,start,body.length-start);
   }
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
  missingRelease=true;
  try { GithubUpdateClient.latest("example/project"); throw new AssertionError("Missing release accepted"); }
  catch (IOException expected) {
   if (!"No stable release found or repository unavailable".equals(expected.getMessage())) throw new AssertionError("Missing release should be explained", expected);
  } finally { missingRelease=false; }
  UpdaterPackageFixtureTest.Ctx c=new UpdaterPackageFixtureTest.Ctx();
  c.pm.installed=UpdaterPackageFixtureTest.info(UpdaterPackageFixtureTest.NAME,"1.0.0",100,0,1);
  c.pm.candidate=UpdaterPackageFixtureTest.info(UpdaterPackageFixtureTest.NAME,"1.0.1",101,0,1);
  breakStream=true;
  try { GithubUpdateClient.downloadAndVerify(c,release,null); throw new AssertionError("Interrupted transfer accepted"); }
  catch (IOException expected) { } finally { breakStream=false; }
  File resumed=GithubUpdateClient.downloadAndVerify(c,release,null);
  if(!sawRange || !java.util.Arrays.equals(valid,Files.readAllBytes(resumed.toPath())))throw new AssertionError("Download did not resume correctly");
  if(!resumed.delete())throw new AssertionError("Could not clean resumed fixture file");
  breakStream=true;try { GithubUpdateClient.downloadAndVerify(c,release,null); throw new AssertionError("Interrupted transfer accepted"); } catch(IOException expected) { } finally {breakStream=false;}
  badRange=true;try { expectFailure(c,release); } finally {badRange=false;}
  breakStream=true;try { GithubUpdateClient.downloadAndVerify(c,release,null); throw new AssertionError("Interrupted transfer accepted"); } catch(IOException expected) { } finally {breakStream=false;}
  ignoreRange=true;try { File restarted=GithubUpdateClient.downloadAndVerify(c,release,null); if(!java.util.Arrays.equals(valid,Files.readAllBytes(restarted.toPath())))throw new AssertionError("Full response was not restarted"); if(!restarted.delete())throw new AssertionError("Could not clean fixture file"); } finally {ignoreRange=false;}
  final long[] progress={0};File result=GithubUpdateClient.downloadAndVerify(c,release,(n,total)->progress[0]=n);
  if(!java.util.Arrays.equals(valid,Files.readAllBytes(result.toPath())) || progress[0]!=valid.length)throw new AssertionError("Download/progress mismatch");
  int requestsBefore=assetRequests; File cached=GithubUpdateClient.downloadAndVerify(c,release,null);
  if(assetRequests!=requestsBefore || !cached.equals(result))throw new AssertionError("Verified cached update should be revalidated without another transfer");
  byte[] damaged=valid.clone(); damaged[0]^=1; Files.write(result.toPath(),damaged);
  requestsBefore=assetRequests; result=GithubUpdateClient.downloadAndVerify(c,release,null);
  if(assetRequests<=requestsBefore || !java.util.Arrays.equals(valid,Files.readAllBytes(result.toPath())))throw new AssertionError("Damaged cached update was not replaced");
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
