package com.example.alertuser;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {
    EditText timerEditText, etNote;
    Button btnStart;
    TextView aboutDeveloper, btnMinus, btnPlus;
    CheckBox cbAddNote;
    ImageButton btnInfo;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.screen_timer_menu, menu);
        return true; // show the 3-dots
    }

    @Override
    protected void onResume() {
        super.onResume();
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit().putString("last_screen", "MAIN").apply();
    }

    @Override
    public boolean onOptionsItemSelected(@NotNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // TODO: open Settings screen or dialog
            return true;
        } else if (id == R.id.action_about) {

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
        } else if (id == R.id.action_help) {
            // TODO: show Help
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Only auto-route when opened from launcher (prevents loops when coming from notifications, etc.)
        boolean launchedFromLauncher =
                Intent.ACTION_MAIN.equals(getIntent().getAction()) &&
                        getIntent().hasCategory(Intent.CATEGORY_LAUNCHER);

        if (launchedFromLauncher) {
            String last = getSharedPreferences("app_prefs", MODE_PRIVATE)
                    .getString("last_screen", "MAIN");

            if ("TIMER".equals(last)) {
                startActivity(new Intent(this, TimerActivity.class));
                finish(); // so back won’t return here
                return;
            }
        }

        timerEditText = findViewById(R.id.editTextTimer);
        btnStart = findViewById(R.id.btnStart);
        //textViewTimer = findViewById(R.id.textViewTimer);
        aboutDeveloper = findViewById(R.id.aboutDeveloper);

        cbAddNote = findViewById(R.id.cbAddNote);
        etNote = findViewById(R.id.etNote);
        btnInfo = findViewById(R.id.btnInfo);

        btnMinus = findViewById(R.id.btnMinus);
        btnPlus = findViewById(R.id.btnPlus);

// init minus button state based on current value
        updateMinusState(parseTimer());

// + increments
        btnPlus.setOnClickListener(v -> {
            int val = parseTimer();
            if (val < 999) {              // optional upper cap
                setTimer(val + 1);
            }
        });

// − decrements (floor = 1)
        btnMinus.setOnClickListener(v -> {
            int val = parseTimer();
            if (val > 1) {
                setTimer(val - 1);
            }
        });

// If user types manually, keep the minus button state in sync
        timerEditText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) updateMinusState(parseTimer());
        });


        cbAddNote.setOnCheckedChangeListener((buttonView, isChecked) -> {
            etNote.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        btnInfo.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("Add note")
                    .setMessage("Use this to jot down a quick note that will be saved with your timer (optional).")
                    .setPositiveButton("OK", null)
                    .show();
        });


        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int currTimer = prefs.getInt("TIMER_MINUTES", 0);
        SharedPreferences.Editor editor = prefs.edit();

        btnStart.setOnClickListener(v -> {

            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                return;
            }

            if (!hasUsageAccessPermission()) {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                return;
            }

            Intent intent = new Intent(this, UsageMonitorService.class);
            int minutes = 0;

            if (!timerEditText.getText().toString().isEmpty() && !timerEditText.getText().toString().equals("0")) {
                stopService(intent);
                minutes = Integer.parseInt(timerEditText.getText().toString());
                editor.putInt("TIMER_MINUTES", minutes).apply();
                //textViewTimer.setText(minutes + " minute/s");
                editor.putBoolean("timer_enabled", true).apply();
                Toast.makeText(getApplicationContext(), "Notify screen timer enabled!", Toast.LENGTH_LONG).show();
                startForegroundService(intent);
                Intent i = new Intent(MainActivity.this, TimerActivity.class);
                startActivity(i);
                finish();
                // optional: animation
                overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
            } else {
                //textViewTimer.setText(" - ");
                editor.putBoolean("timer_enabled", false).apply();
                //stopService(intent);
                Toast.makeText(getApplicationContext(), "Please Enter Timer!", Toast.LENGTH_LONG).show();
            }


        });

    }

    private boolean hasUsageAccessPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private int parseTimer() {
        String s = timerEditText.getText().toString().trim();
        if (s.isEmpty()) return 1;                 // treat empty as 1 for buttons
        try {
            int v = Integer.parseInt(s);
            return Math.max(1, v);                 // clamp to 1 minimum
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private void setTimer(int v) {
        v = Math.max(1, v);
        timerEditText.setText(String.valueOf(v));
        timerEditText.setSelection(timerEditText.getText().length());
        updateMinusState(v);
    }

    private void updateMinusState(int v) {
        boolean canDecrement = v > 1;
        btnMinus.setEnabled(canDecrement);
        btnMinus.setAlpha(canDecrement ? 1f : 0.4f);   // visual feedback
    }

}
