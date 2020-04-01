package com.aefyr.sai.installerx.splitmeta;


import androidx.annotation.Nullable;

import com.aefyr.sai.utils.TextUtils;

import java.util.Map;

public class ConfigSplitMeta extends SplitMeta {

    private String mModule;

    public ConfigSplitMeta(Map<String, String> manifestAttrs) {
        super(manifestAttrs);

        mModule = TextUtils.getNullIfEmpty(manifestAttrs.get("configForSplit"));
    }

    @Nullable
    public String getModule() {
        return mModule;
    }

    public boolean isForModule() {
        return getModule() != null;
    }
}
