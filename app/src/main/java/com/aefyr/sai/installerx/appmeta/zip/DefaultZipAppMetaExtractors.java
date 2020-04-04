package com.aefyr.sai.installerx.appmeta.zip;

import android.content.Context;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.appmeta.zip.apks.SaiZipAppMetaExtractor;
import com.aefyr.sai.installerx.appmeta.zip.xapk.XapkZipAppMetaExtractor;

public class DefaultZipAppMetaExtractors {

    @Nullable
    public static ZipAppMetaExtractor fromArchiveExtension(Context context, @Nullable String archiveExtension) {
        if (archiveExtension == null)
            return null;

        switch (archiveExtension.toLowerCase()) {
            case "xapk":
                return new XapkZipAppMetaExtractor(context);
            case "apks":
                return new SaiZipAppMetaExtractor(context);
        }

        return null;
    }

}
