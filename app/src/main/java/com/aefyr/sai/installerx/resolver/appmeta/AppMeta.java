package com.aefyr.sai.installerx.resolver.appmeta;

import android.net.Uri;

public class AppMeta {

    public String packageName;
    public String appName;
    public long versionCode;
    public String versionName;
    public Uri iconUri;

    public static class Builder {
        private AppMeta mMeta;

        public Builder() {
            mMeta = new AppMeta();
        }

        public Builder setPackageName(String packageName) {
            mMeta.packageName = packageName;
            return this;
        }

        public Builder setAppName(String appName) {
            mMeta.appName = appName;
            return this;
        }

        public Builder setVersionCode(long versionCode) {
            mMeta.versionCode = versionCode;
            return this;
        }

        public Builder setVersionName(String versionName) {
            mMeta.versionName = versionName;
            return this;
        }

        public Builder setIconUri(Uri iconUri) {
            mMeta.iconUri = iconUri;
            return this;
        }

        public AppMeta build() {
            return mMeta;
        }
    }

}
