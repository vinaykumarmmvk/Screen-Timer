package com.enerflow.screentimer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class UsageMonitorService extends Service {

    private Handler handler;
    private Runnable usageCheckRunnable;
    private boolean isScreenOn = true;
    private long screenOnTime = 0, lastBroadcastMinute = -1;
    private int timerLimit;
    private long startTime;
    private int exceedCount = 0;

    private BroadcastReceiver screenReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        timerLimit = prefs.getInt("TIMER_MINUTES", 15);  // default to 15 if not set
        boolean isTimerEnabled = prefs.getBoolean("timer_enabled", true); // default is true

        createNotificationChannel();
        //timerLimit = intent.getIntExtra("TIMER_MINUTES", 1);

        Log.d("OverlayService", "Timer limit set to: " + timerLimit + " minutes");

        if (!isTimerEnabled) {
            stopForeground(true); // remove notification
            stopSelf(); // stop service
            return START_NOT_STICKY; // Don't proceed with time checking
        }

        handler = new Handler();

        startTimer();
        registerScreenReceiver();

        Notification notification = new NotificationCompat.Builder(this, "UsageMonitorChannel")
                .setContentTitle("Monitoring Screen Usage")
                .setContentText("Screen timer app is running in background")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                    1, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            );
        } else {
            startForeground(1, notification);
        }

        return START_STICKY;
    }

    private void startTimer() {
        if (handler != null && usageCheckRunnable != null) {
            handler.removeCallbacks(usageCheckRunnable); // Remove previous one
            usageCheckRunnable = null;
        }

        handler = new Handler();
        startTime = System.currentTimeMillis();

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        usageCheckRunnable = new Runnable() {
            @Override
            public void run() {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();

                if (!isScreenOn) {
                    // While OFF, always enforce zero in storage + UI
                    editor.putLong("elapsed_time", 0L).putLong("ui_elapsed", 0L).apply();
                    sendUiTick(0L, timerLimit);
                    handler.postDelayed(this, 1000);
                    return;
                }

                // Screen is ON → accumulate
                long elapsedTime = System.currentTimeMillis() - startTime;
                long elapsedMinutes = elapsedTime / (1000 * 60);

                Log.d("OverlayService", "Elapsed time: " + elapsedMinutes + " minutes (Limit: " + timerLimit + ")");

                editor.putLong("elapsed_time", elapsedMinutes).apply();

                // Broadcast once per minute (throttled)
                if (elapsedMinutes != lastBroadcastMinute) {
                    lastBroadcastMinute = elapsedMinutes;
                    editor.putLong("ui_elapsed", elapsedMinutes).apply();
                    sendUiTick(elapsedMinutes, timerLimit);
                }

                if (elapsedMinutes >= timerLimit) {
                    // Show overlay popup
                    Intent overlayIntent = new Intent(UsageMonitorService.this, OverlayService.class);
                    overlayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startService(overlayIntent);

                    // Reset timer for the next cycle
                    startTime = System.currentTimeMillis();
                    lastBroadcastMinute = -1;
                    editor.putLong("elapsed_time", 0L).putLong("ui_elapsed", 0L).apply();
                    sendUiTick(0L, timerLimit);
                }

                handler.postDelayed(this, 1000);
            }
        };

        handler.post(usageCheckRunnable);
    }

    private void registerScreenReceiver() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context ctx, Intent intent) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                SharedPreferences.Editor ed = prefs.edit();
                String a = intent.getAction();

                if (Intent.ACTION_SCREEN_OFF.equals(a)) {
                    isScreenOn = false;

                    // Reset timer state
                    startTime = System.currentTimeMillis();   // so next ON starts fresh
                    lastBroadcastMinute = -1;                 // reset throttle
                    ed.putBoolean("screen_on", false);
                    ed.putLong("elapsed_time", 0L);           // true counter reset
                    ed.putLong("ui_elapsed", 0L);             // UI snapshot = 0
                    ed.apply();

                    // Push 0 to the Activity immediately
                    sendUiTick(0L, prefs.getInt("TIMER_MINUTES", 1));

                } else if (Intent.ACTION_SCREEN_ON.equals(a)) {
                    isScreenOn = true;

                    // Start fresh from 0 on every screen-on
                    startTime = System.currentTimeMillis();
                    lastBroadcastMinute = -1;
                    ed.putBoolean("screen_on", true);
                    ed.putLong("elapsed_time", 0L);
                    ed.putLong("ui_elapsed", 0L);
                    ed.apply();

                    // Let UI know we’re starting from 0
                    sendUiTick(0L, prefs.getInt("TIMER_MINUTES", 1));
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, filter);
        }

    }

    private void sendUiTick(long elapsedMinutes, int limitMinutes) {
        Intent tick = new Intent("com.enerflow.alertuser.USAGE_TICK");
        tick.setPackage(getPackageName());
        tick.putExtra("elapsed", elapsedMinutes);
        tick.putExtra("limit", limitMinutes);
        sendBroadcast(tick);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    "UsageMonitorChannel",
                    "Usage Monitor Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Clean up the broadcast receiver
        if (screenReceiver != null) {
            try {
                unregisterReceiver(screenReceiver);
            } catch (IllegalArgumentException e) {
                Log.w("UsageMonitorService", "Receiver not registered: " + e.getMessage());
            }
            screenReceiver = null;
        }

        // Also stop the handler callback
        if (handler != null && usageCheckRunnable != null) {
            handler.removeCallbacks(usageCheckRunnable);
            usageCheckRunnable = null;
        }

        Log.d("UsageMonitorService", "Service destroyed and receiver unregistered");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
