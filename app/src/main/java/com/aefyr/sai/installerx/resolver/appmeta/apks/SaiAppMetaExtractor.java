package com.aefyr.sai.installerx.resolver.appmeta.apks;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.resolver.appmeta.AppMeta;
import com.aefyr.sai.installerx.resolver.appmeta.AppMetaExtractor;
import com.aefyr.sai.installerx.resolver.meta.ApkSourceFile;
import com.aefyr.sai.model.backup.SaiExportedAppMeta;
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

    private AppMeta mAppMeta;

    private boolean mSeenMetaFile = false;
    private boolean mSeenIconFile = false;

    public SaiAppMetaExtractor(Context context) {
        mContext = context.getApplicationContext();
        mAppMeta = new AppMeta();
    }

    @Override
    public boolean wantEntry(ApkSourceFile.Entry entry) {
        return entry.getLocalPath().equals(SaiExportedAppMeta.META_FILE) || entry.getLocalPath().equals(SaiExportedAppMeta.ICON_FILE);
    }

    @Override
    public void consumeEntry(ApkSourceFile.Entry entry, InputStream entryInputStream) {
        if (entry.getLocalPath().equals(SaiExportedAppMeta.META_FILE)) {
            try {
                SaiExportedAppMeta meta = SaiExportedAppMeta.deserialize(IOUtils.readStream(entryInputStream));
                mAppMeta.packageName = meta.packageName();
                mAppMeta.appName = meta.label();
                mAppMeta.versionName = meta.versionName();
                mAppMeta.versionCode = meta.versionCode();
                mSeenMetaFile = true;
            } catch (Exception e) {
                Log.w(TAG, "Unable to extract meta", e);
            }
        }

        if (entry.getLocalPath().equals(SaiExportedAppMeta.ICON_FILE)) {
            File iconFile = Utils.createTempFileInCache(mContext, "SaiZipAppMetaExtractor", "png");
            if (iconFile == null)
                return;

            try (InputStream in = entryInputStream; OutputStream out = new FileOutputStream(iconFile)) {
                IOUtils.copyStream(in, out);
                mAppMeta.iconUri = Uri.fromFile(iconFile);
                mSeenIconFile = true;
            } catch (IOException e) {
                Log.w(TAG, "Unable to extract icon", e);
            }
        }
    }

    @Nullable
    @Override
    public AppMeta buildMeta() {
        if (mSeenMetaFile && mSeenIconFile)
            return mAppMeta;

        return null;
    }
}
