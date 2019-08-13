package com.aefyr.sai.model.backup;

import android.os.Parcel;
import android.os.Parcelable;

public class PackageMeta implements Parcelable {

    public String packageName;
    public String label;
    public boolean hasSplits;
    public boolean isSystemApp;

    public PackageMeta(String packageName, String label) {
        this.packageName = packageName;
        this.label = label;
    }

    private PackageMeta(Parcel in) {
        packageName = in.readString();
        label = in.readString();
        hasSplits = in.readInt() == 1;
        isSystemApp = in.readInt() == 1;
    }

    public static final Creator<PackageMeta> CREATOR = new Creator<PackageMeta>() {
        @Override
        public PackageMeta createFromParcel(Parcel in) {
            return new PackageMeta(in);
        }

        @Override
        public PackageMeta[] newArray(int size) {
            return new PackageMeta[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeString(label);
        dest.writeInt(hasSplits ? 1 : 0);
        dest.writeInt(isSystemApp ? 1 : 0);
    }

    public static class Builder {
        private PackageMeta mPackageMeta;

        public Builder(String packageName) {
            mPackageMeta = new PackageMeta(packageName, "?");
        }

        public Builder setLabel(String label) {
            mPackageMeta.label = label;
            return this;
        }

        public Builder setHasSplits(boolean hasSplits) {
            mPackageMeta.hasSplits = hasSplits;
            return this;
        }

        public Builder setIsSystemApp(boolean isSystemApp) {
            mPackageMeta.isSystemApp = isSystemApp;
            return this;
        }

        public PackageMeta build() {
            return mPackageMeta;
        }
    }
}
