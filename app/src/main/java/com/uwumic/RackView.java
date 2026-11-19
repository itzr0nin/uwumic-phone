package com.uwumic;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

/**
 * The whole screen, drawn by hand in a 346 x 676 "design unit" space that is scaled to fit any phone
 * (so it always fits on one page, no scrolling). Dark and light themes, tap targets and the gain fader live here.
 */
public class RackView extends View {
    public interface Listener {
        void onToggle();
        void onMute();
        void onGain(int percent);
        void onTheme(String name);
    }

    public interface PortListener {
        /** Where the port number is drawn (pixels), so the real text box can sit exactly on it. */
        void onPortRect(int l, int t, int r, int b, float unit);
    }

    static final float DW = 346f, DH = 682f;
    private static final float L = 15f, R = 331f;

    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rf = new RectF();
    private final Typeface display, displayItalic, mono, monoBold;
    private final Listener listener;
    private PortListener portListener;
    private Theme th;
    private String ip = "";
    private float u = 1f, ox, oy;
    private final RectF rToggle = new RectF(), rMute = new RectF(), rSlider = new RectF(), rLight = new RectF(), rDark = new RectF();
    private int touch;
    private long lastT = SystemClock.elapsedRealtime();
    private long lastPk;
    private int pps;
    private float sliderX0, sliderX1;

    public RackView(Context ctx, Listener listener, String themeName) {
        super(ctx);
        this.listener = listener;
        this.th = Theme.byName(themeName);
        display = Fonts.display(ctx);
        displayItalic = Typeface.create(display, Typeface.ITALIC);
        mono = Fonts.mono(ctx);
        monoBold = Fonts.monoBold(ctx);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // glow (shadow layers) needs software drawing
    }

    public void setPortListener(PortListener l) { portListener = l; notifyPort(); }
    public void setIp(String ip) { this.ip = ip == null ? "" : ip; }
    public Theme theme() { return th; }

    public void setThemeName(String name) {
        th = Theme.byName(name);
        invalidate();
    }

