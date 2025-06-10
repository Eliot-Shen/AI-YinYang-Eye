package com.example.YinYangEye.model;

public class UploadHistory {
    private String imagePath;
    private double confidence;
    private String uploadTime;

    // Constructor
    public UploadHistory(String imagePath, double confidence, String uploadTime) {
        this.imagePath = imagePath;
        this.confidence = confidence;
        this.uploadTime = uploadTime;
    }

    // Getters and Setters
    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(String uploadTime) {
        this.uploadTime = uploadTime;
    }
}