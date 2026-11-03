const fs = require('fs'), path = require('path'), cp = require('child_process');
const root = path.resolve(__dirname, '..');
const source = fs.readFileSync(path.join(root, 'TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java'), 'utf8');
const start = source.indexOf('public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler, RecyclerView.State state)');
const tail = source.slice(start);
const begin = tail.indexOf('final boolean hasStories =');
const end = tail.indexOf('boolean hasHiddenArchive =', begin);
if (start < 0 || begin < 0 || end < 0) throw Error('Production scroll reservation block missing');
const block = tail.slice(begin, end).replaceAll('DialogsActivity.this.hasStories', 'outerStories');
const fixture = `public class Beta8StoryScrollReservationTest {
 boolean outerStories=true, floating, fixScrollYAfterArchiveOpened=false; int communityId=0;
 static class ActionBar {boolean isActionModeShowed(){return false;}} ActionBar actionBar=new ActionBar();
 static class DialogStoriesCell {static int HEIGHT_IN_DP=81;}
 static class ListView {int getPaddingTop(){return 24;}}
 static class ViewPage {ListView listView=new ListView();} ViewPage viewPage=new ViewPage();
 static class Sliding {boolean hasFragment(){return false;}} Sliding rightSlidingDialogContainer=new Sliding();
 int dp(int value){return value;} boolean usesFloatingMainHeader(){return floating;}
 int reservation(){${block} return pTop;}
 public static void main(String[] args){
 Beta8StoryScrollReservationTest test=new Beta8StoryScrollReservationTest();
 if(test.reservation()!=24-81)throw new AssertionError("Native stories collapse reservation changed");
 test.floating=true;
 if(test.reservation()!=24)throw new AssertionError("Compact Stories incorrectly subtract native 81dp from first-item boundary: "+test.reservation());
 System.out.println("Beta8StoryScrollReservationTest passed: production scroll reservation native/compact boundary");
 }} `;
const out = path.join(root, 'build', 'beta8-story-scroll-test'); fs.mkdirSync(out,{recursive:true});
const file=path.join(out,'Beta8StoryScrollReservationTest.java');fs.writeFileSync(file,fixture);
const javaHome=process.env.JAVA_HOME||path.resolve(root,'../jdk17/jdk-17.0.20.1+1');
for(const [exe,args] of [['javac',['-d',out,file]],['java',['-cp',out,'Beta8StoryScrollReservationTest']]]) {
 const result=cp.spawnSync(path.join(javaHome,'bin',exe+'.exe'),args,{stdio:'inherit'});
 if(result.error)throw result.error; if(result.status)process.exit(result.status);
}
