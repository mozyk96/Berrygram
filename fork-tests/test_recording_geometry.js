const fs = require('fs'), path = require('path'), cp = require('child_process');
const root = path.resolve(__dirname, '..');
const source = fs.readFileSync(path.join(root, 'TMessagesProj/src/main/java/org/telegram/ui/Components/InstantCameraView.java'), 'utf8');
const start = source.indexOf('newSize = Math.max(1, Math.min(dp(272)');
if (start < 0) throw new Error('Recording preview sizing is missing');
const size = source.slice(start, source.indexOf(';', start) + 1);
const dir = path.join(root, 'build/fork-smoke/recording-display');
fs.mkdirSync(dir, {recursive:true});
fs.writeFileSync(path.join(dir, 'RecordingDisplayGeometryTest.java'), `
class RecordingDisplayGeometryTest {
    static int padTop, padBottom;
    static int dp(int n) { return n; }
    static int getPaddingTop() { return padTop; }
    static int getPaddingBottom() { return padBottom; }
    static class MeasureSpec { static int getSize(int n) { return n; } }
    static int preview(int widthMeasureSpec, int heightMeasureSpec, int top, int bottom) {
        padTop=top; padBottom=bottom; int newSize; ${size} return newSize;
    }
    public static void main(String[] args) {
        int[][] cases={{360,800,88,80},{320,568,80,80},{640,360,80,80},{240,480,80,80},{800,1280,88,100}};
        for(int[] c:cases) {
            int size=preview(c[0],c[1],c[2],c[3]);
            if(size>272 || size>c[0]-32 || size>c[1]-c[2]-c[3]-80 || size<1) throw new AssertionError("Preview exceeds safe display area");
        }
        if(preview(360,800,88,80)!=272) throw new AssertionError("Typical phone target differs");
        System.out.println("Recording display geometry passed (production sizing, portrait, narrow, landscape, tablet; no camera claims)");
    }
}`);
for(const [exe,args] of [['javac.exe',['-d',dir,path.join(dir,'RecordingDisplayGeometryTest.java')]],['java.exe',['-cp',dir,'RecordingDisplayGeometryTest']]]) {
    const result=cp.spawnSync(path.join(process.env.JAVA_HOME,'bin',exe),args,{encoding:'utf8'});
    if(result.stdout) process.stdout.write(result.stdout);
    if(result.status!==0) throw new Error(result.stderr || 'Recording sizing fixture failed');
}
