package com.uwumic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * One control that is both the live loudness meter and the mic volume slider (0-100%):
 * the bright fill is how loud you are right now, the thin line and hexagon handle are the volume.
 */
public class LevelSlider extends View {
    public interface Listener { void onChange(int percent); }

    private static final int MAX = 100;
    private static final int CYAN = 0xFF00E5FF, AMBER = 0xFFF5B544, RED = 0xFFFF0A45, GREY = 0xFF6B7280;
    private final float d;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hex = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hexShine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path hexPath = new Path();
    private final RectF rect = new RectF();
    private float level;
    private int percent = 100;
    private boolean muted;
    private Listener listener;

    public LevelSlider(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        track.setColor(0xFF141C29);
        line.setStrokeWidth(2 * d);
        hexShine.setColor(0x66FFFFFF);
    }

    public void setListener(Listener l) { listener = l; }

    public void setPercent(int p) {
        p = Math.max(0, Math.min(MAX, p));
        if (p != percent) { percent = p; invalidate(); }
    }

    public void setLevel(float v) {
        v = Math.max(0f, Math.min(1f, v));
        if (v != level) { level = v; invalidate(); }
    }

    public void setMuted(boolean m) {
        if (m != muted) { muted = m; invalidate(); }
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), (int) (36 * d));
    }

    private void hexagon(float x, float cy, float hw, float hh) {
        hexPath.reset();
        hexPath.moveTo(x, cy - hh);
        hexPath.lineTo(x + hw, cy - hh * 0.55f);
        hexPath.lineTo(x + hw, cy + hh * 0.55f);
        hexPath.lineTo(x, cy + hh);
        hexPath.lineTo(x - hw, cy + hh * 0.55f);
        hexPath.lineTo(x - hw, cy - hh * 0.55f);
        hexPath.close();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float pad = 12 * d, left = pad, right = getWidth() - pad, span = right - left;
        float cy = getHeight() / 2f, th = 8 * d;
        rect.set(left, cy - th / 2, right, cy + th / 2);
        canvas.drawRoundRect(rect, 2 * d, 2 * d, track);

        if (!muted && level > 0f) {
            int c = level > 0.9f ? RED : level > 0.7f ? AMBER : CYAN;
            fill.setColor((0xCC << 24) | (c & 0xFFFFFF));
            canvas.save();
            canvas.clipRect(left, rect.top, left + span * level, rect.bottom);
            canvas.drawRoundRect(rect, 2 * d, 2 * d, fill);
            canvas.restore();
        }

        float x = left + span * percent / MAX;
        if (!muted && percent > 0) {
            line.setColor(CYAN);
            canvas.drawLine(left, cy, x, cy, line);
        }
        hex.setColor(muted ? GREY : CYAN);
        hexagon(x, cy, 9 * d, 14 * d);
        canvas.drawPath(hexPath, hex);
        hexagon(x, cy, 4 * d, 7 * d);
        canvas.drawPath(hexPath, hexShine);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                moveTo(e.getX());
                return true;
            case MotionEvent.ACTION_MOVE:
                moveTo(e.getX());
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(false);
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void moveTo(float x) {
        float pad = 12 * d, span = getWidth() - 2 * pad;
        if (span <= 0) return;
        int p = Math.round(Math.max(0f, Math.min(1f, (x - pad) / span)) * MAX);
        if (p != percent) {
            percent = p;
            invalidate();
            if (listener != null) listener.onChange(p);
        }
    }
}
