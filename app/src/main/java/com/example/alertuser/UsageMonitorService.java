package com.example.alertuser;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class UsageMonitorService extends Service {

    private Handler handler;
    private Runnable usageCheckRunnable;
    private long screenOnTime = 0;
    private boolean isScreenOn = true;
    private int timerLimit;
    private long startTime;
    private int exceedCount = 0;

    private BroadcastReceiver screenReceiver;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        timerLimit = prefs.getInt("TIMER_MINUTES", 15);  // default to 15 if not set
        boolean isTimerEnabled = prefs.getBoolean("timer_enabled", true); // default is true
        //timerLimit = intent.getIntExtra("TIMER_MINUTES", 1);

        if (!isTimerEnabled) {
            stopForeground(true); // remove notification
            stopSelf(); // stop service
            return START_NOT_STICKY; // Don't proceed with time checking
        }

        handler = new Handler();

        startTimer();
        registerScreenReceiver();

        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, "UsageMonitorChannel")
                .setContentTitle("Monitoring Usage")
                .setContentText("App is running in background")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .build();
        startForeground(1, notification);

        return START_STICKY;
    }

    private void startTimer() {
        usageCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isScreenOn) {
                    screenOnTime += 1000;

                    if (screenOnTime >= (timerLimit * 60 * 1000)) {
                        // Reset for next cycle
                        screenOnTime = 0;

                        // Show overlay popup
                        Intent overlayIntent = new Intent(UsageMonitorService.this, OverlayService.class);
                        overlayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startService(overlayIntent);
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(usageCheckRunnable);
    }

    private void registerScreenReceiver() {
        screenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                    isScreenOn = false;
                } else if (Intent.ACTION_SCREEN_ON.equals(intent.getAction())) {
                    isScreenOn = true;
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_SCREEN_ON);
        registerReceiver(screenReceiver, filter);
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

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
