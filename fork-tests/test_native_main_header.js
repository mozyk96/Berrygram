const fs=require('fs'),path=require('path'),cp=require('child_process');
const root=path.resolve(__dirname,'..');
const s=fs.readFileSync(path.join(root,'TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java'),'utf8');
const stories=fs.readFileSync(path.join(root,'TMessagesProj/src/main/java/org/telegram/ui/Stories/DialogStoriesCell.java'),'utf8');
for(const retired of ['forkFloatingMainHeader','usesFloatingMainHeader','updateForkMainHeaderLayout','getReservedSearchHeight','configureForkHeaderButton','applyFloatingStoriesState']) if(s.includes(retired))throw Error('Retired main header remains: '+retired);
for(const retired of ['floatingCompactHeader','forkHideBranding'])if(stories.includes(retired))throw Error('Retired Stories geometry remains: '+retired);
for(const kept of ['contentView.addView(actionBar, layoutParams)','actionBar.setTitle(ssb, statusDrawable)','lockApplication(passcodeItem)','checkAutoscrollToStories','canvas.clipRect(0, getActionBarTop() + getActionBarFullHeight()'])if(!s.includes(kept))throw Error('Native behavior absent: '+kept);
function method(signature){let a=s.indexOf(signature);if(a<0)throw Error(signature);let b=s.indexOf('{',a),n=0;do{if(s[b]==='{')n++;if(s[b]==='}')n--;b++;}while(n);return s.slice(a,b);}
const fixture=`public class NativeMainHeaderTest {
 boolean hasStories;float scrollYOffset,progressToActionMode,searchAnimationProgress,y; Sliding rightSlidingDialogContainer=new Sliding();
 static class Sliding {boolean open;float openedProgress;boolean hasFragment(){return open;}}
 static class DialogStoriesCell {static int HEIGHT_IN_DP=81;}
 static final int SEARCH_FIELD_HEIGHT=48;int dp(int x){return x;}float getY(){return y;}
 ${method('private int getMaxScrollYOffsetWithoutSearch()')}
 ${method('private int getMaxScrollYOffset()')}
 ${method('public int getActionBarTop()')}
 public static void main(String[]args){NativeMainHeaderTest t=new NativeMainHeaderTest();
 for(boolean stories:new boolean[]{false,true}){t.hasStories=stories;int expected=stories?81:0;if(t.getMaxScrollYOffsetWithoutSearch()!=expected||t.getMaxScrollYOffset()!=expected+48)throw new AssertionError("Native scroll reservation");
 for(int offset:new int[]{0,-24,-48,-81,-129})for(int y:new int[]{0,12,-16}){t.scrollYOffset=offset;t.y=y;t.progressToActionMode=0;t.searchAnimationProgress=0;t.rightSlidingDialogContainer.open=false;if(t.getActionBarTop()!=(int)(offset-y))throw new AssertionError("Native clip/scroll mismatch");t.searchAnimationProgress=1;if(t.getActionBarTop()!=-y)throw new AssertionError("Search inherits collapsed offset");t.searchAnimationProgress=0;t.progressToActionMode=1;if(t.getActionBarTop()!=-y)throw new AssertionError("Selection inherits collapsed offset");t.progressToActionMode=0;t.rightSlidingDialogContainer.open=true;t.rightSlidingDialogContainer.openedProgress=1;if(t.getActionBarTop()!=-y)throw new AssertionError("Sliding fragment inherits collapsed offset");}}
 System.out.println("Native main header production scroll geometry passed (stories, search, selection, translated parent, sliding fragment)");}
}`;
const out=path.join(root,'build/beta9-native-header');fs.mkdirSync(out,{recursive:true});const f=path.join(out,'NativeMainHeaderTest.java');fs.writeFileSync(f,fixture);const j=process.env.JAVA_HOME||path.resolve(root,'../jdk17/jdk-17.0.20.1+1');for(const [bin,args]of [['javac',['-d',out,f]],['java',['-cp',out,'NativeMainHeaderTest']]]){let r=cp.spawnSync(path.join(j,'bin',bin+'.exe'),args,{stdio:'inherit'});if(r.error)throw r.error;if(r.status)process.exit(r.status);}
