package com.aefyr.sai.installer.rooted;

import android.content.Context;

import com.aefyr.sai.R;
import com.aefyr.sai.installer.ShellSAIPackageInstaller;
import com.aefyr.sai.shell.Shell;
import com.aefyr.sai.shell.SuShell;

public class RootedSAIPackageInstaller extends ShellSAIPackageInstaller {
    private static RootedSAIPackageInstaller sInstance;

    public static RootedSAIPackageInstaller getInstance(Context c) {
        synchronized (RootedSAIPackageInstaller.class) {
            return sInstance != null ? sInstance : new RootedSAIPackageInstaller(c);
        }
    }

    private RootedSAIPackageInstaller(Context c) {
        super(c);
        sInstance = this;
    }

    @Override
    protected Shell getShell() {
        return SuShell.getInstance();
    }

    @Override
    protected String getInstallerName() {
        return "Rooted";
    }

    @Override
    protected String getShellUnavailableMessage() {
        return getContext().getString(R.string.installer_error_root_no_root);
    }
}
