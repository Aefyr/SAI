package com.aefyr.sai.backup;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;

import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.BackupNameFormat;
import com.aefyr.sai.utils.DbgPreferencesHelper;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.utils.saf.FileUtils;
import com.aefyr.sai.utils.saf.SafUtils;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

public class BackupUtils {
    private static final String TAG = "BackupUtils";

    @SuppressLint("DefaultLocale")
    @Nullable
    public static Uri createBackupFile(Context c, Uri backupDirUri, PackageMeta packageMeta) {

        if (ContentResolver.SCHEME_FILE.equals(backupDirUri.getScheme())) {
            return createBackupUriViaFileIO(c, new File(Objects.requireNonNull(backupDirUri.getPath())), packageMeta);
        } else if (ContentResolver.SCHEME_CONTENT.equals(backupDirUri.getScheme())) {
            return createBackupUriViaSaf(c, backupDirUri, packageMeta);
        }

        return null;
    }

    @SuppressLint("DefaultLocale")
    @Nullable
    private static Uri createBackupUriViaFileIO(Context c, File backupsDir, PackageMeta packageMeta) {
        if (!backupsDir.exists() && !backupsDir.mkdir()) {
            Log.e(TAG, "Unable to mkdir:" + backupsDir.toString());
            return null;
        }

        String backupFileName = getFileNameForPackageMeta(c, packageMeta);

        File backupFile = new File(backupsDir, Utils.escapeFileName(String.format("%s.apks", backupFileName)));
        int suffix = 0;
        while (backupFile.exists()) {
            suffix++;
            backupFile = new File(backupsDir, Utils.escapeFileName(String.format("%s(%d).apks", backupFileName, suffix)));
        }

        try {
            if (!backupFile.createNewFile())
                return null;
        } catch (IOException e) {
            Log.e(TAG, "Unable to create backup file", e);
            return null;
        }

        return Uri.fromFile(backupFile);
    }

    @SuppressLint("DefaultLocale")
    @Nullable
    private static Uri createBackupUriViaSaf(Context c, Uri backupDirUri, PackageMeta packageMeta) {
        DocumentFile backupDirFile = DocumentFile.fromTreeUri(c, backupDirUri);
        if (backupDirFile == null)
            return null;

        String backupFileName = getFileNameForPackageMeta(c, packageMeta);

        String actualBackupFileName = String.format("%s.apks", backupFileName);
        int suffix = 0;
        while (true) {
            DocumentFile backupFileCandidate = DocumentFile.fromSingleUri(c, SafUtils.buildChildDocumentUri(backupDirUri, actualBackupFileName));
            if (backupFileCandidate == null || !backupFileCandidate.exists())
                break;

            actualBackupFileName = String.format("%s(%d).apks", backupFileName, ++suffix);
        }

        DocumentFile backupFile = backupDirFile.createFile("saf/sucks", FileUtils.buildValidFatFilename(actualBackupFileName));
        if (backupFile == null)
            return null;

        return backupFile.getUri();
    }

    private static String getFileNameForPackageMeta(Context c, PackageMeta packageMeta) {
        String backupFileName = BackupNameFormat.format(PreferencesHelper.getInstance(c).getBackupFileNameFormat(), packageMeta);
        if (DbgPreferencesHelper.getInstance(c).shouldReplaceDots())
            backupFileName = backupFileName.replace('.', ',');

        if (backupFileName.length() > 160)
            backupFileName = backupFileName.substring(0, 160);

        return Utils.escapeFileName(backupFileName);
    }
}
