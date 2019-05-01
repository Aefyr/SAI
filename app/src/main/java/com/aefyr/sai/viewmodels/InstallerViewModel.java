package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.installer.PackageInstallerProvider;
import com.aefyr.sai.installer.SAIPackageInstaller;
import com.aefyr.sai.model.apksource.DefaultApkSource;
import com.aefyr.sai.model.apksource.ZipApkSource;
import com.aefyr.sai.model.filedescriptor.ContentUriFileDescriptor;
import com.aefyr.sai.model.filedescriptor.FileDescriptor;
import com.aefyr.sai.model.filedescriptor.NormalFileDescriptor;
import com.aefyr.sai.utils.Event;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class InstallerViewModel extends AndroidViewModel implements SAIPackageInstaller.InstallationStatusListener {
    public static final String EVENT_PACKAGE_INSTALLED = "package_installed";
    public static final String EVENT_INSTALLATION_FAILED = "installation_failed";

    private SAIPackageInstaller mInstaller;
    private Context mContext;

    public enum InstallerState {
        IDLE, INSTALLING
    }

    private MutableLiveData<InstallerState> mState = new MutableLiveData<>();
    private MutableLiveData<Event<String[]>> mEvents = new MutableLiveData<>();

    public InstallerViewModel(@NonNull Application application) {
        super(application);
        mContext = application;
        ensureInstallerActuality();
    }

    public LiveData<InstallerState> getState() {
        return mState;
    }

    public LiveData<Event<String[]>> getEvents() {
        return mEvents;
    }

    public void installPackages(List<File> apkFiles) {
        ensureInstallerActuality();

        ArrayList<FileDescriptor> descriptors = new ArrayList<>(apkFiles.size());
        for (File f : apkFiles)
            descriptors.add(new NormalFileDescriptor(f));

        mInstaller.startInstallationSession(mInstaller.createInstallationSession(new DefaultApkSource(descriptors)));
    }

    public void installPackagesFromZip(File zipWithApkFiles) {
        ensureInstallerActuality();
        mInstaller.startInstallationSession(mInstaller.createInstallationSession(new ZipApkSource(mContext, new NormalFileDescriptor(zipWithApkFiles))));
    }

    public void installPackagesFromContentProviderZip(Uri zipContentUri) {
        ensureInstallerActuality();
        mInstaller.startInstallationSession(mInstaller.createInstallationSession(new ZipApkSource(mContext, new ContentUriFileDescriptor(mContext, zipContentUri))));
    }

    private void ensureInstallerActuality() {
        SAIPackageInstaller actualInstaller = PackageInstallerProvider.getInstaller(mContext);
        if (actualInstaller != mInstaller) {
            if (mInstaller != null)
                mInstaller.removeStatusListener(this);

            mInstaller = actualInstaller;
            mInstaller.addStatusListener(this);
            mState.setValue(mInstaller.isInstallationInProgress() ? InstallerState.INSTALLING : InstallerState.IDLE);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mInstaller.removeStatusListener(this);
    }

    @Override
    public void onStatusChanged(long installationID, SAIPackageInstaller.InstallationStatus status, @Nullable String packageNameOrErrorDescription) {
        switch (status) {
            case QUEUED:
            case INSTALLING:
                mState.setValue(InstallerState.INSTALLING);
                break;
            case INSTALLATION_SUCCEED:
                mState.setValue(InstallerState.IDLE);
                mEvents.setValue(new Event<>(new String[]{EVENT_PACKAGE_INSTALLED, packageNameOrErrorDescription}));
                break;
            case INSTALLATION_FAILED:
                mState.setValue(InstallerState.IDLE);
                mEvents.setValue(new Event<>(new String[]{EVENT_INSTALLATION_FAILED, packageNameOrErrorDescription}));
                break;
        }
    }
}
