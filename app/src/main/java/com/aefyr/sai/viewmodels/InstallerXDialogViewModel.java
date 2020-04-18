package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.adapters.selection.SimpleKeyStorage;
import com.aefyr.sai.installer.ApkSourceBuilder;
import com.aefyr.sai.installer2.base.model.SaiPiSessionParams;
import com.aefyr.sai.installer2.impl.FlexSaiPackageInstaller;
import com.aefyr.sai.installerx.SplitApkSourceMeta;
import com.aefyr.sai.installerx.SplitPart;
import com.aefyr.sai.installerx.postprocessing.DeviceInfoAwarePostprocessor;
import com.aefyr.sai.installerx.postprocessing.SortPostprocessor;
import com.aefyr.sai.installerx.resolver.appmeta.DefaultAppMetaExtractor;
import com.aefyr.sai.installerx.resolver.meta.impl.DefaultSplitApkSourceMetaResolver;
import com.aefyr.sai.installerx.resolver.urimess.SourceType;
import com.aefyr.sai.installerx.resolver.urimess.UriMessResolutionError;
import com.aefyr.sai.installerx.resolver.urimess.UriMessResolutionResult;
import com.aefyr.sai.installerx.resolver.urimess.UriMessResolver;
import com.aefyr.sai.installerx.resolver.urimess.impl.AndroidUriHost;
import com.aefyr.sai.installerx.resolver.urimess.impl.DefaultUriMessResolver;
import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.utils.Logs;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.SimpleAsyncTask;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InstallerXDialogViewModel extends AndroidViewModel {
    private static final String TAG = "InstallerXVM";

    private FlexSaiPackageInstaller mInstaller;
    private PreferencesHelper mPrefsHelper;

    private MutableLiveData<State> mState = new MutableLiveData<>(State.NO_DATA);
    private MutableLiveData<SplitApkSourceMeta> mMeta = new MutableLiveData<>();
    private Warning mWarning;

    private LoadMetaTask mLoadMetaTask;

    private Selection<String> mPartsSelection = new Selection<>(new SimpleKeyStorage());
    private List<UriMessResolutionResult> mResolutionResults;

    public InstallerXDialogViewModel(@NonNull Application application) {
        super(application);
        mInstaller = FlexSaiPackageInstaller.getInstance(getApplication());
        mPrefsHelper = PreferencesHelper.getInstance(getApplication());
    }

    public LiveData<State> getState() {
        return mState;
    }

    public LiveData<SplitApkSourceMeta> getMeta() {
        return mMeta;
    }

    public Warning getWarning() {
        return mWarning;
    }

    public Selection<String> getPartsSelection() {
        return mPartsSelection;
    }

    public void setApkSourceFiles(List<File> apkSourceFiles) {
        if (mLoadMetaTask != null)
            mLoadMetaTask.cancel();

        mState.setValue(State.LOADING);
        mResolutionResults = null;
        mLoadMetaTask = new LoadMetaTask(new LoadMetaTaskInput(apkSourceFiles, null)).execute();
    }

    public void setApkSourceUris(List<Uri> apkSourceUris) {
        if (mLoadMetaTask != null)
            mLoadMetaTask.cancel();

        mState.setValue(State.LOADING);
        mResolutionResults = null;
        mLoadMetaTask = new LoadMetaTask(new LoadMetaTaskInput(null, apkSourceUris)).execute();
    }

    public void cancelParsing() {
        if (mLoadMetaTask == null || !mLoadMetaTask.isOngoing())
            return;

        mLoadMetaTask.cancel();
        mState.setValue(State.NO_DATA);
    }

    public void enqueueInstallation() {
        if (mResolutionResults == null)
            return;

        if (mResolutionResults.size() == 1) {
            enqueueSingleFiltered(mResolutionResults.get(0));
            return;
        }

        for (UriMessResolutionResult resolutionResult : mResolutionResults) {
            if (!resolutionResult.isSuccessful())
                continue;

            ApkSourceBuilder apkSourceBuilder = null;

            if (resolutionResult.sourceType().equals(SourceType.ZIP)) {
                apkSourceBuilder = new ApkSourceBuilder(getApplication())
                        .fromZipContentUri(resolutionResult.uris().get(0));

            } else if (resolutionResult.sourceType().equals(SourceType.APK_FILES)) {
                apkSourceBuilder = new ApkSourceBuilder(getApplication())
                        .fromApkContentUris(resolutionResult.uris());
            }

            if (apkSourceBuilder != null) {
                apkSourceBuilder.setZipExtractionEnabled(mPrefsHelper.shouldExtractArchives())
                        .setReadZipViaZipFileEnabled(mPrefsHelper.shouldUseZipFileApi())
                        .setSigningEnabled(mPrefsHelper.shouldSignApks());

                install(apkSourceBuilder.build());
            }
        }
    }

    private void enqueueSingleFiltered(UriMessResolutionResult result) {
        ApkSourceBuilder apkSourceBuilder = null;

        if (result.sourceType() == SourceType.ZIP) {
            apkSourceBuilder = new ApkSourceBuilder(getApplication())
                    .fromZipContentUri(result.uris().get(0));

        } else if (result.sourceType() == SourceType.APK_FILES) {
            apkSourceBuilder = new ApkSourceBuilder(getApplication())
                    .fromApkContentUris(result.uris());
        }

        if (apkSourceBuilder != null) {
            apkSourceBuilder.setZipExtractionEnabled(mPrefsHelper.shouldExtractArchives())
                    .setReadZipViaZipFileEnabled(mPrefsHelper.shouldUseZipFileApi())
                    .setSigningEnabled(mPrefsHelper.shouldSignApks());

            if (result.isSuccessful())
                apkSourceBuilder.filterApksByLocalPath(new HashSet<>(mPartsSelection.getSelectedKeys()), false);

            install(apkSourceBuilder.build());
        }

    }

    private void install(ApkSource apkSource) {
        mInstaller.enqueueSession(mInstaller.createSessionOnInstaller(mPrefsHelper.getInstaller(), new SaiPiSessionParams(apkSource)));
    }

    public enum State {
        NO_DATA, LOADING, LOADED, WARNING, ERROR
    }

    private static class LoadMetaTaskInput {
        List<File> apkSourceFiles;
        List<Uri> apkSourceContentUris;

        private LoadMetaTaskInput(@Nullable List<File> apkSourceFiles, @Nullable List<Uri> apkSourceContentUris) {
            this.apkSourceFiles = apkSourceFiles;
            this.apkSourceContentUris = apkSourceContentUris;
        }
    }

    private static class LoadMetaTaskResult {
        SplitApkSourceMeta meta;
        Set<String> splitsToSelect;
        List<UriMessResolutionResult> resolutionResults;

        private LoadMetaTaskResult(@Nullable SplitApkSourceMeta meta, @Nullable Set<String> splitsToSelect, @NonNull List<UriMessResolutionResult> resolutionResults) {
            this.meta = meta;
            this.splitsToSelect = splitsToSelect;
            this.resolutionResults = resolutionResults;
        }
    }

    private class LoadMetaTask extends SimpleAsyncTask<LoadMetaTaskInput, LoadMetaTaskResult> {

        private LoadMetaTask(LoadMetaTaskInput input) {
            super(input);
        }

        @Override
        protected LoadMetaTaskResult doWork(LoadMetaTaskInput input) {
            List<Uri> apkSourceUris = flattenInputToUris(input);
            if (apkSourceUris.size() == 0)
                throw new IllegalArgumentException("Expected at least 1 file in input");

            DefaultSplitApkSourceMetaResolver metaResolver = new DefaultSplitApkSourceMetaResolver(getApplication(), new DefaultAppMetaExtractor(getApplication()));
            metaResolver.addPostprocessor(new DeviceInfoAwarePostprocessor(getApplication()));
            metaResolver.addPostprocessor(new SortPostprocessor());

            UriMessResolver uriMessResolver = new DefaultUriMessResolver(getApplication(), metaResolver);
            List<UriMessResolutionResult> resolutionResults = uriMessResolver.resolve(apkSourceUris, new AndroidUriHost(getApplication()));

            if (resolutionResults.size() != 1) {
                return new LoadMetaTaskResult(null, null, resolutionResults);
            }

            UriMessResolutionResult resolutionResult = resolutionResults.get(0);
            if (resolutionResult.isSuccessful()) {
                SplitApkSourceMeta meta = resolutionResult.meta();
                HashSet<String> splitsToSelect = new HashSet<>();

                for (SplitPart part : meta.flatSplits()) {
                    if (part.isRecommended())
                        splitsToSelect.add(part.localPath());
                }

                return new LoadMetaTaskResult(meta, splitsToSelect, resolutionResults);
            }

            return new LoadMetaTaskResult(null, null, resolutionResults);
        }

        private List<Uri> flattenInputToUris(LoadMetaTaskInput input) {
            List<Uri> uris = new ArrayList<>();

            if (input.apkSourceContentUris != null)
                uris.addAll(input.apkSourceContentUris);

            if (input.apkSourceFiles != null) {
                for (File file : input.apkSourceFiles)
                    uris.add(Uri.fromFile(file));
            }

            return uris;
        }

        @Override
        protected void onWorkDone(LoadMetaTaskResult result) {
            mResolutionResults = result.resolutionResults;

            if (mResolutionResults.size() == 0) {
                mWarning = new Warning(getApplication().getString(R.string.installerx_dialog_warn_no_files), false);
                mState.setValue(State.WARNING);
            } else if (mResolutionResults.size() == 1) {
                UriMessResolutionResult uriMessResolutionResult = mResolutionResults.get(0);
                if (uriMessResolutionResult.isSuccessful()) {
                    mState.setValue(State.LOADED);
                    mMeta.setValue(result.meta);
                    mPartsSelection.clear();
                    mPartsSelection.batchSetSelected(result.splitsToSelect, true);
                } else {
                    UriMessResolutionError error = uriMessResolutionResult.error();
                    if (error.doesTryingToInstallNonethelessMakeSense()) {
                        mWarning = new Warning(getApplication().getString(R.string.installerx_dialog_resolution_error_non_critical, uriMessResolutionResult.error().message()), true);
                    } else {
                        mWarning = new Warning(getApplication().getString(R.string.installerx_dialog_resolution_error_critical, uriMessResolutionResult.error().message()), false);
                    }
                    mState.setValue(State.WARNING);
                }
            } else {
                mWarning = new Warning(getApplication().getString(R.string.installerx_dialog_warn_multiple_files), true);
                mState.setValue(State.WARNING);
            }
        }

        @Override
        protected void onError(Exception exception) {
            Log.w(TAG, "Error while parsing meta for an apk", exception);
            Logs.logException(exception);

            mResolutionResults = null;
            mState.setValue(State.ERROR);
        }
    }

    public static class Warning {
        String mMessage;
        boolean mCanInstallAnyway;

        private Warning(String message, boolean canInstallAnyway) {
            mMessage = message;
            mCanInstallAnyway = canInstallAnyway;
        }

        public String message() {
            return mMessage;
        }

        public boolean canInstallAnyway() {
            return mCanInstallAnyway;
        }
    }


}
