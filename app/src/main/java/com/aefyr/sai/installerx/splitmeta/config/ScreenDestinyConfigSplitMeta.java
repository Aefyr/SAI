package com.aefyr.sai.installerx.splitmeta.config;

import android.util.DisplayMetrics;

import androidx.annotation.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ScreenDestinyConfigSplitMeta extends ConfigSplitMeta {

    private static final String LDPI = "ldpi";
    private static final String MDPI = "mdpi";
    private static final String TVDPI = "tvdpi";
    private static final String HDPI = "hdpi";
    private static final String XHDPI = "xhdpi";
    private static final String XXHDPI = "xxhdpi";
    private static final String XXXHDPI = "xxxhdpi";

    private static final Map<String, Integer> DENSITY_NAME_TO_DENSITY = new HashMap<>();

    static {
        DENSITY_NAME_TO_DENSITY.put(LDPI, DisplayMetrics.DENSITY_LOW);
        DENSITY_NAME_TO_DENSITY.put(MDPI, DisplayMetrics.DENSITY_MEDIUM);
        DENSITY_NAME_TO_DENSITY.put(TVDPI, DisplayMetrics.DENSITY_TV);
        DENSITY_NAME_TO_DENSITY.put(HDPI, DisplayMetrics.DENSITY_HIGH);
        DENSITY_NAME_TO_DENSITY.put(XHDPI, DisplayMetrics.DENSITY_XHIGH);
        DENSITY_NAME_TO_DENSITY.put(XXHDPI, DisplayMetrics.DENSITY_XXHIGH);
        DENSITY_NAME_TO_DENSITY.put(XXXHDPI, DisplayMetrics.DENSITY_XXXHIGH);
    }

    private String mDensityName;
    private int mDensity;

    public ScreenDestinyConfigSplitMeta(Map<String, String> manifestAttrs) {
        super(manifestAttrs);

        mDensityName = Objects.requireNonNull(getDensityFromSplitName(Objects.requireNonNull(splitName())));
        mDensity = Objects.requireNonNull(DENSITY_NAME_TO_DENSITY.get(mDensityName));
    }

    public int density() {
        return mDensity;
    }

    public String densityName() {
        return mDensityName;
    }

    public static boolean isScreenDensitySplit(String splitName) {
        return getDensityFromSplitName(splitName) != null;
    }

    @Nullable
    public static String getDensityFromSplitName(String splitName) {
        int configPartIndex = splitName.lastIndexOf("config.");
        if (configPartIndex == -1 || (configPartIndex != 0 && splitName.charAt(configPartIndex - 1) != '.'))
            return null;

        String densityName = splitName.substring(configPartIndex + ("config.".length()));
        if (DENSITY_NAME_TO_DENSITY.containsKey(densityName))
            return densityName;

        return null;
    }

}
