package com.aefyr.sai.model.installerx;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SplitCategory {

    private String mId;
    private String mName;
    private String mDescription;
    private List<SplitPart> mParts;

    public SplitCategory(String id, String name, @Nullable String description) {
        mId = id;
        mName = name;
        mDescription = description;
        mParts = new ArrayList<>();
    }

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
