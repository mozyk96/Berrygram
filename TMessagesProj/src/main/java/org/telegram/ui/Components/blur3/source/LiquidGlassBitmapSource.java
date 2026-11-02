package org.telegram.ui.Components.blur3.source;

import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
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
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private RuntimeShader shader;
    private BitmapShader bitmapShader;
    private Bitmap shaderBitmap;
    private boolean unavailable;

    @Override public void draw(Canvas canvas, float left, float top, float right, float bottom) {
        Bitmap bitmap = getBitmap();
        if (unavailable || !canvas.isHardwareAccelerated() || bitmap == null || bitmap.isRecycled()) {
            super.draw(canvas, left, top, right, bottom);
            return;
        }
        try {
            if (shader == null) shader = new RuntimeShader(PROGRAM);
            if (bitmapShader == null || bitmap != shaderBitmap) {
                shaderBitmap = bitmap;
                bitmapShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                shader.setInputShader("backdrop", bitmapShader);
                paint.setShader(shader);
            }
            bitmapShader.setLocalMatrix(getMatrix());
            shader.setFloatUniform("bounds", left, top, right, bottom);
            shader.setFloatUniform("strength", org.telegram.messenger.fork.panels.ForkPanelSettings.effectStrength() / 100f);
            canvas.drawRect(left, top, right, bottom, paint);
        } catch (RuntimeException e) {
            unavailable = true;
            super.draw(canvas, left, top, right, bottom);
        }
    }
}
