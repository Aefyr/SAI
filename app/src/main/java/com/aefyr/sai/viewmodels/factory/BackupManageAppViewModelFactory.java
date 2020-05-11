package com.aefyr.sai.viewmodels.factory;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class BackupManageAppViewModelFactory implements ViewModelProvider.Factory {

    private Context mAppContext;
    private String mPackage;

    public BackupManageAppViewModelFactory(Context context, String pkg) {
        mAppContext = context.getApplicationContext();
        mPackage = pkg;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            return modelClass.getConstructor(Context.class, String.class).newInstance(mAppContext, mPackage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