    // ---------------------------------------------------------------- layout
    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        u = Math.min(w / DW, h / DH);
        ox = (w - DW * u) / 2f;
        oy = Math.max(0f, (h - DH * u) / 2f);
        notifyPort();
    }

    private void notifyPort() {
        if (portListener != null && getWidth() > 0) {
            // the middle of the port LCD (326 + 34 .. 326 + 76), starting where the IP digits start
            portListener.onPortRect(Math.round(ox + (L + 22) * u), Math.round(oy + 364f * u),
                    Math.round(ox + (R - 100) * u), Math.round(oy + 398f * u), u);
        }
    }

    // ---------------------------------------------------------------- touch
    private boolean in(RectF r, float x, float y) { return r.contains(x, y); }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        float x = (e.getX() - ox) / u, y = (e.getY() - oy) / u;
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                touch = in(rLight, x, y) ? 4 : in(rDark, x, y) ? 5 : in(rToggle, x, y) ? 1 : in(rMute, x, y) ? 2 : in(rSlider, x, y) ? 3 : 0;
                if (touch == 3) {
                    if (getParent() != null) getParent().requestDisallowInterceptTouchEvent(true);
                    drag(x);
                }
                return touch != 0;
            case MotionEvent.ACTION_MOVE:
                if (touch == 3) drag(x);
                return true;
            case MotionEvent.ACTION_UP:
                if (touch == 1 && in(rToggle, x, y)) listener.onToggle();
                else if (touch == 2 && in(rMute, x, y)) listener.onMute();
                else if (touch == 4 && in(rLight, x, y)) listener.onTheme("light");
                else if (touch == 5 && in(rDark, x, y)) listener.onTheme("dark");
                touch = 0;
                return true;
            case MotionEvent.ACTION_CANCEL:
                touch = 0;
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void drag(float x) {
        float v = (x - sliderX0) / (sliderX1 - sliderX0);
        listener.onGain(Math.round(Math.max(0f, Math.min(1f, v)) * 100f));
    }

    // ---------------------------------------------------------------- drawing helpers (design units)
    private float text(Canvas c, String s, float x, float cy, float size, int color, Typeface tf, Paint.Align al, float em) {
        p.setStyle(Paint.Style.FILL);
        p.setColor(color);
        p.setTypeface(tf);
        p.setTextSize(size);
        p.setTextAlign(al);
        p.setLetterSpacing(em);
        Paint.FontMetrics fm = p.getFontMetrics();
        c.drawText(s, x, cy - (fm.ascent + fm.descent) / 2f, p);
        return p.measureText(s);
    }

    private float glowText(Canvas c, String s, float x, float cy, float size, int color, Typeface tf, Paint.Align al, float em, int glow) {
        if (!th.light) {
            p.setShadowLayer(7f * u, 0f, 0f, glow);
        }
        float w = text(c, s, x, cy, size, color, tf, al, em);
        p.clearShadowLayer();
        return w;
    }

    private float width(String s, float size, Typeface tf, float em) {
        p.setTypeface(tf);
        p.setTextSize(size);
        p.setLetterSpacing(em);
        return p.measureText(s);
    }

    private void rr(Canvas c, float l, float t, float r, float b, float rad, int fill, int stroke, float sw) {
        p.setPathEffect(null);
        if (fill != 0) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(fill);
            c.drawRoundRect(l, t, r, b, rad, rad, p);
        }
        if (stroke != 0 && sw > 0) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(sw);
            p.setColor(stroke);
            c.drawRoundRect(l + sw / 2, t + sw / 2, r - sw / 2, b - sw / 2, rad, rad, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    private void line(Canvas c, float x1, float y1, float x2, float y2, int color, float w, boolean dashed) {
        p.setStyle(Paint.Style.STROKE);
        p.setColor(color);
        p.setStrokeWidth(w);
        p.setPathEffect(dashed ? new DashPathEffect(new float[]{3f, 3f}, 0f) : null);
        c.drawLine(x1, y1, x2, y2, p);
        p.setPathEffect(null);
        p.setStyle(Paint.Style.FILL);
    }

    private void circle(Canvas c, float cx, float cy, float r, int fill, int stroke, float sw) {
        if (fill != 0) {
            p.setStyle(Paint.Style.FILL);
            p.setColor(fill);
            c.drawCircle(cx, cy, r, p);
        }
        if (stroke != 0) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(sw);
            p.setColor(stroke);
            c.drawCircle(cx, cy, r, p);
            p.setStyle(Paint.Style.FILL);
        }
    }

    /** A card: hard offset shadow, face and an outline (thicker in the light theme). */
    private void card(Canvas c, float l, float t, float r, float b) {
        float off = th.light ? 3f : 2.5f;
        rr(c, l + off, t + off, r + off, b + off, 9f, th.shadow, 0, 0);
        rr(c, l, t, r, b, 9f, th.card, th.edge, th.light ? 2f : 1.5f);
    }

    /** Small boxed label. align LEFT: x is the left edge, RIGHT: x is the right edge. Returns its width. */
    private float chip(Canvas c, String s, float x, float cy, float h, float size, int fg, int bg, int edge, Paint.Align al, float em, int dot) {
        float tw = width(s, size, monoBold, em);
        float w = tw + 14f + (dot != 0 ? 11f : 0f);
        float x0 = al == Paint.Align.RIGHT ? x - w : x;
        rr(c, x0, cy - h / 2f, x0 + w, cy + h / 2f, 3f, bg, edge, th.light ? 1.5f : 1f);
        float tx = x0 + 7f;
        if (dot != 0) {
            circle(c, tx + 3f, cy, 3f, dot, 0, 0);
            tx += 11f;
        }
        text(c, s, tx, cy, size, fg, monoBold, Paint.Align.LEFT, em);
        return w;
    }

    // ---------------------------------------------------------------- the screen
    @Override
    protected void onDraw(Canvas c) {
        long now = SystemClock.elapsedRealtime();
        if (now - lastT >= 1000) {
            pps = (int) ((State.pkts - lastPk) * 1000L / (now - lastT));
            lastPk = State.pkts;
            lastT = now;
        }
        final boolean on = State.running, muted = State.muted, live = on && State.clients > 0;
        final int pct = Math.round(State.gain * 100f);
        final boolean blink = (now / 900L) % 2 == 0;

        c.drawColor(th.bg);
        c.save();
        c.translate(ox, oy);
        c.scale(u, u);
        if (th.light) { // faint technical grid
            for (float gx = -ox / u - ((-ox / u) % 24f); gx < DW + ox / u; gx += 24f) line(c, gx, -oy / u, gx, DH + oy / u, th.grid, 0.6f, false);
            for (float gy = -oy / u - ((-oy / u) % 24f); gy < DH + oy / u; gy += 24f) line(c, -ox / u, gy, DW + ox / u, gy, th.grid, 0.6f, false);
        }
        header(c);
        statusCard(c, 84f, on, live, blink);
        lcdCard(c, 234f, false, on);
        lcdCard(c, 326f, true, on);
        actionButton(c, 424f, on);
        gainCard(c, 502f, on, muted, pct);
        c.restore();
    }

    private void header(Canvas c) {
        float x = 18f, cy = 38f, size = 27f;
        float wUwU = text(c, "UwU ", x, cy, size, th.brand, displayItalic, Paint.Align.LEFT, 0f);
        float micX = x + wUwU;
        float wMic = text(c, "Mic", micX, cy, size, th.coral, displayItalic, Paint.Align.LEFT, 0f);
        rr(c, micX, 51f, micX + wMic, 54f, 0f, th.teal, 0, 0);
        chip(c, "AUDIO TX", micX + wMic + 9f, 36f, 21f, 8.6f, th.tealText, th.chipBg, th.chipEdge, Paint.Align.LEFT, 0.02f, 0);
        text(c, "WIRELESS HIGH-FIDELITY AUDIO STREAMER", 18f, 66f, 7.1f, th.txt2, mono, Paint.Align.LEFT, 0.12f);
        // LIGHT | DARK
        float sx0 = 248f, sx1 = 328f, sy0 = 50f, sy1 = 70f, mid = (sx0 + sx1) / 2f;
        rr(c, sx0 + 2f, sy0 + 2f, sx1 + 2f, sy1 + 2f, 3f, th.shadow, 0, 0);
        rr(c, sx0, sy0, sx1, sy1, 3f, th.switchBg, th.switchEdge, th.light ? 2f : 1.2f);
        rLight.set(sx0, sy0, mid, sy1);
        rDark.set(mid, sy0, sx1, sy1);
        RectF sel = th.light ? rLight : rDark;
        rr(c, sel.left + 2f, sel.top + 2f, sel.right - 2f, sel.bottom - 2f, 2f, th.selBg, th.selEdge, 1f);
        circle(c, sel.left + 8f, (sy0 + sy1) / 2f, 2.4f, th.teal, 0, 0);
        text(c, "LIGHT", rLight.centerX() + (th.light ? 4f : 0f), (sy0 + sy1) / 2f, 6.8f, th.light ? th.txt : th.dim, monoBold, Paint.Align.CENTER, 0.08f);
        text(c, "DARK", rDark.centerX() + (th.light ? 0f : 4f), (sy0 + sy1) / 2f, 6.8f, th.light ? th.dim : th.txt, monoBold, Paint.Align.CENTER, 0.08f);
    }

    private void statusCard(Canvas c, float t, boolean on, boolean live, boolean blink) {
        card(c, L, t, R, t + 138f);
        path.reset();
        path.moveTo(L + 12f, t + 17.5f);
        path.lineTo(L + 18f, t + 22f);
        path.lineTo(L + 12f, t + 26.5f);
        path.close();
        p.setStyle(Paint.Style.FILL);
        p.setColor(on ? th.teal : th.coral);
        c.drawPath(path, p);
        text(c, "STREAM STATUS // TX", L + 25f, t + 22f, 9.2f, th.txt, monoBold, Paint.Align.LEFT, 0.06f);
        if (on) {
            chip(c, "MODE: TRANSMITTING", R - 12f, t + 22f, 17f, 7.2f, th.tealText, th.chipBg, th.chipEdge, Paint.Align.RIGHT, 0.05f, 0);
        } else {
            text(c, "MODE: NORMAL", R - 12f, t + 22f, 7.6f, th.dim, monoBold, Paint.Align.RIGHT, 0.05f);
        }
        line(c, L + 12f, t + 38f, R - 12f, t + 38f, th.edge, 1f, true);
        // LED + big state word
        int led = on ? th.ledLive : th.coral;
        float cy = t + 62f, ledx = L + 25f;
        if (!th.light) p.setShadowLayer((blink && on ? 9f : 6f) * u, 0f, 0f, led);
        circle(c, ledx, cy, 6.5f, led, 0, 0);
        p.clearShadowLayer();
        circle(c, ledx, cy, 9f, 0, led, 1.6f);
        String word = on ? "BROADCASTING" : "STOPPED";
        float avail = (R - 12f - 80f - 8f) - (L + 44f), size = 23f, ww = width(word, size, display, 0.06f);
        if (ww > avail) size = size * avail / ww; // never runs under the tag on the right
        glowText(c, word, L + 44f, cy, size, on ? th.liveText : th.coral, display, Paint.Align.LEFT, 0.06f, on ? th.ledLive : th.coral);
        // right-hand tag
        if (live) {
            float w = 78f;
            rr(c, R - 12f - w + 2f, cy - 12f + 2f, R - 12f + 2f, cy + 12f + 2f, 3f, th.shadow, 0, 0);
            rr(c, R - 12f - w, cy - 12f, R - 12f, cy + 12f, 3f, th.coral, th.light ? th.edge : 0, 1.5f);
            circle(c, R - 12f - w + 13f, cy, 3f, 0xFFFFFFFF, 0, 0);
            text(c, "LIVE TX", R - 12f - w + 22f, cy, 8.6f, 0xFFFFFFFF, monoBold, Paint.Align.LEFT, 0.1f);
            circle(c, R - 12f - 50f, cy + 20f, 1.8f, th.coral, 0, 0);
            text(c, "ON AIR", R - 12f - 45f, cy + 20f, 6.6f, th.coral, monoBold, Paint.Align.LEFT, 0.1f);
        } else if (on) {
            chip(c, "WAITING FOR PC", R - 12f, cy - 3f, 24f, 7.6f, th.amber, th.amberBg, th.amberEdge, Paint.Align.RIGHT, 0.06f, th.amber);
            text(c, "NO CLIENT YET", R - 12f, cy + 20f, 6.6f, th.dim, mono, Paint.Align.RIGHT, 0.08f);
        } else {
            chip(c, "STANDBY", R - 12f, cy - 3f, 24f, 8.4f, th.amber, th.amberBg, th.amberEdge, Paint.Align.RIGHT, 0.08f, th.amber);
            text(c, "CH-01 [LOCAL]", R - 12f, cy + 20f, 6.6f, th.dim, mono, Paint.Align.RIGHT, 0.08f);
        }
        line(c, L + 12f, t + 92f, R - 12f, t + 92f, th.edge, 1f, true);
        chip(c, "TX: 48 kHz // PCM16 MONO", L + 12f, t + 114f, 22f, 7.4f, th.txt2, th.tagBg, th.tagEdge, Paint.Align.LEFT, 0.04f, 0);
        chip(c, "PACKETS: " + (live ? pps : 0) + "/s", R - 12f, t + 114f, 22f, 7.4f, th.tealText, th.tagBg, th.tagEdge, Paint.Align.RIGHT, 0.04f, 0);
    }

    private void lcdCard(Canvas c, float t, boolean port, boolean on) {
        card(c, L, t, R, t + 84f);
        // icon
        int ic = th.teal;
        if (!port) { // three little boxes joined by lines
            rr(c, L + 19f, t + 12f, L + 25f, t + 17f, 1f, 0, ic, 1.4f);
            rr(c, L + 12f, t + 23f, L + 18f, t + 28f, 1f, 0, ic, 1.4f);
            rr(c, L + 26f, t + 23f, L + 32f, t + 28f, 1f, 0, ic, 1.4f);
            line(c, L + 22f, t + 17f, L + 22f, t + 20.5f, ic, 1.4f, false);
            line(c, L + 15f, t + 20.5f, L + 29f, t + 20.5f, ic, 1.4f, false);
            line(c, L + 15f, t + 20.5f, L + 15f, t + 23f, ic, 1.4f, false);
            line(c, L + 29f, t + 20.5f, L + 29f, t + 23f, ic, 1.4f, false);
        } else { // wifi-like arcs
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(1.5f);
            p.setColor(ic);
            rf.set(L + 14f, t + 14f, L + 30f, t + 30f);
            c.drawArc(rf, 225f, 90f, false, p);
            rf.set(L + 17f, t + 17f, L + 27f, t + 27f);
            c.drawArc(rf, 225f, 90f, false, p);
            p.setStyle(Paint.Style.FILL);
            circle(c, L + 22f, t + 25f, 1.8f, ic, 0, 0);
        }
        text(c, port ? "TARGET PORT" : "HOST IP ADDRESS", L + 40f, t + 20f, 8.8f, th.txt, monoBold, Paint.Align.LEFT, 0.08f);
        chip(c, port ? "TCP SOCKET" : "IPv4", R - 12f, t + 20f, 18f, 7.2f, th.tagText, th.tagBg, th.tagEdge, Paint.Align.RIGHT, 0.04f, 0);
        // the LCD well
        float wl = L + 8f, wt = t + 34f, wr = R - 8f, wb = t + 76f;
        rr(c, wl, wt, wr, wb, 4f, th.lcdBg, th.lcdEdge, th.light ? 2f : 1.6f);
        if (th.lcdScan != 0) {
            for (float y = wt + 3f; y < wb - 2f; y += 3f) line(c, wl + 2f, y, wr - 2f, y, th.lcdScan, 0.7f, false);
        }
        int digits = on ? th.lcdLive : th.lcdIdle;
        if (!port) {
            String shown = ip.isEmpty() ? "NO WI-FI" : ip;
            float size = ip.isEmpty() ? 17f : 21.5f;
            glowText(c, shown, L + 22f, t + 55f, size, ip.isEmpty() ? th.coral : digits, monoBold, Paint.Align.LEFT, 0.1f, digits);
        }
        String tag = port ? (on ? "LISTENING" : "TAP TO EDIT") : (ip.isEmpty() ? "OFFLINE" : "ONLINE");
        chip(c, tag, R - 20f, t + 55f, 20f, 7.4f, th.lcdTag, th.lcdBg, th.lcdTag, Paint.Align.RIGHT, 0.06f, 0);
    }

    private void actionButton(Canvas c, float t, boolean on) {
        float h = 64f;
        int face = on ? th.stopFace : th.startFace, hi = on ? th.stopHi : th.startHi;
        int fg = on ? th.stopText : th.startText;
        rr(c, L + 3f, t + 4f, R + 3f, t + h + 4f, 10f, th.shadow, 0, 0);
        rr(c, L, t, R, t + h, 10f, hi, th.light ? th.edge : 0, 2f);
        rr(c, L, t + 4f, R, t + h, 10f, face, th.light ? th.edge : 0, th.light ? 2f : 0f);
        if (th.light) rr(c, L, t, R, t + h, 10f, 0, th.edge, 2f);
        rToggle.set(L, t, R, t + h);
        rr(c, L + 16f, t + 15f, L + 50f, t + 49f, 6f, th.iconBg, th.iconEdge, 2f);
        p.setStyle(Paint.Style.FILL);
        p.setColor(th.iconEdge);
        if (on) {
            rr(c, L + 27f, t + 26f, L + 39f, t + 38f, 1.5f, th.iconEdge, 0, 0);
        } else {
            path.reset();
            path.moveTo(L + 28f, t + 24f);
            path.lineTo(L + 40f, t + 32f);
            path.lineTo(L + 28f, t + 40f);
            path.close();
            c.drawPath(path, p);
        }
        text(c, on ? "STOP BROADCAST" : "START BROADCAST", L + 64f, t + 27f, 16f, fg, display, Paint.Align.LEFT, 0.07f);
        text(c, on ? "HALT WIRELESS AUDIO TRANSMISSION" : "TRANSMIT WIRELESS AUDIO", L + 64f, t + 46f, 6.6f, fg, mono, Paint.Align.LEFT, 0.1f);
    }

    private void micIcon(Canvas c, float cx, float cy, int color, boolean slashed) {
        rr(c, cx - 3f, cy - 8f, cx + 3f, cy + 2f, 3f, 0, color, 1.6f);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(1.6f);
        p.setColor(color);
        rf.set(cx - 6f, cy - 5f, cx + 6f, cy + 6f);
        c.drawArc(rf, 0f, 180f, false, p);
        p.setStyle(Paint.Style.FILL);
        line(c, cx, cy + 6f, cx, cy + 9f, color, 1.6f, false);
        if (slashed) line(c, cx - 8f, cy - 9f, cx + 8f, cy + 9f, color, 2f, false);
    }

    private void gainCard(Canvas c, float t, boolean on, boolean muted, int pct) {
        card(c, L, t, R, t + 166f);
        micIcon(c, L + 20f, t + 22f, th.teal, false);
        text(c, "MIC GAIN", L + 36f, t + 22f, 9.6f, th.txt, monoBold, Paint.Align.LEFT, 0.08f);
        // OUTPUT: 100% (0 dB)
        String db = muted || pct == 0 ? "-\u221e dB" : pct == 100 ? "0 dB" : String.format(java.util.Locale.US, "%.1f dB", 20.0 * Math.log10(pct / 100.0));
        String val = muted ? "MUTED" : pct + "%";
        float f = 8f, bx1 = R - 12f, bx0 = bx1 - 132f;
        rr(c, bx0, t + 10f, bx1, t + 34f, 3f, th.outBg, th.outEdge, th.light ? 1.6f : 1.2f);
        float w1 = width("OUTPUT: ", f, mono, 0.04f), w2 = width(val, f + 2f, monoBold, 0.04f), w3 = width(" (" + db + ")", f, mono, 0.02f);
        float sx = (bx0 + bx1) / 2f - (w1 + w2 + w3) / 2f;
        text(c, "OUTPUT: ", sx, t + 22f, f, th.txt2, mono, Paint.Align.LEFT, 0.04f);
        text(c, val, sx + w1, t + 22f, f + 2f, muted ? th.coral : th.tealText, monoBold, Paint.Align.LEFT, 0.04f);
        text(c, " (" + db + ")", sx + w1 + w2, t + 22f, f, th.txt2, mono, Paint.Align.LEFT, 0.02f);
        // fader: groove, live loudness, cap
        sliderX0 = L + 18f;
        sliderX1 = R - 18f;
        float cy = t + 66f, span = sliderX1 - sliderX0;
        rSlider.set(sliderX0 - 14f, cy - 22f, sliderX1 + 14f, cy + 22f);
        rr(c, sliderX0 - 2f, cy - 5f, sliderX1 + 2f, cy + 5f, 5f, th.trackFace, th.trackEdge, th.light ? 1.6f : 1f);
        if (on && !muted && State.level > 0f) {
            float lw = Math.max(6f, span * Math.min(1f, State.level));
            rr(c, sliderX0 - 1f, cy - 3.5f, sliderX0 - 1f + lw, cy + 3.5f, 3.5f, th.teal, 0, 0);
        }
        float cx = sliderX0 + span * (muted ? 0f : pct / 100f);
        rr(c, cx - 6f + 1.5f, cy - 11f + 2f, cx + 6f + 1.5f, cy + 11f + 2f, 3f, th.capEdge, 0, 0);
        rr(c, cx - 6f, cy - 11f, cx + 6f, cy + 11f, 3f, th.cap, th.capEdge, 2f);
        line(c, cx - 2f, cy - 6f, cx - 2f, cy + 6f, 0x66FFFFFF, 1.2f, false);
        line(c, L + 12f, t + 86f, R - 12f, t + 86f, th.edge, 1f, true);
        // scale
        float ly = t + 102f;
        int plain = th.txt2;
        if (muted) {
            chip(c, "0% [MUTED]", sliderX0 - 4f, ly, 17f, 6.8f, th.coral, th.muteTint, th.coral, Paint.Align.LEFT, 0.04f, 0);
        } else {
            text(c, "0% (MUTE)", sliderX0, ly, 6.9f, th.dim, mono, Paint.Align.LEFT, 0.02f);
        }
        text(c, "25%", sliderX0 + span * 0.25f, ly, 7.4f, plain, monoBold, Paint.Align.CENTER, 0.02f);
        text(c, "50%", sliderX0 + span * 0.50f, ly, 7.4f, plain, monoBold, Paint.Align.CENTER, 0.02f);
        text(c, "75%", sliderX0 + span * 0.75f, ly, 7.4f, plain, monoBold, Paint.Align.CENTER, 0.02f);
        if (!muted && pct == 100) {
            chip(c, "100% [MAX]", sliderX1 + 4f, ly, 17f, 6.8f, th.coral, th.muteTint, th.coral, Paint.Align.RIGHT, 0.04f, 0);
        } else {
            text(c, muted ? "100% [MAX]" : "100%", sliderX1, ly, 6.9f, th.dim, mono, Paint.Align.RIGHT, 0.02f);
        }
        // mute button
        float my0 = t + 118f, my1 = t + 156f;
        rMute.set(L + 12f, my0, R - 12f, my1);
        rr(c, L + 14f, my0 + 2.5f, R - 10f, my1 + 2.5f, 6f, th.shadow, 0, 0);
        rr(c, L + 12f, my0, R - 12f, my1, 6f, muted ? th.muteTint : th.muteBg, muted ? th.coral : th.muteEdge, th.light ? 2f : 1.5f);
        String label = muted ? "UNMUTE MICROPHONE" : "MUTE MICROPHONE";
        float lw = width(label, 10.5f, display, 0.06f);
        float gx = (L + R) / 2f - (22f + lw) / 2f;
        micIcon(c, gx + 8f, (my0 + my1) / 2f, th.coral, !muted);
        text(c, label, gx + 22f, (my0 + my1) / 2f, 10.5f, th.txt, display, Paint.Align.LEFT, 0.06f);
    }
}
