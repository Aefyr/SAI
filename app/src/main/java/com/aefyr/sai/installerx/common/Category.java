package com.aefyr.sai.installerx.common;

public enum Category {
    BASE_APK("base"),
    FEATURE("feature"),
    CONFIG_ABI("config_abi"),
    CONFIG_DENSITY("config_dpi"),
    CONFIG_LOCALE("config_locale"),
    UNKNOWN("unknown");

    private String mId;

    private Category(String id) {
        mId = id;
    }

    public String id() {
        return mId;
    }
}
