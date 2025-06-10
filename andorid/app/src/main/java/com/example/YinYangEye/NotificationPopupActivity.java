package com.example.YinYangEye;

import android.os.Bundle;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chap03.R;

public class NotificationPopupActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 设置全屏样式
        requestWindowFeature(Window.FEATURE_NO_TITLE); // 去掉标题栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN); // 设置全屏

        setContentView(R.layout.activity_notification_popup);

        // 设置关闭按钮的点击事件
        findViewById(R.id.popup_close_button).setOnClickListener(v -> finish());
    }
}