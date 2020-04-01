package com.aefyr.sai.installerx;

import com.aefyr.sai.model.installerx.SplitApkSourceMeta;

import java.io.File;

public interface SplitApkSourceMetaResolver {

    //TODO resolution progress listener would be nice
    SplitApkSourceMeta resolveFor(File apkSourceFile);

}
