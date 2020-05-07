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

    public static final String FILTER_BACKUP_STATUS = "backup_status";
    public static final String FILTER_BACKUP_STATUS_MODE_WHATEVER = "whatever";
    public static final String FILTER_BACKUP_STATUS_MODE_NO_BACKUP = "no_backup";
    public static final String FILTER_BACKUP_STATUS_MODE_SAME_VERSION = "same_version";
    public static final String FILTER_BACKUP_STATUS_MODE_HIGHER_VERSION = "higher_version";
    public static final String FILTER_BACKUP_STATUS_MODE_LOWER_VERSION = "lower_version";
    public static final String FILTER_BACKUP_STATUS_MODE_APP_NOT_INSTALLED = "app_not_installed";

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

    public enum BackupStatusFilterMode {
        WHATEVER, NO_BACKUP, SAME_VERSION, HIGHER_VERSION, LOWER_VERSION, APP_NOT_INSTALLED;

        public static BackupStatusFilterMode from(SingleChoiceFilterConfigOption option) {
            switch (option.id()) {
                case FILTER_BACKUP_STATUS_MODE_NO_BACKUP:
                    return BackupStatusFilterMode.NO_BACKUP;
                case FILTER_BACKUP_STATUS_MODE_SAME_VERSION:
                    return BackupStatusFilterMode.SAME_VERSION;
                case FILTER_BACKUP_STATUS_MODE_HIGHER_VERSION:
                    return BackupStatusFilterMode.HIGHER_VERSION;
                case FILTER_BACKUP_STATUS_MODE_LOWER_VERSION:
                    return BackupStatusFilterMode.LOWER_VERSION;
                case FILTER_BACKUP_STATUS_MODE_APP_NOT_INSTALLED:
                    return BackupStatusFilterMode.APP_NOT_INSTALLED;
            }

            return BackupStatusFilterMode.WHATEVER;
        }
    }

    private SimpleFilterMode mSplitApkFilter;
    private SimpleFilterMode mSystemAppFilter;

    private SortMode mSortMode;
    private boolean mSortAscending;

    private BackupStatusFilterMode mBackupStatusFilter;

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
                    case FILTER_BACKUP_STATUS:
                        mBackupStatusFilter = BackupStatusFilterMode.from(((SingleChoiceFilterConfig) filterConfig).getSelectedOption());
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
        mBackupStatusFilter = BackupStatusFilterMode.values()[prefs.getInt(FILTER_BACKUP_STATUS, 0)];
    }

    public void saveToPrefs(SharedPreferences prefs) {
        prefs.edit()
                .putInt(FILTER_SORT, getSort().ordinal())
                .putBoolean(SORT_ASCENDING, mSortAscending)
                .putInt(FILTER_SPLIT, getSplitApkFilter().ordinal())
                .putInt(FILTER_SYSTEM_APP, getSystemAppFilter().ordinal())
                .putInt(FILTER_BACKUP_STATUS, getBackupStatusFilter().ordinal())
                .apply();
    }

    private String getString(Context c, @StringRes int id) {
        return c.getString(id);
    }

    private SingleChoiceFilterConfig createYesNoWhateverFilterConfig(Context c, String id, CharSequence name) {
        return new SingleChoiceFilterConfig(id, name)
                .addOption(FILTER_MODE_WHATEVER, getString(c, R.string.backup_filter_common_option_doesnt_matter))
                .addOption(FILTER_MODE_YES, getString(c, R.string.backup_filter_common_option_yes))
                .addOption(FILTER_MODE_NO, getString(c, R.string.no));
    }

    public ComplexFilterConfig toComplexFilterConfig(Context c) {
        ArrayList<FilterConfig> filters = new ArrayList<>();

        //Sort
        SortFilterConfig sortFilter = new SortFilterConfig(FILTER_SORT, getString(c, R.string.backup_filter_sort))
                .addOption(SORT_NAME, getString(c, R.string.backup_filter_sort_option_name))
                .addOption(SORT_INSTALL, getString(c, R.string.backup_filter_sort_option_installed))
                .addOption(SORT_UPDATE, getString(c, R.string.backup_filter_sort_option_updated));

        SortFilterConfigOption selectedSortOption = sortFilter.options().get(getSort().ordinal());
        selectedSortOption.setSelected();
        selectedSortOption.setAscending(mSortAscending);

        filters.add(sortFilter);

        //Split APK
        SingleChoiceFilterConfig splitApkFilter = createYesNoWhateverFilterConfig(c, FILTER_SPLIT, getString(c, R.string.backup_filter_split_apk));
        splitApkFilter.options().get(getSplitApkFilter().ordinal()).setSelected();
        filters.add(splitApkFilter);

        //System app
        SingleChoiceFilterConfig systemAppFilter = createYesNoWhateverFilterConfig(c, FILTER_SYSTEM_APP, getString(c, R.string.backup_filter_system_app));
        systemAppFilter.options().get(getSystemAppFilter().ordinal()).setSelected();
        filters.add(systemAppFilter);

        //Backup status
        SingleChoiceFilterConfig backupStatusFilter = new SingleChoiceFilterConfig(FILTER_BACKUP_STATUS, getString(c, R.string.backup_filter_backup_status))
                .addOption(FILTER_BACKUP_STATUS_MODE_WHATEVER, getString(c, R.string.backup_filter_common_option_doesnt_matter))
                .addOption(FILTER_BACKUP_STATUS_MODE_NO_BACKUP, getString(c, R.string.backup_filter_backup_status_option_no_backup))
                .addOption(FILTER_BACKUP_STATUS_MODE_SAME_VERSION, getString(c, R.string.backup_filter_backup_status_option_same_version))
                .addOption(FILTER_BACKUP_STATUS_MODE_HIGHER_VERSION, getString(c, R.string.backup_filter_backup_status_option_higher_version))
                .addOption(FILTER_BACKUP_STATUS_MODE_LOWER_VERSION, getString(c, R.string.backup_filter_backup_status_option_lower_version))
                .addOption(FILTER_BACKUP_STATUS_MODE_APP_NOT_INSTALLED, getString(c, R.string.backup_filter_backup_status_option_app_not_installed));
        backupStatusFilter.options().get(getBackupStatusFilter().ordinal()).setSelected();
        filters.add(backupStatusFilter);

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

    public BackupStatusFilterMode getBackupStatusFilter() {
        return mBackupStatusFilter;
    }


}
