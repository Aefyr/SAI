package com.aefyr.sai.installerx;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.resolver.SplitApkSourceMetaResolver;
import com.aefyr.sai.model.common.PackageMeta;

import java.util.ArrayList;
import java.util.List;

public class SplitApkSourceMeta {

    private PackageMeta mPackageMeta;
    private List<SplitCategory> mSplits;
    private List<SplitPart> mHiddenSplits;

    public SplitApkSourceMeta(@Nullable PackageMeta pkgMeta, List<SplitCategory> splits, List<SplitPart> hiddenSplits) {
        mPackageMeta = pkgMeta;
        mSplits = splits;
        mHiddenSplits = hiddenSplits;
    }

    @Nullable
    public PackageMeta packageMeta() {
        return mPackageMeta;
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
