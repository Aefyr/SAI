package com.aefyr.sai.model.backup;

import android.os.Parcel;
import android.os.Parcelable;

public class PackageMeta implements Parcelable {

    public String packageName;
    public String label;

    public PackageMeta(String packageName, String label) {
        this.packageName = packageName;
        this.label = label;
    }

    private PackageMeta(Parcel in) {
        packageName = in.readString();
        label = in.readString();
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
    }
}
