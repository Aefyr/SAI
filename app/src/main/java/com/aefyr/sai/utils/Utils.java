package com.aefyr.sai.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.annotation.Nullable;

public class Utils {

    @Nullable
    public static String getAppLabel(Context c, String packageName) {
        try {
            PackageManager pm = c.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            return pm.getApplicationLabel(appInfo).toString();
        } catch (Exception e) {
            return null;
        }
    }
}
