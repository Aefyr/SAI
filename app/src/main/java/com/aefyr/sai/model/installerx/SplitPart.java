package com.aefyr.sai.model.installerx;

import androidx.annotation.Nullable;

public class SplitPart {

    private String mId;
    private String mBackingApkFile;
    private String mName;
    private String mDescription;
    private boolean mRequired;

    public SplitPart(String id, String name, String backingApkFile, @Nullable String description, boolean required) {
        mId = id;
        mBackingApkFile = backingApkFile;
        mName = name;
        mDescription = description;
        mRequired = required;
    }

    public String id() {
        return mId;
    }

    public String backingApkFile() {
        return mBackingApkFile;
    }

    public String name() {
        return mName;
    }

    @Nullable
    public String description() {
        return mDescription;
    }

    public boolean isRequired() {
        return mRequired;
    }

}
