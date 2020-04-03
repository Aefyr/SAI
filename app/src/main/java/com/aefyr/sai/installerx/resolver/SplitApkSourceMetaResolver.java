package com.aefyr.sai.installerx.resolver;

import com.aefyr.sai.installerx.SplitApkSourceMeta;

import java.io.File;

public interface SplitApkSourceMetaResolver {

    //TODO resolution progress listener would be nice

    /**
     * @param apkSourceFile    apk source file
     * @param originalFileName the original file name of that file (for example if the actual file is a file descriptor link)
     * @return
     * @throws Exception
     */
    SplitApkSourceMeta resolveFor(File apkSourceFile, String originalFileName) throws Exception;

}
