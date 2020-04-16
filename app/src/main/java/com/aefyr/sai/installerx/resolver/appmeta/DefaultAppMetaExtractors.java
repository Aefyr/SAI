package com.aefyr.sai.installerx.resolver.appmeta;

import android.content.Context;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.resolver.appmeta.apks.SaiAppMetaExtractor;
import com.aefyr.sai.installerx.resolver.appmeta.xapk.XapkAppMetaExtractor;

public class DefaultAppMetaExtractors {

    @Nullable
    public static AppMetaExtractor fromArchiveExtension(Context context, @Nullable String archiveExtension) {
        if (archiveExtension == null)
            return null;

        switch (archiveExtension.toLowerCase()) {
            case "xapk":
                return new XapkAppMetaExtractor(context);
            case "apks":
                return new SaiAppMetaExtractor(context);
            default:
                return null;
        }
    }

}
