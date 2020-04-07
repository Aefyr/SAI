package com.aefyr.sai.model.apksource;

import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.model.filedescriptor.FileDescriptor;

import java.io.InputStream;
import java.util.List;

public class DefaultApkSource implements ApkSource {

    private List<FileDescriptor> mApkFileDescriptors;
    private FileDescriptor mCurrentApk;

    public DefaultApkSource(List<FileDescriptor> apkFileDescriptors) {
        mApkFileDescriptors = apkFileDescriptors;
    }

    @Override
    public boolean nextApk() {
        if (mApkFileDescriptors.size() == 0)
            return false;

        mCurrentApk = mApkFileDescriptors.remove(0);
        return true;
    }

    @Override
    public InputStream openApkInputStream() throws Exception {
        return mCurrentApk.open();
    }

    @Override
    public long getApkLength() throws Exception {
        return mCurrentApk.length();
    }

    @Override
    public String getApkName() throws Exception {
        return mCurrentApk.name();
    }

    @Override
    public String getApkLocalPath() throws Exception {
        return mCurrentApk.name();
    }

    @Nullable
    @Override
    public String getAppName() {
        try {
            return mApkFileDescriptors.size() == 1 ? mApkFileDescriptors.get(0).name() : null;
        } catch (Exception e) {
            Log.w("DefaultApkSource", "Unable to get app name", e);
            return null;
        }
    }
}
