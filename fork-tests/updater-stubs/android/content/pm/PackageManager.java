package android.content.pm;
public abstract class PackageManager { public static final int GET_SIGNING_CERTIFICATES=1,GET_SIGNATURES=2; public abstract PackageInfo getPackageArchiveInfo(String path,int flags); public abstract PackageInfo getPackageInfo(String name,int flags); }
