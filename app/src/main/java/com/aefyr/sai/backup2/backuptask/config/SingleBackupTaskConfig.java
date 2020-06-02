package com.aefyr.sai.backup2.backuptask.config;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.Nullable;

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

    private SingleBackupTaskConfig(@Nullable String backupStorageId, PackageMeta packageMeta) {
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
    }

    public PackageMeta packageMeta() {
        return mPackageMeta;
    }

    public List<File> apksToBackup() {
        return mApksToBackup;
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

        public SingleBackupTaskConfig.Builder addApk(File apkFile) {
            mConfig.mApksToBackup.add(apkFile);
            return this;
        }

        public SingleBackupTaskConfig.Builder addAllApks(Collection<File> apkFiles) {
            mConfig.mApksToBackup.addAll(apkFiles);
            return this;
        }

        public SingleBackupTaskConfig build() {
            return mConfig;
        }
    }


}
