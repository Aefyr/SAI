package com.aefyr.sai.model.backup;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.StringRes;

import com.aefyr.flexfilter.builtin.filter.singlechoice.SingleChoiceFilterConfig;
import com.aefyr.flexfilter.builtin.filter.singlechoice.SingleChoiceFilterConfigOption;
import com.aefyr.flexfilter.builtin.filter.sort.SortFilterConfig;
import com.aefyr.flexfilter.builtin.filter.sort.SortFilterConfigOption;
import com.aefyr.flexfilter.config.core.ComplexFilterConfig;
import com.aefyr.flexfilter.config.core.FilterConfig;
import com.aefyr.sai.R;

import java.util.ArrayList;

public class BackupPackagesFilterConfig {

    public static final String FILTER_SORT = "sort";

    public static final String SORT_NAME = "name";
    public static final String SORT_INSTALL = "install";
    public static final String SORT_UPDATE = "update";

    public static final String FILTER_SPLIT = "split";
    public static final String FILTER_SYSTEM_APP = "system_app";

    public static final String FILTER_MODE_YES = "yes";
    public static final String FILTER_MODE_NO = "no";
    public static final String FILTER_MODE_WHATEVER = "whatever";

    private static final String SORT_ASCENDING = "sort_ascending";

    public enum SimpleFilterMode {
        WHATEVER, YES, NO;

        public static SimpleFilterMode from(SingleChoiceFilterConfigOption option) {
            switch (option.id()) {
                case FILTER_MODE_YES:
                    return SimpleFilterMode.YES;
                case FILTER_MODE_NO:
                    return SimpleFilterMode.NO;
            }

            return SimpleFilterMode.WHATEVER;
        }
    }

    public enum SortMode {
        NAME, INSTALL_TIME, UPDATE_TIME
    }

    private SimpleFilterMode mSplitApkFilter;
    private SimpleFilterMode mSystemAppFilter;

    private SortMode mSortMode;
    private boolean mSortAscending;

    public BackupPackagesFilterConfig(ComplexFilterConfig config) {
        for (FilterConfig filterConfig : config.filters()) {
            if (filterConfig instanceof SingleChoiceFilterConfig) {
                switch (((SingleChoiceFilterConfig) filterConfig).id()) {
                    case FILTER_SPLIT:
                        mSplitApkFilter = SimpleFilterMode.from(((SingleChoiceFilterConfig) filterConfig).getSelectedOption());
                        break;
                    case FILTER_SYSTEM_APP:
                        mSystemAppFilter = SimpleFilterMode.from(((SingleChoiceFilterConfig) filterConfig).getSelectedOption());
                        break;
                }
                continue;
            }

            if (filterConfig instanceof SortFilterConfig) {
                SortFilterConfigOption sortOption = ((SortFilterConfig) filterConfig).getSelectedOption();
                mSortAscending = sortOption.ascending();
                switch (sortOption.id()) {
                    case SORT_NAME:
                        mSortMode = SortMode.NAME;
                        break;
                    case SORT_INSTALL:
                        mSortMode = SortMode.INSTALL_TIME;
                        break;
                    case SORT_UPDATE:
                        mSortMode = SortMode.UPDATE_TIME;
                        break;
                }
            }

        }
    }

    public BackupPackagesFilterConfig(SharedPreferences prefs) {
        mSortMode = SortMode.values()[prefs.getInt(FILTER_SORT, 0)];
        mSortAscending = prefs.getBoolean(SORT_ASCENDING, true);
        mSplitApkFilter = SimpleFilterMode.values()[prefs.getInt(FILTER_SPLIT, 1)];
        mSystemAppFilter = SimpleFilterMode.values()[prefs.getInt(FILTER_SYSTEM_APP, 0)];
    }

    public void saveToPrefs(SharedPreferences prefs) {
        prefs.edit()
                .putInt(FILTER_SORT, getSort().ordinal())
                .putBoolean(SORT_ASCENDING, mSortAscending)
                .putInt(FILTER_SPLIT, getSplitApkFilter().ordinal())
                .putInt(FILTER_SYSTEM_APP, getSystemAppFilter().ordinal())
                .apply();
    }

    private String getString(Context c, @StringRes int id) {
        return c.getString(id);
    }

    private SingleChoiceFilterConfig createYesNoWhateverFilterConfig(Context c, String id, CharSequence name) {
        return new SingleChoiceFilterConfig(id, name)
                .addOption(BackupPackagesFilterConfig.FILTER_MODE_WHATEVER, getString(c, R.string.backup_filter_common_option_doesnt_matter))
                .addOption(BackupPackagesFilterConfig.FILTER_MODE_YES, getString(c, R.string.backup_filter_common_option_yes))
                .addOption(BackupPackagesFilterConfig.FILTER_MODE_NO, getString(c, R.string.no));
    }

    public ComplexFilterConfig toComplexFilterConfig(Context c) {
        ArrayList<FilterConfig> filters = new ArrayList<>();

        //Sort
        SortFilterConfig sortFilter = new SortFilterConfig(BackupPackagesFilterConfig.FILTER_SORT, getString(c, R.string.backup_filter_sort))
                .addOption(BackupPackagesFilterConfig.SORT_NAME, getString(c, R.string.backup_filter_sort_option_name))
                .addOption(BackupPackagesFilterConfig.SORT_INSTALL, getString(c, R.string.backup_filter_sort_option_installed))
                .addOption(BackupPackagesFilterConfig.SORT_UPDATE, getString(c, R.string.backup_filter_sort_option_updated));

        SortFilterConfigOption nameOption = sortFilter.options().get(getSort().ordinal());
        nameOption.setSelected();
        nameOption.setAscending(mSortAscending);

        filters.add(sortFilter);

        //Split APK
        SingleChoiceFilterConfig splitApkFilter = createYesNoWhateverFilterConfig(c, BackupPackagesFilterConfig.FILTER_SPLIT, getString(c, R.string.backup_filter_split_apk));
        splitApkFilter.options().get(getSplitApkFilter().ordinal()).setSelected();
        filters.add(splitApkFilter);

        //System app
        SingleChoiceFilterConfig systemAppFilter = createYesNoWhateverFilterConfig(c, BackupPackagesFilterConfig.FILTER_SYSTEM_APP, getString(c, R.string.backup_filter_system_app));
        systemAppFilter.options().get(getSystemAppFilter().ordinal()).setSelected();
        filters.add(systemAppFilter);

        return new ComplexFilterConfig(filters);
    }

    public SimpleFilterMode getSplitApkFilter() {
        return mSplitApkFilter;
    }

    public SimpleFilterMode getSystemAppFilter() {
        return mSystemAppFilter;
    }

    public SortMode getSort() {
        return mSortMode;
    }


}
