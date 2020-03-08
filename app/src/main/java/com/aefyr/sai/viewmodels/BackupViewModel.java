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
import com.aefyr.sai.backup.BackupRepository;
import com.aefyr.sai.model.backup.BackupPackagesFilterConfig;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.Event2;
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

    private BackupRepository mBackupRepo;
    private Observer<List<PackageMeta>> mBackupRepoPackagesObserver;

    private ComplexFilterConfig mComplexFilterConfig;
    private final BackupCustomFilterFactory mFilterFactory = new BackupCustomFilterFactory();

    private String mCurrentSearchQuery = "";

    private MutableLiveData<List<PackageMeta>> mPackagesLiveData = new MutableLiveData<>();

    private MutableLiveData<BackupPackagesFilterConfig> mBackupFilterConfig = new MutableLiveData<>();

    private final SimpleKeyStorage<String> mKeyStorage = new SimpleKeyStorage<>();
    private final Selection<String> mSelection = new Selection<>(mKeyStorage);

    private MutableLiveData<Event2> mSelectionClearEvent = new MutableLiveData<>();

    private LiveFilterApplier<PackageMeta> mLiveFilterApplier = new LiveFilterApplier<>();
    private final Observer<List<PackageMeta>> mLiveFilterObserver = (packages) -> {
        mPackagesLiveData.setValue(packages);

        if (mSelection.hasSelection())
            reviseSelection(packages);
    };


    public BackupViewModel(@NonNull Application application) {
        super(application);

        mFilterPrefs = application.getSharedPreferences("backup_filter", Context.MODE_PRIVATE);

        BackupPackagesFilterConfig filterConfig = new BackupPackagesFilterConfig(mFilterPrefs);
        mBackupFilterConfig.setValue(filterConfig);
        mComplexFilterConfig = filterConfig.toComplexFilterConfig(application);

        mPackagesLiveData.setValue(new ArrayList<>());

        mBackupRepo = BackupRepository.getInstance(application);
        mBackupRepoPackagesObserver = (packages) -> search(mCurrentSearchQuery);
        mBackupRepo.getPackages().observeForever(mBackupRepoPackagesObserver);

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

    public LiveData<List<PackageMeta>> getPackages() {
        return mPackagesLiveData;
    }

    public LiveData<BackupPackagesFilterConfig> getBackupFilterConfig() {
        return mBackupFilterConfig;
    }

    public void search(String query) {
        mCurrentSearchQuery = query;
        mLiveFilterApplier.apply(createComplexFilter(query), new ArrayList<>(mBackupRepo.getPackages().getValue()));
    }

    public Selection<String> getSelection() {
        return mSelection;
    }

    public LiveData<Event2> getSelectionClearEvent() {
        return mSelectionClearEvent;
    }

    public void selectAllApps() {
        List<PackageMeta> packages = getPackages().getValue();
        if (packages == null)
            return;

        Collection<String> keys = new ArrayList<>(packages.size());
        for (PackageMeta pkg : packages) {
            keys.add(pkg.packageName);
        }

        getSelection().batchSetSelected(keys, true);
    }

    private void reviseSelection(List<PackageMeta> newPackagesList) {
        Stopwatch sw = new Stopwatch();

        HashSet<String> newPackageListPackages = new HashSet<>();
        for (PackageMeta packageMeta : newPackagesList) {
            newPackageListPackages.add(packageMeta.packageName);
        }

        ArrayList<String> packagesToDeselect = new ArrayList<>();
        for (String selectedPackage : mSelection.getSelectedKeys()) {
            if (!newPackageListPackages.contains(selectedPackage))
                packagesToDeselect.add(selectedPackage);
        }

        mSelection.batchSetSelected(packagesToDeselect, false);

        Log.d(TAG, String.format("Revised selection in %d ms.", sw.millisSinceStart()));
    }

    private ComplexCustomFilter<PackageMeta> createComplexFilter(String searchQuery) {
        return new ComplexCustomFilter.Builder<PackageMeta>()
                .with(mComplexFilterConfig, mFilterFactory)
                .add(new SearchFilter(searchQuery))
                .build();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        mBackupRepo.getPackages().removeObserver(mBackupRepoPackagesObserver);
        mLiveFilterApplier.asLiveData().removeObserver(mLiveFilterObserver);
    }

    private static class SearchFilter implements CustomFilter<PackageMeta> {

        private String mQuery;

        SearchFilter(String query) {
            mQuery = query;
        }

        @Override
        public boolean filterSimple(PackageMeta packageMeta) {
            if (mQuery.length() == 0)
                return false;

            //Check if app label matches
            String[] wordsInLabel = packageMeta.label.toLowerCase().split(" ");
            boolean labelMatches = false;
            for (String word : wordsInLabel) {
                if (word.startsWith(mQuery)) {
                    labelMatches = true;
                    break;
                }
            }

            //Check if app packages matches
            boolean packagesMatches = packageMeta.packageName.toLowerCase().startsWith(mQuery);

            return !labelMatches && !packagesMatches;
        }
    }

    //TODO clean this up
    private static class BackupCustomFilterFactory implements DefaultCustomFilterFactory<PackageMeta> {

        @Override
        public CustomFilter<PackageMeta> createCustomSingleChoiceFilter(SingleChoiceFilterConfig config) {
            switch (config.id()) {
                case BackupPackagesFilterConfig.FILTER_SPLIT:
                    return createSplitFilter(config);
                case BackupPackagesFilterConfig.FILTER_SYSTEM_APP:
                    return createSystemAppFilter(config);
            }
            throw new IllegalArgumentException("Unsupported filter: " + config.id());
        }

        @Override
        public CustomFilter<PackageMeta> createCustomSortFilter(SortFilterConfig config) {
            return new CustomFilter<PackageMeta>() {
                @Override
                public List<PackageMeta> filterComplex(List<PackageMeta> list) {
                    SortFilterConfigOption selectedOption = config.getSelectedOption();
                    switch (selectedOption.id()) {
                        case BackupPackagesFilterConfig.SORT_NAME:
                            Collections.sort(list, (o1, o2) -> (selectedOption.ascending() ? 1 : -1) * o1.label.compareToIgnoreCase(o2.label));
                            break;
                        case BackupPackagesFilterConfig.SORT_INSTALL:
                            Collections.sort(list, (o1, o2) -> (selectedOption.ascending() ? 1 : -1) * Long.compare(o1.installTime, o2.installTime));
                            break;
                        case BackupPackagesFilterConfig.SORT_UPDATE:
                            Collections.sort(list, (o1, o2) -> (selectedOption.ascending() ? 1 : -1) * Long.compare(o1.updateTime, o2.updateTime));
                            break;
                    }

                    return list;
                }
            };
        }

        private CustomFilter<PackageMeta> createSplitFilter(SingleChoiceFilterConfig config) {
            return new CustomFilter<PackageMeta>() {
                @Override
                public boolean filterSimple(PackageMeta packageMeta) {
                    String selectedOption = config.getSelectedOption().id();
                    switch (selectedOption) {
                        case BackupPackagesFilterConfig.FILTER_MODE_YES:
                            return !packageMeta.hasSplits;
                        case BackupPackagesFilterConfig.FILTER_MODE_NO:
                            return packageMeta.hasSplits;
                    }

                    return false;
                }
            };
        }

        private CustomFilter<PackageMeta> createSystemAppFilter(SingleChoiceFilterConfig config) {
            return new CustomFilter<PackageMeta>() {
                @Override
                public boolean filterSimple(PackageMeta packageMeta) {
                    String selectedOption = config.getSelectedOption().id();
                    switch (selectedOption) {
                        case BackupPackagesFilterConfig.FILTER_MODE_YES:
                            return !packageMeta.isSystemApp;
                        case BackupPackagesFilterConfig.FILTER_MODE_NO:
                            return packageMeta.isSystemApp;
                    }

                    return false;
                }
            };
        }
    }
}
