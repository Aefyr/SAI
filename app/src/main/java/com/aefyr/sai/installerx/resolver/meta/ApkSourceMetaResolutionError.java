package com.aefyr.sai.installerx.resolver.meta;

public class ApkSourceMetaResolutionError {


    String mMessage;
    boolean mDoesTryingToInstallNonethelessMakeSense;

    public ApkSourceMetaResolutionError(String message, boolean doesTryingToInstallNonethelessMakeSense) {
        mMessage = message;
        mDoesTryingToInstallNonethelessMakeSense = doesTryingToInstallNonethelessMakeSense;
    }

    public String message() {
        return mMessage;
    }

    public boolean doesTryingToInstallNonethelessMakeSense() {
        return mDoesTryingToInstallNonethelessMakeSense;
    }


}
