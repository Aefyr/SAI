package com.aefyr.sai.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.utils.Theme;

public class DarkLightThemeSelectionViewModel extends AndroidViewModel {

    private Theme mThemeManager;

    private MutableLiveData<Theme.ThemeDescriptor> mLightTheme = new MutableLiveData<>();
    private MutableLiveData<Theme.ThemeDescriptor> mDarkTheme = new MutableLiveData<>();

    public DarkLightThemeSelectionViewModel(@NonNull Application application) {
        super(application);

        mThemeManager = Theme.getInstance(getApplication());
        mLightTheme.setValue(mThemeManager.getLightTheme());
        mDarkTheme.setValue(mThemeManager.getDarkTheme());
    }

    public LiveData<Theme.ThemeDescriptor> getLightTheme() {
        return mLightTheme;
    }

    public void setLightTheme(Theme.ThemeDescriptor lightTheme) {
        mLightTheme.setValue(lightTheme);
    }

    public LiveData<Theme.ThemeDescriptor> getDarkTheme() {
        return mDarkTheme;
    }

    public void setDarkTheme(Theme.ThemeDescriptor darkTheme) {
        mDarkTheme.setValue(darkTheme);
    }
}

