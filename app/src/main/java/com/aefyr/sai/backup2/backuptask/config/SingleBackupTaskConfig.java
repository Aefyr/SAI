package com.aefyr.sai.backup2.backuptask.config;

import android.os.Parcel;
import android.os.Parcelable;

import com.aefyr.sai.model.common.PackageMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class SingleBackupTaskConfig implements Parcelable, BackupTaskConfig {

    public static final Parcelable.Creator<SingleBackupTaskConfig> CREATOR = new Parcelable.Creator<SingleBackupTaskConfig>() {
        @Override
        public SingleBackupTaskConfig createFromParcel(Parcel in) {
            return new SingleBackupTaskConfig(in);
        }

        @Override
        public SingleBackupTaskConfig[] newArray(int size) {
            return new SingleBackupTaskConfig[size];
        }
    };
    private String mBackupStorageId;
    private PackageMeta mPackageMeta;
    private ArrayList<File> mApksToBackup = new ArrayList<>();
    private boolean mExportMode = false;

    private SingleBackupTaskConfig(String backupStorageId, PackageMeta packageMeta) {
        mBackupStorageId = backupStorageId;
        mPackageMeta = packageMeta;
    }

    SingleBackupTaskConfig(Parcel in) {
        mBackupStorageId = in.readString();
        mPackageMeta = in.readParcelable(PackageMeta.class.getClassLoader());

        ArrayList<String> apkFilePaths = new ArrayList<>();
        in.readStringList(apkFilePaths);
        for (String apkFilePath : apkFilePaths)
            mApksToBackup.add(new File(apkFilePath));

        mExportMode = in.readByte() == 1;
    }

    public PackageMeta packageMeta() {
        return mPackageMeta;
    }

    public List<File> apksToBackup() {
        return mApksToBackup;
    }

    public boolean exportMode() {
        return mExportMode;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mBackupStorageId);
        dest.writeParcelable(mPackageMeta, flags);

        ArrayList<String> apkFilePaths = new ArrayList<>();
        for (File apkFile : mApksToBackup)
            apkFilePaths.add(apkFile.getAbsolutePath());
        dest.writeStringList(apkFilePaths);

        dest.writeByte((byte) (mExportMode ? 1 : 0));
    }

    @Override
    public String getBackupStorageId() {
        return mBackupStorageId;
    }

    public static class Builder {
        private SingleBackupTaskConfig mConfig;

        public Builder(String backupStorageId, PackageMeta packageMeta) {
            mConfig = new SingleBackupTaskConfig(backupStorageId, packageMeta);
        }

        public Builder addApk(File apkFile) {
            mConfig.mApksToBackup.add(apkFile);
            return this;
        }

        public Builder addAllApks(Collection<File> apkFiles) {
            mConfig.mApksToBackup.addAll(apkFiles);
            return this;
        }

        public Builder setExportMode(boolean exportMode) {
            mConfig.mExportMode = exportMode;
            return this;
        }

        public SingleBackupTaskConfig build() {
            return mConfig;
        }
    }


}
