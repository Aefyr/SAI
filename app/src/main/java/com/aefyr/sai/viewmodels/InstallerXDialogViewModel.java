package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
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
import com.aefyr.sai.installerx.resolver.impl.DefaultSplitApkSourceMetaResolver;
import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.utils.Logs;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.SimpleAsyncTask;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
    private List<Uri> mUrisToInstall;

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
        mLoadMetaTask = new LoadMetaTask(new LoadMetaTaskInput(apkSourceFiles, null)).execute();
    }

    public void setApkSourceUris(List<Uri> apkSourceUris) {
        if (mLoadMetaTask != null)
            mLoadMetaTask.cancel();

        mState.setValue(State.LOADING);
        mLoadMetaTask = new LoadMetaTask(new LoadMetaTaskInput(null, apkSourceUris)).execute();
    }

    public void cancelParsing() {
        if (mLoadMetaTask == null || !mLoadMetaTask.isOngoing())
            return;

        mLoadMetaTask.cancel();
        mState.setValue(State.NO_DATA);
    }

    public void enqueueInstallation() {
        if (mUrisToInstall == null)
            return;

        if (mUrisToInstall.size() == 1) {
            ApkSource apkSource = new ApkSourceBuilder(getApplication())
                    .fromZipContentUri(mUrisToInstall.get(0))
                    .setZipExtractionEnabled(mPrefsHelper.shouldExtractArchives())
                    .setReadZipViaZipFileEnabled(mPrefsHelper.shouldUseZipFileApi())
                    .setSigningEnabled(mPrefsHelper.shouldSignApks())
                    .filterApksInZip(new HashSet<>(mPartsSelection.getSelectedKeys()), false)
                    .build();

            install(apkSource);
        } else {
            for (Uri uri : mUrisToInstall) {
                ApkSource apkSource = new ApkSourceBuilder(getApplication())
                        .fromZipContentUri(uri)
                        .setZipExtractionEnabled(mPrefsHelper.shouldExtractArchives())
                        .setReadZipViaZipFileEnabled(mPrefsHelper.shouldUseZipFileApi())
                        .setSigningEnabled(mPrefsHelper.shouldSignApks())
                        .build();

                install(apkSource);
            }
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
        List<Uri> urisToInstall;

        private LoadMetaTaskResult(@Nullable SplitApkSourceMeta meta, @Nullable Set<String> splitsToSelect, @NonNull List<Uri> urisToInstall) {
            this.meta = meta;
            this.splitsToSelect = splitsToSelect;
            this.urisToInstall = urisToInstall;
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

            if (apkSourceUris.size() != 1) {
                return new LoadMetaTaskResult(null, null, apkSourceUris);
            }

            Uri singleApkSourceUri = apkSourceUris.get(0);
            ParcelFileDescriptor fd = null;
            try {
                fd = openUriFd(singleApkSourceUri);
                File fdFile = parcelFdToFile(fd);
                SplitApkSourceMeta meta = new DefaultSplitApkSourceMetaResolver(getApplication()).resolveFor(fdFile);
                HashSet<String> splitsToSelect = new HashSet<>();

                for (SplitPart part : meta.flatSplits()) {
                    if (part.isRecommended())
                        splitsToSelect.add(part.id());
                }

                return new LoadMetaTaskResult(meta, splitsToSelect, apkSourceUris);
            } catch (Exception e) {
                Log.w(TAG, "Error while parsing meta for an apk", e);
                Logs.logException(e);

                return new LoadMetaTaskResult(null, null, apkSourceUris);
            } finally {
                try {
                    if (fd != null)
                        fd.close();
                } catch (Exception e) {
                    Log.w(TAG, "Unable to close file descriptor", e);
                }
            }
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

        private ParcelFileDescriptor openUriFd(Uri uri) throws IOException {
            ParcelFileDescriptor fd = getApplication().getContentResolver().openFileDescriptor(uri, "r");
            Objects.requireNonNull(fd);
            return fd;
        }

        private File parcelFdToFile(ParcelFileDescriptor fd) {
            return new File("/proc/self/fd/" + fd.getFd());
        }

        @Override
        protected void onWorkDone(LoadMetaTaskResult result) {
            mUrisToInstall = result.urisToInstall;

            if (result.urisToInstall.size() == 1) {
                if (result.meta != null) {
                    mState.setValue(State.LOADED);
                    mMeta.setValue(result.meta);
                    mPartsSelection.clear();
                    mPartsSelection.batchSetSelected(result.splitsToSelect, true);
                } else {
                    mWarning = new Warning(getApplication().getString(R.string.installerx_dialog_warn_parsing_fail), true);
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

            mUrisToInstall = null;
            mState.setValue(State.ERROR);
        }
    }

    public class Warning {
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
