package com.enerflow.screentimer;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

public class TimerActivity extends AppCompatActivity {
    Button btnStop;
    TextView textViewCurrTimer, textViewScreenTimer, textViewCount;
    com.google.android.material.progressindicator.CircularProgressIndicator progress;

    private final android.content.BroadcastReceiver usageTickReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            long elapsed = intent.getLongExtra("elapsed", 0L);
            int limit = intent.getIntExtra("limit", 1);
            updateTimerUI(elapsed, limit);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit().putString("last_screen", "TIMER").apply();

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int limit = Math.max(1, prefs.getInt("TIMER_MINUTES", 1));
        boolean screenOn = prefs.getBoolean("screen_on", true);
        long uiElapsed = prefs.getLong("ui_elapsed", 0L);
        // Paint using the snapshot persisted by the service
        updateTimerUI(screenOn ? uiElapsed : 0L, limit);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);  // your second XML

        btnStop = findViewById(R.id.btnStop);
        textViewScreenTimer = findViewById(R.id.textViewScreenTimer);
        textViewCurrTimer = findViewById(R.id.textViewCurrTimer);
        textViewCount = findViewById(R.id.textViewCount);
        progress = findViewById(R.id.progress);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        int currTimer = prefs.getInt("TIMER_MINUTES", 0);
        long elapsedTime = prefs.getLong("elapsed_time", 0);

        //textViewCurrTimer.setText(String.valueOf(elapsedTime));
        textViewScreenTimer.setText(String.valueOf(currTimer));

        updateTimerUI(elapsedTime, currTimer);

        btnStop.setOnClickListener(v -> {

            Intent intent = new Intent(this, UsageMonitorService.class);
            int minutes = 0;
            //textViewTimer.setText(" - ");
            editor.putBoolean("timer_enabled", false).apply();
            stopService(intent);
            Toast.makeText(getApplicationContext(), "Timer stopped!", Toast.LENGTH_LONG).show();

            Intent i = new Intent(TimerActivity.this, MainActivity.class);
            startActivity(i);
            finish();

        });

    }

    @Override protected void onStart() {
        super.onStart();
        registerReceiver(usageTickReceiver, new android.content.IntentFilter("com.enerflow.alertuser.USAGE_TICK"));
    }

    @Override protected void onStop() {
        super.onStop();
        try { unregisterReceiver(usageTickReceiver); } catch (Exception ignored) {}
    }


    private void updateTimerUI(long elapsedMinutes, int limitMinutes) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int count = prefs.getInt("count", 0);

        // Clamp + avoid divide by zero
        if (limitMinutes <= 0) limitMinutes = 1;

        // Update "elapsed" text — choose your preferred format ("Xm" or just number)
        textViewCurrTimer.setText(elapsedMinutes + "min");
        textViewCount.setText(String.valueOf(count));

        // Compute percent and update progress
        float pct = (elapsedMinutes * 100f) / limitMinutes;
        int progressPct = Math.max(0, Math.min(100, Math.round(pct)));

        // Make sure it's determinate and max=100
        if (progress.isIndeterminate()) progress.setIndeterminate(false);
        if (progress.getMax() != 100) progress.setMax(100);

        progress.setProgress(progressPct, true); // true = animate
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.screen_timer_menu, menu);
        return true; // show the 3-dots
    }
    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        int id = item.getItemId();
        /*if (id == R.id.action_settings) {
            // open Settings screen or dialog
            return true;
        }*/ if (id == R.id.action_about) {

            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_about_app, null);

            AlertDialog aboutAppDialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

// Transparent background so our rounded card shows properly
            if (aboutAppDialog.getWindow() != null) {
                aboutAppDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

            // Close btn
            Button btnClose = dialogView.findViewById(R.id.btnDialogClose);
            btnClose.setOnClickListener(v -> aboutAppDialog.dismiss());

            aboutAppDialog.show();
            return true;
        } else if (id == R.id.action_developer) {

            LayoutInflater inflater = LayoutInflater.from(this);
            View dialogView = inflater.inflate(R.layout.dialog_about_developer, null);

            AlertDialog dialog = new AlertDialog.Builder(this)
                    .setView(dialogView)
                    .create();

// Transparent background so our rounded card shows properly
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            }

// Close btn
            Button btnClose = dialogView.findViewById(R.id.btnDialogClose);
            btnClose.setOnClickListener(v -> dialog.dismiss());

// Contact actions
            LinearLayout emailChip = dialogView.findViewById(R.id.btnEmail);
            LinearLayout linkedinChip = dialogView.findViewById(R.id.btnLinkedIn);

// open email app
            emailChip.setOnClickListener(v -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:mmvinaykumar.mm@gmail.com"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Screen Timer feedback");
                startActivity(Intent.createChooser(emailIntent, "Send email"));
            });

// open LinkedIn profile
            linkedinChip.setOnClickListener(v -> {
                Intent browserIntent = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://www.linkedin.com/in/vinaykumar-mysuru-manjunath-33b522ba/")
                );
                startActivity(browserIntent);
            });

            dialog.show();

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
