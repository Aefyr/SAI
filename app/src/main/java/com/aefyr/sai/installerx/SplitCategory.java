package com.aefyr.sai.installerx;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SplitCategory {

    private Category mCategory;
    private String mName;
    private String mDescription;
    private List<SplitPart> mParts;

    public SplitCategory(Category category, String name, @Nullable String description) {
        mCategory = category;
        mName = name;
        mDescription = description;
        mParts = new ArrayList<>();
    }

    public Category category() {
        return mCategory;
    }

    public String id() {
        return mCategory.id();
    }

    public String name() {
        return mName;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    @Nullable
    public String description() {
        return mDescription;
    }

    public SplitCategory addPart(SplitPart part) {
        mParts.add(part);
        return this;
    }

    public SplitCategory addParts(Collection<SplitPart> parts) {
        mParts.addAll(parts);
        return this;
    }

    public List<SplitPart> parts() {
        return mParts;
    }


}
