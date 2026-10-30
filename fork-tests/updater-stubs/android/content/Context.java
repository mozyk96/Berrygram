package android.content;
import android.content.pm.PackageManager; import java.io.File;
public abstract class Context { public abstract PackageManager getPackageManager(); public abstract String getPackageName(); public abstract File getCacheDir(); }
