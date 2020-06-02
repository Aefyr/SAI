package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.aefyr.flexfilter.applier.ComplexCustomFilter;
import com.aefyr.flexfilter.applier.CustomFilter;
import com.aefyr.flexfilter.applier.LiveFilterApplier;
import com.aefyr.flexfilter.builtin.DefaultCustomFilterFactory;
import com.aefyr.flexfilter.builtin.filter.singlechoice.SingleChoiceFilterConfig;
import com.aefyr.flexfilter.builtin.filter.sort.SortFilterConfig;
import com.aefyr.flexfilter.builtin.filter.sort.SortFilterConfigOption;
import com.aefyr.flexfilter.config.core.ComplexFilterConfig;
import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.adapters.selection.SimpleKeyStorage;
import com.aefyr.sai.backup2.BackupApp;
import com.aefyr.sai.backup2.BackupManager;
import com.aefyr.sai.backup2.BackupStatus;
import com.aefyr.sai.backup2.BackupStorageProvider;
import com.aefyr.sai.backup2.impl.DefaultBackupManager;
import com.aefyr.sai.model.backup.BackupPackagesFilterConfig;
import com.aefyr.sai.utils.Stopwatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

//TODO applier should have setConfig or something
public class BackupViewModel extends AndroidViewModel {
    private static final String TAG = "BackupVM";

    private SharedPreferences mFilterPrefs;

    private BackupManager mBackupManager;
    private Observer<List<BackupApp>> mBackupRepoPackagesObserver;

    private ComplexFilterConfig mComplexFilterConfig;
    private final BackupCustomFilterFactory mFilterFactory = new BackupCustomFilterFactory();

    private String mCurrentSearchQuery = "";

    private MutableLiveData<List<BackupApp>> mPackagesLiveData = new MutableLiveData<>();

    private MutableLiveData<BackupPackagesFilterConfig> mBackupFilterConfig = new MutableLiveData<>();

    private final SimpleKeyStorage<String> mKeyStorage = new SimpleKeyStorage<>();
    private final Selection<String> mSelection = new Selection<>(mKeyStorage);

    private LiveFilterApplier<BackupApp> mLiveFilterApplier = new LiveFilterApplier<>();
    private final Observer<List<BackupApp>> mLiveFilterObserver = (apps) -> {
        mPackagesLiveData.setValue(apps);

        if (mSelection.hasSelection())
            reviseSelection(apps);
    };


    public BackupViewModel(@NonNull Application application) {
        super(application);

        mFilterPrefs = application.getSharedPreferences("backup_filter", Context.MODE_PRIVATE);

        BackupPackagesFilterConfig filterConfig = new BackupPackagesFilterConfig(mFilterPrefs);
        mBackupFilterConfig.setValue(filterConfig);
        mComplexFilterConfig = filterConfig.toComplexFilterConfig(application);

        mPackagesLiveData.setValue(new ArrayList<>());

        mBackupManager = DefaultBackupManager.getInstance(getApplication());
        mBackupRepoPackagesObserver = (packages) -> search(mCurrentSearchQuery);
        mBackupManager.getApps().observeForever(mBackupRepoPackagesObserver);

        mLiveFilterApplier.asLiveData().observeForever(mLiveFilterObserver);
        mBackupFilterConfig.setValue(new BackupPackagesFilterConfig(mComplexFilterConfig));
    }

    public void applyFilterConfig(ComplexFilterConfig config) {
        mComplexFilterConfig = config;

        BackupPackagesFilterConfig newFilterConfig = new BackupPackagesFilterConfig(config);
        newFilterConfig.saveToPrefs(mFilterPrefs);
        mBackupFilterConfig.setValue(newFilterConfig);

        search(mCurrentSearchQuery);
    }

    public ComplexFilterConfig getRawFilterConfig() {
        return mComplexFilterConfig;
    }

    public LiveData<List<BackupApp>> getPackages() {
        return mPackagesLiveData;
    }

    public LiveData<BackupPackagesFilterConfig> getBackupFilterConfig() {
        return mBackupFilterConfig;
    }

    public void search(String query) {
        mCurrentSearchQuery = query;
        mLiveFilterApplier.apply(createComplexFilter(query), new ArrayList<>(mBackupManager.getApps().getValue()));
    }

    public Selection<String> getSelection() {
        return mSelection;
    }

    public void selectAllApps() {
        List<BackupApp> packages = getPackages().getValue();
        if (packages == null)
            return;

        Collection<String> keys = new ArrayList<>(packages.size());
        for (BackupApp pkg : packages) {
            keys.add(pkg.packageMeta().packageName);
        }

        getSelection().batchSetSelected(keys, true);
    }

    public void reindexBackups() {
        mBackupManager.reindex();
    }

    public LiveData<BackupManager.IndexingStatus> getIndexingStatus() {
        return mBackupManager.getIndexingStatus();
    }

    public BackupStorageProvider getDefaultStorageProvider() {
        return mBackupManager.getDefaultBackupStorageProvider();
    }

    private void reviseSelection(List<BackupApp> newPackagesList) {
        Stopwatch sw = new Stopwatch();

        HashSet<String> newPackageListPackages = new HashSet<>();
        for (BackupApp app : newPackagesList) {
            if (app.isInstalled())
                newPackageListPackages.add(app.packageMeta().packageName);
        }

        ArrayList<String> packagesToDeselect = new ArrayList<>();
        for (String selectedPackage : mSelection.getSelectedKeys()) {
            if (!newPackageListPackages.contains(selectedPackage))
                packagesToDeselect.add(selectedPackage);
        }

        mSelection.batchSetSelected(packagesToDeselect, false);

        Log.d(TAG, String.format("Revised selection in %d ms.", sw.millisSinceStart()));
    }

