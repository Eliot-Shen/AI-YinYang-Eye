package com.example.YinYangEye.util;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class HistoryDatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "history.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "upload_history";
    private static final String COLUMN_ID = "_id";
    public static final String COLUMN_IMAGE_PATH = "image_path";
    public static final String COLUMN_CONFIDENCE = "confidence";
    public static final String COLUMN_UPLOAD_TIME = "upload_time";

    public HistoryDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_NAME + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COLUMN_IMAGE_PATH + " TEXT, " +
                COLUMN_CONFIDENCE + " REAL, " +
                COLUMN_UPLOAD_TIME + " TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void addHistory(String imagePath, double confidence, String uploadTime) {
        SQLiteDatabase db = this.getWritableDatabase();

        // 检查是否已经存在相同的图片路径
        String[] columns = {COLUMN_IMAGE_PATH};
        String selection = COLUMN_IMAGE_PATH + " = ?";
        String[] selectionArgs = {imagePath};
        Cursor cursor = db.query(TABLE_NAME, columns, selection, selectionArgs, null, null, null);

        if (cursor.getCount() == 0) { // 如果不存在相同的图片路径
            ContentValues values = new ContentValues();
            values.put(COLUMN_IMAGE_PATH, imagePath);
            values.put(COLUMN_CONFIDENCE, confidence);
            values.put(COLUMN_UPLOAD_TIME, uploadTime);
            db.insert(TABLE_NAME, null, values);
        } else {
            Log.d("HistoryDatabaseHelper", "Duplicate image path found. Skipping insertion.");
        }

        cursor.close();
        db.close();
    }

    public List<com.example.YinYangEye.model.UploadHistory> getAllHistory() {
        List<com.example.YinYangEye.model.UploadHistory> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);

        if (cursor.moveToFirst()) {
            int imagePathIndex = cursor.getColumnIndex(COLUMN_IMAGE_PATH);
            int confidenceIndex = cursor.getColumnIndex(COLUMN_CONFIDENCE);
            int uploadTimeIndex = cursor.getColumnIndex(COLUMN_UPLOAD_TIME);

            if (imagePathIndex == -1 || confidenceIndex == -1 || uploadTimeIndex == -1) {
                Log.e("HistoryDatabaseHelper", "One or more columns not found in the database table.");
                cursor.close();
                db.close();
                return historyList;
            }

            do {
                String imagePath = cursor.getString(imagePathIndex);
                double confidence = cursor.getDouble(confidenceIndex);
                String uploadTime = cursor.getString(uploadTimeIndex);

                com.example.YinYangEye.model.UploadHistory history = new com.example.YinYangEye.model.UploadHistory(imagePath, confidence, uploadTime);
                historyList.add(history);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return historyList;
    }

    // 删除单个历史记录
    public void deleteHistory(String imagePath) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_NAME, COLUMN_IMAGE_PATH + " = ?", new String[]{imagePath});
        db.close();
    }

    // 删除所有历史记录
    public void deleteAllHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_NAME);
        db.close();
    }
}