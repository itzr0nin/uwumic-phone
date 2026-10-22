package com.uwumic;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;

/** Dark blueprint background: faint grid, a cyan glow top-left and a red glow at the bottom. */
public class GridBackground extends Drawable {
    private final float step;
    private final Paint base = new Paint();
    private final Paint tealGlow = new Paint();
    private final Paint redGlow = new Paint();
    private final Paint grid = new Paint();

    public GridBackground(float density) {
        step = 24 * density;
        grid.setColor(0x0FFFFFFF);
        grid.setStrokeWidth(Math.max(1f, density * 0.6f));
    }

    @Override
    protected void onBoundsChange(Rect b) {
        float w = b.width(), h = b.height();
        base.setShader(new LinearGradient(0, 0, 0, h, 0xFF0A1219, 0xFF07090F, Shader.TileMode.CLAMP));
        tealGlow.setShader(new RadialGradient(0, 0, Math.max(w, 1f) * 1.1f,
                new int[]{0x3300E5FF, 0x0000E5FF}, null, Shader.TileMode.CLAMP));
        redGlow.setShader(new RadialGradient(w * 0.5f, h, Math.max(w, 1f) * 1.0f,
                new int[]{0x30FF0A45, 0x00FF0A45}, null, Shader.TileMode.CLAMP));
    }

    @Override
    public void draw(Canvas canvas) {
        Rect b = getBounds();
        canvas.drawRect(b, base);
        canvas.drawRect(b, tealGlow);
        canvas.drawRect(b, redGlow);
        for (float x = b.left; x <= b.right; x += step) canvas.drawLine(x, b.top, x, b.bottom, grid);
        for (float y = b.top; y <= b.bottom; y += step) canvas.drawLine(b.left, y, b.right, y, grid);
    }

    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter cf) { }
    @Override public int getOpacity() { return PixelFormat.OPAQUE; }
}
