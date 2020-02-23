package com.aefyr.sai.utils;

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
            return Integer.parseInt(Objects.requireNonNull(Utils.getSystemProperty("ro.miui.ui.version.name")));
        } catch (Exception e) {
            return -1;
        }
    }
}
