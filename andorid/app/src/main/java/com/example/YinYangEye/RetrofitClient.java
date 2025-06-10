package com.example.YinYangEye;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static Retrofit retrofit;
    public static String BASE_URL = "http://121.36.33.141:50005"; // 默认值

    // 提供一个方法用于设置 BASE_URL 并创建 Retrofit 实例
    public static void setBaseUrl(String baseUrl) {
        BASE_URL = baseUrl; // 更新 BASE_URL
        if (retrofit != null) {
            // 如果 Retrofit 实例已创建，重新初始化 Retrofit
            retrofit = null;
        }
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}