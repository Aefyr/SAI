package com.aefyr.sai.installer.shizuku;

import android.content.Context;

import com.aefyr.sai.installer.ShellSAIPackageInstaller;
import com.aefyr.sai.shell.Shell;
import com.aefyr.sai.shell.ShizukuShell;

public class ShizukuSAIPackageInstaller extends ShellSAIPackageInstaller {
    private static ShizukuSAIPackageInstaller sInstance;

    public static ShizukuSAIPackageInstaller getInstance(Context c) {
        synchronized (ShizukuSAIPackageInstaller.class) {
            return sInstance != null ? sInstance : new ShizukuSAIPackageInstaller(c);
        }
    }

    private ShizukuSAIPackageInstaller(Context c) {
        super(c);
        sInstance = this;
    }

    @Override
    protected Shell getShell() {
        return ShizukuShell.getInstance();
    }

    @Override
    protected String getInstallerName() {
        return "Shizuku";
    }
}
