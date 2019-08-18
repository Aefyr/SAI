package com.aefyr.sai.ui.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aefyr.sai.utils.Theme;

@SuppressLint("Registered") //This is only a base class for other activities
public class ThemedActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        Theme.apply(this);
        super.onCreate(savedInstanceState);
    }
}
