package org.telegram.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.messenger.fork.panels.GraphicsColorPolicy;
import org.telegram.ui.ActionBar.Theme;

/** Small HSV editor; changing it previews only, persistence is controlled by its caller. */
final class ForkHsvPicker extends LinearLayout {
    interface Listener { void onColor(int color); }
    private final float[] hsv = new float[3];
    private final EditText hex;
    private final TextView sample;
    private final android.graphics.drawable.GradientDrawable sampleBackground = new android.graphics.drawable.GradientDrawable();
    private final SeekBar hue;
    private final Square square;
    private final Listener listener;
    private boolean updating;

    ForkHsvPicker(Context context, int initial, Listener listener) {
        super(context);
        this.listener = listener;
        setOrientation(VERTICAL);
        int pad = AndroidUtilities.dp(20);
        setPadding(pad, 0, pad, 0);
        Color.colorToHSV(initial, hsv);
        square = new Square(context);
        square.setContentDescription(ForkLocale.get(R.string.ForkColorSquare));
        LayoutParams squareParams = new LayoutParams(AndroidUtilities.dp(180), AndroidUtilities.dp(180));
        squareParams.gravity = android.view.Gravity.CENTER_HORIZONTAL;
        addView(square, squareParams);
        hue = new SeekBar(context);
        hue.setMax(359);
        hue.setContentDescription(ForkLocale.get(R.string.ForkColorHue));
        android.graphics.drawable.GradientDrawable hueTrack = new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.RED, Color.YELLOW, Color.GREEN, Color.CYAN, Color.BLUE, Color.MAGENTA, Color.RED});
        hueTrack.setCornerRadius(AndroidUtilities.dp(6));
        hueTrack.setSize(1, AndroidUtilities.dp(12));
        hue.setProgressDrawable(hueTrack);
        hue.setProgress(Math.min(359, Math.round(hsv[0])));
        addView(hue, new LayoutParams(LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48)));
        hex = new EditText(context);
        hex.setSingleLine(true);
        hex.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        hex.setHint("#RRGGBB");
        hex.setContentDescription(ForkLocale.get(R.string.ForkColorHex));
        hex.setFilters(new InputFilter[]{new InputFilter.LengthFilter(7)});
        addView(hex, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        sample = new TextView(context);
        sample.setGravity(android.view.Gravity.CENTER);
        sample.setText(ForkLocale.get(R.string.ForkColorPreview));
        sampleBackground.setCornerRadius(AndroidUtilities.dp(12));
        sample.setBackground(sampleBackground);
        addView(sample, new LayoutParams(LayoutParams.MATCH_PARENT, AndroidUtilities.dp(48)));
        hue.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar b, int value, boolean user) { if (user) { hsv[0] = value; update(true); } }
            @Override public void onStartTrackingTouch(SeekBar b) { }
            @Override public void onStopTrackingTouch(SeekBar b) { }
        });
        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (updating) return;
                Integer value = GraphicsColorPolicy.parseHex(s.toString());
                if (value != null) { Color.colorToHSV(value, hsv); hue.setProgress(Math.round(hsv[0]) % 360); update(false); }
            }
            @Override public void afterTextChanged(Editable e) { }
        });
        update(true);
    }
    int color() { return Color.HSVToColor(hsv); }
    boolean valid() { return GraphicsColorPolicy.parseHex(hex.getText().toString()) != null; }
    int selectedColor() { Integer value = GraphicsColorPolicy.parseHex(hex.getText().toString()); return value == null ? color() : value; }
    void showError() { hex.setError(ForkLocale.get(R.string.ForkColorInvalid)); }
    private void update(boolean updateHex) {
        int color = color();
        if (updateHex) { updating = true; hex.setText(GraphicsColorPolicy.formatHex(color)); hex.setSelection(hex.length()); updating = false; }
        sample.setTextColor(GraphicsColorPolicy.contrastText(color));
        sampleBackground.setColor(color);
        square.invalidate();
        if (listener != null) listener.onColor(color);
    }
    private final class Square extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float[] pure = {0f, 1f, 1f};
        private LinearGradient saturationGradient, valueGradient;
        private int cachedHue = -1, cachedWidth = -1, cachedHeight = -1;
        Square(Context c) { super(c); setFocusable(true); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            int hueValue = Math.round(hsv[0]);
            if (w != cachedWidth || h != cachedHeight) {
                cachedWidth = w;
                cachedHeight = h;
                valueGradient = new LinearGradient(0, 0, 0, h, Color.TRANSPARENT, Color.BLACK, Shader.TileMode.CLAMP);
                cachedHue = -1;
            }
            if (hueValue != cachedHue) {
                cachedHue = hueValue;
                pure[0] = hsv[0];
                saturationGradient = new LinearGradient(0, 0, w, 0, Color.WHITE, Color.HSVToColor(pure), Shader.TileMode.CLAMP);
            }
            paint.setShader(saturationGradient);
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(valueGradient);
            canvas.drawRect(0, 0, w, h, paint);
            paint.setShader(null);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(AndroidUtilities.dp(2));
            paint.setColor(GraphicsColorPolicy.contrastText(color()));
            canvas.drawCircle(hsv[1] * w, (1f - hsv[2]) * h, AndroidUtilities.dp(7), paint);
            paint.setStyle(Paint.Style.FILL);
        }
        @Override public boolean onTouchEvent(MotionEvent event) {
            int action = event.getActionMasked();
            if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE && action != MotionEvent.ACTION_UP) return false;
            if (getWidth() <= 0 || getHeight() <= 0) return false;
            hsv[1] = Math.max(0f, Math.min(1f, event.getX() / getWidth()));
            hsv[2] = 1f - Math.max(0f, Math.min(1f, event.getY() / getHeight()));
            update(true);
            if (action == MotionEvent.ACTION_UP) performClick();
            return true;
        }
        @Override public boolean performClick() { super.performClick(); return true; }
        @Override public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {
            float step = 0.05f;
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT) hsv[1] = Math.max(0f, hsv[1] - step);
            else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_RIGHT) hsv[1] = Math.min(1f, hsv[1] + step);
            else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_UP) hsv[2] = Math.min(1f, hsv[2] + step);
            else if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN) hsv[2] = Math.max(0f, hsv[2] - step);
            else return super.onKeyDown(keyCode, event);
            update(true);
            return true;
        }
    }
}
