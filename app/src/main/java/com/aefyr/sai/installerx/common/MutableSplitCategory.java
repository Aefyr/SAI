package com.aefyr.sai.installerx.common;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MutableSplitCategory implements SplitCategory {

    private Category mCategory;
    private String mName;
    private String mDescription;
    private List<MutableSplitPart> mParts;

    public MutableSplitCategory(Category category, String name, @Nullable String description) {
        mCategory = category;
        mName = name;
        mDescription = description;
        mParts = new ArrayList<>();
    }

    public void setCategory(Category category) {
        mCategory = category;
    }

    @Override
    public Category category() {
        return mCategory;
    }

    @Override
    public String id() {
        return mCategory.id();
    }

    public void setName(String name) {
        mName = name;
    }

    @Override
    public String name() {
        return mName;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    @Nullable
    @Override
    public String description() {
        return mDescription;
    }

    public MutableSplitCategory addPart(MutableSplitPart part) {
        mParts.add(part);
        return this;
    }

    public MutableSplitCategory addParts(Collection<MutableSplitPart> parts) {
        mParts.addAll(parts);
        return this;
    }

    @Override
    public List<SplitPart> parts() {
        return (List<SplitPart>) ((Object) mParts);
    }

    public List<MutableSplitPart> getPartsList() {
        return mParts;
    }

    public SealedSplitCategory seal() {
        List<SealedSplitPart> sealedSplitParts = new ArrayList<>();

        for (MutableSplitPart mutableSplitPart : getPartsList())
            sealedSplitParts.add(mutableSplitPart.seal());

        return new SealedSplitCategory(category(), name(), description(), sealedSplitParts);
    }

}
