package com.uwumic;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.InputFilter;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.Locale;

public class MainActivity extends Activity {
    // palette
    private static final int BG = 0xFF07090F, PANEL = 0xFF0D121B, FIELD = 0xFF080B11;
    private static final int EDGE = 0xFF1A2433, EDGE2 = 0xFF263244, TEAL_EDGE = 0xFF0F3D48;
    private static final int CYAN = 0xFF00E5FF, RED = 0xFFFF0A45, GREEN = 0xFF2EE6A6, AMBER = 0xFFF5B544;
    private static final int INK = 0xFFF2F6FA, SOFT = 0xFFC9D1DC, DIM = 0xFF8A94A6, DIMMER = 0xFF5B6678;

    private final Handler h = new Handler(Looper.getMainLooper());
    private SharedPreferences sp;
    private Typeface tfDisplay, tfMono, tfMonoBold;

    // status panel
    private TextView statusText, modeLabel, topMode, formatPill, standby, timer;
    private View dot, divider;
    // address panels
    private TextView ipText, ipRight, portRight;
    private ImageView lockIcon;
    private View portDot;
    private LinearLayout ipField, portField;
    private EditText portEdit;
    // actions
    private TextView action, errText, muteBtn;
    // gain
    private ImageView gainIcon;
    private TextView gainRight, gainMuted, lblLeft, lblMid, lblRight;
    private LevelSlider slider;

