package com.uwumic;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

public class MainActivity extends Activity {
    private final Handler h = new Handler(Looper.getMainLooper());
    private SharedPreferences sp;
    private TextView info, gainLabel, err;
    private EditText port;
    private Button start, mute;
    private ProgressBar meter;
    private String ip = "";
    private int tickCount;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        sp = getSharedPreferences("pm", 0);
        State.gain = sp.getInt("gain", 100) / 100f;
        int p = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(p, p * 2, p, p);

        TextView title = new TextView(this);
        title.setText("UwU Mic");
        title.setTextSize(28);
        info = new TextView(this);
        info.setTextSize(16);
        info.setPadding(0, p / 2, 0, p / 2);

        port = new EditText(this);
        port.setHint("Port");
        port.setInputType(InputType.TYPE_CLASS_NUMBER);
        port.setText(sp.getString("port", "8125"));

        start = new Button(this);
        start.setOnClickListener(v -> toggle());

        gainLabel = new TextView(this);
        gainLabel.setText("Mic volume: " + Math.round(State.gain * 100) + "%");
        SeekBar sb = new SeekBar(this);
        sb.setMax(300);
        sb.setProgress(Math.round(State.gain * 100));
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int v, boolean user) {
                State.gain = v / 100f;
                gainLabel.setText("Mic volume: " + v + "%");
                sp.edit().putInt("gain", v).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });

        mute = new Button(this);
        mute.setText("Mute");
        mute.setOnClickListener(v -> {
            State.muted = !State.muted;
            mute.setText(State.muted ? "Unmute" : "Mute");
        });

        meter = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        meter.setMax(100);
        err = new TextView(this);
        err.setTextColor(Color.RED);

        l.addView(title);
        l.addView(info);
        l.addView(port);
        l.addView(start);
        l.addView(gainLabel);
        l.addView(sb);
        l.addView(mute);
        l.addView(meter);
        l.addView(err);
        ScrollView sv = new ScrollView(this);
        sv.addView(l);
        setContentView(sv);
        tick.run();
    }

    private final Runnable tick = new Runnable() {
        @Override public void run() {
            if (tickCount++ % 10 == 0) ip = lanIp();
            boolean on = State.running;
            start.setText(on ? "Stop" : "Start");
            port.setEnabled(!on);
            if (on) {
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                info.setText("In the PC client enter:\n\nIP:    " + (ip.isEmpty() ? "(connect to Wi-Fi)" : ip)
                        + "\nPort:  " + port.getText() + "\n\nPC connected: " + (State.clients > 0 ? "yes" : "no"));
            } else {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                info.setText("Stopped.\nPhone IP: " + (ip.isEmpty() ? "not on Wi-Fi" : ip));
            }
            meter.setProgress((int) (State.level * 100));
            err.setText(State.error);
            h.postDelayed(this, 100);
        }
    };

    private void toggle() {
        if (State.running) {
            stopService(new Intent(this, UwUMicService.class));
            return;
        }
        String pt = port.getText().toString().trim();
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
            i.putExtra("port", Integer.parseInt(port.getText().toString().trim()));
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
