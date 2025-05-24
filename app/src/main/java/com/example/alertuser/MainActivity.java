package com.example.alertuser;

import android.app.AppOpsManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    EditText timerEditText;
    Button startStopButton;
    Switch switchTimer;
    Boolean timerFlag;
    TextView textViewTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        timerEditText = findViewById(R.id.editTextTimer);
        startStopButton = findViewById(R.id.startStopButton);
        switchTimer = findViewById(R.id.switch_timer);
        textViewTimer = findViewById(R.id.textViewTimer);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        timerFlag = prefs.getBoolean("timer_enabled", true);
        int currTimer = prefs.getInt("TIMER_MINUTES", 0);
        SharedPreferences.Editor editor = prefs.edit();

        textViewTimer.setText(currTimer);
        if(timerFlag) {
            switchTimer.setChecked(true);
            switchTimer.setText("Enable Timer");
            startStopButton.setText("Start");
        }
        else {
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
            }
            else {
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

            int minutes = Integer.parseInt(timerEditText.getText().toString());
            editor.putInt("TIMER_MINUTES", minutes).apply();

            Intent intent = new Intent(this, UsageMonitorService.class);
            //intent.putExtra("TIMER_MINUTES", minutes);

            if (timerFlag) {

                startForegroundService(intent);
            }
            else
                stopService(intent);
        });
    }

    private boolean hasUsageAccessPermission() {
        AppOpsManager appOps = (AppOpsManager) getSystemService(APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName());
        return mode == AppOpsManager.MODE_ALLOWED;
    }
}
