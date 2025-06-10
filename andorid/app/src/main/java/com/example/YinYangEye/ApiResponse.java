package com.example.YinYangEye;

import com.google.gson.annotations.SerializedName;

public class ApiResponse {
    @SerializedName("message")
    private String message;

    @SerializedName("confidence")
    private double confidence;

    @SerializedName("error")
    private String error;

    @SerializedName("image")
    private String image;

    public String getMessage() {
        return message;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getError() {
        return error;
    }

    public String getImage() {
        return image;
    }
}