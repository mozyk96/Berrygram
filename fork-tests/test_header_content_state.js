const fs=require('fs'),cp=require('child_process'),path=require('path');const root=path.resolve(__dirname,'..');const s=fs.readFileSync(path.join(root,'TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java'),'utf8');
function method(signature){let start=s.indexOf(signature),end=s.indexOf('{',start),n=0;if(start<0)throw Error(signature);do{if(s[end]==='{')n++;if(s[end]==='}')n--;end++;}while(n);return s.slice(start,end);}
const apply=method('private void applyFloatingStoriesState');const top=method('public int getActionBarTop()');const clip=s.match(/canvas.clipRect\(0, usesFloatingMainHeader\(\) \? getMainHeaderSafeInset\(\)\s*: ([^,]+), getMeasuredWidth/);if(!clip)throw Error('Native clip expression not found');
const fixture=`public class HeaderContentStateTest {
 static class View {static int VISIBLE=0,GONE=8;int visibility;void setVisibility(int v){visibility=v;}}
 static class Sliding {float openedProgress;boolean hasFragment(){return false;}}
 View dialogStoriesCell=new View(); Sliding rightSlidingDialogContainer=new Sliding();boolean hasStories,animateToHasStories,dialogStoriesCellVisible;float progressToShowStories,progressToDialogStoriesCell,scrollAdditionalOffset,scrollYOffset,progressToActionMode,searchAnimationProgress,y;Object storiesVisibilityAnimator2;
 boolean isInPreviewMode(){return false;}void setScrollY(float v){scrollYOffset=v;}float getY(){return y;}int getActionBarFullHeight(){return 180;}
 ${apply}
 ${top}
 int nativeClip(){return ${clip[1]};}
 public static void main(String[]args){HeaderContentStateTest t=new HeaderContentStateTest();
 for(boolean visible:new boolean[]{true,false})for(boolean peers:new boolean[]{true,false}){t.dialogStoriesCellVisible=visible;t.scrollAdditionalOffset=-81;t.scrollYOffset=-81;t.applyFloatingStoriesState(peers);if(t.dialogStoriesCell.visibility!=(visible?View.VISIBLE:View.GONE))throw new AssertionError("Real recorder entry hidden on peer removal");if(t.scrollYOffset!=0||t.scrollAdditionalOffset!=0)throw new AssertionError("Legacy 81dp row offset survived compact state");if(t.progressToDialogStoriesCell!=(visible?1:0))throw new AssertionError("Stale zero alpha after return");}
 for(int y:new int[]{0,12,24,-16})for(int offset:new int[]{0,-48,-81,-129}){t.y=y;t.scrollYOffset=offset;int clipTop=t.nativeClip();if(clipTop!=180+offset-y)throw new AssertionError("Archive native clip double-counted parent translation");}
 System.out.println("HeaderContentStateTest passed (production compact visibility/offset and translated archive clipping)");}
}`;
const out=path.join(root,'build/beta8-content-state');fs.mkdirSync(out,{recursive:true});let file=path.join(out,'HeaderContentStateTest.java');fs.writeFileSync(file,fixture);let home=process.env.JAVA_HOME||path.resolve(root,'../jdk17/jdk-17.0.20.1+1');for(let[bin,args]of[['javac',['-d',out,file]],['java',['-cp',out,'HeaderContentStateTest']]]){let r=cp.spawnSync(path.join(home,'bin',bin+'.exe'),args,{stdio:'inherit'});if(r.error)throw r.error;if(r.status)process.exit(r.status);}
