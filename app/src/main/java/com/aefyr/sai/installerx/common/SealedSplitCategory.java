package com.aefyr.sai.installerx.common;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

public class SealedSplitCategory implements SplitCategory {

    private Category mCategory;
    private String mName;
    private String mDescription;
    private List<SplitPart> mParts;

    public SealedSplitCategory(Category category, String name, @Nullable String description, List<SealedSplitPart> parts) {
        mCategory = category;
        mName = name;
        mDescription = description;
        mParts = Collections.unmodifiableList(parts);
    }

    @Override
    public Category category() {
        return mCategory;
    }

    @Override
    public String id() {
        return mCategory.id();
    }

    @Override
    public String name() {
        return mName;
    }

    @Nullable
    @Override
    public String description() {
        return mDescription;
    }

    @Override
    public List<SplitPart> parts() {
        return mParts;
    }

}
