const fs = require('fs'), cp = require('child_process'), path = require('path');
const root = path.resolve(__dirname, '..');
const text = fs.readFileSync(path.join(root, 'TMessagesProj/src/main/java/org/telegram/ui/Stories/DialogStoriesCell.java'), 'utf8');
function method(signature) {
 const start = text.indexOf(signature); if (start < 0) throw Error(signature);
 let i = text.indexOf('{', start), depth = 0, end = i;
 do { if(text[end]==='{')depth++; if(text[end]==='}')depth--; end++; } while(depth);
 return text.slice(start,end);
}
const setter = method('public void setFloatingCompactHeader');
let draw = method('protected void dispatchDraw(Canvas canvas)');
// Exercise the real compact branch; upstream expanded rendering has its own Android dependencies.
draw = draw.slice(0, draw.indexOf('        canvas.save();')) + '\n}';
const fixture = `import java.util.*;
class Canvas {}
class FrameLayout {
 static final int VISIBLE=0, INVISIBLE=4; int visibility=VISIBLE; boolean clip, paddingClip; float alpha=1,x,y; LayoutParams params=new LayoutParams();
 static class LayoutParams {int topMargin;}
 void setVisibility(int v){visibility=v;} void setAlpha(float v){alpha=v;} void setTranslationX(float v){x=v;} void setTranslationY(float v){y=v;}
 void setClipChildren(boolean v){clip=v;} void setClipToPadding(boolean v){paddingClip=v;} Object getLayoutParams(){return params;} void setLayoutParams(LayoutParams p){params=p;}
 void requestLayout(){} protected void dispatchDraw(Canvas c){}
}
public class CompactStoriesStateTest extends FrameLayout {
 static class StoryCell extends FrameLayout {boolean drawInParent=true;void setProgressToCollapsed(float a,float b,float c,boolean d){} }
 static class Recycler extends FrameLayout {ArrayList<StoryCell> children=new ArrayList<>();int getChildCount(){return children.size();}StoryCell getChildAt(int i){return children.get(i);} }
 static class Adapter {int notifications;void notifyDataSetChanged(){notifications++;}}
 boolean floatingCompactHeader; int currentState=1;static final int COLLAPSED_STATE=1,FAKE_TOP_PADDING=4;
 Recycler recyclerListView=new Recycler(),listViewMini=new Recycler();Adapter adapter=new Adapter();ArrayList<StoryCell> viewsDrawInParent=new ArrayList<>();
 static int dp(int n){return n;}
 ${setter}
 ${draw}
 public static void main(String[] args){
  CompactStoriesStateTest test=new CompactStoriesStateTest();
  for(int i=0;i<12;i++){StoryCell cell=new StoryCell();cell.x=42;cell.y=-5;cell.alpha=0;test.recyclerListView.children.add(cell);test.viewsDrawInParent.add(cell);}
  test.setFloatingCompactHeader(true);test.dispatchDraw(new Canvas());
  if(test.recyclerListView.visibility!=VISIBLE||test.listViewMini.visibility!=INVISIBLE||!test.clip||!test.recyclerListView.clip)throw new AssertionError("Compact list hidden or bleeding into controls");
  if(test.recyclerListView.params.topMargin!=0||test.recyclerListView.children.size()!=12||!test.viewsDrawInParent.isEmpty())throw new AssertionError("Lost peers or duplicate parent drawing");
  for(StoryCell cell:test.recyclerListView.children)if(cell.drawInParent||cell.x!=0||cell.y!=0||cell.alpha!=1)throw new AssertionError("Stale collapsed transform");
  test.setFloatingCompactHeader(true);if(test.adapter.notifications!=1)throw new AssertionError("Repeated layout invalidation");
  test.setFloatingCompactHeader(false);if(test.recyclerListView.visibility!=INVISIBLE||test.listViewMini.visibility!=VISIBLE||test.clip||test.recyclerListView.params.topMargin!=4)throw new AssertionError("Stock contextual layout not restored");
  System.out.println("CompactStoriesStateTest passed (production compact render/state methods, mock Android views)");
 }
}`;
const out=path.join(root,'build','compact-stories-test');fs.mkdirSync(out,{recursive:true});const file=path.join(out,'CompactStoriesStateTest.java');fs.writeFileSync(file,fixture);
const home=process.env.JAVA_HOME||path.resolve(root,'../jdk17/jdk-17.0.20.1+1');
for(const [exe,args] of [['javac',['-d',out,file]],['java',['-cp',out,'CompactStoriesStateTest']]]){const r=cp.spawnSync(path.join(home,'bin',exe+'.exe'),args,{stdio:'inherit'});if(r.error)throw r.error;if(r.status)process.exit(r.status);}
