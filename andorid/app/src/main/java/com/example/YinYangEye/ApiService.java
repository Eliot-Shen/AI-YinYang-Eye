package com.example.YinYangEye;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import okhttp3.Response;
import okhttp3.ResponseBody;

public interface ApiService {
    @GET("get_image")
    Call<ApiResponse> getImage();
    @Multipart
    @POST("/detect")
    Call<ApiResponse> detectFakeOrTrue(@Part MultipartBody.Part file);
}

