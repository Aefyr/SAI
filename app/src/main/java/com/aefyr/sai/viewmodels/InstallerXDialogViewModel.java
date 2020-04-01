package com.aefyr.sai.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.adapters.selection.SimpleKeyStorage;
import com.aefyr.sai.installerx.impl.DummySplitApkSourceMetaResolver;
import com.aefyr.sai.model.installerx.SplitApkSourceMeta;
import com.aefyr.sai.utils.SimpleAsyncTask;

import java.io.File;

public class InstallerXDialogViewModel extends AndroidViewModel {

    private MutableLiveData<SplitApkSourceMeta> mMeta = new MutableLiveData<>();

    private LoadMetaTask mLoadMetaTask;

    private Selection<String> mPartsSelection = new Selection<>(new SimpleKeyStorage());

    public InstallerXDialogViewModel(@NonNull Application application) {
        super(application);
        mLoadMetaTask = new LoadMetaTask(new File("")).execute();
    }

    public LiveData<SplitApkSourceMeta> getMeta() {
        return mMeta;
    }

    public Selection<String> getPartsSelection() {
        return mPartsSelection;
    }

    private class LoadMetaTask extends SimpleAsyncTask<File, SplitApkSourceMeta> {

        public LoadMetaTask(File file) {
            super(file);
        }

        @Override
        protected SplitApkSourceMeta doWork(File file) throws Exception {
            return new DummySplitApkSourceMetaResolver(getApplication()).resolveFor(file);
        }

        @Override
        protected void onWorkDone(SplitApkSourceMeta splitApkSourceMeta) {
            mMeta.setValue(splitApkSourceMeta);
        }

        @Override
        protected void onError(Exception exception) {
            //TODO handle error
        }
    }


}
