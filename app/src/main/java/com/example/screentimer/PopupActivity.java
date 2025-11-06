package com.example.screentimer;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PopupActivity extends AppCompatActivity {

    private TextView textViewQuote;
    private Button buttonCloseApps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.popup_activity);

        textViewQuote = findViewById(R.id.textViewQuote);
        buttonCloseApps = findViewById(R.id.buttonCloseApps);

        String quote = getIntent().getStringExtra("quote");
        int count = getIntent().getIntExtra("count", 0);

        textViewQuote.setText(quote + "\n\nExceed Count Today: " + count);

        buttonCloseApps.setOnClickListener(v -> closeAllApps());
    }

    private void closeAllApps() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            am.killBackgroundProcesses(getPackageName());
        }
    }
}

