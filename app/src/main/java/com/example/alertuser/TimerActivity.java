package com.example.alertuser;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class TimerActivity extends AppCompatActivity {
    Button btnStop;

    @Override
    protected void onResume() {
        super.onResume();
        getSharedPreferences("app_prefs", MODE_PRIVATE)
                .edit().putString("last_screen", "TIMER").apply();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_timer);  // your second XML

        btnStop = findViewById(R.id.btnStop);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        int currTimer = prefs.getInt("TIMER_MINUTES", 0);
        SharedPreferences.Editor editor = prefs.edit();


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
}
