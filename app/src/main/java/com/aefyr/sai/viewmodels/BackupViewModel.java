package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
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
import com.aefyr.flexfilter.config.core.FilterConfig;
import com.aefyr.sai.R;
import com.aefyr.sai.backup.BackupRepository;
import com.aefyr.sai.model.backup.BackupPackagesFilterConfig;
import com.aefyr.sai.model.common.PackageMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

//TODO applier should have setConfig or something
public class BackupViewModel extends AndroidViewModel {

    private Context mContext;

    private BackupRepository mBackupRepo;
    private Observer<List<PackageMeta>> mBackupRepoPackagesObserver;

    private ComplexFilterConfig mFilterConfig;
    private final BackupCustomFilterFactory mFilterFactory = new BackupCustomFilterFactory();

    private String mCurrentSearchQuery = "";

    private MutableLiveData<List<PackageMeta>> mPackagesLiveData = new MutableLiveData<>();

    private MutableLiveData<BackupPackagesFilterConfig> mBackupFilterConfig = new MutableLiveData<>();

    private LiveFilterApplier<PackageMeta> mLiveFilterApplier = new LiveFilterApplier<>();
    private final Observer<List<PackageMeta>> mLiveFilterObserver = (packages) -> mPackagesLiveData.setValue(packages);


    public BackupViewModel(@NonNull Application application) {
        super(application);
        mContext = application;

        mFilterConfig = createDefaultFilterConfig();

        mPackagesLiveData.setValue(new ArrayList<>());

        mBackupRepo = BackupRepository.getInstance(mContext);
        mBackupRepoPackagesObserver = (packages) -> search(mCurrentSearchQuery);
        mBackupRepo.getPackages().observeForever(mBackupRepoPackagesObserver);

        mLiveFilterApplier.asLiveData().observeForever(mLiveFilterObserver);
        mBackupFilterConfig.setValue(new BackupPackagesFilterConfig(mFilterConfig));
    }

    public void applyFilterConfig(ComplexFilterConfig config) {
        mFilterConfig = config;
        mBackupFilterConfig.setValue(new BackupPackagesFilterConfig(config));
        search(mCurrentSearchQuery);
    }

    public ComplexFilterConfig getRawFilterConfig() {
        return mFilterConfig;
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

    private ComplexCustomFilter<PackageMeta> createComplexFilter(String searchQuery) {
        return new ComplexCustomFilter.Builder<PackageMeta>()
                .with(mFilterConfig, mFilterFactory)
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

    private String getString(@StringRes int id) {
        return getApplication().getString(id);
    }

    private SingleChoiceFilterConfig createYesNoWhateverFilterConfig(String id, CharSequence name) {
        return new SingleChoiceFilterConfig(id, name)
                .addOption(BackupPackagesFilterConfig.FILTER_MODE_WHATEVER, getString(R.string.backup_filter_common_option_doesnt_matter))
                .addOption(BackupPackagesFilterConfig.FILTER_MODE_YES, getString(R.string.backup_filter_common_option_yes))
                .addOption(BackupPackagesFilterConfig.FILTER_MODE_NO, getString(R.string.no));
    }

    private ComplexFilterConfig createDefaultFilterConfig() {
        ArrayList<FilterConfig> filters = new ArrayList<>();

        //Sort
        SortFilterConfig sortFilter = new SortFilterConfig("sort", getString(R.string.backup_filter_sort))
                .addOption(BackupPackagesFilterConfig.SORT_NAME, getString(R.string.backup_filter_sort_option_name))
                .addOption(BackupPackagesFilterConfig.SORT_INSTALL, getString(R.string.backup_filter_sort_option_installed))
                .addOption(BackupPackagesFilterConfig.SORT_UPDATE, getString(R.string.backup_filter_sort_option_updated));

        SortFilterConfigOption nameOption = sortFilter.options().get(0);
        nameOption.setSelected();
        nameOption.setAscending(true);

        filters.add(sortFilter);

        //Split APK
        SingleChoiceFilterConfig splitApkFilter = createYesNoWhateverFilterConfig(BackupPackagesFilterConfig.FILTER_SPLIT, getString(R.string.backup_filter_split_apk));
        splitApkFilter.options().get(1).setSelected();
        filters.add(splitApkFilter);

        //System app
        SingleChoiceFilterConfig systemAppFilter = createYesNoWhateverFilterConfig(BackupPackagesFilterConfig.FILTER_SYSTEM_APP, getString(R.string.backup_filter_system_app));
        systemAppFilter.options().get(0).setSelected();
        filters.add(systemAppFilter);

        return new ComplexFilterConfig(filters);
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
