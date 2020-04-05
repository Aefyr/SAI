package com.aefyr.sai.installerx.resolver.meta;

import com.aefyr.sai.installerx.SplitApkSourceMeta;

public interface SplitApkSourceMetaResolver {

    //TODO resolution progress listener would be nice

    /**
     * @param apkSourceFile    apk source file
     * @return
     * @throws Exception
     */
    SplitApkSourceMeta resolveFor(ApkSourceFile apkSourceFile) throws Exception;

}
