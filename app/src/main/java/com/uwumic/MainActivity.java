package com.uwumic;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputFilter;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class MainActivity extends Activity implements RackView.Listener, RackView.PortListener {
    private final Handler h = new Handler(Looper.getMainLooper());
    private SharedPreferences sp;
    private FrameLayout root;
    private RackView rack;
    private EditText portEdit;
    private String themeName = "dark";
    private String lastError = "";
    private boolean lastRunning;
    private boolean keepOn;
    private int tickCount;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("pm", 0);
        State.gain = Math.min(100, sp.getInt("gain", 100)) / 100f;
        themeName = "light".equals(sp.getString("theme", "dark")) ? "light" : "dark";

        root = new FrameLayout(this);
        rack = new RackView(this, this, themeName);
        root.addView(rack, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // the port number is a real text box, laid exactly over the LCD that RackView draws
        portEdit = new EditText(this);
        portEdit.setBackground(null);
        portEdit.setPadding(0, 0, 0, 0);
        portEdit.setSingleLine(true);
        portEdit.setIncludeFontPadding(false);
        portEdit.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        portEdit.setInputType(InputType.TYPE_CLASS_NUMBER);
        portEdit.setImeOptions(EditorInfo.IME_ACTION_DONE);
        portEdit.setFilters(new InputFilter[]{new InputFilter.LengthFilter(5)});
        portEdit.setTypeface(Fonts.monoBold(this));
        portEdit.setLetterSpacing(0.1f);
        portEdit.setText(sp.getString("port", "8125"));
        root.addView(portEdit, new FrameLayout.LayoutParams(1, 1));

        setContentView(root);
        rack.setPortListener(this);
        applyTheme();
        tick.run();
    }

    // ------------------------------------------------------------------ look
    private void applyTheme() {
        Theme t = Theme.byName(themeName);
        getWindow().setStatusBarColor(t.bg);
        getWindow().setNavigationBarColor(t.bg);
        root.setBackgroundColor(t.bg);
        root.setSystemUiVisibility(t.light ? View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR : 0);
        styleEdit();
    }

    private void styleEdit() {
        Theme t = Theme.byName(themeName);
        int col = State.running ? t.lcdLive : t.lcdIdle;
        portEdit.setTextColor(col);
        portEdit.setHighlightColor(0x5500C9B7);
        portEdit.setEnabled(!State.running);
        if (t.light) portEdit.setShadowLayer(0f, 0f, 0f, 0);
        else portEdit.setShadowLayer(8f, 0f, 0f, col);
    }

    @Override
    public void onPortRect(int l, int t, int r, int b, float u) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(Math.max(1, r - l), Math.max(1, b - t));
        lp.leftMargin = l;
        lp.topMargin = t;
        portEdit.setLayoutParams(lp);
        portEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX, 21.5f * u);
    }

    // ------------------------------------------------------------------ live updates
    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (tickCount++ % 20 == 0) rack.setIp(lanIp());
            if (State.running != lastRunning) {
                lastRunning = State.running;
                styleEdit();
            }
            if (State.running && !keepOn) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                keepOn = true;
            } else if (!State.running && keepOn) {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                keepOn = false;
            }
            String err = State.error == null ? "" : State.error;
            if (!err.isEmpty() && !err.equals(lastError)) {
                Toast.makeText(MainActivity.this, err, Toast.LENGTH_LONG).show();
            }
            lastError = err;
            rack.invalidate();
            h.postDelayed(this, 50);
        }
    };

    // ------------------------------------------------------------------ taps from the screen
    @Override
    public void onToggle() {
        toggle();
    }

    @Override
    public void onMute() {
        State.muted = !State.muted;
    }

    @Override
    public void onGain(int percent) {
        State.gain = percent / 100f;
        if (percent > 0) State.muted = false;
        sp.edit().putInt("gain", percent).apply();
    }

    @Override
    public void onTheme(String name) {
        themeName = "light".equals(name) ? "light" : "dark";
        sp.edit().putString("theme", themeName).apply();
        rack.setThemeName(themeName);
        applyTheme();
    }

    // ------------------------------------------------------------------ start / stop
    private void toggle() {
        if (State.running) {
            stopService(new Intent(this, UwUMicService.class));
            return;
        }
        String pt = portEdit.getText().toString().trim();
        if (pt.isEmpty()) {
            State.error = "Enter a port number first.";
            return;
        }
        sp.edit().putString("port", pt).apply();
        portEdit.clearFocus();
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