    private ComplexCustomFilter<BackupApp> createComplexFilter(String searchQuery) {
        return new ComplexCustomFilter.Builder<BackupApp>()
                .with(mComplexFilterConfig, mFilterFactory)
                .add(new SearchFilter(searchQuery))
                .build();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mBackupManager.getApps().removeObserver(mBackupRepoPackagesObserver);
        mLiveFilterApplier.asLiveData().removeObserver(mLiveFilterObserver);
    }

    private static class SearchFilter implements CustomFilter<BackupApp> {

        private String mQuery;

        SearchFilter(String query) {
            mQuery = query;
        }

        @Override
        public boolean filterSimple(BackupApp app) {
            if (mQuery.length() == 0)
                return false;

            //Check if app label matches
            String[] wordsInLabel = app.packageMeta().label.toLowerCase().split(" ");
            boolean labelMatches = false;
            for (String word : wordsInLabel) {
                if (word.startsWith(mQuery)) {
                    labelMatches = true;
                    break;
                }
            }

            //Check if app packages matches
            boolean packagesMatches = app.packageMeta().packageName.toLowerCase().startsWith(mQuery);

            return !labelMatches && !packagesMatches;
        }
    }

    //TODO clean this up
    private static class BackupCustomFilterFactory implements DefaultCustomFilterFactory<BackupApp> {

        @Override
        public CustomFilter<BackupApp> createCustomSingleChoiceFilter(SingleChoiceFilterConfig config) {
            switch (config.id()) {
                case BackupPackagesFilterConfig.FILTER_SPLIT:
                    return createSplitFilter(config);
                case BackupPackagesFilterConfig.FILTER_SYSTEM_APP:
                    return createSystemAppFilter(config);
                case BackupPackagesFilterConfig.FILTER_BACKUP_STATUS:
                    return createBackupStatusFilter(config);
            }
            throw new IllegalArgumentException("Unsupported filter: " + config.id());
        }

        @Override
        public CustomFilter<BackupApp> createCustomSortFilter(SortFilterConfig config) {
            return new CustomFilter<BackupApp>() {
                @Override
                public List<BackupApp> filterComplex(List<BackupApp> list) {
                    SortFilterConfigOption selectedOption = config.getSelectedOption();
                    switch (selectedOption.id()) {
                        case BackupPackagesFilterConfig.SORT_NAME:
                            Collections.sort(list, (o1, o2) -> (selectedOption.ascending() ? 1 : -1) * o1.packageMeta().label.compareToIgnoreCase(o2.packageMeta().label));
                            break;
                        case BackupPackagesFilterConfig.SORT_INSTALL:
                            Collections.sort(list, (o1, o2) -> (selectedOption.ascending() ? 1 : -1) * Long.compare(o1.packageMeta().installTime, o2.packageMeta().installTime));
                            break;
                        case BackupPackagesFilterConfig.SORT_UPDATE:
                            Collections.sort(list, (o1, o2) -> (selectedOption.ascending() ? 1 : -1) * Long.compare(o1.packageMeta().updateTime, o2.packageMeta().updateTime));
                            break;
                    }

                    return list;
                }
            };
        }

        private CustomFilter<BackupApp> createSplitFilter(SingleChoiceFilterConfig config) {
            return new CustomFilter<BackupApp>() {
                @Override
                public boolean filterSimple(BackupApp app) {
                    String selectedOption = config.getSelectedOption().id();
                    switch (selectedOption) {
                        case BackupPackagesFilterConfig.FILTER_MODE_YES:
                            return !app.packageMeta().hasSplits;
                        case BackupPackagesFilterConfig.FILTER_MODE_NO:
                            return app.packageMeta().hasSplits;
                    }

                    return false;
                }
            };
        }

        private CustomFilter<BackupApp> createSystemAppFilter(SingleChoiceFilterConfig config) {
            return new CustomFilter<BackupApp>() {
                @Override
                public boolean filterSimple(BackupApp app) {
                    String selectedOption = config.getSelectedOption().id();
                    switch (selectedOption) {
                        case BackupPackagesFilterConfig.FILTER_MODE_YES:
                            return !app.packageMeta().isSystemApp;
                        case BackupPackagesFilterConfig.FILTER_MODE_NO:
                            return app.packageMeta().isSystemApp;
                    }

                    return false;
                }
            };
        }

        private CustomFilter<BackupApp> createBackupStatusFilter(SingleChoiceFilterConfig config) {
            return new CustomFilter<BackupApp>() {
                @Override
                public boolean filterSimple(BackupApp app) {
                    String selectedOption = config.getSelectedOption().id();
                    switch (selectedOption) {
                        case BackupPackagesFilterConfig.FILTER_BACKUP_STATUS_MODE_NO_BACKUP:
                            return app.backupStatus() != BackupStatus.NO_BACKUP;
                        case BackupPackagesFilterConfig.FILTER_BACKUP_STATUS_MODE_SAME_VERSION:
                            return app.backupStatus() != BackupStatus.SAME_VERSION;
                        case BackupPackagesFilterConfig.FILTER_BACKUP_STATUS_MODE_HIGHER_VERSION:
                            return app.backupStatus() != BackupStatus.HIGHER_VERSION;
                        case BackupPackagesFilterConfig.FILTER_BACKUP_STATUS_MODE_LOWER_VERSION:
                            return app.backupStatus() != BackupStatus.LOWER_VERSION;
                        case BackupPackagesFilterConfig.FILTER_BACKUP_STATUS_MODE_APP_NOT_INSTALLED:
                            return app.backupStatus() != BackupStatus.APP_NOT_INSTALLED;
                    }

                    return false;
                }
            };
        }
    }
}
