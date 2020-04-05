package com.aefyr.sai.installerx.resolver.urimess.impl;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.aefyr.sai.R;
import com.aefyr.sai.installerx.SplitApkSourceMeta;
import com.aefyr.sai.installerx.resolver.meta.SplitApkSourceMetaResolver;
import com.aefyr.sai.installerx.resolver.urimess.SourceType;
import com.aefyr.sai.installerx.resolver.urimess.UriHost;
import com.aefyr.sai.installerx.resolver.urimess.UriMessResolutionError;
import com.aefyr.sai.installerx.resolver.urimess.UriMessResolutionResult;
import com.aefyr.sai.installerx.resolver.urimess.UriMessResolver;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.utils.saf.SafUtils;

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
                        SplitApkSourceMeta meta = mMetaResolver.resolveFor(SafUtils.parcelFdToFile(fd), fileName);
                        results.add(UriMessResolutionResult.success(SourceType.ZIP, Collections.singletonList(uri), meta));
                    } catch (Exception e) {
                        results.add(UriMessResolutionResult.failure(SourceType.ZIP, Collections.singletonList(uri), new UriMessResolutionError(e.getMessage(), true)));
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

        //TODO support apk files
        if (apkFileUris.size() > 0) {
            results.add(UriMessResolutionResult.failure(SourceType.APK_FILES, apkFileUris, new UriMessResolutionError(".apk files are currently not supported", false)));
        }

        return results;
    }

}
