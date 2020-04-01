package com.aefyr.sai.installerx.splitmeta;

import java.util.Map;

public class UnknownSplitMeta extends SplitMeta {

    private Map<String, String> mManifestAttrs;

    public UnknownSplitMeta(Map<String, String> manifestAttrs) {
        super(manifestAttrs);
        mManifestAttrs = manifestAttrs;
    }

    public Map<String, String> getManifestAttrs() {
        return mManifestAttrs;
    }

}
