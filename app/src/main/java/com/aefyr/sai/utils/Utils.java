package com.aefyr.sai.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.io.PrintWriter;
import java.io.StringWriter;

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

    public static String throwableToString(Throwable throwable) {
        StringWriter sw = new StringWriter(1024);
        PrintWriter pw = new PrintWriter(sw);

        throwable.printStackTrace(pw);
        pw.close();

        return sw.toString();
    }

}
