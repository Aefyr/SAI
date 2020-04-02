package com.aefyr.sai.installerx.resolver;

import com.aefyr.sai.installerx.SplitApkSourceMeta;

import java.io.File;

public interface SplitApkSourceMetaResolver {

    //TODO resolution progress listener would be nice
    SplitApkSourceMeta resolveFor(File apkSourceFile) throws Exception;

}
