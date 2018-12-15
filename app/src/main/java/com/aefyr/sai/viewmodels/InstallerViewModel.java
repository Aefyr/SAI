package com.aefyr.sai.viewmodels;

import android.app.Application;

import com.aefyr.sai.installer.LivePackagesInstaller;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class InstallerViewModel extends AndroidViewModel {
    private LivePackagesInstaller mInstaller;

    public InstallerViewModel(@NonNull Application application) {
        super(application);
        mInstaller = new LivePackagesInstaller(application.getApplicationContext());
    }

    public void installPackages(List<File> apkFiles) {
        mInstaller.installPackages(apkFiles);
    }

    public LivePackagesInstaller getInstaller() {
        return mInstaller;
    }

    public void resetInstaller() {
        mInstaller.reset();
    }
}
