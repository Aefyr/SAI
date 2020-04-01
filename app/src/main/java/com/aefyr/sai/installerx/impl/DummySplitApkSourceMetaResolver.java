package com.aefyr.sai.installerx.impl;

import android.content.Context;

import com.aefyr.sai.installerx.SplitApkSourceMetaResolver;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.model.installerx.SplitApkSourceMeta;
import com.aefyr.sai.model.installerx.SplitCategory;
import com.aefyr.sai.model.installerx.SplitPart;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DummySplitApkSourceMetaResolver implements SplitApkSourceMetaResolver {

    private Context mContext;

    public DummySplitApkSourceMetaResolver(Context context) {
        mContext = context.getApplicationContext();
    }

    @Override
    public SplitApkSourceMeta resolveFor(File apkSourceFile) {
        List<SplitCategory> dummyCategories = new ArrayList<>();
        dummyCategories.add(new SplitCategory("dummy", "Dummy", "Dummy category desc")
                .addPart(new SplitPart("dummypart1", "Dummy part 1", "fake1", "Dummy part 1 desc", false))
                .addPart(new SplitPart("dummypart2", "Dummy part 2", "fake2", "Dummy part 2 desc", true))
                .addPart(new SplitPart("dummypart3", "Dummy part 3", "fake3", "Dummy part 3 desc", false)));

        return new SplitApkSourceMeta(PackageMeta.forPackage(mContext, mContext.getPackageName()), dummyCategories, Collections.emptyList());
    }

}
