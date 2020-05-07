package com.aefyr.sai.installerx.resolver.meta;

public interface SplitApkSourceMetaResolver {

    //TODO resolution progress listener would be nice

    /**
     * @param apkSourceFile apk source file
     * @return
     * @throws Exception
     */
    ApkSourceMetaResolutionResult resolveFor(ApkSourceFile apkSourceFile) throws Exception;

}
