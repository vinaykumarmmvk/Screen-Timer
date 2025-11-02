package com.example.alertuser;

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
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.screen_timer_menu, menu);
        return true; // show the 3-dots
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
}
