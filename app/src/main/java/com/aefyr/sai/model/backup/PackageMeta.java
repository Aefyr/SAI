package com.aefyr.sai.model.backup;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

public class PackageMeta implements Parcelable {

    public String packageName;
    public String label;
    public boolean hasSplits;
    public boolean isSystemApp;
    public long versionCode;
    public String versionName;
    public Uri iconUri;

    public PackageMeta(String packageName, String label) {
        this.packageName = packageName;
        this.label = label;
    }

    private PackageMeta(Parcel in) {
        packageName = in.readString();
        label = in.readString();
        hasSplits = in.readInt() == 1;
        isSystemApp = in.readInt() == 1;
        versionCode = in.readLong();
        versionName = in.readString();
        iconUri = in.readParcelable(Uri.class.getClassLoader());
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
        dest.writeLong(versionCode);
        dest.writeString(versionName);
        dest.writeParcelable(iconUri, 0);
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

        public Builder serVersionCode(long versionCode) {
            mPackageMeta.versionCode = versionCode;
            return this;
        }

        public Builder setVersionName(String versionName) {
            mPackageMeta.versionName = versionName;
            return this;
        }

        public Builder setIcon(int iconResId) {
            if (iconResId == 0) {
                mPackageMeta.iconUri = null;
                return this;
            }

            mPackageMeta.iconUri = new Uri.Builder()
                    .scheme(ContentResolver.SCHEME_ANDROID_RESOURCE)
                    .authority(mPackageMeta.packageName)
                    .path(String.valueOf(iconResId))
                    .build();

            return this;
        }

        public PackageMeta build() {
            return mPackageMeta;
        }
    }
}
