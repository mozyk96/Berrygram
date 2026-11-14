const fs = require('fs');
const path = require('path');
const cp = require('child_process');
const root = path.resolve(__dirname, '..');
const read = file => fs.readFileSync(path.join(root, 'TMessagesProj/src/main/java/org/telegram/ui/Components/chat', file), 'utf8');
function method(source, signature) {
    const start = source.indexOf(signature);
    if (start < 0) throw new Error('Missing production method: ' + signature);
    let end = source.indexOf('{', start), depth = 0;
    do {
        if (source[end] === '{') depth++;
        if (source[end] === '}') depth--;
        end++;
    } while (depth);
    return source.slice(start, end).replaceAll('@NonNull ', '');
}
const container = read('ChatInputViewsContainer.java');
if (!container.includes('ViewPositionWatcher.computeRectInParent(input.messageEditTextContainer, this, inputFieldBounds)') ||
    container.includes('offsetDescendantRectToMyCoords(input.messageEditTextContainer')) {
    throw new Error('Composer glass must use the translated field coordinates');
}
const source = read('ViewPositionWatcher.java');
const dir = path.join(root, 'build/fork-smoke/composer-coordinates');
fs.mkdirSync(dir, {recursive: true});
fs.writeFileSync(path.join(dir, 'ComposerCoordinatesTest.java'), `
public class ComposerCoordinatesTest {
    interface ViewParent {}
    static class View implements ViewParent {
        View parent; float x, y; int width = 200, height = 48, sx, sy;
        View(View parent, float x, float y) { this.parent=parent; this.x=x; this.y=y; }
        float getX() { return x; } float getY() { return y; }
        int getWidth() { return width; } int getHeight() { return height; }
        int getScrollX() { return sx; } int getScrollY() { return sy; }
        ViewParent getParent() { return parent; }
    }
    static class RectF {
        float left, top, right, bottom;
        void set(float l, float t, float r, float b) { left=l; top=t; right=r; bottom=b; }
    }
    static final float[] tmpOffset = new float[2];
    ${method(source, 'private static boolean computeOffsetInRoot(')}
    ${method(source, 'public static boolean computeRectInParent(')}
    static void check(View field, View host, float top) {
        RectF rect=new RectF();
        if (!computeRectInParent(field, host, rect) || Math.abs(rect.top-top)>0.001f ||
            rect.bottom-rect.top != field.height || rect.left != 6) throw new AssertionError("Incorrect translated field bounds");
    }
    public static void main(String[] args) {
        View host=new View(null, 50, 100), island=new View(host, 0, 700);
        View input=new View(island, 0, 0), textRow=new View(input, 0, 1), field=new View(textRow, 6, 0);
        for (int inset : new int[] {0, 24, 48, 320}) {
            island.y=700-inset-9;
            check(field, host, 700-inset-9+1);
            field.height=120; check(field, host, 700-inset-9+1); field.height=48;
            input.y=27; check(field, host, 700-inset-9+1+27); input.y=0;
            island.sy=3; check(field, host, 700-inset-9+1-3); island.sy=0;
        }
        System.out.println("Composer translated bounds passed (navigation, IME, multiline, hide translation; fake view host)");
    }
}
`);
const bin = path.join(process.env.JAVA_HOME, 'bin');
for (const [command, args] of [['javac.exe', ['-encoding','UTF-8','-d',dir,path.join(dir,'ComposerCoordinatesTest.java')]], ['java.exe', ['-cp',dir,'ComposerCoordinatesTest']]]) {
    const result = cp.spawnSync(path.join(bin, command), args, {encoding:'utf8'});
    if (result.stdout) process.stdout.write(result.stdout);
    if (result.status !== 0) throw new Error(result.stderr || 'Composer coordinate test failed');
}
