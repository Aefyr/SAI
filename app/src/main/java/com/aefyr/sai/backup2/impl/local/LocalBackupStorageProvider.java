package com.aefyr.sai.backup2.impl.local;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;
import com.aefyr.sai.backup2.BackupStorage;
import com.aefyr.sai.backup2.BackupStorageProvider;
import com.aefyr.sai.backup2.impl.local.prefs.LocalBackupStoragePrefConstants;
import com.aefyr.sai.backup2.impl.local.ui.fragments.LocalBackupStorageSettingsFragment;
import com.aefyr.sai.backup2.impl.local.ui.fragments.LocalBackupStorageSetupFragment;
import com.aefyr.sai.utils.saf.SafUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalBackupStorageProvider implements BackupStorageProvider, SharedPreferences.OnSharedPreferenceChangeListener {
    public static final String STORAGE_ID = BuildConfig.APPLICATION_ID + ".local_storage";

    private static LocalBackupStorageProvider sInstance;

    private Context mContext;
    private SharedPreferences mPrefs;
    private LocalBackupStorage mStorage;

    private AtomicBoolean mIsSetup = new AtomicBoolean(false);
    private MutableLiveData<Boolean> mIsSetupLiveData = new MutableLiveData<>(false);

    private Map<OnConfigChangeListener, OnConfigChangeListenerHandlerWrapper> mConfigChangeListeners = new ConcurrentHashMap<>();

    public static synchronized LocalBackupStorageProvider getInstance(Context context) {
        return sInstance != null ? sInstance : new LocalBackupStorageProvider(context);
    }

    private LocalBackupStorageProvider(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = context.getSharedPreferences(LocalBackupStoragePrefConstants.PREFS_NAME, Context.MODE_PRIVATE);
        mStorage = new LocalBackupStorage(this, mContext);

        mPrefs.registerOnSharedPreferenceChangeListener(this);

        Uri backupDirUri = getBackupDirUri();
        if (backupDirUri != null && !isUriValid(backupDirUri)) {
            mPrefs.edit().remove(LocalBackupStoragePrefConstants.KEY_BACKUP_DIR_URI).apply();
        }

        invalidateIsSetup();

        sInstance = this;
    }

    @Override
    public String getId() {
        return STORAGE_ID;
    }

    @Override
    public String getName() {
        return mContext.getString(R.string.backup_lbs_provider_name);
    }

    @Override
    public Fragment createSettingsFragment() {
        return LocalBackupStorageSettingsFragment.newInstance();
    }

    @Override
    public LiveData<Boolean> getIsSetupLiveData() {
        return mIsSetupLiveData;
    }

    @Override
    public Fragment createSetupFragment() {
        return LocalBackupStorageSetupFragment.newInstance();
    }

    @Override
    public boolean isSetup() {
        return mIsSetup.get();
    }

    @Override
    public BackupStorage getStorage() {
        return mStorage;
    }

    @Nullable
    public Uri getBackupDirUri() {
        String rawUri = mPrefs.getString(LocalBackupStoragePrefConstants.KEY_BACKUP_DIR_URI, null);
        if (rawUri == null)
            return null;

        return Uri.parse(rawUri);
    }

    public void setBackupDirUri(Uri uri) {
        mPrefs.edit().putString(LocalBackupStoragePrefConstants.KEY_BACKUP_DIR_URI, uri.toString()).apply();
    }

    public String getBackupNameFormat() {
        return mPrefs.getString(LocalBackupStoragePrefConstants.KEY_BACKUP_FILE_NAME_FORMAT, LocalBackupStoragePrefConstants.DEFAULT_VALUE_BACKUP_FILE_NAME_FORMAT);
    }

    public void setBackupNameFormat(String format) {
        mPrefs.edit().putString(LocalBackupStoragePrefConstants.KEY_BACKUP_FILE_NAME_FORMAT, format).apply();
    }

    public void addOnConfigChangeListener(OnConfigChangeListener listener, Handler handler) {
        mConfigChangeListeners.put(listener, new OnConfigChangeListenerHandlerWrapper(listener, handler));
    }

    public void removeOnConfigChangeListener(OnConfigChangeListener listener) {
        mConfigChangeListeners.remove(listener);
    }

    private boolean isUriValid(Uri uri) {
        DocumentFile docFile = SafUtils.docFileFromTreeUriOrFileUri(mContext, uri);
        return docFile != null && docFile.canRead() && docFile.canWrite();
    }

    private void notifyBackupDirChanged() {
        for (OnConfigChangeListener listener : mConfigChangeListeners.values())
            listener.onBackupDirChanged();
    }

    private void invalidateIsSetup() {
        mIsSetup.set(getBackupDirUri() != null);
        mIsSetupLiveData.postValue(mIsSetup.get());
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (LocalBackupStoragePrefConstants.KEY_BACKUP_DIR_URI.equals(key)) {
            invalidateIsSetup();
            notifyBackupDirChanged();
        }
    }

    public interface OnConfigChangeListener {

        void onBackupDirChanged();

    }

    private static class OnConfigChangeListenerHandlerWrapper implements OnConfigChangeListener {

        private OnConfigChangeListener mListener;
        private Handler mHandler;


        private OnConfigChangeListenerHandlerWrapper(OnConfigChangeListener listener, Handler handler) {
            mListener = listener;
            mHandler = handler;
        }

        @Override
        public void onBackupDirChanged() {
            mHandler.post(() -> mListener.onBackupDirChanged());
        }
    }
}
