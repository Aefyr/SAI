package com.aefyr.sai.backup2.impl;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;

import androidx.annotation.Nullable;

import com.aefyr.sai.backup2.BackupFileMeta;
import com.aefyr.sai.backup2.BackupIndex;
import com.aefyr.sai.backup2.impl.db.BackupDao;
import com.aefyr.sai.backup2.impl.db.BackupMetaEntity;
import com.aefyr.sai.common.AppDatabase;

import java.util.ArrayList;
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
    public BackupFileMeta getBackupMetaForUri(String storageId, Uri uri) {
        return entityToMetaOrNull(mDao.getBackupMetaForUri(storageId, uri.toString()));
    }

    @Nullable
    @Override
    public BackupFileMeta getLatestBackupForPackage(String pkg) {
        return entityToMetaOrNull(mDao.getLatestBackupForPackage(pkg));
    }

    @Override
    public void addEntry(BackupFileMeta meta) {
        //TODO this is no good, but since this dao is only used from a single thread, it should be fine
        try {
            mDao.add(BackupMetaEntity.fromBackupFileMeta(meta));
        } catch (SQLiteConstraintException e) {
            mDao.update(BackupMetaEntity.fromBackupFileMeta(meta));
        }
    }

    @Override
    public void deleteEntryByUri(String storageId, Uri uri) {
        mDao.removeByUri(storageId, uri.toString());
    }

    @Override
    public List<String> getAllPackages() {
        return mDao.getAllPackages();
    }

    @Override
    public List<BackupFileMeta> getAllBackupsForPackage(String pkg) {
        List<BackupFileMeta> backupFileMetas = new ArrayList<>();

        for (BackupMetaEntity entity : mDao.getAllBackupsForPackage(pkg)) {
            backupFileMetas.add(entity.toBackupFileMeta());
        }

        return backupFileMetas;
    }

    @Override
    public void rewrite(List<BackupFileMeta> newIndex) throws Exception {
        mDao.runInTransaction(() -> {
            mDao.dropAllEntries();
            for (BackupFileMeta meta : newIndex) {
                mDao.add(BackupMetaEntity.fromBackupFileMeta(meta));
            }
        });
    }

    @Nullable
    private BackupFileMeta entityToMetaOrNull(@Nullable BackupMetaEntity entity) {
        return entity == null ? null : entity.toBackupFileMeta();
    }
}
