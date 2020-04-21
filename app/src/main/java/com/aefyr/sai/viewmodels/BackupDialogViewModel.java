package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.adapters.selection.SimpleKeyStorage;
import com.aefyr.sai.installerx.common.Category;
import com.aefyr.sai.installerx.common.MutableSplitCategory;
import com.aefyr.sai.installerx.common.SplitApkSourceMeta;
import com.aefyr.sai.installerx.common.SplitPart;
import com.aefyr.sai.installerx.postprocessing.SortPostprocessor;
import com.aefyr.sai.installerx.resolver.appmeta.installedapp.InstalledAppAppMetaExtractor;
import com.aefyr.sai.installerx.resolver.meta.ApkSourceMetaResolutionResult;
import com.aefyr.sai.installerx.resolver.meta.impl.DefaultSplitApkSourceMetaResolver;
import com.aefyr.sai.installerx.resolver.meta.impl.InstalledAppApkSourceFile;
import com.aefyr.sai.model.backup.SplitApkPart;
import com.aefyr.sai.utils.SimpleAsyncTask;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BackupDialogViewModel extends AndroidViewModel {

    private MutableLiveData<LoadingState> mLoadingState = new MutableLiveData<>();
    private MutableLiveData<List<SplitApkPart>> mParts = new MutableLiveData<>();

    private LoadPackageTask mLoadPackageTask;

    private final SimpleKeyStorage<String> mKeyStorage = new SimpleKeyStorage<>();
    private final Selection<String> mSelection = new Selection<>(mKeyStorage);

    public BackupDialogViewModel(@NonNull Application application) {
        super(application);
        mLoadingState.setValue(LoadingState.EMPTY);
        mParts.setValue(Collections.emptyList());
    }

    public void setPackage(String pkg) {
        if (mLoadPackageTask != null)
            mLoadPackageTask.cancel();

        mLoadingState.setValue(LoadingState.LOADING);

        mLoadPackageTask = new LoadPackageTask(pkg).execute();
    }

    public LiveData<LoadingState> getLoadingState() {
        return mLoadingState;
    }

    public LiveData<List<SplitApkPart>> getSplitParts() {
        return mParts;
    }

    public Selection<String> getSelection() {
        return mSelection;
    }

    public List<File> getSelectedSplitParts() {
        if (mParts.getValue() == null)
            return Collections.emptyList();

        ArrayList<File> selectedParts = new ArrayList<>();
        for (SplitApkPart part : mParts.getValue()) {
            if (mSelection.isSelected(part.toKey()))
                selectedParts.add(part.getPath());
        }

        return selectedParts;
    }


    private class LoadPackageTask extends SimpleAsyncTask<String, List<SplitApkPart>> {

        private LoadPackageTask(String s) {
            super(s);
        }

        @Override
        protected List<SplitApkPart> doWork(String pkg) throws Exception {
            try {
                return getParsedParts(pkg);
            } catch (Exception e) {
                Log.w("BackupVM", "Unable to get parsed pkg parts", e);
                return getRawParts(pkg);
            }
        }

        private List<SplitApkPart> getParsedParts(String pkg) throws Exception {
            DefaultSplitApkSourceMetaResolver metaResolver = new DefaultSplitApkSourceMetaResolver(getApplication(), new InstalledAppAppMetaExtractor(getApplication()));
            metaResolver.addPostprocessor(new SortPostprocessor());
            metaResolver.addPostprocessor(parserContext -> {
                MutableSplitCategory baseApkCategory = parserContext.getCategory(Category.BASE_APK);
                if (baseApkCategory == null || baseApkCategory.getPartsList().size() == 0)
                    return;

                baseApkCategory.getPartsList().get(0).setName(parserContext.getAppMeta().appName);
            });
            ApkSourceMetaResolutionResult result = metaResolver.resolveFor(new InstalledAppApkSourceFile(getApplication(), pkg));

            if (!result.isSuccessful())
                throw new RuntimeException(result.error().message());

            SplitApkSourceMeta meta = result.meta();
            List<SplitApkPart> parts = new ArrayList<>();
            for (SplitPart splitPart : meta.flatSplits()) {
                parts.add(new SplitApkPart(splitPart.name(), new File(splitPart.localPath())));
            }

            return parts;
        }

        private List<SplitApkPart> getRawParts(String pkg) throws Exception {
            PackageManager pm = getApplication().getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(pkg, 0);

            List<SplitApkPart> parts = new ArrayList<>();
            parts.add(new SplitApkPart(appInfo.loadLabel(pm).toString(), new File(appInfo.publicSourceDir)));

            if (appInfo.splitPublicSourceDirs != null) {
                for (String splitPath : appInfo.splitPublicSourceDirs) {
                    File splitApkPartFile = new File(splitPath);
                    parts.add(new SplitApkPart(splitApkPartFile.getName(), splitApkPartFile));
                }
            }

            return parts;
        }

        @Override
        protected void onWorkDone(List<SplitApkPart> splitApkParts) {
            mSelection.clear();
            for (SplitApkPart part : splitApkParts)
                mSelection.setSelected(part.toKey(), true);

            mParts.setValue(splitApkParts);
            mLoadingState.setValue(LoadingState.LOADED);
        }

        @Override
        protected void onError(Exception exception) {
            Log.w("BackupDialogVM", exception);
            mLoadingState.setValue(LoadingState.FAILED);
        }
    }

    public enum LoadingState {
        EMPTY, LOADING, LOADED, FAILED
    }
}
