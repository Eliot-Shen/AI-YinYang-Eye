package com.example.YinYangEye.ui.notifications;

import static com.example.YinYangEye.RetrofitClient.BASE_URL;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.YinYangEye.RetrofitClient;
import com.example.chap03.databinding.FragmentNotificationsBinding;

public class NotificationsFragment extends Fragment {

    private FragmentNotificationsBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 添加输入框和按钮
        EditText etBaseUrl = binding.etBaseUrl;
        Button btnSetBaseUrl = binding.btnSetBaseUrl;

        // 设置按钮点击事件
        btnSetBaseUrl.setOnClickListener(v -> {
            String baseUrl = etBaseUrl.getText().toString().trim();
            if (!baseUrl.isEmpty()) {
                // 调用 RetrofitClient 设置 BASE_URL
                RetrofitClient.setBaseUrl(baseUrl);
                Toast.makeText(getContext(), "Base URL set to: " + baseUrl, Toast.LENGTH_SHORT).show();
            } else {
                // 如果用户没有输入链接，使用默认值
                RetrofitClient.setBaseUrl(BASE_URL);
                Toast.makeText(getContext(), "Using default Base URL", Toast.LENGTH_SHORT).show();
            }
        });

        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}