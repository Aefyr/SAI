package com.aefyr.sai.backup2.impl;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.aefyr.sai.backup2.Backup;
import com.aefyr.sai.backup2.BackupIndex;
import com.aefyr.sai.backup2.impl.db.BackupDao;
import com.aefyr.sai.backup2.impl.db.BackupEntity;
import com.aefyr.sai.common.AppDatabase;

import java.util.List;

public class DaoBackedBackupIndex implements BackupIndex {

    private static DaoBackedBackupIndex sInstance;

    private BackupDao mDao;

    public static synchronized DaoBackedBackupIndex getInstance(Context context) {
        return sInstance != null ? sInstance : new DaoBackedBackupIndex(context);
    }

    private DaoBackedBackupIndex(Context context) {
        mDao = AppDatabase.getInstance(context).backupDao();
        sInstance = this;
    }

    @Nullable
    @Override
    public Backup getBackupMetaForUri(String storageId, Uri uri) {
        return mDao.getBackupMetaForUri(storageId, uri.toString());
    }

    @Nullable
    @Override
    public Backup getLatestBackupForPackage(String pkg) {
        return mDao.getLatestBackupForPackage(pkg);
    }

    @Override
    public void addEntry(Backup backup) {
        //TODO this is no good, but since this dao is only used from a single thread, it should be fine
        try {
            mDao.add(BackupEntity.fromBackup(backup));
        } catch (SQLiteConstraintException e) {
            mDao.update(BackupEntity.fromBackup(backup));
        }
    }

    @Override
    public Backup deleteEntryByUri(String storageId, Uri uri) {
        BackupEntity backupEntity = mDao.getBackupMetaForUri(storageId, uri.toString());
        if (backupEntity == null)
            return null;

        mDao.removeByUri(storageId, uri.toString());
        return backupEntity;
    }

    @Override
    public List<String> getAllPackages() {
        return mDao.getAllPackages();
    }

    @Override
    public List<Backup> getAllBackupsForPackage(String pkg) {
        return fixList(mDao.getAllBackupsForPackage(pkg));
    }

    @SuppressWarnings("unchecked")
    @Override
    public LiveData<List<Backup>> getAllBackupsForPackageLiveData(String pkg) {
        return (LiveData<List<Backup>>) ((Object) mDao.getAllBackupsForPackageLiveData(pkg));
    }

    @Override
    public void rewrite(List<Backup> newIndex) throws Exception {
        mDao.runInTransaction(() -> {
            mDao.dropAllEntries();
            for (Backup backup : newIndex) {
                mDao.add(BackupEntity.fromBackup(backup));
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> fixList(List<? extends T> list) {
        return (List<T>) list;
    }
}
