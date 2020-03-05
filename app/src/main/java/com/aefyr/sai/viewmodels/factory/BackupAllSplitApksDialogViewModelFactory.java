package com.aefyr.sai.viewmodels.factory;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

public class BackupAllSplitApksDialogViewModelFactory implements ViewModelProvider.Factory {

    private Context mAppContext;
    private ArrayList<String> mPackages;

    public BackupAllSplitApksDialogViewModelFactory(@NonNull Context applicationContext, @Nullable ArrayList<String> packages) {
        mAppContext = applicationContext.getApplicationContext();
        mPackages = packages;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            return modelClass.getConstructor(Context.class, ArrayList.class).newInstance(mAppContext, mPackages);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
