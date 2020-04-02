package com.aefyr.sai.installerx;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.splitmeta.SplitMeta;

//TODO maybe make a read only interface for this
public class SplitPart {

    private SplitMeta mMeta;
    private String mId;
    private String mBackingApkFile;
    private String mName;
    private String mDescription;
    private boolean mRequired;
    private boolean mRecommended;

    public SplitPart(SplitMeta meta, String id, String name, String backingApkFile, @Nullable String description, boolean required, boolean recommended) {
        mMeta = meta;
        mId = id;
        mBackingApkFile = backingApkFile;
        mName = name;
        mDescription = description;
        mRequired = required;
        mRecommended = recommended;
    }

    public SplitMeta meta() {
        return mMeta;
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

    public void setRequired(boolean required) {
        mRequired = required;
    }

    public boolean isRequired() {
        return mRequired;
    }

    public void setRecommended(boolean recommended) {
        mRecommended = recommended;
    }

    public boolean isRecommended() {
        return mRecommended || mRequired;
    }

}
