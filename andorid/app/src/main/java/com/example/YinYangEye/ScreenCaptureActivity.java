package com.example.YinYangEye;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.media.projection.MediaProjectionManager;


public class ScreenCaptureActivity extends Activity {
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 获取 MediaProjectionManager 实例
        MediaProjectionManager mediaProjectionManager =
                (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        if (mediaProjectionManager != null) {
            // 请求屏幕捕获权限
            Intent captureIntent = mediaProjectionManager.createScreenCaptureIntent();
            startActivityForResult(captureIntent, REQUEST_CODE_SCREEN_CAPTURE);
        } else {
            Log.e("ScreenCaptureActivity", "MediaProjectionManager is null");
            finish();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            // 将屏幕捕获权限的结果传递给 AudioFocusService
            Intent serviceIntent = new Intent(this, AudioFocusService.class);
            serviceIntent.putExtra("resultCode", resultCode);
            serviceIntent.putExtra("resultData", data);
            startService(serviceIntent);

            // 结束当前 Activity
            finish();
        }
    }
}