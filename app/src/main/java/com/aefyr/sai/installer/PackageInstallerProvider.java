package com.aefyr.sai.installer;

import android.content.Context;

import com.aefyr.sai.installer.rooted.RootedSAIPackageInstaller;
import com.aefyr.sai.installer.rootless.RootlessSAIPackageInstaller;
import com.aefyr.sai.installer.shizuku.ShizukuSAIPackageInstaller;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.PreferencesValues;

public class PackageInstallerProvider {
    public static SAIPackageInstaller getInstaller(Context c) {

        switch (PreferencesHelper.getInstance(c).getInstaller()) {
            case PreferencesValues.INSTALLER_ROOTLESS:
                return RootlessSAIPackageInstaller.getInstance(c);
            case PreferencesValues.INSTALLER_ROOTED:
                return RootedSAIPackageInstaller.getInstance(c);
            case PreferencesValues.INSTALLER_SHIZUKU:
                return ShizukuSAIPackageInstaller.getInstance(c);
        }

        return RootlessSAIPackageInstaller.getInstance(c);
    }
}
