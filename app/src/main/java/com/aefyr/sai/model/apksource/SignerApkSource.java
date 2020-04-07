package com.aefyr.sai.model.apksource;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.pseudoapksigner.PseudoApkSigner;
import com.aefyr.sai.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SignerApkSource implements ApkSource {
    private static final String TAG = "SignerApkSource";
    private static final String FILE_NAME_PAST = "testkey.past";
    private static final String FILE_NAME_PRIVATE_KEY = "testkey.pk8";

    private ApkSource mWrappedApkSource;
    private Context mContext;
    private boolean mIsPrepared;
    private PseudoApkSigner mApkSigner;
    private File mTempDir;

    private File mCurrentSignedApkFile;

    public SignerApkSource(Context c, ApkSource apkSource) {
        mContext = c;
        mWrappedApkSource = apkSource;
    }

    @Override
    public boolean nextApk() throws Exception {
        if (!mWrappedApkSource.nextApk()) {
            return false;
        }

        if (!mIsPrepared) {
            checkAndPrepareSigningEnvironment();
            createTempDir();
            mApkSigner = new PseudoApkSigner(new File(getSigningEnvironmentDir(), FILE_NAME_PAST), new File(getSigningEnvironmentDir(), FILE_NAME_PRIVATE_KEY));
        }

        mCurrentSignedApkFile = new File(mTempDir, getApkName());
        mApkSigner.sign(mWrappedApkSource.openApkInputStream(), new FileOutputStream(mCurrentSignedApkFile));

        return true;
    }

    @Override
    public InputStream openApkInputStream() throws Exception {
        return new FileInputStream(mCurrentSignedApkFile);
    }

    @Override
    public long getApkLength() {
        return mCurrentSignedApkFile.length();
    }

    @Override
    public String getApkName() throws Exception {
        return mWrappedApkSource.getApkName();
    }

    @Override
    public String getApkLocalPath() throws Exception {
        return mWrappedApkSource.getApkLocalPath();
    }

    @Override
    public void close() throws Exception {
        if (mTempDir != null)
            IOUtils.deleteRecursively(mTempDir);

        mWrappedApkSource.close();
    }

    @Nullable
    @Override
    public String getAppName() {
        return mWrappedApkSource.getAppName();
    }

    private void checkAndPrepareSigningEnvironment() throws Exception {
        File signingEnvironment = getSigningEnvironmentDir();
        File pastFile = new File(signingEnvironment, FILE_NAME_PAST);
        File privateKeyFile = new File(signingEnvironment, FILE_NAME_PRIVATE_KEY);

        if (pastFile.exists() && privateKeyFile.exists()) {
            mIsPrepared = true;
            return;
        }

        Log.d(TAG, "Preparing signing environment...");
        signingEnvironment.mkdir();

        IOUtils.copyFileFromAssets(mContext, FILE_NAME_PAST, pastFile);
        IOUtils.copyFileFromAssets(mContext, FILE_NAME_PRIVATE_KEY, privateKeyFile);

        mIsPrepared = true;
    }

    private File getSigningEnvironmentDir() {
        return new File(mContext.getFilesDir(), "signing");
    }

    private void createTempDir() {
        mTempDir = new File(mContext.getFilesDir(), String.valueOf(System.currentTimeMillis()));
        mTempDir.mkdirs();
    }
}
