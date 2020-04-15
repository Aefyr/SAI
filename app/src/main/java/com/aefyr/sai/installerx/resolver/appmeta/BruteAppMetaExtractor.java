package com.aefyr.sai.installerx.resolver.appmeta;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

/**
 * Extracts AppMeta from an APK directly, currently not very efficient since it copies the APK to a temp file
 */
public class BruteAppMetaExtractor {
    private static final String TAG = "BruteAppMetaExtractor";

    private Context mContext;

    public BruteAppMetaExtractor(Context c) {
        mContext = c.getApplicationContext();
    }

    //TODO maybe cache meta somehow
    @Nullable
    public AppMeta extract(InputStream apkInputStream) {
        File apkFile = null;
        try {
            apkFile = Utils.createTempFileInCache(mContext, "BruteAppMetaExtractor", "apk");
            if (apkFile == null)
                throw new IOException("Unable to create temp file");

            try (InputStream in = apkInputStream; OutputStream out = new FileOutputStream(apkFile)) {
                IOUtils.copyStream(in, out);
            }

            PackageManager pm = mContext.getPackageManager();
            PackageInfo packageInfo = Objects.requireNonNull(pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0));

            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            applicationInfo.sourceDir = apkFile.getAbsolutePath();
            applicationInfo.publicSourceDir = apkFile.getAbsolutePath();

            String label = applicationInfo.loadLabel(pm).toString();

            Uri iconUri = null;
            try {
                Drawable iconDrawable = applicationInfo.loadIcon(pm);
                File iconFile = Utils.createTempFileInCache(mContext, "BruteAppMetaExtractor", "png");
                Utils.saveDrawableAsPng(iconDrawable, iconFile);
                iconUri = Uri.fromFile(iconFile);
            } catch (Exception e) {
                Log.w(TAG, "Unable to save icon to a file", e);
            }

            return new AppMeta.Builder()
                    .setPackageName(packageInfo.packageName)
                    .setVersionCode(packageInfo.versionCode)
                    .setVersionName(packageInfo.versionName)
                    .setAppName(label)
                    .setIconUri(iconUri)
                    .build();

        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        } finally {
            if (apkFile != null)
                apkFile.delete();
        }
    }

}
