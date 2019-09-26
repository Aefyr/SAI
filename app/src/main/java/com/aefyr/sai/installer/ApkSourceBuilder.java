package com.aefyr.sai.installer;

import android.content.Context;
import android.net.Uri;

import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.model.apksource.DefaultApkSource;
import com.aefyr.sai.model.apksource.SignerApkSource;
import com.aefyr.sai.model.apksource.ZipApkSource;
import com.aefyr.sai.model.apksource.ZipExtractorApkSource;
import com.aefyr.sai.model.filedescriptor.ContentUriFileDescriptor;
import com.aefyr.sai.model.filedescriptor.FileDescriptor;
import com.aefyr.sai.model.filedescriptor.NormalFileDescriptor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ApkSourceBuilder {

    private Context mContext;

    private boolean mSourceSet;
    private List<File> mApkFiles;
    private File mZipFile;
    private Uri mZipUri;
    private List<Uri> mApkUris;

    private boolean mSigningEnabled;
    private boolean mZipExtractionEnabled;

    public ApkSourceBuilder(Context c) {
        mContext = c;
    }

    public ApkSourceBuilder fromApkFiles(List<File> apkFiles) {
        ensureSourceSetOnce();
        mApkFiles = apkFiles;
        return this;
    }

    public ApkSourceBuilder fromZipFile(File zipFile) {
        ensureSourceSetOnce();
        mZipFile = zipFile;
        return this;
    }

    public ApkSourceBuilder fromZipContentUri(Uri zipUri) {
        ensureSourceSetOnce();
        mZipUri = zipUri;
        return this;
    }

    public ApkSourceBuilder fromApkContentUris(List<Uri> uris) {
        ensureSourceSetOnce();
        mApkUris = uris;
        return this;
    }

    public ApkSourceBuilder setSigningEnabled(boolean enabled) {
        mSigningEnabled = enabled;
        return this;
    }

    public ApkSourceBuilder setZipExtractionEnabled(boolean enabled) {
        mZipExtractionEnabled = enabled;
        return this;
    }

    public ApkSource build() {
        ApkSource apkSource;

        if (mApkFiles != null) {
            List<FileDescriptor> apkFileDescriptors = new ArrayList<>(mApkFiles.size());
            for (File apkFile : mApkFiles)
                apkFileDescriptors.add(new NormalFileDescriptor(apkFile));

            apkSource = new DefaultApkSource(apkFileDescriptors);
        } else if (mZipFile != null) {
            if (mZipExtractionEnabled)
                apkSource = new ZipExtractorApkSource(mContext, new NormalFileDescriptor(mZipFile));
            else
                apkSource = new ZipApkSource(mContext, new NormalFileDescriptor(mZipFile));
        } else if (mZipUri != null) {
            if (mZipExtractionEnabled)
                apkSource = new ZipExtractorApkSource(mContext, new ContentUriFileDescriptor(mContext, mZipUri));
            else
                apkSource = new ZipApkSource(mContext, new ContentUriFileDescriptor(mContext, mZipUri));
        } else if (mApkUris != null) {
            List<FileDescriptor> apkUriDescriptors = new ArrayList<>(mApkUris.size());
            for (Uri apkUri : mApkUris)
                apkUriDescriptors.add(new ContentUriFileDescriptor(mContext, apkUri));

            apkSource = new DefaultApkSource(apkUriDescriptors);
        } else {
            throw new IllegalStateException("No source set");
        }

        if (mSigningEnabled)
            apkSource = new SignerApkSource(mContext, apkSource);

        return apkSource;
    }

    private void ensureSourceSetOnce() {
        if (mSourceSet)
            throw new IllegalStateException("Source can be only be set once");
        mSourceSet = true;
    }
}
