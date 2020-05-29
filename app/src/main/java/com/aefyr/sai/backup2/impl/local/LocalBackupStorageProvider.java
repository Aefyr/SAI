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
import com.aefyr.sai.backup2.impl.local.ui.fragments.LocalBackupStorageConfigFragment;
import com.aefyr.sai.utils.saf.SafUtils;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class LocalBackupStorageProvider implements BackupStorageProvider {
    public static final String STORAGE_ID = BuildConfig.APPLICATION_ID + ".local_storage";
    private static final String PREFS_KEY_BACKUP_DIR_URI = "backup_dir_uri";


    private static LocalBackupStorageProvider sInstance;

    private Context mContext;
    private SharedPreferences mPrefs;
    private LocalBackupStorage mStorage;

    private AtomicBoolean mIsConfigured = new AtomicBoolean(false);
    private MutableLiveData<Boolean> mIsConfiguredLiveData = new MutableLiveData<>(false);

    private Map<OnConfigChangeListener, OnConfigChangeListenerHandlerWrapper> mConfigChangeListeners = new ConcurrentHashMap<>();

    public static synchronized LocalBackupStorageProvider getInstance(Context context) {
        return sInstance != null ? sInstance : new LocalBackupStorageProvider(context);
    }

    private LocalBackupStorageProvider(Context context) {
        mContext = context.getApplicationContext();
        mPrefs = context.getSharedPreferences("local_backup_storage", Context.MODE_PRIVATE);
        mStorage = new LocalBackupStorage(this, mContext);

        Uri backupDirUri = getBackupDirUri();
        if (backupDirUri != null && !isUriValid(backupDirUri)) {
            invalidateBackupDirUri();
        }

        setIsConfigured(getBackupDirUri() != null);

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
    public Fragment createConfigFragment() {
        return LocalBackupStorageConfigFragment.newInstance();
    }

    @Override
    public LiveData<Boolean> getIsConfiguredLiveData() {
        return mIsConfiguredLiveData;
    }

    @Override
    public boolean isConfigured() {
        return mIsConfigured.get();
    }

    @Override
    public BackupStorage getStorage() {
        return mStorage;
    }

    public void setBackupDirUri(Uri uri) {
        if (uri.equals(getBackupDirUri()))
            return;

        mPrefs.edit().putString(PREFS_KEY_BACKUP_DIR_URI, uri.toString()).apply();

        setIsConfigured(true);
        notifyBackupDirChanged();
    }

    @Nullable
    public Uri getBackupDirUri() {
        String rawUri = mPrefs.getString(PREFS_KEY_BACKUP_DIR_URI, null);
        if (rawUri == null)
            return null;

        return Uri.parse(rawUri);
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

    private void setIsConfigured(boolean isConfigured) {
        mIsConfigured.set(isConfigured);
        mIsConfiguredLiveData.postValue(isConfigured);
    }

    private void invalidateBackupDirUri() {
        mPrefs.edit().remove(PREFS_KEY_BACKUP_DIR_URI).apply();
        setIsConfigured(false);
        notifyBackupDirChanged();
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
