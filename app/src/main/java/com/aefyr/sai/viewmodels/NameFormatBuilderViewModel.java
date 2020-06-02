package com.aefyr.sai.viewmodels;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.adapters.selection.SimpleKeyStorage;
import com.aefyr.sai.model.backup.BackupNameFormatBuilder;
import com.aefyr.sai.model.common.PackageMeta;

import java.util.Collection;
import java.util.Objects;

public class NameFormatBuilderViewModel extends ViewModel implements Selection.Observer<BackupNameFormatBuilder.Part> {

    private PackageMeta mOwnMeta;
    private final Selection<BackupNameFormatBuilder.Part> mSelection = new Selection<>(new SimpleKeyStorage());

    private final BackupNameFormatBuilder mBackupNameFormatBuilder;
    private MutableLiveData<BackupNameFormatBuilder> mLiveFormat;

    public NameFormatBuilderViewModel(Context appContext, String format) {
        mOwnMeta = Objects.requireNonNull(PackageMeta.forPackage(appContext, appContext.getPackageName()));

        mBackupNameFormatBuilder = BackupNameFormatBuilder.fromFormatString(format);
        for (BackupNameFormatBuilder.Part part : mBackupNameFormatBuilder.getParts())
            mSelection.setSelected(part, true);

        mLiveFormat = new MutableLiveData<>(mBackupNameFormatBuilder);

        mSelection.addObserver(this);

    }

    public Selection<BackupNameFormatBuilder.Part> getSelection() {
        return mSelection;
    }

    public PackageMeta getOwnMeta() {
        return mOwnMeta;
    }

    public LiveData<BackupNameFormatBuilder> getFormat() {
        return mLiveFormat;
    }

    @Override
    protected void onCleared() {
        mSelection.removeObserver(this);
    }

    @Override
    public void onKeySelectionChanged(Selection selection, BackupNameFormatBuilder.Part key, boolean selected) {
        if (selected)
            mBackupNameFormatBuilder.addPart(key);
        else
            mBackupNameFormatBuilder.removePart(key);

        mLiveFormat.setValue(mBackupNameFormatBuilder);
    }

    @Override
    public void onCleared(Selection<BackupNameFormatBuilder.Part> selection) {
        mBackupNameFormatBuilder.getParts().clear();
        mLiveFormat.setValue(mBackupNameFormatBuilder);
    }

    @Override
    public void onMultipleKeysSelectionChanged(Selection<BackupNameFormatBuilder.Part> selection, Collection<BackupNameFormatBuilder.Part> parts, boolean selected) {
        if (selected) {
            for (BackupNameFormatBuilder.Part part : parts)
                mBackupNameFormatBuilder.addPart(part);
        } else {
            for (BackupNameFormatBuilder.Part part : parts)
                mBackupNameFormatBuilder.removePart(part);
        }

        mLiveFormat.setValue(mBackupNameFormatBuilder);
    }

    public static class Factory implements ViewModelProvider.Factory {

        private Context mAppContext;
        private String mFormat;

        public Factory(Context context, String format) {
            mAppContext = context.getApplicationContext();
            mFormat = format;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new NameFormatBuilderViewModel(mAppContext, mFormat);
        }
    }
}
