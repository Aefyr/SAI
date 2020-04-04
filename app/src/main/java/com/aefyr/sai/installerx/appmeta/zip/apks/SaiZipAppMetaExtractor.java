package com.aefyr.sai.installerx.appmeta.zip.apks;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;

import com.aefyr.sai.installerx.appmeta.AppMeta;
import com.aefyr.sai.installerx.appmeta.zip.ZipAppMetaExtractor;
import com.aefyr.sai.model.backup.SaiExportedAppMeta;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;

public class SaiZipAppMetaExtractor implements ZipAppMetaExtractor {
    private static final String TAG = "SaiMetaExtractor";

    private Context mContext;

    private AppMeta mAppMeta;

    public SaiZipAppMetaExtractor(Context context) {
        mContext = context.getApplicationContext();
        mAppMeta = new AppMeta();
    }

    @Override
    public boolean wantEntry(ZipEntry entry) {
        return entry.getName().equals(SaiExportedAppMeta.META_FILE) || entry.getName().equals(SaiExportedAppMeta.ICON_FILE);
    }

    @Override
    public void consumeEntry(ZipEntry entry, InputStream entryInputStream) {
        if (entry.getName().equals(SaiExportedAppMeta.META_FILE)) {
            try {
                SaiExportedAppMeta meta = new Gson().fromJson(IOUtils.readStream(entryInputStream, StandardCharsets.UTF_8), SaiExportedAppMeta.class);
                mAppMeta.packageName = meta.packageName();
                mAppMeta.appName = meta.label();
                mAppMeta.versionName = meta.versionName();
                mAppMeta.versionCode = meta.versionCode();
            } catch (Exception e) {
                Log.w(TAG, "Unable to extract meta", e);
            }
        }

        if (entry.getName().equals(SaiExportedAppMeta.ICON_FILE)) {
            File iconFile = Utils.createTempFileInCache(mContext, "SaiZipAppMetaExtractor", "png");
            if (iconFile == null)
                return;

            try (InputStream in = entryInputStream; OutputStream out = new FileOutputStream(iconFile)) {
                IOUtils.copyStream(in, out);
                mAppMeta.iconUri = Uri.fromFile(iconFile);
            } catch (IOException e) {
                Log.w(TAG, "Unable to extract icon", e);
            }
        }
    }

    @NonNull
    @Override
    public AppMeta buildMeta() {
        return mAppMeta;
    }
}
