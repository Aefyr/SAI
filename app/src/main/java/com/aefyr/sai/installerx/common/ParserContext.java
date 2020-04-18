package com.aefyr.sai.installerx.common;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.resolver.appmeta.AppMeta;
import com.aefyr.sai.installerx.resolver.meta.Notice;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ParserContext {

    private Map<Category, MutableSplitCategory> mIndex = new HashMap<>();
    private List<MutableSplitCategory> mCategories = new ArrayList<>();
    private List<Notice> mNotices = new ArrayList<>();
    private AppMeta mAppMeta;

    public ParserContext() {

    }

    @Nullable
    public MutableSplitCategory getCategory(Category category) {
        return mIndex.get(category);
    }

    public MutableSplitCategory getOrCreateCategory(Category category, String name, String description) {
        MutableSplitCategory splitCategory = mIndex.get(category);
        if (splitCategory == null) {
            splitCategory = new MutableSplitCategory(category, name, description);
            mIndex.put(category, splitCategory);
            mCategories.add(splitCategory);
        }

        return splitCategory;
    }

    /**
     * @return a reference to the list of categories in this ParserContext
     */
    public List<MutableSplitCategory> getCategoriesList() {
        return mCategories;
    }

    public List<SplitCategory> sealCategories() {
        List<SplitCategory> sealedCategories = new ArrayList<>();

        for (MutableSplitCategory mutableSplitCategory : mCategories) {
            sealedCategories.add(mutableSplitCategory.seal());
        }

        return sealedCategories;
    }

    public void addNotice(Notice notice) {
        mNotices.add(notice);
    }

    public List<Notice> getNotices() {
        return mNotices;
    }

    public void setAppMeta(AppMeta appMeta) {
        mAppMeta = appMeta;
    }

    public AppMeta getAppMeta() {
        return mAppMeta;
    }

}
