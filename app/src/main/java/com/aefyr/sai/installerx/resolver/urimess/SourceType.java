package com.aefyr.sai.installerx.resolver.urimess;

public enum SourceType {
    UNKNOWN,
    /**
     * MessResolutionResult with this SourceType will contain a single zip uri in {@link UriMessResolutionResult#uris()}
     */
    ZIP,

    /**
     * MessResolutionResult with this SourceType will one o more apk file uris in {@link UriMessResolutionResult#uris()}
     */
    APK_FILES
}
