package com.aefyr.sai.installerx.resolver.appmeta.brute;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.resolver.appmeta.AppMeta;
import com.aefyr.sai.installerx.resolver.appmeta.AppMetaExtractor;
import com.aefyr.sai.installerx.resolver.meta.ApkSourceFile;
import com.aefyr.sai.model.backup.SaiExportedAppMeta;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Extracts AppMeta from an APK directly, currently not very efficient since it copies the APK to a temp file
 */
//TODO probably make a shared cache for all app meta extractors
public class BruteAppMetaExtractor implements AppMetaExtractor {
    private static final String TAG = "BruteAppMetaExtractor";
    private static final String HASH_ALGORITHM = "SHA-256";

    private Context mContext;

    private static final Map<String, Object> mHashLocks = new HashMap<>();

    public BruteAppMetaExtractor(Context c) {
        mContext = c.getApplicationContext();
    }

    //TODO maybe cache meta somehow
    @Nullable
    @Override
    public AppMeta extract(ApkSourceFile apkSourceFile, ApkSourceFile.Entry baseApkEntry) {
        try {
            AppMeta cachedAppMeta = findCachedMeta(apkSourceFile, baseApkEntry);
            if (cachedAppMeta != null)
                return cachedAppMeta;

            if (!PreferencesHelper.getInstance(mContext).isBruteParserEnabled() || baseApkEntry.getSize() >= 100 * 1000 * 1000) {
                Log.i(TAG, "Brute parser disabled or base apk entry size is more than 100MBs");
                return null;
            }

            return extractAppMeta(apkSourceFile, baseApkEntry);
        } catch (Exception e) {
            Log.w(TAG, e);
            return null;
        }
    }

    @Nullable
    private AppMeta findCachedMeta(ApkSourceFile apkSourceFile, ApkSourceFile.Entry baseApkEntry) throws Exception {
        String apkHash = getStreamHash(apkSourceFile.openEntryInputStream(baseApkEntry));

        synchronized (getLockForHash(apkHash)) {
            File metaFile = getMetaFileForHash(apkHash);
            File iconFile = getIconFileForHash(apkHash);

            if (!metaFile.exists() || !iconFile.exists())
                return null;

            SaiExportedAppMeta saiExportedAppMeta;
            try {
                saiExportedAppMeta = SaiExportedAppMeta.deserialize(IOUtils.readFile(metaFile));
            } catch (Exception e) {
                Log.w(TAG, String.format("Unable to read meta for hash %s, deleting meta files", apkHash));
                metaFile.delete();
                iconFile.delete();
                return null;
            }

            AppMeta appMeta = new AppMeta();
            appMeta.packageName = saiExportedAppMeta.packageName();
            appMeta.appName = saiExportedAppMeta.label();
            appMeta.versionName = saiExportedAppMeta.versionName();
            appMeta.versionCode = saiExportedAppMeta.versionCode();
            appMeta.iconUri = Uri.fromFile(iconFile);

            Log.i(TAG, String.format("Cache hit for file %s, apk hash is %s", apkSourceFile.getName(), apkHash));

            return appMeta;
        }
    }

    private AppMeta extractAppMeta(ApkSourceFile apkSourceFile, ApkSourceFile.Entry baseApkEntry) throws Exception {
        File apkFile = null;
        try {
            apkFile = Utils.createTempFileInCache(mContext, "BruteAppMetaExtractor", "apk");
            if (apkFile == null)
                throw new IOException("Unable to create temp file");

            MessageDigest messageDigest = MessageDigest.getInstance(HASH_ALGORITHM);
            try (DigestInputStream in = new DigestInputStream(apkSourceFile.openEntryInputStream(baseApkEntry), messageDigest); OutputStream out = new FileOutputStream(apkFile)) {
                IOUtils.copyStream(in, out);
            }
            String apkHash = Utils.bytesToHex(messageDigest.digest());

            PackageManager pm = mContext.getPackageManager();
            PackageInfo packageInfo = Objects.requireNonNull(pm.getPackageArchiveInfo(apkFile.getAbsolutePath(), 0));

            ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            applicationInfo.sourceDir = apkFile.getAbsolutePath();
            applicationInfo.publicSourceDir = apkFile.getAbsolutePath();

            String label = applicationInfo.loadLabel(pm).toString();

            Uri iconUri = null;
            synchronized (getLockForHash(apkHash)) {
                File iconFile = getIconFileForHash(apkHash);
                if (!iconFile.exists()) {
                    try {
                        Drawable iconDrawable = applicationInfo.loadIcon(pm);
                        Utils.saveDrawableAsPng(iconDrawable, iconFile);
                        iconUri = Uri.fromFile(iconFile);
                    } catch (Exception e) {
                        Log.w(TAG, "Unable to save icon to a file", e);
                        iconFile.delete();
                    }
                }
            }

            AppMeta appMeta = new AppMeta.Builder()
                    .setPackageName(packageInfo.packageName)
                    .setVersionCode(packageInfo.versionCode)
                    .setVersionName(packageInfo.versionName)
                    .setAppName(label)
                    .setIconUri(iconUri)
                    .build();

            cacheAppMeta(apkHash, appMeta);

            Log.i(TAG, String.format("Extracted app meta for file %s, apk hash is %s", apkSourceFile.getName(), apkHash));

            return appMeta;

        } finally {
            if (apkFile != null)
                apkFile.delete();
        }
    }

    private void cacheAppMeta(String apkHash, AppMeta appMeta) {
        File appMetaFile = getMetaFileForHash(apkHash);
        synchronized (getLockForHash(apkHash)) {
            if (appMetaFile.exists())
                return;

            try {
                PackageMeta packageMeta = new PackageMeta.Builder(appMeta.packageName)
                        .setLabel(appMeta.appName)
                        .setVersionCode(appMeta.versionCode)
                        .setVersionName(appMeta.versionName)
                        .build();

                SaiExportedAppMeta saiExportedAppMeta = SaiExportedAppMeta.fromPackageMeta(packageMeta, System.currentTimeMillis());

                try (FileOutputStream outputStream = new FileOutputStream(appMetaFile)) {
                    outputStream.write(saiExportedAppMeta.serialize());
                }

            } catch (Exception e) {
                Log.w(TAG, "Unable to cache AppMeta for apkHash " + apkHash, e);
                appMetaFile.delete();
            }
        }
    }

    private Object getLockForHash(String hash) {
        synchronized (mHashLocks) {
            Object lock = mHashLocks.get(hash);
            if (lock == null) {
                lock = new Object();
                mHashLocks.put(hash, lock);
            }

            return lock;
        }
    }

    private String getStreamHash(InputStream inputStream) throws Exception {
        return Utils.bytesToHex(IOUtils.hashStream(inputStream, MessageDigest.getInstance(HASH_ALGORITHM)));
    }

    private File getCacheDir() {
        File cacheDir = new File(mContext.getCacheDir(), "BruteAppMetaExtractor.CachedMeta");
        if (!cacheDir.exists() && !cacheDir.mkdir())
            throw new RuntimeException("Unable to create cache dir");

        return cacheDir;
    }

    private File getIconFileForHash(String hash) {
        return new File(getCacheDir(), hash + ".png");
    }

    private File getMetaFileForHash(String hash) {
        return new File(getCacheDir(), hash + ".json");
    }


}
