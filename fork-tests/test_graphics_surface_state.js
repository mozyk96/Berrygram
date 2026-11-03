const fs = require('fs');
const cp = require('child_process');
const path = require('path');
const root = path.resolve(__dirname, '..');
function method(file, signature) {
    const source = fs.readFileSync(path.join(root, 'TMessagesProj/src/main/java', file), 'utf8');
    const start = source.indexOf(signature);
    if (start < 0) throw new Error('Missing production method ' + signature);
    let end = source.indexOf('{', start), depth = 0;
    do { if (source[end] === '{') depth++; if (source[end] === '}') depth--; end++; } while (depth);
    return source.slice(start, end);
}
let popup = method('org/telegram/ui/ActionBar/ActionBarMenuItem.java', 'private void updateForkMenuBlur()');
const names = ['ForkGraphicsSurface', 'ForkGlassSurface', 'PanelPolicy', 'ForkPanelSettings', 'BlurredBackgroundDrawableSource', 'BlurredBackgroundDrawableRenderNode', 'BlurredBackgroundDrawable', 'BlurredBackgroundSourceBitmap', 'LiquidGlassBitmapSource'];
for (const name of names) popup = popup.replace(new RegExp('org\\.telegram\\.[\\w.]+\\.' + name, 'g'), name);
popup = popup.replaceAll('android.graphics.Color.WHITE', '-1');
const intensity = method('org/telegram/ui/Components/blur3/drawable/BlurredBackgroundDrawableRenderNode.java', 'static float liquidIntensity(');
const update = method('org/telegram/ui/Components/blur3/LiquidGlassEffect.java', 'public void update(');
const fixture = `public class GraphicsSurfaceStateTest {
    static class Drawable {}
    static class Popup { Drawable bg; int changes; Drawable getBackgroundDrawable() { return bg; } void setBackgroundDrawable(Drawable d) { bg=d; changes++; } void setBackground(Drawable d) { setBackgroundDrawable(d); } void setBackgroundColor(int c) {} }
    static class ForkGraphicsSurface { static String style="blur"; static String effectiveStyle(){return style;} static int materialColor(Object c,int f){return f;} }
    static class ForkPanelSettings { static int radius=24; static int radiusDp(){return radius;} }
    static class ForkGlassSurface { static int tint=123; static ForkGlassSurface provider(Object p){return new ForkGlassSurface();} int getBackgroundColor(){return tint;} }
    static class PanelPolicy { static int surfaceRadiusDp(String s,int r){return r;} }
    static class Theme { static int key_actionBarDefaultSubmenuBackground; static int getColor(int k,Object r){return ForkGlassSurface.tint;} static Drawable createRoundRectDrawable(int r,int c){return new Drawable();} }
    static class Build { static class VERSION { static int SDK_INT=33; } }
    static class BlurredBackgroundSourceBitmap {}
    static class LiquidGlassBitmapSource extends BlurredBackgroundSourceBitmap { LiquidGlassBitmapSource(BlurredBackgroundSourceBitmap s){} }
    static class BlurredBackgroundDrawable extends Drawable { BlurredBackgroundDrawable setColorProvider(Object p){return this;} BlurredBackgroundDrawable setRadius(int r){return this;} BlurredBackgroundDrawable setPadding(int p){return this;} }
    static class BlurredBackgroundDrawableSource extends BlurredBackgroundDrawable { Object getSource(){return null;} void setSource(Object s){} }
    static class BlurredBackgroundDrawableRenderNode extends BlurredBackgroundDrawable { void setLiquidGlassEffectAllowed(){} }
    static class Factory { BlurredBackgroundDrawable create(Popup p,boolean b){return new BlurredBackgroundDrawableSource();} }
    Popup popupLayout=new Popup(); Factory subMenuFactory=new Factory(); Object resourcesProvider;
    String forkMenuStyleApplied; int forkMenuRadiusApplied=-1, forkMenuColorApplied; Drawable forkMenuBackgroundApplied;
    Object getContext(){return null;} int dp(int n){return n;}
    ${popup}
    ${intensity}
    static class Color { static int alpha(int c){return c>>>24;} static int red(int c){return c>>16&255;} static int green(int c){return c>>8&255;} static int blue(int c){return c&255;} }
    static class RenderEffect { static Object createRuntimeShaderEffect(Shader s,String n){return s;} }
    static class Node { int changes; int getWidth(){return 100;} int getHeight(){return 100;} void setRenderEffect(Object e){changes++;} }
    static class Shader { float intensity; void setFloatUniform(String n,float... f){if(n.equals("refract_intensity"))intensity=f[0];} }
    static class Effect {
        Node node=new Node(); Shader shader=new Shader(); Object effect;
        float resolutionX,resolutionY,centerX,centerY,sizeX,sizeY,radiusLeftTop,radiusRightTop,radiusRightBottom,radiusLeftBottom,thickness,intensity,index; int foregroundColor;
        ${update}
    }
    static void check(boolean b,String m){if(!b)throw new AssertionError(m);}
    public static void main(String[] args){
        GraphicsSurfaceStateTest p=new GraphicsSurfaceStateTest();
        p.updateForkMenuBlur(); Drawable initial=p.popupLayout.bg;
        p.updateForkMenuBlur(); check(p.popupLayout.bg==initial && p.popupLayout.changes==1,"Unchanged popup recreated");
        ForkGlassSurface.tint++; p.updateForkMenuBlur(); check(p.popupLayout.bg!=initial,"Theme/strength tint stale");
        initial=p.popupLayout.bg; p.popupLayout.bg=new Drawable(); p.updateForkMenuBlur(); check(p.popupLayout.bg!=initial && p.popupLayout.bg instanceof BlurredBackgroundDrawable,"Replaced popup not restored");
        ForkGraphicsSurface.style="liquid"; p.updateForkMenuBlur(); initial=p.popupLayout.bg;
        ForkGraphicsSurface.style="blur"; p.updateForkMenuBlur(); check(p.popupLayout.bg!=initial,"Liquid source retained for Blur");
        ForkGraphicsSurface.style="material"; p.updateForkMenuBlur(); initial=p.popupLayout.bg;
        p.updateForkMenuBlur(); check(p.popupLayout.bg==initial,"Fallback rebuilt");
        check(liquidIntensity(.75f,0)==0 && liquidIntensity(.75f,100)==.75f && liquidIntensity(.75f,-5)==0 && liquidIntensity(.75f,200)==.75f,"Intensity bounds");
        Effect e=new Effect();
        for(int strength=0;strength<=100;strength++){
            float value=liquidIntensity(.75f,strength);
            e.update(0,0,100,100,24,24,24,24,10,value,1.5f,0x88000000);
            check(Math.abs(e.shader.intensity-value)<.0001f,"Shader skipped strength "+strength);
        }
        int changes=e.node.changes; e.update(0,0,100,100,24,24,24,24,10,.75f,1.5f,0x88000000); check(e.node.changes==changes,"Unchanged shader rebound");
        System.out.println("GraphicsSurfaceStateTest passed (production popup state/shader update methods; mock Android rendering)");
    }
}`;
const output = path.join(root, 'build/fork-smoke/graphics-state');
fs.mkdirSync(output, { recursive: true });
const target = path.join(output, 'GraphicsSurfaceStateTest.java');
fs.writeFileSync(target, fixture);
const javaHome = process.env.JAVA_HOME || path.resolve(root, '../jdk17/jdk-17.0.20.1+1');
for (const [exe,args] of [['javac',['-d',output,target]],['java',['-cp',output,'GraphicsSurfaceStateTest']]]) {
    const result=cp.spawnSync(path.join(javaHome,'bin',exe+'.exe'),args,{stdio:'inherit'});
    if(result.error) throw result.error;
    if(result.status!==0) process.exit(result.status||1);
}
