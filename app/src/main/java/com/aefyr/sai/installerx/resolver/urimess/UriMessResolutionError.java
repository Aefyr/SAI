package com.aefyr.sai.installerx.resolver.urimess;

public class UriMessResolutionError {

    String mMessage;
    boolean mDoesTryingToInstallNonethelessMakeSense;

    public UriMessResolutionError(String message, boolean doesTryingToInstallNonethelessMakeSense) {
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
