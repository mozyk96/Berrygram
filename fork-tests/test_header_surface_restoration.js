const fs=require('fs'),cp=require('child_process'),path=require('path');const root=path.resolve(__dirname,'..');
const source=process.argv.includes('--baseline')?cp.execFileSync('git',['show','68081a6:TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java'],{cwd:root,encoding:'utf8'}):fs.readFileSync(path.join(root,'TMessagesProj/src/main/java/org/telegram/ui/DialogsActivity.java'),'utf8');
function method(signature){let start=source.indexOf(signature),brace=source.indexOf('{',start),end=brace,depth=0;if(start<0)throw Error(signature);do{if(source[end]==='{')depth++;if(source[end]==='}')depth--;end++;}while(depth);return source.slice(start,end).replaceAll('org.telegram.messenger.fork.panels.ForkPanelSettings','PanelSettings');}
const update=method('private void updateForkHeaderSurfaces()');
const intact=source.includes('private boolean forkHeaderSurfaceIntact')?method('private boolean forkHeaderSurfaceIntact'):'';
const fixture=`import java.util.*;
public class HeaderSurfaceRestorationTest {
 static class Item {Object background;Object getBackground(){return background;}}
 static class View {Object getContext(){return this;}}
 static class Theme {static int key_windowBackgroundWhite=1,key_actionBarDefaultSelector=2;}
 static class PanelSettings {static int strength=50;static int effectStrength(){return strength;}}
 static class ForkGraphicsSurface {static String style="liquid";static String effectiveStyle(){return style;}static int materialColor(Object c,int color){return color;}}
 View fragmentView=new View();Item searchItem=new Item(),passcodeItem=new Item(),downloadsItem=new Item(),optionsItem=new Item();
 String forkHeaderSurfaceStyle;int forkHeaderSurfaceColor,forkHeaderSurfaceStrength=-1,forkHeaderSurfaceSelector,tint=10,binds;
 IdentityHashMap<Item,Object> forkHeaderSurfaceBackgrounds=new IdentityHashMap<>();
 boolean isForkMainHeader(){return true;}int getThemedColor(int key){return tint+key;}
 void updateForkHeaderSurface(Item item,String style,int color){if(item!=null){item.background=new Object();forkHeaderSurfaceBackgrounds.put(item,item.background);binds++;}}
 ${intact.replaceAll('ActionBarMenuItem','Item')}
 ${update}
 public static void main(String[] args){HeaderSurfaceRestorationTest t=new HeaderSurfaceRestorationTest();t.updateForkHeaderSurfaces();if(t.binds!=4)throw new AssertionError("Initial surfaces absent");
 for(int i=0;i<20;i++)t.updateForkHeaderSurfaces();if(t.binds!=4)throw new AssertionError("Per-frame drawable churn");
 for(int i=0;i<5;i++){int before=t.binds;t.searchItem.background=new Object();t.passcodeItem.background=null;t.optionsItem.background=new Object();t.updateForkHeaderSurfaces();if(t.binds!=before+4)throw new AssertionError("Search/native reset lost graphic surfaces");}
 int before=t.binds;ForkGraphicsSurface.style="blur";t.updateForkHeaderSurfaces();if(t.binds!=before+4)throw new AssertionError("Blur switch stale");
 before=t.binds;ForkGraphicsSurface.style="liquid";t.updateForkHeaderSurfaces();if(t.binds!=before+4)throw new AssertionError("Liquid switch stale");
 before=t.binds;t.tint++;t.updateForkHeaderSurfaces();if(t.binds!=before+4)throw new AssertionError("Theme stale");
 before=t.binds;PanelSettings.strength=75;t.updateForkHeaderSurfaces();if(t.binds!=before+4)throw new AssertionError("Strength stale");
 t.forkHeaderSurfaceBackgrounds.clear();before=t.binds;t.updateForkHeaderSurfaces();if(t.binds!=before+4)throw new AssertionError("Reparent ownership stale");
 System.out.println("HeaderSurfaceRestorationTest passed (production binding policy; native resets, themes, modes, reparent, no redraw allocation)");}
}`;
const out=path.join(root,'build/beta8-header-restoration');fs.mkdirSync(out,{recursive:true});const file=path.join(out,'HeaderSurfaceRestorationTest.java');fs.writeFileSync(file,fixture);const home=process.env.JAVA_HOME||path.resolve(root,'../jdk17/jdk-17.0.20.1+1');for(const [bin,args]of[['javac',['-d',out,file]],['java',['-cp',out,'HeaderSurfaceRestorationTest']]]){let r=cp.spawnSync(path.join(home,'bin',bin+'.exe'),args,{stdio:'inherit'});if(r.error)throw r.error;if(r.status)process.exit(r.status);}
