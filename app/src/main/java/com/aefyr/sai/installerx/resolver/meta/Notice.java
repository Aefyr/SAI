package com.aefyr.sai.installerx.resolver.meta;

import androidx.annotation.Nullable;

public class Notice {

    private String mType;
    private String mContext;
    private String mText;

    public Notice(String type, @Nullable String context, String text) {
        mType = type;
        mContext = context;
        mText = text;
    }

    public String type() {
        return mType;
    }

    @Nullable
    public String context() {
        return mContext;
    }

    public String text() {
        return mText;
    }

}
