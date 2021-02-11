package com.aefyr.sai.installerx.resolver.urimess.impl;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

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
                case "apkm":
                    try (UriHost.UriAsFile uriAsFile = uriHost.openUriAsFile(uri)) {
                        ApkSourceMetaResolutionResult resolutionResult = mMetaResolver.resolveFor(new ZipFileApkSourceFile(uriAsFile.file(), fileName));
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

        private MultipleApkFilesApkSourceFile(List<Uri> uris, UriHost uriHost) {
            mUris = uris;
            mUriHost = uriHost;
        }

        @Override
        public List<Entry> listEntries() {
            List<Entry> entries = new ArrayList<>();
            for (Uri uri : mUris) {
                String name = mUriHost.getFileNameFromUri(uri);
                entries.add(new InternalEntry(uri, name, name, mUriHost.getFileSizeFromUri(uri)));
            }

            return entries;
        }

        @Override
        public InputStream openEntryInputStream(Entry entry) throws Exception {
            return mUriHost.openUriInputStream(((InternalEntry) entry).mUri);
        }

        @Override
        public String getName() {
            return "whatever.whatever";
        }

        private static class InternalEntry extends Entry {

            private Uri mUri;

            private InternalEntry(Uri uri, String name, String localPath, long size) {
                super(name, localPath, size);
                mUri = uri;
            }
        }
    }

}
