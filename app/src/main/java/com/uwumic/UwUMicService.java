package com.uwumic;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;



/** Runs the HTTPS/WebSocket server and captures the microphone, all on the phone. */
public class UwUMicService extends Service {
    private TcpServer server;
    private volatile boolean running;
    private Thread recThread;
    private PowerManager.WakeLock wakeLock;
    private WifiManager.WifiLock wifiLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (running) return START_STICKY;
        showNotification();
        int port = intent != null ? intent.getIntExtra("port", 8125) : 8125;
        try {
            server = new TcpServer(port);
            server.start();
        } catch (Exception e) {
            State.error = "Cannot start on port " + port + ": " + e.getMessage();
            stopForeground(true);
            stopSelf();
            return START_NOT_STICKY;
        }
        State.error = "";
        running = true;
        State.running = true;
        acquireLocks();
        recThread = new Thread(this::recordLoop, "mic");
        recThread.start();
        return START_STICKY;
    }

    private void recordLoop() {
        AudioRecord ar = null;
        try {
            int min = AudioRecord.getMinBufferSize(State.RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            ar = new AudioRecord(MediaRecorder.AudioSource.MIC, State.RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 16384));
            ar.startRecording();
            short[] s = new short[960]; // 20 ms
            while (running) {
                int n = ar.read(s, 0, s.length);
                if (n <= 0) continue;
                float g = State.muted ? 0f : State.gain;
                byte[] o = new byte[n * 2];
                int peak = 0;
                for (int i = 0; i < n; i++) {
                    int v = Math.round(s[i] * g);
                    if (v > 32767) v = 32767; else if (v < -32768) v = -32768;
                    o[2 * i] = (byte) v;
                    o[2 * i + 1] = (byte) (v >> 8);
                    peak = Math.max(peak, Math.abs(v));
                }
                State.level = peak / 32768f;
                if (State.clients > 0 && server != null) server.broadcast(o);
            }
        } catch (Exception e) {
            State.error = "Mic error: " + e.getMessage();
        } finally {
            if (ar != null) {
                try { ar.stop(); } catch (Exception ignored) { }
                ar.release();
            }
        }
    }

    private void showNotification() {
        String ch = "uwumic";
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationManager nm = getSystemService(NotificationManager.class);
            nm.createNotificationChannel(new NotificationChannel(ch, "UwU Mic", NotificationManager.IMPORTANCE_LOW));
        }
        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(this, ch) : new Notification.Builder(this);
        Notification n = b.setContentTitle("UwU Mic is streaming")
                .setSmallIcon(android.R.drawable.ic_btn_speak_now).build();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE);
        } else {
            startForeground(1, n);
        }
    }

    private void acquireLocks() {
        try {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "uwumic:mic");
            wakeLock.acquire();
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
            wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "uwumic");
            wifiLock.acquire();
        } catch (Exception ignored) { }
    }

    @Override
    public void onDestroy() {
        running = false;
        State.running = false;
        State.level = 0;
        State.clients = 0;
        if (server != null) server.stop();
        try { if (wakeLock != null && wakeLock.isHeld()) wakeLock.release(); } catch (Exception ignored) { }
        try { if (wifiLock != null && wifiLock.isHeld()) wifiLock.release(); } catch (Exception ignored) { }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
