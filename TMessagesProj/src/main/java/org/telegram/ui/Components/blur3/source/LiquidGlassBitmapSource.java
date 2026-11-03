package org.telegram.ui.Components.blur3.source;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.graphics.RuntimeShader;
import android.graphics.Shader;
import androidx.annotation.RequiresApi;

/** Local backdrop refraction for a single popup, with a plain blur fallback. */
@RequiresApi(33)
public final class LiquidGlassBitmapSource extends BlurredBackgroundSourceBitmap {
    private static final String PROGRAM =
            "uniform shader backdrop; uniform float4 bounds; uniform float strength;" +
            "half4 main(float2 p) {" +
            " float2 halfSize = max((bounds.zw-bounds.xy)*0.5, float2(1.0));" +
            " float2 d = (p-(bounds.xy+bounds.zw)*0.5)/halfSize;" +
            " float edge = smoothstep(0.55, 1.0, max(abs(d.x), abs(d.y)));" +
            " return backdrop.eval(p + d*edge*6.0*strength);" +
            "}";
    private final BlurredBackgroundSourceBitmap upstream;

    public LiquidGlassBitmapSource() { this(null); }
    public LiquidGlassBitmapSource(BlurredBackgroundSourceBitmap upstream) { this.upstream = upstream; }

    @Override public Bitmap getBitmap() { return upstream != null ? upstream.getBitmap() : super.getBitmap(); }
    @Override public Matrix getMatrix() { return upstream != null ? upstream.getMatrix() : super.getMatrix(); }

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private RuntimeShader shader;
    private BitmapShader bitmapShader;
    private Bitmap shaderBitmap;
    private final Matrix shaderMatrix = new Matrix();
    private boolean unavailable;

    private void clearShader() {
        paint.setShader(null);
        shader = null;
        bitmapShader = null;
        shaderBitmap = null;
    }

    @Override public void setBitmap(Bitmap bitmap) {
        if (bitmap != getBitmap()) clearShader();
        super.setBitmap(bitmap);
    }

    private void drawFallback(Canvas canvas, float left, float top, float right, float bottom) {
        if (upstream != null) upstream.draw(canvas, left, top, right, bottom);
        else super.draw(canvas, left, top, right, bottom);
    }

    @Override public void draw(Canvas canvas, float left, float top, float right, float bottom) {
        Bitmap bitmap = getBitmap();
        if (bitmap == null || bitmap.isRecycled()) {
            if (shaderBitmap != null) clearShader();
            return;
        }
        if (unavailable || !canvas.isHardwareAccelerated()) {
            drawFallback(canvas, left, top, right, bottom);
            return;
        }
        try {
            if (shader == null) shader = new RuntimeShader(PROGRAM);
            boolean changedBitmap = bitmapShader == null || bitmap != shaderBitmap;
            if (changedBitmap) {
                shaderBitmap = bitmap;
                bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            }
            // RuntimeShader captures the child shader's native instance when it is bound.
            if (changedBitmap || !shaderMatrix.equals(getMatrix())) {
                shaderMatrix.set(getMatrix());
                bitmapShader.setLocalMatrix(shaderMatrix);
                shader.setInputShader("backdrop", bitmapShader);
                paint.setShader(shader);
            }
            shader.setFloatUniform("bounds", left, top, right, bottom);
            shader.setFloatUniform("strength", org.telegram.messenger.fork.panels.ForkPanelSettings.effectStrength() / 100f);
            canvas.drawRect(left, top, right, bottom, paint);
        } catch (RuntimeException e) {
            unavailable = true;
            drawFallback(canvas, left, top, right, bottom);
        }
    }
}
