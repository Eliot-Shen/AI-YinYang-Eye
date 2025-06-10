package com.example.YinYangEye;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.chap03.R;
import java.util.Objects;


public class UploadImageActivity extends AppCompatActivity {

    private BroadcastReceiver uploadResultReceiver;

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    openGallery();
                } else {
                    Toast.makeText(UploadImageActivity.this, "存储权限被拒绝，无法选择图片！", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    Uri imageUri = data.getData(); // 获取单个图片 URI
                    if (imageUri != null) {
                        // 授予 URI 的读取权限
                        getContentResolver().takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startImageUploadService(imageUri); // 修改为只上传单个图片
                    } else {
                        Toast.makeText(UploadImageActivity.this, "No image selected!", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private TextView tvUploadResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload_image_activity);


        Button btn_upload = findViewById(R.id.btn_upload);
        Button btn_view_history = findViewById(R.id.btn_view_history); // 获取查看历史记录按钮
        tvUploadResult = findViewById(R.id.tv_upload_result);
        btn_upload.setOnClickListener(view -> requestStoragePermission());
        btn_view_history.setOnClickListener(view -> {
            // 跳转到历史记录界面
            Intent intent = new Intent(UploadImageActivity.this, HistoryActivity.class);
            startActivity(intent);
        });

        // 注册广播接收器
        uploadResultReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Objects.equals(intent.getAction(), ImageUploadService.UPLOAD_RESULT_ACTION)) {
                    String result = intent.getStringExtra(ImageUploadService.UPLOAD_RESULT_EXTRA);
                    tvUploadResult.setText(result);
                }
            }
        };

        IntentFilter intentFilter = new IntentFilter(ImageUploadService.UPLOAD_RESULT_ACTION);
        LocalBroadcastManager.getInstance(this).registerReceiver(uploadResultReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 取消注册广播接收器
        LocalBroadcastManager.getInstance(this).unregisterReceiver(uploadResultReceiver);
    }

    private void requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            } else {
                openGallery();
            }
        } else {
            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        galleryLauncher.launch(intent);
    }

    private void startImageUploadService(Uri imageUri) {
        Intent serviceIntent = new Intent(this, ImageUploadService.class);
        serviceIntent.setAction(ImageUploadService.ACTION_UPLOAD_FROM_URI); // 设置动作
        serviceIntent.putExtra("imageUri", imageUri); // 直接传递单个 Uri
        startService(serviceIntent);
    }
}