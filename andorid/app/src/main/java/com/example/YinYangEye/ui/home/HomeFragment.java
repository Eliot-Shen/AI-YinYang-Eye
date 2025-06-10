package com.example.YinYangEye.ui.home;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.YinYangEye.AudioFocusService;
import com.example.YinYangEye.ScreenCaptureActivity;
import com.example.YinYangEye.HistoryActivity;
import com.example.YinYangEye.MediaProjectionService;
import com.example.chap03.R;
import com.example.chap03.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // 创建高优先级通知通道
        createHighPriorityNotificationChannel();
        // 创建自定义通知通道
        createCustomNotificationChannel();
        //创建MediaProjectionService的通道
        createMediaProjectionNotificationChannel();

        final TextView textView = binding.howtoUse;
        homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);

        Button startServiceButton = root.findViewById(R.id.start_service);
        Button stopServiceButton = root.findViewById(R.id.stop_service);
        Button btn_view_history = root.findViewById(R.id.btn_view_history);

        startServiceButton.setOnClickListener(v -> {
            // 启动 AudioFocusService
            Intent startAudioFocusIntent = new Intent(requireContext(), AudioFocusService.class);
            requireContext().startService(startAudioFocusIntent);

            // 启动 ScreenCaptureActivity 以请求屏幕捕获权限
            Intent screenCaptureIntent = new Intent(requireContext(), ScreenCaptureActivity.class);
            screenCaptureIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(screenCaptureIntent);
        });

        stopServiceButton.setOnClickListener(v -> {
            Intent stopAudioFocusIntent = new Intent(requireContext(), AudioFocusService.class);
            Intent stopScreenShotIntent = new Intent(requireContext(), MediaProjectionService.class);
            requireContext().stopService(stopAudioFocusIntent);
            requireContext().stopService(stopScreenShotIntent);
        });

        btn_view_history.setOnClickListener(view -> {
            // 跳转到历史记录界面
            Intent intent = new Intent(requireContext(), HistoryActivity.class);
            startActivity(intent);
        });


        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void createHighPriorityNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "high_priority_channel", "High Priority Channel", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Channel for high priority notifications");
        channel.setShowBadge(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createCustomNotificationChannel() {

        NotificationChannel channel = new NotificationChannel(
                "custom_notification_channel", // 渠道 ID
                "Custom Notification Channel", // 渠道名称
                NotificationManager.IMPORTANCE_HIGH); // 渠道重要性
        channel.setDescription("Channel for custom notifications");
        channel.setShowBadge(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

    private void createMediaProjectionNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                "mediaProjectionChannel", "MediaProjection", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Channel for media projection notifications");
        channel.setShowBadge(true);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        NotificationManager notificationManager = requireContext().getSystemService(NotificationManager.class);
        if (notificationManager != null) {
            notificationManager.createNotificationChannel(channel);
        }
    }

}