package com.aefyr.sai.viewmodels.factory;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;

public class BatchBackupDialogViewModelFactory implements ViewModelProvider.Factory {

    private Context mAppContext;
    private ArrayList<String> mSelectedPackages;

    public BatchBackupDialogViewModelFactory(@NonNull Context applicationContext, @Nullable ArrayList<String> selectedPackages) {
        mAppContext = applicationContext.getApplicationContext();
        mSelectedPackages = selectedPackages;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        try {
            return modelClass.getConstructor(Context.class, ArrayList.class).newInstance(mAppContext, mSelectedPackages);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
