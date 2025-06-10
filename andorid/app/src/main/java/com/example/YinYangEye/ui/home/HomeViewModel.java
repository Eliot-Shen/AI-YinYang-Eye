package com.example.YinYangEye.ui.home;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class HomeViewModel extends ViewModel {
    private final MutableLiveData<String> text = new MutableLiveData<>();

    public LiveData<String> getText() {
        return text;
    }

    public void setText(String content) {
        text.setValue(content);
    }
}