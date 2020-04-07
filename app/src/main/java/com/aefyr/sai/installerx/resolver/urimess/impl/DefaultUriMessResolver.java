package com.aefyr.sai.installerx.resolver.urimess.impl;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.aefyr.sai.installerx.resolver.meta.ApkSourceFile;
import com.aefyr.sai.installerx.resolver.meta.ApkSourceMetaResolutionResult;
import com.aefyr.sai.installerx.resolver.meta.SplitApkSourceMetaResolver;
import com.aefyr.sai.installerx.resolver.meta.impl.ZipFileApkSourceFile;
import com.aefyr.sai.installerx.resolver.urimess.SourceType;
import com.aefyr.sai.installerx.resolver.urimess.UriHost;
import com.aefyr.sai.installerx.resolver.urimess.UriMessResolutionError;
import com.aefyr.sai.installerx.resolver.urimess.UriMessResolutionResult;
import com.aefyr.sai.installerx.resolver.urimess.UriMessResolver;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.utils.saf.SafUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DefaultUriMessResolver implements UriMessResolver {
    private static final String TAG = "DefaultMessResolver";

    private Context mContext;
    private SplitApkSourceMetaResolver mMetaResolver;

    public DefaultUriMessResolver(Context context, SplitApkSourceMetaResolver metaResolver) {
        mContext = context;
        mMetaResolver = metaResolver;
    }

    @Override
    public List<UriMessResolutionResult> resolve(Collection<Uri> uris, UriHost uriHost) {

        List<UriMessResolutionResult> results = new ArrayList<>();
        List<Uri> apkFileUris = new ArrayList<>();

        for (Uri uri : uris) {
            String fileName = uriHost.getFileNameFromUri(uri);
            String extension = Utils.getExtension(fileName);
            if (extension == null) {
                Log.w(TAG, "Unable to get extension for uri " + uri);
                continue;
            }

            switch (extension.toLowerCase()) {
                case "zip":
                case "apks":
                case "xapk":
                    try (ParcelFileDescriptor fd = uriHost.openUriAsParcelFd(uri)) {
                        ApkSourceMetaResolutionResult resolutionResult = mMetaResolver.resolveFor(new ZipFileApkSourceFile(SafUtils.parcelFdToFile(fd), fileName));
                        if (resolutionResult.isSuccessful())
                            results.add(UriMessResolutionResult.success(SourceType.ZIP, Collections.singletonList(uri), resolutionResult.meta()));
                        else
                            results.add(UriMessResolutionResult.failure(SourceType.ZIP, Collections.singletonList(uri), new UriMessResolutionError(resolutionResult.error().message(), resolutionResult.error().doesTryingToInstallNonethelessMakeSense())));

                    } catch (Exception e) {
                        Log.w(TAG, "Exception while resolving split meta", e);
                        results.add(UriMessResolutionResult.failure(SourceType.ZIP, Collections.singletonList(uri), new UriMessResolutionError(e.getLocalizedMessage(), true)));
                    }
                    break;
                case "apk":
                    apkFileUris.add(uri);
                    break;
                default:
                    results.add(UriMessResolutionResult.failure(SourceType.UNKNOWN, Collections.singletonList(uri), new UriMessResolutionError(mContext.getString(R.string.installerx_default_mess_resolver_error_unknown_extension, extension), false)));
                    break;
            }
        }

        //TODO maybe group single apks by package
        if (apkFileUris.size() > 0) {
            try {
                ApkSourceMetaResolutionResult resolutionResult = mMetaResolver.resolveFor(new MultipleApkFilesApkSourceFile(apkFileUris, uriHost));
                if (resolutionResult.isSuccessful())
                    results.add(UriMessResolutionResult.success(SourceType.APK_FILES, apkFileUris, resolutionResult.meta()));
                else
                    results.add(UriMessResolutionResult.failure(SourceType.APK_FILES, apkFileUris, new UriMessResolutionError(resolutionResult.error().message(), resolutionResult.error().doesTryingToInstallNonethelessMakeSense())));
            } catch (Exception e) {
                Log.w(TAG, "Exception while resolving split meta", e);
                results.add(UriMessResolutionResult.failure(SourceType.APK_FILES, apkFileUris, new UriMessResolutionError(e.getMessage(), true)));
            }
        }

        return results;
    }

    private static class MultipleApkFilesApkSourceFile implements ApkSourceFile {

        private List<Uri> mUris;
        private UriHost mUriHost;

        int mCurrentIndex = -1;

        private MultipleApkFilesApkSourceFile(List<Uri> uris, UriHost uriHost) {
            mUris = uris;
            mUriHost = uriHost;
        }

        @Nullable
        @Override
        public Entry nextEntry() {
            mCurrentIndex++;

            if (mCurrentIndex >= mUris.size())
                return null;

            String name = mUriHost.getFileNameFromUri(mUris.get(mCurrentIndex));
            return new Entry(name, name);
        }

        @Override
        public InputStream openEntryInputStream() throws Exception {
            return mUriHost.openUriInputStream(mUris.get(mCurrentIndex));
        }

        @Override
        public String getName() {
            return "whatever.whatever";
        }
    }

}
