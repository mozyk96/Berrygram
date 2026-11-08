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
 static void verifyFailureType(Ctx c,File apk,Class<?> type) throws Exception {
  Method m=GithubUpdateClient.class.getDeclaredMethod("verifyPackage",Context.class,File.class,String.class); m.setAccessible(true);
  try { m.invoke(null,c,apk,"1.0.1"); throw new AssertionError("Expected rejection"); }
  catch (InvocationTargetException e) { if (!type.isInstance(e.getCause())) throw new AssertionError("Unexpected rejection",e.getCause()); }
 }
 static File apk(boolean arm) throws IOException { return apk(arm,23,16); }
 static File apk(boolean arm,int minSdk,int valueType) throws IOException {
  File f=File.createTempFile("berrygram-fixture-", ".apk"); f.deleteOnExit();
  try(ZipOutputStream z=new ZipOutputStream(new FileOutputStream(f))){
   java.nio.ByteBuffer manifest=java.nio.ByteBuffer.allocate(76).order(java.nio.ByteOrder.LITTLE_ENDIAN);
   manifest.putShort((short)3).putShort((short)8).putInt(76);
   manifest.putShort((short)0x180).putShort((short)8).putInt(12).putInt(0x0101020c);
   manifest.putShort((short)0x102).putShort((short)16).putInt(56).putInt(1).putInt(-1);
   manifest.putInt(-1).putInt(0).putShort((short)20).putShort((short)20).putShort((short)1).putShort((short)0).putShort((short)0).putShort((short)0);
   manifest.putInt(-1).putInt(0).putInt(-1).putShort((short)8).put((byte)0).put((byte)valueType).putInt(minSdk);
   z.putNextEntry(new ZipEntry("AndroidManifest.xml"));z.write(manifest.array());z.closeEntry();
   z.putNextEntry(new ZipEntry(arm?"lib/arm64-v8a/libfixture.so":"lib/x86/libfixture.so"));z.write(1);z.closeEntry();} return f;
 }
 public static void main(String[] args) throws Exception {
  Ctx c=new Ctx();c.pm.installed=info(NAME,"1.0.0",100,0,1); File good=apk(true);
  c.pm.candidate=info(NAME,"1.0.1",101,0,1);verify(c,good,true);
  c.pm.candidate=info("org.other","1.0.1",101,0,1);verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.2",101,0,1);verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",101,ApplicationInfo.FLAG_DEBUGGABLE,1);verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",101,ApplicationInfo.FLAG_TEST_ONLY,1);verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",100,0,1);verify(c,good,false);
  verifyFailureType(c,good,GithubUpdateClient.VersionNotNewerException.class);
  c.pm.candidate=info(NAME,"1.0.1",99,0,2); verifyFailureType(c,good,SecurityException.class);
  c.pm.candidate=info(NAME,"1.0.1",101,0,2);verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",101,0,1);verify(c,apk(false),false);
  c.pm.candidate.signingInfo=new SigningInfo(new Signature[0]);verify(c,good,false);
  c.pm.candidate=null;verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",101,0,1);c.pm.candidate.applicationInfo=null;verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",101,0,1); c.pm.candidate.applicationInfo.minSdkVersion=99;verify(c,good,false);
  c.pm.candidate=info(NAME,"1.0.1",101,0,1);
  android.os.Build.VERSION.SDK_INT=22;verify(c,good,false);
  android.os.Build.VERSION.SDK_INT=23;verify(c,good,true);
  verify(c,apk(true,24,16),false); verify(c,apk(true,23,3),false);
  // A historical beta install receives the official build only when its version code is higher.
  c.pm.installed=info(NAME,"1.0.1-beta.1",100,0,1);c.pm.candidate=info(NAME,"1.0.1",101,0,1);verify(c,good,true);
  c.pm.candidate=info(NAME,"1.0.1",100,0,1);verify(c,good,false);
  android.os.Build.VERSION.SDK_INT=27;
  c.pm.candidate=info(NAME,"1.0.1",101,0,1);verify(c,good,true);
  c.pm.candidate=info(NAME,"1.0.1",101,0,2);verify(c,good,false);
  System.out.println("UpdaterPackageFixtureTest passed (mock PackageManager; no APK signature verification)");
 }
}
