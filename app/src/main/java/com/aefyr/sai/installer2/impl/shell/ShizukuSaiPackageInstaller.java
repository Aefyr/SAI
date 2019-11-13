package com.aefyr.sai.installer2.impl.shell;

import android.content.Context;

import com.aefyr.sai.R;
import com.aefyr.sai.shell.Shell;
import com.aefyr.sai.shell.ShizukuShell;

public class ShizukuSaiPackageInstaller extends ShellSaiPackageInstaller {

    private static ShizukuSaiPackageInstaller sInstance;

    public static ShizukuSaiPackageInstaller getInstance(Context c) {
        synchronized (ShizukuSaiPackageInstaller.class) {
            return sInstance != null ? sInstance : new ShizukuSaiPackageInstaller(c);
        }
    }

    private ShizukuSaiPackageInstaller(Context c) {
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

    @Override
    protected String getShellUnavailableMessage() {
        return getContext().getString(R.string.installer_error_shizuku_unavailable);
    }

    @Override
    protected String tag() {
        return "ShizukuSaiPi";
    }
}
