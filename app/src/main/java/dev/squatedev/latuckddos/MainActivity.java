/*________________________________________________*//*


               Author: @Squatedev
               age: 19
               name: null


/*________________________________________________*/

package dev.squatedev.latuckddos;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;

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
    private AlertDialog permissionDialog;
    private boolean hasPermission = false;
    private boolean okay = false;
    private Button btn2, btn3;
    private final String url = "https://github.com/SquateDev/Latuck-Network-Resilience-Tester";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        btn2 = findViewById(R.id.tester_a);
        btn3 = findViewById(R.id.Source);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        checkPermission();
    }

    private final ActivityResultLauncher<String[]> permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allGranted = permissions.get(Manifest.permission.READ_EXTERNAL_STORAGE) == Boolean.TRUE;
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    allGranted = allGranted && permissions.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == Boolean.TRUE;
                }
                if (allGranted) {
                    hasPermission = true;
                    dismissDialog();
                    start();
                } else {
                    showPermissionDialog();
                }
            });


    private final ActivityResultLauncher<Intent> manageStorageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (Environment.isExternalStorageManager()) {
                        hasPermission = true;
                        dismissDialog();
                        start();
                    } else {
                        showPermissionDialog();
                    }
                }
            });



    private void dismissDialog() {
        if (permissionDialog != null && permissionDialog.isShowing()) {
            permissionDialog.dismiss();
            permissionDialog = null;
        }
    }

    @SuppressLint("ObsoleteSdkInt")
    private void checkPermission() {
        if (hasPermission) {
            start();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                hasPermission = true;
                start();
            } else {
                requestManageStorage();
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                hasPermission = true;
                start();
            } else {
                requestPermissions();
            }
        } else {
            hasPermission = true;
            start();
        }
    }

    private void requestManageStorage() {
        permissionDialog = new AlertDialog.Builder(this)
                .setTitle("Доступ к хранилищу")
                .setMessage("Для работы приложения необходимо разрешение на доступ к файлам")
                .setPositiveButton("Разрешить", (dialog, which) -> {
                    dismissDialog();
                    okay = true;
                    @SuppressLint("InlinedApi") Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    manageStorageLauncher.launch(intent);
                })
                .setNegativeButton("Выйти", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            });
        } else {
            permissionLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            });
        }
    }

    private void showPermissionDialog() {
        permissionDialog = new AlertDialog.Builder(this)
                .setTitle("Доступ не получен")
                .setMessage("Без доступа к файлам приложение не может работать")
                .setPositiveButton("Попробовать снова", (dialog, which) -> {
                    dismissDialog();
                    checkPermission();
                    okay = true;
                })
                .setNegativeButton("Выйти", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    private void start() {
        btn2.setOnClickListener(v -> startActivity(new Intent(this, ActivityTEST.class)));
        btn3.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!hasPermission && okay) {
            checkPermission();
            okay = false;
        }
    }
}