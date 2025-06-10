package com.example.YinYangEye;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chap03.R;

public class FullScreenActivity extends AppCompatActivity {
    @SuppressLint("DefaultLocale")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_full_screen);

        // 获取通知传递的内容
        String message = getIntent().getStringExtra("message");
        double confidence = getIntent().getDoubleExtra("confidence", 0.0);

        // 显示通知内容
        TextView messageTextView = findViewById(R.id.full_screen_message);
        TextView confidenceTextView = findViewById(R.id.full_screen_confidence);

        messageTextView.setText(message);
        confidenceTextView.setText(String.format("Confidence: %.2f", confidence));
    }
}