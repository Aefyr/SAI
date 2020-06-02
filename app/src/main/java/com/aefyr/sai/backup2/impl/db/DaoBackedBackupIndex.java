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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class DaoBackedBackupIndex implements BackupIndex {
    private static final String TAG = "DaoBackedBackupIndex";

    private static DaoBackedBackupIndex sInstance;

    private Context mContext;
    private AppDatabase mAppDb;

    private BackupDao mDao;

    private Set<File> mVacuumImmuneFiles = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public static synchronized DaoBackedBackupIndex getInstance(Context context) {
        return sInstance != null ? sInstance : new DaoBackedBackupIndex(context);
    }

    private DaoBackedBackupIndex(Context context) {
        mContext = context.getApplicationContext();
        mAppDb = AppDatabase.getInstance(mContext);
        mDao = mAppDb.backupDao();

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

        File iconFile = createIconFile();
        mVacuumImmuneFiles.add(iconFile);
        try {
            writeBackupIcon(iconFile, iconProvider.getIconInputStream(backup));
        } catch (Exception e) {
            Log.w(TAG, "Unable to write backup icon", e);
            writeDefaultIcon(iconFile);
        }

        mAppDb.runInTransaction(() -> {
            if (mDao.getBackupMetaForUri(backup.uri().toString()) != null)
                mDao.removeByUri(backup.uri().toString());

            BackupEntity backupEntity = BackupEntity.fromBackup(backup, iconFile);

            mDao.insertBackup(backupEntity);

            for (BackupComponent component : backup.components()) {
                mDao.insertBackupComponent(BackupComponentEntity.fromBackupComponent(backup.uri(), component));
            }
        });

        mVacuumImmuneFiles.remove(iconFile);
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

        Map<Backup, File> icons = new HashMap<>();

        for (Backup backup : newIndex) {
            File iconFile = createIconFile();
            mVacuumImmuneFiles.add(iconFile);
            try {
                writeBackupIcon(iconFile, iconProvider.getIconInputStream(backup));
            } catch (Exception e) {
                Log.w(TAG, "Unable to write backup icon", e);
                writeDefaultIcon(iconFile);
            }

            icons.put(backup, iconFile);
        }

        mAppDb.runInTransaction(() -> {
            mDao.dropAllEntries();
            for (Backup backup : newIndex) {
                File iconFile = Objects.requireNonNull(icons.get(backup));

                mDao.insertBackup(BackupEntity.fromBackup(backup, iconFile));

                for (BackupComponent component : backup.components()) {
                    mDao.insertBackupComponent(BackupComponentEntity.fromBackupComponent(backup.uri(), component));
                }
            }
        });

        for (File iconFile : icons.values()) {
            mVacuumImmuneFiles.remove(iconFile);
        }
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

    private File createIconFile() {
        return new File(getIconsDir(), UUID.randomUUID().toString() + ".png");
    }

    private void writeBackupIcon(File iconFile, InputStream iconInputStream) throws IOException {
        try (InputStream in = iconInputStream; FileOutputStream out = new FileOutputStream(iconFile)) {
            IOUtils.copyStream(in, out);
        } catch (Exception e) {
            iconFile.delete();
            throw e;
        }
    }

    private void writeDefaultIcon(File iconFile) throws IOException {
        writeBackupIcon(iconFile, mContext.getAssets().open("placeholder_app_icon.png"));
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
                if (mVacuumImmuneFiles.contains(iconFile)) {
                    filesSkipped++;
                    continue;
                }

                if (mDao.containsIcon(iconFile.getAbsolutePath())) {
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
}
