package com.aefyr.sai.model.backup;

import com.aefyr.sai.model.common.AppFeature;

public class SimpleAppFeature implements AppFeature {

    private String mText;

    public SimpleAppFeature(String text) {
        mText = text;
    }

    @Override
    public CharSequence toText() {
        return mText;
    }
}
