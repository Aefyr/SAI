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

    /**
     * @param id id is equal to the entry name in an archive
     */
    public SplitPart(SplitMeta meta, String id, String name, @Nullable String description, boolean required, boolean recommended) {
        mMeta = meta;
        mId = id;
        mName = name;
        mDescription = description;
        mRequired = required;
        mRecommended = recommended;
    }

    public SplitMeta meta() {
        return mMeta;
    }

    /**
     * Id is equal to the entry name in an archive
     *
     * @return id of this part
     */
    public String id() {
        return mId;
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
