package com.example.YinYangEye;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.LruCache;
import android.view.WindowManager;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.chap03.R;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Objects;

public class MediaProjectionService extends Service {
    private static final String TAG = "MediaProjectionService";
    private MediaProjection mediaProjection;
    private ImageReader imageReader;
    private final Handler handler = new Handler();
    private Runnable screenshotTask;
    // 缓存池，用于存储截取到的图片
    private LruCache<String, Bitmap> bitmapCache;
    private ImageUploadService imageUploadService;
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG, "ImageUploadService connected");
            imageUploadService = ((ImageUploadService.LocalBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "ImageUploadService disconnected");
            imageUploadService = null;
        }
    };


    private final BroadcastReceiver getCacheImagesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Objects.equals(intent.getAction(), "com.example.GET_CACHE_IMAGES")) {
                Bitmap bitmap = getBitmapFromCache(); // 从缓存中取出一张图片
                if (bitmap != null) {
                    byte[] bitmapBytes = bitmapToByteArray(bitmap); // 将 Bitmap 转换为字节数组
                    Intent uploadIntent = new Intent(context, ImageUploadService.class);
                    uploadIntent.setAction(ImageUploadService.ACTION_UPLOAD_FROM_CACHE);
                    uploadIntent.putExtra("bitmap", bitmapBytes);// 传递单张图片的字节数组
                    context.startService(uploadIntent);
                } else {
                    Log.w(TAG, "No image available in cache");
                }
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // 初始化缓存池
        int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        int cacheSize = maxMemory / 8; // 使用最大内存的 1/8 作为缓存大小
        bitmapCache = new LruCache<>(cacheSize) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount() / 1024; // 以 KB 为单位计算缓存大小
            }
        };

        IntentFilter filter = new IntentFilter("com.example.GET_CACHE_IMAGES");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 对于 Android 14 及以上版本，显式指定 RECEIVER_NOT_EXPORTED
            registerReceiver(getCacheImagesReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            // 对于旧版本，保持原有注册方式
            registerReceiver(getCacheImagesReceiver, filter);
        }

        Intent uploadServiceIntent = new Intent(this, ImageUploadService.class);
        bindService(uploadServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // 从 Intent 中获取屏幕捕获权限的结果
        int resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
        Intent resultData = intent.getParcelableExtra("resultData");

        if (resultCode == Activity.RESULT_OK && resultData != null) {
            // 启动前台服务
            startForegroundService();

            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
            mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, resultData);

            // 获取设备屏幕信息
            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            windowManager.getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;
            int screenDensityDpi = displayMetrics.densityDpi;

            imageReader = ImageReader.newInstance(screenWidth, screenHeight, PixelFormat.RGBA_8888, 2);
            mediaProjection.createVirtualDisplay("ScreenCapture", screenWidth, screenHeight, screenDensityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, imageReader.getSurface(), null, null);

            // 启动定时截图任务
            startScreenshotTask();
        } else {
            // 如果屏幕捕获权限未授予，记录错误并停止服务
            Log.e(TAG, "屏幕捕获权限未授予，服务停止");
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void startForegroundService() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("mediaProjectionChannel",
                                                            "MediaProjection",
                                                                   NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);

        Notification notification = new NotificationCompat.Builder(this, "mediaProjectionChannel")
                .setSmallIcon(R.mipmap.logo)
                .setContentTitle("Screen Capture Service")
                .setContentText("Running ScreenShotService")
                .build();

        int foregroundServiceType = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
        }

        // 在后台启动前台服务时，指定前台服务类型
        startForeground(1, notification, foregroundServiceType);
    }

    private void startScreenshotTask() {
        screenshotTask = new Runnable() {
            @Override
            public void run() {
                takeScreenshot();
                // 每隔5秒执行一次
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(screenshotTask);
    }

    private void takeScreenshot() {
        Image image = imageReader.acquireLatestImage();

        if (image != null) {
            try {
                Bitmap bitmap = convertImageToBitmap(image);
                if (bitmap != null) {
                    uploadBitmap(bitmap);
                    Log.d(TAG, "takeScreenshot: ScreenShot succeed");
                }
            } finally {
                image.close();
            }
        } else {
            Log.w(TAG, "Failed to acquire image");
        }
    }

    private Bitmap convertImageToBitmap(Image image) {
        if (image == null) {
            Log.w(TAG, "Failed to acquire image");
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        // 创建 bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // 将 ByteBuffer 转换为 Bitmap
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();


        int offset = 0;
        for (int row = 0; row < height; row++) {
            for (int col = 0; col < width; col++) {
                int pixelOffset = offset + col * pixelStride;
                int r = buffer.get(pixelOffset) & 0xFF;
                int g = buffer.get(pixelOffset + 1) & 0xFF;
                int b = buffer.get(pixelOffset + 2) & 0xFF;
                int a = buffer.get(pixelOffset + 3) & 0xFF;

                int pixel = (a << 24) | (r << 16) | (g << 8) | b;
                bitmap.setPixel(col, row, pixel);
            }
            offset += rowStride;
        }

        return bitmap;
    }

    private void uploadBitmap(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap is null, cannot upload.");
            return;
        }

        // 将 Bitmap 转换为字节数组
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] bitmapBytes = baos.toByteArray();

        if (imageUploadService != null) {
            imageUploadService.addImageToQueue(bitmapBytes);
        } else {
            Log.e(TAG, "ImageUploadService is not connected");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mediaProjection != null) {
            mediaProjection.stop();
            Log.d("MediaProjectionService", "Service Destroyed");
        }
        if (imageReader != null) {
            imageReader.close();
        }
        handler.removeCallbacks(screenshotTask);
        unregisterReceiver(getCacheImagesReceiver);
        unbindService(serviceConnection);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // 添加以下方法,每次只取一张
    public Bitmap getBitmapFromCache() {
        if (bitmapCache == null || bitmapCache.size() == 0) {
            return null; // 缓存为空时返回 null
        }
        // 获取缓存中的第一张图片并移除
        String firstKey = bitmapCache.snapshot().keySet().iterator().next();
        Bitmap bitmap = bitmapCache.get(firstKey);
        bitmapCache.remove(firstKey);
        Log.d("MediaProjectionService", "getBitmapFromCache");
        return bitmap;
    }

    private byte[] bitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        return stream.toByteArray();
    }

}