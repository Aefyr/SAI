package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class BackupViewModel extends AndroidViewModel {

    private Context mContext;

    public BackupViewModel(@NonNull Application application) {
        super(application);
        mContext = application;
    }
}
