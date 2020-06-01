package com.aefyr.sai.backup2.impl.db;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.aefyr.sai.backup2.Backup;
import com.aefyr.sai.backup2.BackupComponent;
import com.aefyr.sai.backup2.BackupIndex;
import com.aefyr.sai.common.AppDatabase;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Stopwatch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DaoBackedBackupIndex implements BackupIndex {
    private static final String TAG = "DaoBackedBackupIndex";

    private static DaoBackedBackupIndex sInstance;

    private Context mContext;
    private AppDatabase mAppDb;

    private BackupDao mDao;
    private BackupIconDao mIconDao;

    private String mIconSessionId = UUID.randomUUID().toString();

    private final Object mFallbackIconLock = new Object();

    public static synchronized DaoBackedBackupIndex getInstance(Context context) {
        return sInstance != null ? sInstance : new DaoBackedBackupIndex(context);
    }

    private DaoBackedBackupIndex(Context context) {
        mContext = context.getApplicationContext();
        mAppDb = AppDatabase.getInstance(mContext);
        mDao = mAppDb.backupDao();
        mIconDao = mAppDb.backupIconDao();

        new Thread(this::vacuumIcons, "DaoBackedBackupIndex.IconsVacuum").start();

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
    public void addEntry(Backup backup, BackupIconProvider iconProvider) throws Exception {

        File iconFile;
        try {
            iconFile = writeBackupIcon(mIconSessionId, iconProvider.getIconInputStream(backup));
        } catch (Exception e) {
            Log.w(TAG, "Unable to write backup icon", e);
            iconFile = getFallbackIconFile();
        }
        File finalIconFile = iconFile;
        String iconId = generateIconFileId(mIconSessionId);

        mDao.runInTransaction(() -> {
            if (mDao.getBackupMetaForUri(backup.uri().toString()) != null)
                mDao.removeByUri(backup.uri().toString());

            BackupEntity backupEntity = BackupEntity.fromBackup(backup, iconId);

            mDao.insertBackup(backupEntity);

            mIconDao.addIcon(BackupIconEntity.create(iconId, mIconSessionId, finalIconFile));

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
    public void rewrite(List<Backup> newIndex, BackupIconProvider iconProvider) throws Exception {

        Map<Backup, BackupIconEntity> icons = new HashMap<>();

        for (Backup backup : newIndex) {
            File iconFile;
            try {
                iconFile = writeBackupIcon(mIconSessionId, iconProvider.getIconInputStream(backup));
            } catch (Exception e) {
                Log.w(TAG, "Unable to write backup icon", e);
                iconFile = getFallbackIconFile();
            }
            String iconId = generateIconFileId(mIconSessionId);

            icons.put(backup, BackupIconEntity.create(iconId, mIconSessionId, iconFile));
        }

        mAppDb.runInTransaction(() -> {
            mIconDao.drop();
            mDao.dropAllEntries();
            for (Backup backup : newIndex) {
                BackupIconEntity iconEntity = Objects.requireNonNull(icons.get(backup));

                mDao.insertBackup(BackupEntity.fromBackup(backup, iconEntity.id));

                mIconDao.addIcon(iconEntity);

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

    private File getIconsDir() {
        File backupIconsDir = new File(mContext.getFilesDir(), "DaoBackedBackupIndex.Icons");
        if (!backupIconsDir.exists() && !backupIconsDir.mkdirs() && !backupIconsDir.exists()) {
            throw new RuntimeException("wtf, can't create icons dir");
        }

        return backupIconsDir;
    }

    private File writeBackupIcon(String sessionId, InputStream iconInputStream) throws IOException {
        File iconFile = new File(getIconsDir(), sessionId + "@" + UUID.randomUUID().toString());
        try (InputStream in = iconInputStream; FileOutputStream out = new FileOutputStream(iconFile)) {
            IOUtils.copyStream(in, out);
        } catch (Exception e) {
            iconFile.delete();
            throw e;
        }

        return iconFile;
    }

    private String generateIconFileId(String sessionId) {
        return sessionId + "/" + UUID.randomUUID().toString();
    }

    private String getSessionIdFromIconFile(File iconFile) {
        return iconFile.getName().split("@")[0];
    }

    private void vacuumIcons() {
        Stopwatch sw = new Stopwatch();
        Log.i(TAG, "Icons vacuum started");

        int filesSkipped = 0;
        int validFiles = 0;
        int filesDeleted = 0;
        try {
            File[] iconFiles = getIconsDir().listFiles();
            if (iconFiles == null || iconFiles.length == 0) {
                Log.i(TAG, "Icons vacuum cancelled, no icon files found");
                return;
            }

            for (File iconFile : iconFiles) {
                String sessionId = getSessionIdFromIconFile(iconFile);
                if (sessionId.equals(mIconSessionId)) {
                    filesSkipped++;
                    continue;
                }

                if (mIconDao.containsIcon(sessionId, iconFile.getAbsolutePath())) {
                    validFiles++;
                } else {
                    iconFile.delete();
                    filesDeleted++;
                }
            }

            Log.i(TAG, String.format("Icons vacuum finished in %d ms.\nValid files: %d\nSkipped files: %d\nDeleted files: %d", sw.millisSinceStart(), validFiles, filesSkipped, filesDeleted));
        } catch (Exception e) {
            Log.w(TAG, String.format("Icons vacuum failed, time spent - %d ms.\nValid files: %d\nSkipped files: %d\nDeleted files: %d", sw.millisSinceStart(), validFiles, filesSkipped, filesDeleted), e);
        }
    }

    private File getFallbackIconFile() throws IOException {
        File fallbackIconDir = new File(mContext.getFilesDir(), "DaoBackedBackupIndex.FallbackIcon");
        fallbackIconDir.mkdirs();

        File fallbackIconFile = new File(mContext.getFilesDir(), "icon.png");

        synchronized (mFallbackIconLock) {
            if (fallbackIconFile.exists()) {
                return fallbackIconFile;
            }

            File tempFile = new File(mContext.getFilesDir(), "icon.png.temp");
            try (InputStream in = mContext.getAssets().open("placeholder_app_icon.png"); OutputStream out = new FileOutputStream(fallbackIconFile)) {
                IOUtils.copyStream(in, out);
                tempFile.renameTo(fallbackIconFile);
            } catch (Exception e) {
                tempFile.delete();
                throw e;
            }

            return fallbackIconFile;
        }
    }
}
