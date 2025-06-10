package com.example.YinYangEye;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.YinYangEye.adapter.HistoryAdapter;
import com.example.YinYangEye.model.UploadHistory;
import com.example.YinYangEye.util.HistoryDatabaseHelper;
import com.example.chap03.R;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private HistoryDatabaseHelper dbHelper;
    private List<UploadHistory> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_history);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Button btnBack = findViewById(R.id.btn_back);
        Button btnDeleteAll = findViewById(R.id.btn_delete_all);

        btnBack.setOnClickListener(v -> finish()); // 返回按钮
        btnDeleteAll.setOnClickListener(v -> deleteAllHistory()); // 删除所有历史记录

        loadHistory();
    }

    private void loadHistory() {
        dbHelper = new HistoryDatabaseHelper(this);
        historyList = dbHelper.getAllHistory();
        adapter = new HistoryAdapter(this, historyList, this::deleteItem);
        recyclerView.setAdapter(adapter);
    }

    private void deleteItem(int position, String imagePath) {
        // 删除单个历史记录
        dbHelper.deleteHistory(imagePath);
        historyList.remove(position);
        adapter.notifyItemRemoved(position);
        Toast.makeText(this, "Deleted image", Toast.LENGTH_SHORT).show();
    }

    private void deleteAllHistory() {
        // 删除所有历史记录
        dbHelper.deleteAllHistory();
        historyList.clear();
        adapter.notifyDataSetChanged();
        Toast.makeText(this, "All history deleted", Toast.LENGTH_SHORT).show();
    }
}