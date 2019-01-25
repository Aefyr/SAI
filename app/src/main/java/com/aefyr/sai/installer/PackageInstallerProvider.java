package com.aefyr.sai.installer;

import android.content.Context;
import android.widget.Toast;

import com.aefyr.sai.R;
import com.aefyr.sai.installer.rooted.RootedSAIPackageInstaller;
import com.aefyr.sai.installer.rootless.RootlessSAIPackageInstaller;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Root;

public class PackageInstallerProvider {
    public static SAIPackageInstaller getInstaller(Context c) {
        if (PreferencesHelper.getInstance(c).shouldUseRoot()) {
            if (Root.requestRoot())
                return RootedSAIPackageInstaller.getInstance(c);

            Toast.makeText(c, R.string.installer_no_root, Toast.LENGTH_LONG).show();
        }
        return RootlessSAIPackageInstaller.getInstance(c);
    }
}
