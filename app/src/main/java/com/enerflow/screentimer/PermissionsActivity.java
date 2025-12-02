package com.enerflow.screentimer;

import android.app.AppOpsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class PermissionsActivity extends AppCompatActivity {

    private TextView tvOverlayStatus, tvUsageStatus;
    private boolean pendingAutoProgress = false; // continue to next step after returning

    @Override protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions);

        tvOverlayStatus = findViewById(R.id.tvOverlayStatus);
        tvUsageStatus   = findViewById(R.id.tvUsageStatus);
        Button btnGrant = findViewById(R.id.btnGrant);

        btnGrant.setOnClickListener(v -> proceed());
    }

    @Override protected void onResume() {
        super.onResume();
        updateStatuses();
        // If we came back from a settings page, auto-continue until both are granted
        if (pendingAutoProgress && !allGranted()) {
            proceed();
        } else if (allGranted()) {
            finishPermissionsFlow();
        }
    }

    private void proceed() {
        if (!hasOverlayPermission()) {
            pendingAutoProgress = true;
            openOverlaySettings();
            return;
        }
        if (!hasUsageAccessPermission()) {
            pendingAutoProgress = true;
            openUsageAccessSettings();
            return;
        }
        finishPermissionsFlow();
    }

    private void finishPermissionsFlow() {
        pendingAutoProgress = false;
        // Done: return to caller (e.g., MainActivity) or go next
        finish();
    }

    // ===== Helpers =====

    private boolean allGranted() {
        return hasOverlayPermission() && hasUsageAccessPermission();
    }

    private void updateStatuses() {
        tvOverlayStatus.setText(hasOverlayPermission()
                ? "Overlay: ✅ Granted"
                : "Overlay: ❌ Not granted");

        tvUsageStatus.setText(hasUsageAccessPermission()
                ? "Usage access: ✅ Granted"
                : "Usage access: ❌ Not granted");
    }

    private boolean hasOverlayPermission() {
        return Settings.canDrawOverlays(this);
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

    private void openOverlaySettings() {
        Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(i);
    }

    private void openUsageAccessSettings() {
        // Opens the Usage Access screen; user must toggle your app ON
        Intent i = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
        startActivity(i);
    }
}
