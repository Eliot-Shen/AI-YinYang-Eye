package com.example.YinYangEye;

import android.app.Activity;
import android.app.Service;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.os.IBinder;
import android.util.Log;

public class AudioFocusService extends Service {
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    // 添加变量以存储屏幕捕获权限的结果
    private int resultCode = Activity.RESULT_CANCELED;
    private Intent resultData = null;

    @Override
    public IBinder onBind(Intent intent) {
        return null; // 该Service不需要绑定
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("AudioFocusService", "Service Created");
        audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);

        AudioFocusChangeListener audioFocusChangeListener = new AudioFocusChangeListener();

        audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                .build();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("AudioFocusService", "Service Started");
        int result = audioManager.requestAudioFocus(audioFocusRequest);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Log.d("AudioFocus", "音频焦点请求成功");
        } else {
            Log.d("AudioFocus", "音频焦点请求失败");
        }

        // 从 Intent 中获取屏幕捕获权限的结果
        if (intent != null) {
            resultCode = intent.getIntExtra("resultCode", Activity.RESULT_CANCELED);
            resultData = intent.getParcelableExtra("resultData");
        }


        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("AudioFocusService", "Service Destroyed");
        audioManager.abandonAudioFocusRequest(audioFocusRequest);
    }

    private void startMediaProjectionService() {
        // 启动 MediaProjectionService，并传递屏幕捕获权限的结果
        Intent serviceIntent = new Intent(this, MediaProjectionService.class);
        serviceIntent.putExtra("resultCode", resultCode);
        serviceIntent.putExtra("resultData", resultData);

        // 使用 startForegroundService 启动服务
        startService(serviceIntent);


    }

    // 内部类：AudioFocusChangeListener
    private class AudioFocusChangeListener implements OnAudioFocusChangeListener {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    Log.d("AudioFocus", "AUDIOFOCUS_GAIN: 获得音频焦点");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    Log.d("AudioFocus", "AUDIOFOCUS_LOSS: 永久失去音频焦点");
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    Log.d("AudioFocus", "AUDIOFOCUS_LOSS_TRANSIENT: 短暂失去音频焦点");
                    // 在这里启动 MediaProjectionService，并传递屏幕捕获权限的结果
                    startMediaProjectionService();
                    break;
            }
        }
    }


}