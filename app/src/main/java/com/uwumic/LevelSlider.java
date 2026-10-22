package com.uwumic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * One bar that is both the live loudness meter and the mic volume slider:
 * the colored fill moves with the sound, the round handle sets the volume (0-100%).
 */
public class LevelSlider extends View {
    public interface Listener { void onChange(int percent); }

    private static final int MAX = 100;
    private final float d;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint thumbRing = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF rect = new RectF();
    private float level;
    private int percent = 100;
    private Listener listener;

    public LevelSlider(Context c) {
        super(c);
        d = c.getResources().getDisplayMetrics().density;
        track.setColor(Color.parseColor("#5A5A5E"));
        thumbFill.setColor(Color.parseColor("#0B0B0D"));
        thumbRing.setColor(Color.parseColor("#3B82F6"));
        thumbRing.setStyle(Paint.Style.STROKE);
        thumbRing.setStrokeWidth(3 * d);
    }

    public void setListener(Listener l) { listener = l; }

    public void setPercent(int p) {
        percent = Math.max(0, Math.min(MAX, p));
        invalidate();
    }

    public void setLevel(float v) {
        level = Math.max(0f, Math.min(1f, v));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        setMeasuredDimension(MeasureSpec.getSize(widthSpec), (int) (48 * d));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float pad = 16 * d, left = pad, right = getWidth() - pad;
        float cy = getHeight() / 2f, bh = 16 * d, span = right - left;
        rect.set(left, cy - bh / 2, right, cy + bh / 2);
        canvas.drawRoundRect(rect, bh / 2, bh / 2, track);

        clip.reset();
        clip.addRoundRect(rect, bh / 2, bh / 2, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        fill.setColor(level > 0.9f ? Color.parseColor("#EF4444")
                : level > 0.7f ? Color.parseColor("#FACC15") : Color.parseColor("#4ADE80"));
        canvas.drawRect(left, rect.top, left + span * level, rect.bottom, fill);
        canvas.restore();

        float x = left + span * percent / MAX;
        canvas.drawCircle(x, cy, 12 * d, thumbFill);
        canvas.drawCircle(x, cy, 12 * d, thumbRing);
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
        float pad = 16 * d, span = getWidth() - 2 * pad;
        if (span <= 0) return;
        int p = Math.round(Math.max(0f, Math.min(1f, (x - pad) / span)) * MAX);
        if (p != percent) {
            percent = p;
            invalidate();
            if (listener != null) listener.onChange(p);
        }
    }
}
