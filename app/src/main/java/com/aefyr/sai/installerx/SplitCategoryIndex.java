package com.aefyr.sai.installerx;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SplitCategoryIndex {

    public Map<Category, SplitCategory> mIndex = new HashMap<>();

    public SplitCategoryIndex() {

    }

    @Nullable
    public SplitCategory get(Category category) {
        return mIndex.get(category);
    }

    public SplitCategory getOrCreate(Category category, String name, String description) {
        SplitCategory splitCategory = mIndex.get(category);
        if (splitCategory == null) {
            splitCategory = new SplitCategory(category, name, description);
            mIndex.put(category, splitCategory);
        }

        return splitCategory;
    }

    public List<SplitCategory> toList() {
        return new ArrayList<>(mIndex.values());
    }

}
