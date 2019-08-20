package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.installer.ApkSourceBuilder;
import com.aefyr.sai.installer.PackageInstallerProvider;
import com.aefyr.sai.installer.SAIPackageInstaller;
import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.utils.Event;
import com.aefyr.sai.utils.PreferencesHelper;

import java.io.File;
import java.util.List;

public class InstallerViewModel extends AndroidViewModel implements SAIPackageInstaller.InstallationStatusListener {
    public static final String EVENT_PACKAGE_INSTALLED = "package_installed";
    public static final String EVENT_INSTALLATION_FAILED = "installation_failed";

    private SAIPackageInstaller mInstaller;
    private Context mContext;
    private PreferencesHelper mPrefsHelper;
    private long mOngoingSessionId;

    public enum InstallerState {
        IDLE, INSTALLING
    }

    private MutableLiveData<InstallerState> mState = new MutableLiveData<>();
    private MutableLiveData<Event<String[]>> mEvents = new MutableLiveData<>();

    public InstallerViewModel(@NonNull Application application) {
        super(application);
        mContext = application;
        mPrefsHelper = PreferencesHelper.getInstance(mContext);
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

        ApkSource apkSource = new ApkSourceBuilder(mContext)
                .fromApkFiles(apkFiles)
                .setSigningEnabled(mPrefsHelper.shouldSignApks())
                .build();

        mOngoingSessionId = mInstaller.createInstallationSession(apkSource);
        mInstaller.startInstallationSession(mOngoingSessionId);
    }

    public void installPackagesFromZip(File zipWithApkFiles) {
        ensureInstallerActuality();

        ApkSource apkSource = new ApkSourceBuilder(mContext)
                .fromZipFile(zipWithApkFiles)
                .setZipExtractionEnabled(mPrefsHelper.shouldSignApks())
                .setSigningEnabled(mPrefsHelper.shouldSignApks())
                .build();

        mOngoingSessionId = mInstaller.createInstallationSession(apkSource);
        mInstaller.startInstallationSession(mOngoingSessionId);
    }

    public void installPackagesFromContentProviderZip(Uri zipContentUri) {
        ensureInstallerActuality();

        ApkSource apkSource = new ApkSourceBuilder(mContext)
                .fromZipContentUri(zipContentUri)
                .setZipExtractionEnabled(mPrefsHelper.shouldSignApks())
                .setSigningEnabled(mPrefsHelper.shouldSignApks())
                .build();

        mOngoingSessionId = mInstaller.createInstallationSession(apkSource);
        mInstaller.startInstallationSession(mOngoingSessionId);
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
        if (installationID != mOngoingSessionId)
            return;

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
