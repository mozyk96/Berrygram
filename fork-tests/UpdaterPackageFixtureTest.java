import android.content.Context;
import android.content.pm.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.zip.*;
import org.telegram.messenger.fork.updater.GithubUpdateClient;

/** Fixture mocks PackageManager; Android package parsing and actual cryptographic signatures are not tested here. */
public final class UpdaterPackageFixtureTest {
 static final String NAME="org.example.app";
 static final class PM extends PackageManager {
  PackageInfo installed, candidate;
  public PackageInfo getPackageArchiveInfo(String path,int flags){return candidate;}
  public PackageInfo getPackageInfo(String name,int flags){return installed;}
 }
 static final class Ctx extends Context {
  final PM pm=new PM(); final File cache;
  Ctx() throws IOException { cache=java.nio.file.Files.createTempDirectory("berrygram-updater-fixture-").toFile(); cache.deleteOnExit(); }
  public PackageManager getPackageManager(){return pm;} public String getPackageName(){return NAME;} public File getCacheDir(){return cache;}
 }
 static PackageInfo info(String pkg,String name,int version,int flags,int signer) {
  PackageInfo i=new PackageInfo(); i.packageName=pkg;i.versionName=name;i.versionCode=version;
  i.applicationInfo=new ApplicationInfo();i.applicationInfo.flags=flags;
  i.signatures=new Signature[]{new Signature(new byte[]{(byte)signer})}; i.signingInfo=new SigningInfo(i.signatures);return i;
 }
 static void verify(Ctx c,File apk,boolean ok) throws Exception {
  Method m=GithubUpdateClient.class.getDeclaredMethod("verifyPackage",Context.class,File.class,String.class);m.setAccessible(true);
  boolean passed=true;try{m.invoke(null,c,apk,"1.0.1");}catch(InvocationTargetException e){passed=false;}
  if(passed!=ok)throw new AssertionError("Unexpected verification result");
 }
 static File apk(boolean arm) throws IOException {
  File f=File.createTempFile("berrygram-fixture-", ".apk"); f.deleteOnExit();
  try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(f))){z.putNextEntry(new ZipEntry(arm?"lib/arm64-v8a/libfixture.so":"lib/x86/libfixture.so"));z.write(1);z.closeEntry();} return f;
 }
 public static void main(String[] args) throws Exception {
  Ctx c=new Ctx();c.pm.installed=info(NAME,"1.0.0",100,0,1); File good=apk(true);
  c.pm.candidate=info(NAME,"1.0.1",101,0,1);verify(c,good,true);
  c.pm.candidate=info("org.other","1.0.1",101,0,1);verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.2",101,0,1);verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",101,ApplicationInfo.FLAG_DEBUGGABLE,1);verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",100,0,1);verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",101,0,2);verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",101,0,1);verify(c,apk(false),false);
  c.pm.candidate.signingInfo=new SigningInfo(new Signature[0]);verify(c,good,false);
  c.pm.candidate=null;verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",101,0,1);c.pm.candidate.applicationInfo=null;verify(c,good,false);
  android.os.Build.VERSION.SDK_INT=27;
  c.pm.candidate=info(NAME,"1.0.1",101,0,1);verify(c,good,true);
  c.pm.candidate=info(NAME,"1.0.1",101,0,2);verify(c,good,false);
  System.out.println("UpdaterPackageFixtureTest passed (mock PackageManager; no APK signature verification)");
 }
}
