"""Execute the production installer runnable with a fake Android host; no installer/device."""
import os
from pathlib import Path
import subprocess
import shutil

root = Path(__file__).resolve().parents[1]
source = (root / "TMessagesProj/src/main/java/org/telegram/ui/ForkUpdaterPanel.java").read_text(encoding="utf-8")
settings = (root / "TMessagesProj/src/main/java/org/telegram/ui/ForkSettingsActivity.java").read_text(encoding="utf-8")
start = source.index("    private final Runnable installWhenResumed")
end = source.index("    private String status", start)
runnable = source[start:end]
start = source.index("    private void scheduleInstallerCheck()")
end = source.index("    @Override public void onWindowFocusChanged", start)
events = source[start:end]
assert "postDelayed" not in runnable
assert "updater.onParentResumed()" in settings and "updater.onParentPaused()" in settings
assert "if (hasFocus) scheduleInstallerCheck();" in source
assert "if (!hasFocus && waitingPermission && permissionSettingsOpened) permissionLeft = true;" in source
harness = r"""
import java.io.File;
import org.telegram.messenger.fork.updater.UpdateInstallIntent;
public class UpdaterRunnableLifecycleTest {
 boolean disposed, attached=true, focused=true, waitingPermission, permissionSettingsOpened, permissionLeft;
 boolean granted; int launches, posted; File verified; Runnable queued;
 final UpdateInstallIntent installIntent = new UpdateInstallIntent();
 static class Build { static class VERSION { static int SDK_INT=35; } }
 static class Parent { boolean paused; boolean isPaused(){ return paused; } }
 final Parent parent=new Parent();
 boolean isAttachedToWindow(){return attached;} boolean hasWindowFocus(){return focused;}
 UpdaterRunnableLifecycleTest getContext(){return this;}
 UpdaterRunnableLifecycleTest getPackageManager(){return this;}
 boolean canRequestPackageInstalls(){return granted;}
 void removeCallbacks(Runnable r){if(queued==r)queued=null;}
 void post(Runnable r){queued=r;posted++;}
 void dispatch(){Runnable r=queued;queued=null;if(r!=null)r.run();}
 void fail(Exception e){installIntent.cancel();}
 void install(){if(installIntent.consume(attached,!parent.paused,focused,verified!=null))launches++;}
 static void check(boolean b){if(!b)throw new AssertionError();}
""" + runnable + events + r"""
 public static void main(String[] args)throws Exception{
  UpdaterRunnableLifecycleTest h=new UpdaterRunnableLifecycleTest();
  h.verified=File.createTempFile("updater-runnable-", ".apk");h.verified.deleteOnExit();
  h.installIntent.request();h.parent.paused=true;h.scheduleInstallerCheck();h.dispatch();
  check(h.launches==0 && h.queued==null); // No inactive polling.
  h.onParentPaused();check(h.queued==null);
  h.parent.paused=false;h.focused=false;h.onParentResumed();h.dispatch();
  check(h.launches==0 && h.queued==null);
  h.focused=true;h.scheduleInstallerCheck();h.dispatch();
  check(h.launches==1 && h.queued==null);
  h.onParentResumed();h.dispatch();check(h.launches==1); // Installer cancellation cannot reopen.
  h.installIntent.request();h.waitingPermission=true;h.permissionSettingsOpened=true;
  h.parent.paused=true;h.onParentPaused();check(h.permissionLeft);
  h.parent.paused=false;h.granted=false;h.onParentResumed();h.dispatch();
  check(!h.installIntent.pending() && h.launches==1 && h.queued==null);
  h.installIntent.request();h.waitingPermission=true;h.permissionLeft=true;h.granted=true;
  h.onParentResumed();h.dispatch();check(h.launches==2 && h.queued==null);
  h.installIntent.request();h.disposed=true;h.scheduleInstallerCheck();check(h.queued==null);
  System.out.println("UpdaterRunnableLifecycleTest passed (production runnable; fake host)");
 }
}
"""
out = root / "build/fork-updater-runnable"
out.mkdir(parents=True, exist_ok=True)
java = out / "UpdaterRunnableLifecycleTest.java"
java.write_text(harness, encoding="utf-8")
bin_dir = Path(os.environ["JAVA_HOME"]) / "bin" if os.environ.get("JAVA_HOME") else None
javac = str(bin_dir / "javac.exe") if bin_dir else shutil.which("javac")
java_cmd = str(bin_dir / "java.exe") if bin_dir else shutil.which("java")
subprocess.run([javac, "-encoding", "UTF-8", "-d", str(out), str(root / "TMessagesProj/src/main/java/org/telegram/messenger/fork/updater/UpdateInstallIntent.java"), str(java)], check=True)
subprocess.run([java_cmd, "-cp", str(out), "UpdaterRunnableLifecycleTest"], check=True)
