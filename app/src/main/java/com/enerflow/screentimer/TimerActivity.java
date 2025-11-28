package com.enerflow.screentimer;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
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
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.jetbrains.annotations.NotNull;

public class TimerActivity extends AppCompatActivity {
    Button btnStop;
    TextView textViewCurrTimer, textViewScreenTimer, textViewCount;
    com.google.android.material.progressindicator.CircularProgressIndicator progress;

    private final android.content.BroadcastReceiver usageTickReceiver = new android.content.BroadcastReceiver() {
        @Override public void onReceive(android.content.Context context, android.content.Intent intent) {
            long elapsedSec = intent.getLongExtra("elapsedSec", 0L);
            int limit = intent.getIntExtra("limit", 1);
            updateTimerUI(elapsedSec, limit);
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
        long uiElapsed = prefs.getLong("ui_elapsed_sec", 0L);
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

        View root = findViewById(R.id.root);                  // outermost container
        View content = findViewById(R.id.contentContainer);   // the block to nudge down

        ViewCompat.setOnApplyWindowInsetsListener(root, (v, insets) -> {
            int topBars = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top;
            int extra = getResources().getDimensionPixelSize(R.dimen.top_extra_gap);
            // Add real status-bar height + your extra gap
            content.setPaddingRelative(
                    content.getPaddingStart(),
                    topBars + extra,
                    content.getPaddingEnd(),
                    content.getPaddingBottom()
            );
            return insets; // don't consume; just apply padding
        });

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
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(usageTickReceiver, new android.content.IntentFilter("com.enerflow.alertuser.USAGE_TICK") , Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usageTickReceiver, new android.content.IntentFilter("com.enerflow.alertuser.USAGE_TICK"));
        }

    }

    @Override protected void onStop() {
        super.onStop();
        try { unregisterReceiver(usageTickReceiver); } catch (Exception ignored) {}
    }


    private void updateTimerUI(long elapsedSec, int limitMinutes) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int count = prefs.getInt("count", 0);

        if (limitMinutes <= 0) limitMinutes = 1;

        // 1) Show mm:ss (or hh:mm:ss if long)
        textViewCurrTimer.setText(formatElapsed(elapsedSec));
        textViewCount.setText(String.valueOf(count));

        // 2) Smooth progress using seconds
        float totalSec = limitMinutes * 60f;
        int pct = Math.max(0, Math.min(100, Math.round((elapsedSec / totalSec) * 100f)));

        if (progress.isIndeterminate()) progress.setIndeterminate(false);
        if (progress.getMax() != 100) progress.setMax(100);
        progress.setProgress(pct, true);
    }

    private String formatElapsed(long sec) {
        long h = sec / 3600;
        long m = (sec % 3600) / 60;
        long s = sec % 60;
        if (h > 0) return String.format(java.util.Locale.getDefault(), "%d:%02d:%02d", h, m, s);
        return String.format(java.util.Locale.getDefault(), "%d:%02d", m, s);
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