    private String ip = "";
    private int tickCount;
    private float levelShown;
    private Boolean lastOn, lastMuted;
    private String lastGainText = "";
    private boolean keepOn;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("pm", 0);
        State.gain = Math.min(100, sp.getInt("gain", 100)) / 100f;
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        tfDisplay = Fonts.display(this);
        tfMono = Fonts.mono(this);
        tfMonoBold = Fonts.monoBold(this);
        buildUi();
        tick.run();
    }

    // ------------------------------------------------------------------ UI construction
    private int dp(float v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    private static int alpha(int color, int a) { return (a << 24) | (color & 0xFFFFFF); }

    private TextView text(String s, float sp, int color, Typeface tf, float spacing) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(tf);
        t.setLetterSpacing(spacing);
        return t;
    }

    private GradientDrawable box(int fill, int stroke, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(Math.max(1, dp(1)), stroke);
        return g;
    }

    private LinearLayout panel(int stroke) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setBackground(box(PANEL, stroke, 3));
        p.setPadding(dp(14), dp(10), dp(14), dp(12));
        return p;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    /** Flexible empty space: shares whatever height is left over, so the screen fills without scrolling. */
    private void gap(LinearLayout parent) {
        parent.addView(new View(this), new LinearLayout.LayoutParams(1, 0, 1f));
    }

    private View spacer() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        return v;
    }

    private LinearLayout.LayoutParams params(int w, int hgt, int left, int top, int right, int bottom) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, hgt);
        p.setMargins(dp(left), dp(top), dp(right), dp(bottom));
        return p;
    }

    private void add(LinearLayout parent, View v, int topDp) {
        parent.addView(v, params(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 0, topDp, 0, 0));
    }

    private ImageView icon(int res, int color, int sizeDp) {
        ImageView i = new ImageView(this);
        setIcon(i, res, color);
        i.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return i;
    }

    private void setIcon(ImageView i, int res, int color) {
        try {
            i.setImageResource(res);
            i.setColorFilter(color);
        } catch (Exception ignored) { }
    }

    private TextView pill(String s, int color) {
        TextView t = text(s, 10, color, tfMonoBold, 0.1f);
        t.setPadding(dp(9), dp(4), dp(9), dp(4));
        t.setBackground(box(alpha(color, 0x1F), alpha(color, 0x99), 3));
        return t;
    }

    private View dotView(int sizeDp, int color) {
        View v = new View(this);
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        v.setBackground(g);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        return v;
    }

    private void buildUi() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(20), dp(16), dp(20), dp(16));

        // title
        TextView title = text("", 26, INK, tfDisplay, 0.04f);
        SpannableStringBuilder t = new SpannableStringBuilder("UwU Mic");
        t.setSpan(new ForegroundColorSpan(CYAN), 4, 7, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        title.setText(t);
        c.addView(title);
        c.addView(text("Wireless Audio Streamer", 12, DIM, tfMono, 0.06f), params(-2, -2, 0, 4, 0, 0));
        gap(c);

        // ---- stream status
        LinearLayout status = panel(TEAL_EDGE);
        add(c, status, 10);
        LinearLayout a = row();
        a.addView(text("STREAM STATUS", 11, DIM, tfMono, 0.12f));
        a.addView(spacer());
        modeLabel = text("MODE", 11, DIMMER, tfMono, 0.12f);
        a.addView(modeLabel);
        topMode = pill("TRANSMITTING", GREEN);
        a.addView(topMode, params(-2, -2, 8, 0, 0, 0));
        status.addView(a);
        formatPill = pill("", GREEN);
        status.addView(formatPill, params(-2, -2, 0, 6, 0, 0));
        divider = new View(this);
        divider.setBackgroundColor(0xFF1B2633);
        status.addView(divider, params(-1, 1, 0, 8, 0, 0));
        LinearLayout cr = row();
        dot = dotView(10, RED);
        statusText = text("STOPPED", 20, RED, tfDisplay, 0.06f);
        cr.addView(dot);
        cr.addView(statusText, params(-2, -2, 10, 0, 0, 0));
        cr.addView(spacer());
        standby = pill("STANDBY", CYAN);
        cr.addView(standby);
        timer = text("", 13, DIM, tfMono, 0.04f);
        cr.addView(timer);
        status.addView(cr, params(-1, -2, 0, 8, 0, 0));

        // ---- host ip
        LinearLayout ipPanel = panel(EDGE);
        add(c, ipPanel, 8);
        LinearLayout ih = row();
        ih.addView(icon(R.drawable.ic_network, CYAN, 16));
        ih.addView(text("HOST IP ADDRESS", 11, SOFT, tfMonoBold, 0.12f), params(-2, -2, 8, 0, 0, 0));
        ih.addView(spacer());
        lockIcon = icon(R.drawable.ic_lock, CYAN, 12);
        ih.addView(lockIcon, params(dp(12), dp(12), 0, 0, 4, 0));
        ipRight = text("IPV4", 10, DIMMER, tfMono, 0.1f);
        ih.addView(ipRight);
        ipPanel.addView(ih);
        ipField = new LinearLayout(this);
        ipField.setGravity(Gravity.CENTER);
        ipField.setPadding(dp(8), dp(8), dp(8), dp(8));
        ipText = text("", 22, INK, tfMonoBold, 0.06f);
        ipField.addView(ipText);
        ipPanel.addView(ipField, params(-1, -2, 0, 8, 0, 0));

        // ---- target port
        LinearLayout portPanel = panel(EDGE);
        add(c, portPanel, 8);
        LinearLayout ph = row();
        ph.addView(icon(R.drawable.ic_antenna, CYAN, 16));
        ph.addView(text("TARGET PORT", 11, SOFT, tfMonoBold, 0.12f), params(-2, -2, 8, 0, 0, 0));
        ph.addView(spacer());
        portDot = dotView(7, GREEN);
        ph.addView(portDot, params(dp(7), dp(7), 0, 0, 6, 0));
        portRight = text("TCP", 10, DIMMER, tfMono, 0.1f);
        ph.addView(portRight);
        portPanel.addView(ph);
        portField = new LinearLayout(this);
        portField.setGravity(Gravity.CENTER);
        portEdit = new EditText(this);
        portEdit.setBackground(null);
        portEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        portEdit.setGravity(Gravity.CENTER);
        portEdit.setSingleLine(true);
        portEdit.setTextColor(INK);
        portEdit.setTypeface(tfMonoBold);
        portEdit.setTextSize(22);
        portEdit.setLetterSpacing(0.06f);
        portEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        portEdit.setText(sp.getString("port", "8125"));
        portEdit.setPadding(dp(8), dp(6), dp(8), dp(6));
        portField.addView(portEdit, new LinearLayout.LayoutParams(-1, -2));
        portPanel.addView(portField, params(-1, -2, 0, 8, 0, 0));

        // ---- start / stop
        action = new TextView(this);
        action.setGravity(Gravity.CENTER);
        action.setTypeface(tfDisplay);
        action.setTextSize(15);
        action.setLetterSpacing(0.08f);
        action.setPadding(dp(16), dp(14), dp(16), dp(14));
        action.setCompoundDrawablePadding(dp(10));
        action.setOnClickListener(v -> toggle());
        gap(c);
        add(c, action, 12);
        errText = text("", 12, RED, tfMono, 0.04f);
        errText.setVisibility(View.GONE);
        add(c, errText, 6);

        // ---- mic gain
        LinearLayout gain = panel(EDGE);
        gap(c);
        add(c, gain, 12);
        LinearLayout gh = row();
        gainIcon = icon(R.drawable.ic_mic, CYAN, 16);
        gh.addView(gainIcon);
        gh.addView(text("MIC GAIN", 11, SOFT, tfMonoBold, 0.12f), params(-2, -2, 8, 0, 0, 0));
        gh.addView(spacer());
        gainRight = text("", 13, DIM, tfMono, 0.04f);
        gh.addView(gainRight);
        gainMuted = pill("MUTED (0%)", RED);
        gh.addView(gainMuted);
        gain.addView(gh);
        slider = new LevelSlider(this);
        slider.setPercent(Math.round(State.gain * 100));
        slider.setListener(v -> {
            State.gain = v / 100f;
            if (v > 0) State.muted = false;
            sp.edit().putInt("gain", v).apply();
        });
        gain.addView(slider, params(-1, -2, 0, 2, 0, 0));
        LinearLayout labels = row();
        lblLeft = text("", 10, DIMMER, tfMono, 0.04f);
        lblMid = text("", 10, CYAN, tfMonoBold, 0.04f);
        lblRight = text("", 10, DIMMER, tfMono, 0.04f);
        lblLeft.setGravity(Gravity.START);
        lblMid.setGravity(Gravity.CENTER);
        lblRight.setGravity(Gravity.END);
        labels.addView(lblLeft, new LinearLayout.LayoutParams(0, -2, 1f));
        labels.addView(lblMid, new LinearLayout.LayoutParams(0, -2, 1f));
        labels.addView(lblRight, new LinearLayout.LayoutParams(0, -2, 1f));
        gain.addView(labels, params(-1, -2, 0, 0, 0, 0));

        // ---- mute
        muteBtn = new TextView(this);
        muteBtn.setGravity(Gravity.CENTER);
        muteBtn.setTypeface(tfMonoBold);
        muteBtn.setTextSize(13);
        muteBtn.setLetterSpacing(0.08f);
        muteBtn.setPadding(dp(16), dp(12), dp(16), dp(12));
        muteBtn.setCompoundDrawablePadding(dp(10));
        muteBtn.setOnClickListener(v -> State.muted = !State.muted);
        add(c, muteBtn, 10);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        sv.setBackground(new GridBackground(getResources().getDisplayMetrics().density));
        sv.addView(c);
        setContentView(sv);
    }

    // ------------------------------------------------------------------ live updates
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (tickCount++ % 20 == 0) ip = lanIp();
            render();
            h.postDelayed(this, 50);
        }
    };

    private void setTxt(TextView t, String s) {
        if (!s.contentEquals(t.getText())) t.setText(s);
    }

    private void vis(View v, boolean show) {
        int want = show ? View.VISIBLE : View.GONE;
        if (v.getVisibility() != want) v.setVisibility(want);
    }

    private void iconLeft(TextView t, int res, int color) {
        try {
            Drawable d = getResources().getDrawable(res, getTheme()).mutate();
            d.setTint(color);
            t.setCompoundDrawablesWithIntrinsicBounds(d, null, null, null);
        } catch (Exception ignored) { }
    }

    private void render() {
        final boolean on = State.running, muted = State.muted, conn = State.clients > 0;
        final int pct = Math.round(State.gain * 100), shownPct = muted ? 0 : pct;

        // ---- styles that only change with state
        if (lastOn == null || lastOn != on) {
            lastOn = on;
            int accent = on ? CYAN : RED;
            statusText.setTextColor(accent);
            ((GradientDrawable) dot.getBackground()).setColor(accent);
            ipField.setBackground(box(FIELD, on ? alpha(CYAN, 0x66) : EDGE2, 3));
            portField.setBackground(box(FIELD, EDGE2, 3));
            ipText.setTextColor(on ? CYAN : INK);
            portEdit.setTextColor(on ? SOFT : INK);
            portEdit.setEnabled(!on);
            action.setBackground(box(on ? RED : CYAN, on ? RED : CYAN, 2));
            action.setTextColor(on ? 0xFFFFFFFF : 0xFF04121A);
            iconLeft(action, on ? R.drawable.ic_stop : R.drawable.ic_play, on ? 0xFFFFFFFF : 0xFF04121A);
            setTxt(action, on ? "STOP BROADCAST" : "START BROADCAST");
            vis(topMode, on);
            vis(formatPill, on);
            vis(divider, on);
            vis(timer, on);
            vis(standby, !on);
            vis(lockIcon, on);
            vis(portDot, on);
            setTxt(modeLabel, on ? "MODE:" : "MODE");
            setTxt(ipRight, on ? "LOCKED \u2022 IPV4" : "IPV4");
            ipRight.setTextColor(on ? CYAN : DIMMER);
            setTxt(statusText, on ? "BROADCASTING" : "STOPPED");
            if (on) setTxt(formatPill, (State.RATE / 1000) + " kHz / " + (State.RATE * 16 / 1000) + " kbps");
            if (on && !keepOn) { getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); keepOn = true; }
            if (!on && keepOn) { getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); keepOn = false; }
            lastGainText = "";
        }
        if (lastMuted == null || lastMuted != muted) {
            lastMuted = muted;
            setIcon(gainIcon, muted ? R.drawable.ic_mic_off : R.drawable.ic_mic, muted ? RED : CYAN);
            vis(gainMuted, muted);
            muteBtn.setTextColor(muted ? RED : DIM);
            muteBtn.setBackground(muted ? box(alpha(RED, 0x22), alpha(RED, 0x99), 3) : box(PANEL, EDGE2, 3));
            iconLeft(muteBtn, R.drawable.ic_mic_off, muted ? RED : DIM);
            setTxt(muteBtn, muted ? "MUTED (TAP TO UNMUTE)" : "MUTE MICROPHONE");
            slider.setMuted(muted);
            lastGainText = "";
        }

        // ---- ip / port
        if (ip.isEmpty()) {
            setTxt(ipText, "NO WI-FI");
        } else {
            setTxt(ipText, ip);
        }
        if (on) {
            setTxt(portRight, conn ? "CONNECTED" : "WAITING FOR PC");
            portRight.setTextColor(conn ? GREEN : AMBER);
            ((GradientDrawable) portDot.getBackground()).setColor(conn ? GREEN : AMBER);
            long s = Math.max(0, (SystemClock.elapsedRealtime() - State.startedAt) / 1000);
            setTxt(timer, String.format(Locale.US, "(%02d:%02d:%02d)", s / 3600, (s / 60) % 60, s % 60));
        } else {
            setTxt(portRight, "TCP");
            portRight.setTextColor(DIMMER);
        }

        // ---- error line
        setTxt(errText, State.error);
        vis(errText, !State.error.isEmpty());

        // ---- gain
        String gainKey = on + "|" + muted + "|" + pct;
        if (!gainKey.equals(lastGainText)) {
            lastGainText = gainKey;
            if (muted) {
                gainRight.setVisibility(View.GONE);
            } else {
                gainRight.setVisibility(View.VISIBLE);
                SpannableStringBuilder sb = new SpannableStringBuilder();
                if (on) {
                    sb.append(pct + "%");
                    sb.setSpan(new ForegroundColorSpan(CYAN), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new StyleSpan(Typeface.BOLD), 0, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    sb.append("OUTPUT: ");
                    int st = sb.length();
                    sb.append(pct + "%");
                    sb.setSpan(new ForegroundColorSpan(CYAN), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.setSpan(new StyleSpan(Typeface.BOLD), st, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    sb.append(pct == 0 ? "  (-inf dB)" : pct == 100 ? "  (0 dB)"
                            : String.format(Locale.US, "  (%.1f dB)", 20 * Math.log10(pct / 100.0)));
                }
                gainRight.setText(sb);
            }
            boolean detail = !on && !muted;
            setTxt(lblLeft, detail ? "0% (MUTE)" : "0%");
            setTxt(lblRight, detail ? "100% (0 dB)" : "100%");
            String word = pct < 25 ? "LOW" : pct > 90 ? "MAX" : "NOMINAL";
            setTxt(lblMid, detail ? pct + "% [" + word + "]" : "");
        }
        slider.setPercent(shownPct);
        levelShown = Math.max(on && !muted ? State.level : 0f, levelShown * 0.8f);
        if (levelShown < 0.01f) levelShown = 0f;
        slider.setLevel(levelShown);
    }

    // ------------------------------------------------------------------ start / stop
    private void toggle() {
        if (State.running) {
            stopService(new Intent(this, UwUMicService.class));
            return;
        }
        String pt = portEdit.getText().toString().trim();
        if (pt.isEmpty()) return;
        sp.edit().putString("port", pt).apply();
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            String[] perms = Build.VERSION.SDK_INT >= 33
                    ? new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.POST_NOTIFICATIONS}
                    : new String[]{Manifest.permission.RECORD_AUDIO};
            requestPermissions(perms, 1);
        } else {
            startSvc();
        }
    }

    private void startSvc() {
        try {
            Intent i = new Intent(this, UwUMicService.class);
            i.putExtra("port", Integer.parseInt(portEdit.getText().toString().trim()));
            if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        } catch (Exception e) {
            State.error = "Start failed: " + e.getMessage();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startSvc();
        } else {
            State.error = "Microphone permission is required.";
        }
    }

    private static String lanIp() {
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback()) continue;
                for (InetAddress a : Collections.list(ni.getInetAddresses())) {
                    if (a instanceof Inet4Address && a.isSiteLocalAddress()) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) { }
        return "";
    }
}
