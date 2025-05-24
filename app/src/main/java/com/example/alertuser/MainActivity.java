package com.example.alertuser;

import android.app.AppOpsManager;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    EditText timerEditText;
    Button startStopButton;
    Switch switchTimer;
    Boolean timerFlag;
    TextView textViewTimer, aboutDeveloper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerEditText = findViewById(R.id.editTextTimer);
        startStopButton = findViewById(R.id.startStopButton);
        switchTimer = findViewById(R.id.switch_timer);
        textViewTimer = findViewById(R.id.textViewTimer);
        aboutDeveloper = findViewById(R.id.aboutDeveloper);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        timerFlag = prefs.getBoolean("timer_enabled", true);
        int currTimer = prefs.getInt("TIMER_MINUTES", 0);
        SharedPreferences.Editor editor = prefs.edit();

        textViewTimer.setText(currTimer + " minutes");
        if (timerFlag) {
            switchTimer.setChecked(true);
            switchTimer.setText("Enable Timer");
            startStopButton.setText("Start");
        } else {
            switchTimer.setChecked(false);
            switchTimer.setText("Disable Timer");
            startStopButton.setText("Stop");
        }

        // Save the switch state to SharedPreferences
        switchTimer.setOnCheckedChangeListener((buttonView, isChecked) -> {
            editor.putBoolean("timer_enabled", isChecked);
            editor.apply();
            timerFlag = isChecked;

            if (isChecked) {
                switchTimer.setText("Enable Timer");
                startStopButton.setText("Start");
            } else {
                switchTimer.setText("Disable Timer");
                startStopButton.setText("Stop");
            }
        });

        startStopButton.setOnClickListener(v -> {
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

            if (timerFlag && !timerEditText.getText().toString().isEmpty()) {
                minutes = Integer.parseInt(timerEditText.getText().toString());
                editor.putInt("TIMER_MINUTES", minutes).apply();
                textViewTimer.setText(minutes + " minute/s");
                startForegroundService(intent);
            }

            if (!timerFlag) {
                textViewTimer.setText(" - ");
                stopService(intent);
            }
        });

        TextView aboutDeveloper = findViewById(R.id.aboutDeveloper);
        TextView aboutApp = findViewById(R.id.aboutApp);

        aboutDeveloper.setOnClickListener(v -> {
            final Dialog aboutDevDialog = new Dialog(MainActivity.this);
            aboutDevDialog.setContentView(R.layout.dialog_about_developer);
            aboutDevDialog.setCancelable(true);

            ImageView emailIcon = aboutDevDialog.findViewById(R.id.emailIcon);
            ImageView linkedinIcon = aboutDevDialog.findViewById(R.id.linkedinIcon);
            Button btnClose = aboutDevDialog.findViewById(R.id.closeButton);

            emailIcon.setOnClickListener(view -> {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:mmvinaykumar.mm@gmail.com"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "App Contact");
                startActivity(Intent.createChooser(emailIntent, "Send email"));
            });

            linkedinIcon.setOnClickListener(view -> {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://www.linkedin.com/in/vinaykumar-mysuru-manjunath-33b522ba/"));
                startActivity(browserIntent);
            });

            btnClose.setOnClickListener(view -> aboutDevDialog.dismiss());

            aboutDevDialog.show();
        });

        aboutApp.setOnClickListener(v -> {
            final Dialog aboutAppDialog = new Dialog(MainActivity.this);
            aboutAppDialog.setContentView(R.layout.dialog_about_app);
            aboutAppDialog.setCancelable(true);

            Button btnClose = aboutAppDialog.findViewById(R.id.closeButton);

            btnClose.setOnClickListener(view -> aboutAppDialog.dismiss());

            aboutAppDialog.show();
        });

    }

    private boolean hasUsageAccessPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
