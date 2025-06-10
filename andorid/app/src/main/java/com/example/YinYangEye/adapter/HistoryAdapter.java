package com.example.YinYangEye.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.YinYangEye.model.UploadHistory;
import com.example.chap03.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private List<UploadHistory> historyList;
    private Context context;
    private OnItemClickListener listener;

    public HistoryAdapter(Context context, List<UploadHistory> historyList, OnItemClickListener listener) {
        this.context = context;
        this.historyList = historyList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UploadHistory history = historyList.get(position);
        holder.confidence.setText("Confidence: " + history.getConfidence());

        // 加载图片
        Glide.with(context)
                .load(history.getImagePath())
                .fitCenter()
                .placeholder(R.drawable.androiduse)//暂定，后期要改
                .error(R.drawable.androiduse)
                .into(holder.imageView);

        // 设置长按事件
        holder.itemView.setOnLongClickListener(v -> {
            listener.onItemLongClick(position, history.getImagePath());
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView confidence;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.image_view);
            confidence = itemView.findViewById(R.id.text_confidence);
        }
    }

    public interface OnItemClickListener {
        void onItemLongClick(int position, String imagePath);
    }
}