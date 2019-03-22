package com.aefyr.sai.installer.rooted;

import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.util.Log;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;
import com.aefyr.sai.installer.SAIPackageInstaller;
import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.utils.Root;
import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RootedSAIPackageInstaller extends SAIPackageInstaller {
    private static final String TAG = "RootedSAIPI";

    @SuppressLint("StaticFieldLeak")//This is application context, lul
    private static RootedSAIPackageInstaller sInstance;

    private AtomicBoolean mIsAwaitingBroadcast = new AtomicBoolean(false);

    private BroadcastReceiver mPackageInstalledBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, intent.toString());
            if (!mIsAwaitingBroadcast.get())
                return;

            String installedPackage = "null";
            try {
                installedPackage = intent.getDataString().replace("package:", "");
                String installerPackage = getContext().getPackageManager().getInstallerPackageName(installedPackage);
                Log.d(TAG, "installerPackage=" + installerPackage);
                if (!installerPackage.equals(BuildConfig.APPLICATION_ID))
                    return;
            } catch (Exception e) {
                Crashlytics.logException(e);
                Log.wtf(TAG, e);
            }

            mIsAwaitingBroadcast.set(false);
            dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_SUCCEED, installedPackage);
            installationCompleted();
        }
    };

    public static RootedSAIPackageInstaller getInstance(Context c) {
        return sInstance != null ? sInstance : new RootedSAIPackageInstaller(c);
    }

    private RootedSAIPackageInstaller(Context c) {
        super(c);
        sInstance = this;
        IntentFilter packageAddedFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        packageAddedFilter.addDataScheme("package");
        getContext().registerReceiver(mPackageInstalledBroadcastReceiver, packageAddedFilter);
    }

    @SuppressLint("DefaultLocale")
    @Override
    protected void installApkFiles(ApkSource apkSource) {
        try {
            if (!Root.requestRoot()) {
                //I don't know if this can even happen, because InstallerViewModel calls PackageInstallerProvider.getInstaller, which checks root access and returns correct installer in response, before every installation
                dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED, getContext().getString(R.string.installer_error_root, getContext().getString(R.string.installer_error_root_no_root)));
                installationCompleted();
                return;
            }

            String result = ensureCommandSucceeded(Root.exec(String.format("pm install-create -r -i %s", BuildConfig.APPLICATION_ID)));
            Pattern sessionIdPattern = Pattern.compile("(\\d+)");
            Matcher sessionIdMatcher = sessionIdPattern.matcher(result);
            sessionIdMatcher.find();
            int sessionId = Integer.parseInt(sessionIdMatcher.group(1));

            while (apkSource.nextApk())
                ensureCommandSucceeded(Root.exec(String.format("pm install-write -S %d %d \"%s\"", apkSource.getApkLength(), sessionId, apkSource.getApkName()), apkSource.openApkInputStream()));

            mIsAwaitingBroadcast.set(true);
            Root.Result installationResult = Root.exec(String.format("pm install-commit %d ", sessionId));
            if (!installationResult.isSuccessful()) {
                mIsAwaitingBroadcast.set(false);
                dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED, getContext().getString(R.string.installer_error_root, installationResult.toString()));
                installationCompleted();
            }
        } catch (Exception e) {
            Log.w(TAG, e);
            dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED, getContext().getString(R.string.installer_error_root, e.getMessage()));
            installationCompleted();
        }
    }

    private String ensureCommandSucceeded(Root.Result result) {
        if (!result.isSuccessful())
            throw new RuntimeException(result.toString());
        return result.out;
    }

    private String getPackageNameFromApk(List<File> apkFiles) {
        for (File apkFile : apkFiles) {
            PackageInfo packageInfo = getContext().getPackageManager().getPackageArchiveInfo(apkFile.getAbsolutePath(), 0);
            if (packageInfo != null)
                return packageInfo.packageName;
        }
        return "null";
    }
}
