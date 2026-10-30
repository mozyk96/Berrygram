package android.content.pm;
public class PackageInfo { public String packageName,versionName; public int versionCode; public ApplicationInfo applicationInfo; public Signature[] signatures; public SigningInfo signingInfo; public long getLongVersionCode(){return versionCode;} }
