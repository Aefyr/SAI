package com.aefyr.sai.installerx.resolver.appmeta.apks;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.resolver.appmeta.AppMeta;
import com.aefyr.sai.installerx.resolver.appmeta.AppMetaExtractor;
import com.aefyr.sai.installerx.resolver.meta.ApkSourceFile;
import com.aefyr.sai.model.backup.SaiExportedAppMeta;
import com.aefyr.sai.model.backup.SaiExportedAppMeta2;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SaiAppMetaExtractor implements AppMetaExtractor {
    private static final String TAG = "SaiMetaExtractor";

    private Context mContext;

    public SaiAppMetaExtractor(Context context) {
        mContext = context.getApplicationContext();
    }

    @Nullable
    @Override
    public AppMeta extract(ApkSourceFile apkSourceFile, ApkSourceFile.Entry baseApkEntry) {
        try {
            boolean seenMetaFile = false;
            AppMeta appMeta = new AppMeta();

            for (ApkSourceFile.Entry entry : apkSourceFile.listEntries()) {

                if (entry.getLocalPath().equals(SaiExportedAppMeta.META_FILE)) {
                    if (seenMetaFile)
                        continue;

                    try {
                        SaiExportedAppMeta meta = SaiExportedAppMeta.deserialize(IOUtils.readStream(apkSourceFile.openEntryInputStream(entry)));
                        appMeta.packageName = meta.packageName();
                        appMeta.appName = meta.label();
                        appMeta.versionName = meta.versionName();
                        appMeta.versionCode = meta.versionCode();
                        seenMetaFile = true;
                    } catch (Exception e) {
                        Log.w(TAG, "Unable to extract meta", e);
                    }
                } else if (entry.getLocalPath().equals(SaiExportedAppMeta2.META_FILE)) {
                    try {
                        SaiExportedAppMeta2 meta = SaiExportedAppMeta2.deserialize(IOUtils.readStream(apkSourceFile.openEntryInputStream(entry)));
                        appMeta.packageName = meta.packageName();
                        appMeta.appName = meta.label();
                        appMeta.versionName = meta.versionName();
                        appMeta.versionCode = meta.versionCode();
                        seenMetaFile = true;
                    } catch (Exception e) {
                        Log.w(TAG, "Unable to extract meta", e);
                    }
                } else if (entry.getLocalPath().equals(SaiExportedAppMeta.ICON_FILE) || entry.getLocalPath().equals(SaiExportedAppMeta2.ICON_FILE)) {
                    File iconFile = Utils.createTempFileInCache(mContext, "SaiZipAppMetaExtractor", "png");
                    if (iconFile == null)
                        continue;

                    try (InputStream in = apkSourceFile.openEntryInputStream(entry); OutputStream out = new FileOutputStream(iconFile)) {
                        IOUtils.copyStream(in, out);
                        appMeta.iconUri = Uri.fromFile(iconFile);
                    } catch (IOException e) {
                        Log.w(TAG, "Unable to extract icon", e);
                    }
                }
            }

            if (seenMetaFile)
                return appMeta;

            return null;
        } catch (Exception e) {
            Log.w(TAG, "Error while extracting meta", e);
            return null;
        }
    }
}
