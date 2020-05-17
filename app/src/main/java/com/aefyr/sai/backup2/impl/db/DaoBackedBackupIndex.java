package com.aefyr.sai.backup2.impl.db;

import android.content.Context;
import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.aefyr.sai.backup2.Backup;
import com.aefyr.sai.backup2.BackupComponent;
import com.aefyr.sai.backup2.BackupIndex;
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
    public Backup getBackupMetaForUri(Uri uri) {
        return mDao.getBackupMetaForUri(uri.toString());
    }

    @Nullable
    @Override
    public Backup getLatestBackupForPackage(String pkg) {
        return mDao.getLatestBackupForPackage(pkg);
    }

    @Override
    public void addEntry(Backup backup) {
        mDao.runInTransaction(() -> {
            if (mDao.getBackupMetaForUri(backup.uri().toString()) != null)
                mDao.removeByUri(backup.uri().toString());

            mDao.insertBackup(BackupEntity.fromBackup(backup));
            for (BackupComponent component : backup.components()) {
                mDao.insertBackupComponent(BackupComponentEntity.fromBackupComponent(backup.uri(), component));
            }
        });
    }

    @Override
    public Backup deleteEntryByUri(Uri uri) {
        BackupWithComponents backupWithComponents = mDao.getBackupMetaForUri(uri.toString());
        if (backupWithComponents == null)
            return null;

        mDao.removeByUri(uri.toString());
        return backupWithComponents;
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
                mDao.insertBackup(BackupEntity.fromBackup(backup));
                for (BackupComponent component : backup.components()) {
                    mDao.insertBackupComponent(BackupComponentEntity.fromBackupComponent(backup.uri(), component));
                }
            }
        });
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> fixList(List<? extends T> list) {
        return (List<T>) list;
    }
}
