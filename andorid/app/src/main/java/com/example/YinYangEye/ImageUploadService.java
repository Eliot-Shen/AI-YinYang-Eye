package com.example.YinYangEye;

import android.annotation.SuppressLint;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Binder;

import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.example.YinYangEye.util.HistoryDatabaseHelper;
import com.example.chap03.R;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ImageUploadService extends Service {
    private static final String TAG = "ImageUploadService";
    public static final String ACTION_UPLOAD_FROM_URI = "upload_from_uri";
    public static final String ACTION_UPLOAD_FROM_CACHE = "upload_from_cache";
    public static final String UPLOAD_RESULT_ACTION = "com.example.yinyangeye.UPLOAD_RESULT";
    public static final String UPLOAD_RESULT_EXTRA = "upload_result";

    private static final String DIRECTORY_NAME = "SavedImages"; // 保存图片的目录名
    private final IBinder binder = new LocalBinder();
    private final BlockingQueue<byte[]> uploadQueue = new LinkedBlockingQueue<>();
    private ThreadPoolExecutor executor;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Service Created");
        startUploadThread();
    }

    private void startUploadThread() {
        executor = new ThreadPoolExecutor(
                1, // 核心线程数
                1, // 最大线程数
                0L, // 闲置线程存活时间
                TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>() // 任务队列
        );

        executor.execute(() -> {
            while (!Thread.interrupted()) {
                try {
                    byte[] bitmapByte = uploadQueue.take(); // 从队列中取出图片
                    uploadImageFromCache(bitmapByte); // 上传图片
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_UPLOAD_FROM_URI.equals(action)) {
                // 直接从 Intent 中获取单个 Uri
                Uri imageUri = intent.getParcelableExtra("imageUri");
                if (imageUri != null) {
                    // 调用上传图片的方法
                    uploadImageFromUri(imageUri);
                } else {
                    Log.e(TAG, "No image URI provided.");
                    sendUploadResult("No image URI provided.");
                }

            } else if (ACTION_UPLOAD_FROM_CACHE.equals(action)) {
                Log.d(TAG, "uploadFromCache: Service started");
                // 从 Intent 中获取图片字节数组
                byte[] imageBytes = intent.getByteArrayExtra("bitmap");
                if (imageBytes != null) {
                    uploadImageFromCache(imageBytes);
                } else {
                    Log.e(TAG, "No image bytes provided.");
                    sendUploadResult("No image bytes provided.");
                }
            } else {
                Log.e(TAG, "Unknown action: " + action);
                sendUploadResult("Unknown action: " + action);
            }
        }
        return START_NOT_STICKY;
    }

    private void uploadImageFromUri(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
            byte[] imageBytes = baos.toByteArray();

            MediaType mediaType = MediaType.parse("image/png");
            RequestBody requestBody = RequestBody.Companion.create(imageBytes, mediaType);

            String fileName = "image_" + System.currentTimeMillis() + ".png";
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, requestBody);

            ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
            Call<ApiResponse> call = apiService.detectFakeOrTrue(filePart); // 修改为单个文件上传
            call.enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ApiResponse apiResponse = response.body();
                        if (apiResponse.getError() != null) {
                            sendUploadResult("Error: " + apiResponse.getError());
                        } else {
                            handleApiResponse(apiResponse, imageUri);
                        }
                    } else {
                        handleErrorResponse(response);
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                    sendUploadResult("Failed to connect to server: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Failed to read image: " + imageUri, e);
            sendUploadResult("Failed to read image: " + e.getMessage());
        }
    }



    private void uploadImageFromCache(byte[] imageBytes) {
        if (imageBytes == null || imageBytes.length == 0) {
            Log.e(TAG, "No image in cache to upload.");
            sendUploadResult("No image in cache to upload.");
            return;
        }

        // 将字节数组转换为 Bitmap
        Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

        // 压缩 Bitmap 为字节数组（如果需要）
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        imageBytes = baos.toByteArray();

        MediaType mediaType = MediaType.parse("image/png");
        RequestBody requestBody = RequestBody.Companion.create(imageBytes, mediaType);

        String fileName = "image_" + System.currentTimeMillis() + ".png";
        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", fileName, requestBody);

        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<ApiResponse> call = apiService.detectFakeOrTrue(filePart);
        call.enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.getError() != null) {
                        sendUploadResult("Error: " + apiResponse.getError());
                    } else {
                        handleApiResponseAndNotice(apiResponse, bitmap);
                        Log.d(TAG, "uploadBitmapandHandleApiResponse: success");
                    }
                } else {
                    handleErrorResponse(response);
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                sendUploadResult("Failed to connect to server: " + t.getMessage());
            }
        });
    }

    @SuppressLint("SimpleDateFormat")
    private void handleApiResponse(ApiResponse apiResponse, Object image) {
        // 记录历史
        String imagePath = null;

        double confidence = apiResponse.getConfidence();
        if (confidence <= 1 && confidence > 0.4) {//0为真，1为假，0.4是阈值（暂定）
            if (image instanceof Uri) {
                Uri imageUri = (Uri) image;
                saveImageFromUri(imageUri);
                imagePath = getPathFromUri((Uri) image);
            } else if (image instanceof Bitmap) {
                Bitmap bitmap = (Bitmap) image;
                imagePath = saveBitmap(bitmap);
            }
        }

        if (imagePath != null) {
            HistoryDatabaseHelper dbHelper = new HistoryDatabaseHelper(this);
            dbHelper.addHistory(imagePath, confidence, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }

        int percentage = (int) Math.round(confidence * 100);//四舍五入
        String message = apiResponse.getMessage();
        fetchImageFromServer(percentage, message);

    }

    private void fetchImageFromServer(int percentage, String message) {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<ApiResponse> call = apiService.getImage(); // 使用 ApiResponse
        call.enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(@NonNull Call<ApiResponse> call, @NonNull Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ApiResponse apiResponse = response.body();
                    if (apiResponse.getImage() != null) {
                        try {
                            // 将 Base64 字符串解码为字节数组
                            byte[] imageBytes = android.util.Base64.decode(apiResponse.getImage(), android.util.Base64.DEFAULT);
                            // 通过广播发送图片数据
                            sendImageBroadcast(imageBytes, percentage, message);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to decode Base64 image", e);
                            sendUploadResult("Failed to decode Base64 image: " + e.getMessage());
                        }
                    } else {
                        sendUploadResult("Failed to fetch image: " + (apiResponse != null ? apiResponse.getError() : "Unknown error"));
                    }
                } else {
                    sendUploadResult("Failed to fetch image, status code: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<ApiResponse> call, @NonNull Throwable t) {
                sendUploadResult("Request failed: " + t.getMessage());
                Log.e(TAG, "Request failed", t);
            }
        });
    }


    private void sendImageBroadcast(byte[] imageBytes, int percentage, String message) {
        Log.d(TAG, "------------------------------------------------测试789");
        Intent intent = new Intent(UPLOAD_RESULT_ACTION);
        if (percentage < 5){

            intent.putExtra(UPLOAD_RESULT_EXTRA, "通知: " + message +":无风险");
        } else if(percentage <20){
//            sendUploadResult("通知: " + message +":低风险");
            intent.putExtra(UPLOAD_RESULT_EXTRA, "通知: " + message +":低风险");
        } else if (percentage <45) {

            intent.putExtra(UPLOAD_RESULT_EXTRA, "通知: " + message +":中风险");
        } else{

            intent.putExtra(UPLOAD_RESULT_EXTRA, "通知: " + message +":高风险");
        }

        intent.putExtra("image_data", imageBytes); // 添加图片数据
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    @SuppressLint({"SimpleDateFormat", "DefaultLocale"})
    private void handleApiResponseAndNotice(ApiResponse apiResponse, Object image) {
        //由于截图时调用的一定是从缓冲区上传图片的方法，所以复制一个处理方法，加一些弹窗的操作在里面，可以先在上面的方法里验证

        // 记录历史
        String imagePath = null;
        double confidence = apiResponse.getConfidence();

        String output;
        output = String.format("%.2f", confidence);  // 将 confidence 格式化为保留两位小数的字符串
        Log.d(TAG, "风险度: " + output);  // 输出到日志中

        if (confidence <= 1 && confidence > 0.35) {//0为真，1为假，0.4是阈值（暂定）
            if (image instanceof Uri) {
                Uri imageUri = (Uri) image;
                saveImageFromUri(imageUri);
                imagePath = getPathFromUri((Uri) image);
            } else if (image instanceof Bitmap) {
                Bitmap bitmap = (Bitmap) image;
                imagePath = saveBitmap(bitmap);
            }
        }

        if (imagePath != null) {
            HistoryDatabaseHelper dbHelper = new HistoryDatabaseHelper(this);
            dbHelper.addHistory(imagePath, confidence, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        }

        int percentage = (int) Math.round(confidence * 100);//四舍五入

        // 弹出通知提醒用户
        if (confidence <= 1 && confidence > 0.35) {
            // 显示自定义通知
            showCustomNotification(confidence);
        }

        sendUploadResult("通知: " + apiResponse.getMessage());
    }


    private void handleErrorResponse(Response<ApiResponse> response) {
        if (response.errorBody() != null) {
            try {
                String errorResponse = response.errorBody().string();
                sendUploadResult("Failed to get response: " + errorResponse);
            } catch (IOException e) {
                sendUploadResult("Failed to read error response: " + e.getMessage());
            } finally {
                response.errorBody().close();
            }
        } else {
            sendUploadResult("Failed to get response: No error body");
        }
    }

    private void saveImageFromUri(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            saveBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Failed to save image from URI: " + imageUri, e);
        }
    }

    private String saveBitmap(Bitmap bitmap) {
        File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), DIRECTORY_NAME);

        if (!directory.exists()) {
            boolean isDirectoryCreated = directory.mkdirs();
            if (!isDirectoryCreated) {
                Log.e(TAG, "Failed to create directory: " + directory.getAbsolutePath());
                return null;
            }
        }

        String fileName = "image_" + System.currentTimeMillis() + ".png";
        File file = new File(directory, fileName);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
            Log.d(TAG, "Image saved to: " + file.getAbsolutePath());
            return file.getAbsolutePath();
        } catch (IOException e) {
            Log.e(TAG, "Failed to save bitmap: ", e);
        }

        return null;
    }


    private void sendUploadResult(String result) {
        Intent intent = new Intent(UPLOAD_RESULT_ACTION);
        intent.putExtra(UPLOAD_RESULT_EXTRA, result);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    public String getPathFromUri(Uri uri) {
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = { "_data" };

            try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndexOrThrow("_data");
                    return cursor.getString(columnIndex);
                }
            } catch (Exception e) {
                Log.e("getPathFromUri", "Failed to get path from URI", e);
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }


    @SuppressLint("DefaultLocale")
    private void showCustomNotification(double confidence) {


        // 创建关闭通知的 Intent
        Intent closeIntent = new Intent(this, NotificationReceiver.class);
        closeIntent.setAction("CLOSE_NOTIFICATION");

        // 加载自定义通知布局
        RemoteViews customView = new RemoteViews(getPackageName(), R.layout.notification_custom);

        // 创建全屏意图
        Intent fullScreenIntent = new Intent(this, NotificationPopupActivity.class);
        fullScreenIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        fullScreenIntent.putExtra("message", "AI警告");
        fullScreenIntent.putExtra("confidence", confidence);
        PendingIntent fullScreenPendingIntent = PendingIntent.getActivity(
                this,
                1,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 构建通知并设置全屏意图
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "custom_notification_channel")
                .setSmallIcon(R.drawable.logo)
//                .setContentTitle("AI Risk Alert")
//                .setContentText(message)
                .setCustomContentView(customView)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
//                .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                .setAutoCancel(true)
                .setFullScreenIntent(fullScreenPendingIntent, true);



        // 发送通知
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
//            int notificationId = (int) System.currentTimeMillis(); // 动态ID
            notificationManager.notify(1, builder.build());
        }
    }


    public void addImageToQueue(byte[] bitmapBytes) {
        uploadQueue.add(bitmapBytes);
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service Destroyed");
        executor.shutdownNow();
    }

    public class LocalBinder extends Binder {
        public ImageUploadService getService() {
            return ImageUploadService.this;
        }
    }

}