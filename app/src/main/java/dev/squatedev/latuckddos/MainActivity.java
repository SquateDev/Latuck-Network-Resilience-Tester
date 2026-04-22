package dev.squatedev.latuckddos;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import dev.squatedev.latuckddos.Activity.ActivityTEST;

public class MainActivity extends AppCompatActivity {
    private Button btn2, btn3, donation_btn;
    private final String url = "https://github.com/SquateDev/Latuck-Network-Resilience-Tester";
    private boolean notificationPermissionGranted = false;

    private final ActivityResultLauncher<String> notificationPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                notificationPermissionGranted = isGranted;
                if (isGranted) {
                    Toast.makeText(this, "Permission granted", Toast.LENGTH_SHORT).show();
                } else {
                    showPermissionDeniedDialog();
                }
                start();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btn2 = findViewById(R.id.tester_a);
        btn3 = findViewById(R.id.Source);
        donation_btn = findViewById(R.id.donation);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkNotificationPermission();
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                notificationPermissionGranted = true;
                start();
            } else {
                if (shouldShowRequestPermissionRationale(android.Manifest.permission.POST_NOTIFICATIONS)) {
                    showPermissionRationaleDialog();
                } else {
                    notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
                }
            }
        } else {
            notificationPermissionGranted = true;
            start();
        }
    }

    private void showPermissionRationaleDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Notification Permission")
                .setMessage("This app needs notification permission to show attack progress.")
                .setPositiveButton("Grant", (dialog, which) ->
                        notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS))
                .setNegativeButton("Cancel", (dialog, which) -> {
                    notificationPermissionGranted = false;
                    start();
                })
                .show();
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permission Denied")
                .setMessage("Notifications are disabled. Enable them in settings?")
                .setPositiveButton("Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void start() {
        btn2.setOnClickListener(v -> startActivity(new Intent(this, ActivityTEST.class)));

        btn3.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });

        donation_btn.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("https://www.donationalerts.com/r/derzai"));
            startActivity(intent);
        });
    }
}