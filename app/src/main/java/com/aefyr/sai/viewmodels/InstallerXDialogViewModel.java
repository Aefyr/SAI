package com.aefyr.sai.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.adapters.selection.SimpleKeyStorage;
import com.aefyr.sai.installerx.SplitApkSourceMeta;
import com.aefyr.sai.installerx.SplitPart;
import com.aefyr.sai.installerx.resolver.impl.DefaultSplitApkSourceMetaResolver;
import com.aefyr.sai.utils.SimpleAsyncTask;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class InstallerXDialogViewModel extends AndroidViewModel {

    private MutableLiveData<SplitApkSourceMeta> mMeta = new MutableLiveData<>();

    private LoadMetaTask mLoadMetaTask;

    private Selection<String> mPartsSelection = new Selection<>(new SimpleKeyStorage());

    public InstallerXDialogViewModel(@NonNull Application application) {
        super(application);
    }

    public LiveData<SplitApkSourceMeta> getMeta() {
        return mMeta;
    }

    public Selection<String> getPartsSelection() {
        return mPartsSelection;
    }

    public void parseApkSourceFile(File apkSourceFile) {
        if (mLoadMetaTask != null)
            mLoadMetaTask.cancel();

        mLoadMetaTask = new LoadMetaTask(apkSourceFile).execute();
    }

    private static class LoadMetaTaskResult {
        SplitApkSourceMeta meta;
        Set<String> splitsToSelect;

        private LoadMetaTaskResult(SplitApkSourceMeta meta, Set<String> splitsToSelect) {
            this.meta = meta;
            this.splitsToSelect = splitsToSelect;
        }
    }

    private class LoadMetaTask extends SimpleAsyncTask<File, LoadMetaTaskResult> {

        public LoadMetaTask(File file) {
            super(file);
        }

        @Override
        protected LoadMetaTaskResult doWork(File file) throws Exception {
            SplitApkSourceMeta meta = new DefaultSplitApkSourceMetaResolver(getApplication()).resolveFor(file);
            HashSet<String> splitsToSelect = new HashSet<>();

            for (SplitPart part : meta.flatSplits()) {
                if (part.isRecommended())
                    splitsToSelect.add(part.id());
            }

            return new LoadMetaTaskResult(meta, splitsToSelect);
        }

        @Override
        protected void onWorkDone(LoadMetaTaskResult result) {
            mMeta.setValue(result.meta);
            mPartsSelection.batchSetSelected(result.splitsToSelect, true);
        }

        @Override
        protected void onError(Exception exception) {
            //TODO handle error
            throw new RuntimeException(exception);
        }
    }


}
