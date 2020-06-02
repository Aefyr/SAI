package com.aefyr.sai.utils;

import android.annotation.SuppressLint;
import android.os.Build;
import android.text.TextUtils;

import java.util.Objects;

public class MiuiUtils {

    public static boolean isMiui() {
        return !TextUtils.isEmpty(Utils.getSystemProperty("ro.miui.ui.version.name"));
    }

    public static String getMiuiVersionName() {
        String versionName = Utils.getSystemProperty("ro.miui.ui.version.name");
        return !TextUtils.isEmpty(versionName) ? versionName : "???";
    }

    public static int getMiuiVersionCode() {
        try {
            return Integer.parseInt(Objects.requireNonNull(Utils.getSystemProperty("ro.miui.ui.version.code")));
        } catch (Exception e) {
            return -1;
        }
    }

    public static String getActualMiuiVersion() {
        return Build.VERSION.INCREMENTAL;
    }

    private static int[] parseVersionIntoParts(String version) {
        try {
            String[] versionParts = version.split("\\.");
            int[] intVersionParts = new int[versionParts.length];

            for (int i = 0; i < versionParts.length; i++)
                intVersionParts[i] = Integer.parseInt(versionParts[i]);

            return intVersionParts;
        } catch (Exception e) {
            return new int[]{-1};
        }
    }

    /**
     * @return 0 if versions are equal, values less than 0 if ver1 is lower than ver2, value more than 0 if ver1 is higher than ver2
     */
    private static int compareVersions(String version1, String version2) {
        if (version1.equals(version2))
            return 0;

        int[] version1Parts = parseVersionIntoParts(version1);
        int[] version2Parts = parseVersionIntoParts(version2);

        for (int i = 0; i < version2Parts.length; i++) {
            if (i >= version1Parts.length)
                return -1;

            if (version1Parts[i] < version2Parts[i])
                return -1;

            if (version1Parts[i] > version2Parts[i])
                return 1;
        }

        return 1;
    }

    public static boolean isActualMiuiVersionAtLeast(String targetVer) {
        return compareVersions(getActualMiuiVersion(), targetVer) >= 0;
    }

    @SuppressLint("PrivateApi")
    public static boolean isMiuiOptimizationDisabled() {
        if ("0".equals(Utils.getSystemProperty("persist.sys.miui_optimization")))
            return true;

        try {
            return (boolean) Class.forName("android.miui.AppOpsUtils")
                    .getDeclaredMethod("isXOptMode")
                    .invoke(null);
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean isFixedMiui() {
        return isActualMiuiVersionAtLeast("20.2.20") || isMiuiOptimizationDisabled();
    }
}
