package com.aefyr.sai.installerx;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.appmeta.AppMeta;
import com.aefyr.sai.installerx.resolver.SplitApkSourceMetaResolver;

import java.util.ArrayList;
import java.util.List;

public class SplitApkSourceMeta {

    private AppMeta mAppMeta;
    private List<SplitCategory> mSplits;
    private List<SplitPart> mHiddenSplits;

    public SplitApkSourceMeta(@Nullable AppMeta appMeta, List<SplitCategory> splits, List<SplitPart> hiddenSplits) {
        mAppMeta = appMeta;
        mSplits = splits;
        mHiddenSplits = hiddenSplits;
    }

    @Nullable
    public AppMeta appMeta() {
        return mAppMeta;
    }

    /**
     * @return splits that may be shown to user for toggling non-required ones
     */
    public List<SplitCategory> splits() {
        return mSplits;
    }

    public List<SplitPart> flatSplits() {
        ArrayList<SplitPart> parts = new ArrayList<>();

        for (SplitCategory category : splits()) {
            parts.addAll(category.parts());
        }

        return parts;
    }

    /**
     * @return splits that shouldn't be shown to user if a {@link SplitApkSourceMetaResolver} decides so
     */
    public List<SplitPart> hiddenSplits() {
        return mHiddenSplits;
    }

}
