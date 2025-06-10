package com.example.YinYangEye;

import android.app.Activity;
import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.chap03.R;

public class MediaProjectionManagerService extends Service {
    private static final String TAG = "MediaProjectionManager";
    private MediaProjection mediaProjection;
    private final int NOTIFICATION_ID = 1;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
        Intent resultData = intent.getParcelableExtra("resultData");

        if (resultCode == Activity.RESULT_OK && resultData != null) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);
            startForegroundService();

            // 发送广播通知 MediaProjectionService
            Intent broadcastIntent = new Intent("com.example.MEDIA_PROJECTION_READY");
            sendBroadcast(broadcastIntent);
        } else {
            Log.e(TAG, "屏幕捕获权限未授予，服务停止");
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void startForegroundService() {
        Notification notification = new NotificationCompat.Builder(this, "mediaProjectionChannel")
                .setSmallIcon(R.mipmap.logo)
                .setContentTitle("MediaProjection Service")
                .setContentText("Running MediaProjection")
                .build();

        int foregroundServiceType = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
        }

        startForeground(NOTIFICATION_ID, notification, foregroundServiceType);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaProjection != null) {
            mediaProjection.stop();
            Log.d(TAG, "MediaProjectionManagerService Destroyed");
        }
    }
}