package com.aefyr.sai.installerx.resolver.appmeta.xapk;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.resolver.appmeta.AppMeta;
import com.aefyr.sai.installerx.resolver.appmeta.AppMetaExtractor;
import com.aefyr.sai.installerx.resolver.meta.ApkSourceFile;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

//TODO handle manifest.json versions
public class XapkAppMetaExtractor implements AppMetaExtractor {
    private static final String TAG = "XapkMetaExtractor";

    private static final String META_FILE = "manifest.json";
    private static final String ICON_FILE = "icon.png";

    private Context mContext;

    public XapkAppMetaExtractor(Context context) {
        mContext = context.getApplicationContext();
    }

    @Nullable
    @Override
    public AppMeta extract(ApkSourceFile apkSourceFile, ApkSourceFile.Entry baseApkEntry) {
        try {
            boolean seenMetaFile = false;
            AppMeta appMeta = new AppMeta();

            for (ApkSourceFile.Entry entry : apkSourceFile.listEntries()) {

                if (entry.getLocalPath().equals(META_FILE)) {
                    JSONObject metaJson = new JSONObject(IOUtils.readStream(apkSourceFile.openEntryInputStream(entry), StandardCharsets.UTF_8));
                    appMeta.packageName = metaJson.optString("package_name");
                    appMeta.appName = metaJson.optString("name");
                    appMeta.versionName = metaJson.optString("version_name");
                    appMeta.versionCode = metaJson.optLong("version_code");
                    seenMetaFile = true;
                } else if (entry.getLocalPath().equals(ICON_FILE)) {
                    File iconFile = Utils.createTempFileInCache(mContext, "XapkZipAppMetaExtractor", "png");
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
