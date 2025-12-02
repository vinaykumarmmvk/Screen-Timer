package com.enerflow.screentimer;

import static androidx.core.app.ServiceCompat.startForeground;

import android.app.AlertDialog;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
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

import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

public class MainActivity extends AppCompatActivity {
    EditText timerEditText, etNote;
    Button btnStart;
    TextView aboutDeveloper, btnMinus, btnPlus;
    CheckBox cbAddNote;
    ImageButton btnInfo;
    // add in MainActivity fields:
    private android.app.Dialog permissionsDialog;
    private View dialogView;
    private boolean pendingAutoProgress = false;

    private void openOverlaySettings() {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private void openUsageAccessSettings() {
        Intent i = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(i);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.screen_timer_menu, menu);
        return true; // show the 3-dots
    }

    @Override
    protected void onResume() {
        super.onResume();
        startFlowOrRunApp();
        // refresh the dialog if it's open
        if (permissionsDialog != null && permissionsDialog.isShowing() && dialogView != null) {
            TextView tvOverlay = dialogView.findViewById(R.id.tvOverlayStatus);
            TextView tvUsage   = dialogView.findViewById(R.id.tvUsageStatus);
            updatePermissionStatuses(tvOverlay, tvUsage);

            if (pendingAutoProgress && !hasAllCriticalPermissions()) {
                proceedPermissions(tvOverlay, tvUsage);
            } else if (hasAllCriticalPermissions()) {
                pendingAutoProgress = false;
                permissionsDialog.dismiss();
                onPermissionsReady();
            }
        }

        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit().putString("last_screen", "MAIN").apply();
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


    private boolean hasAllCriticalPermissions() {
        // same logic as in PermissionsActivity (or reuse a small util)
        return Settings.canDrawOverlays(this) && hasUsageAccessPermission();
    }

    private boolean hasUsageAccessPermission() {
        AppOpsManager aom = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
        int mode = aom.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        if (mode == AppOpsManager.MODE_DEFAULT) {
            return checkSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return mode == AppOpsManager.MODE_ALLOWED;
    }

    private void startFlowOrRunApp() {
        if (!hasAllCriticalPermissions()) {
            showPermissionsDialog();
            return;
        }
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

        timerEditText = findViewById(R.id.editTextTimer);
        btnStart = findViewById(R.id.btnStart);
        //textViewTimer = findViewById(R.id.textViewTimer);
        aboutDeveloper = findViewById(R.id.aboutDeveloper);

        cbAddNote = findViewById(R.id.checkBoxAddNote);
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
                    .setMessage("By default a set of motivational quotes are shown.\nIf you want a custom message consider adding it in the add note field.")
                    .setPositiveButton("OK", null)
                    .show();
        });


        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int currTimer = prefs.getInt("TIMER_MINUTES", 0);
        SharedPreferences.Editor editor = prefs.edit();

        btnStart.setOnClickListener(v -> {

            if (cbAddNote.isChecked()) {
                editor.putBoolean("isUserQuote", true).apply();
                editor.putString("userQuote", etNote.getText().toString()).apply();
            }
            else
                editor.putBoolean("isUserQuote", false).apply();

            if (!hasAllCriticalPermissions()) {
                showPermissionsDialog();
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
                ContextCompat.startForegroundService(this, new Intent(this, UsageMonitorService.class));

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

    private void showPermissionsDialog() {
        if (permissionsDialog != null && permissionsDialog.isShowing()) return;

        dialogView = getLayoutInflater().inflate(R.layout.activity_permissions, null, false);

        TextView tvOverlay = dialogView.findViewById(R.id.tvOverlayStatus);
        TextView tvUsage   = dialogView.findViewById(R.id.tvUsageStatus);
        Button btnGrant    = dialogView.findViewById(R.id.btnGrant);

        btnGrant.setOnClickListener(v -> proceedPermissions(tvOverlay, tvUsage));

        permissionsDialog = new androidx.appcompat.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        // optional nice background if your layout card has rounded corners
        if (permissionsDialog.getWindow() != null) {
            permissionsDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        permissionsDialog.show();
        updatePermissionStatuses(tvOverlay, tvUsage);
    }

    private void updatePermissionStatuses(TextView tvOverlay, TextView tvUsage) {
        tvOverlay.setText(Settings.canDrawOverlays(this)
                ? "Overlay: ✅ Granted" : "Overlay: ❌ Not granted");
        tvUsage.setText(hasUsageAccessPermission()
                ? "Usage access: ✅ Granted" : "Usage access: ❌ Not granted");
    }

    private void proceedPermissions(TextView tvOverlay, TextView tvUsage) {
        if (!Settings.canDrawOverlays(this)) {
            pendingAutoProgress = true;
            openOverlaySettings();
            return;
        }
        if (!hasUsageAccessPermission()) {
            pendingAutoProgress = true;
            openUsageAccessSettings();
            return;
        }
        // Both granted
        pendingAutoProgress = false;
        if (permissionsDialog != null && permissionsDialog.isShowing()) permissionsDialog.dismiss();
        onPermissionsReady();
    }

    private void onPermissionsReady() {
        // Called when both permissions are granted.
        // If you want to immediately start your flow from here you can:
        // e.g., simulate the same logic you run inside btnStart click,
        // or just return and let the user press Start.
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
