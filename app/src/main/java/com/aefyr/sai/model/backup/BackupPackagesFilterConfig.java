package com.aefyr.sai.model.backup;

import com.aefyr.flexfilter.builtin.filter.singlechoice.SingleChoiceFilterConfig;
import com.aefyr.flexfilter.builtin.filter.singlechoice.SingleChoiceFilterConfigOption;
import com.aefyr.flexfilter.builtin.filter.sort.SortFilterConfig;
import com.aefyr.flexfilter.config.core.ComplexFilterConfig;
import com.aefyr.flexfilter.config.core.FilterConfig;

public class BackupPackagesFilterConfig {

    public static final String SORT_NAME = "name";
    public static final String SORT_INSTALL = "install";
    public static final String SORT_UPDATE = "update";

    public static final String FILTER_SPLIT = "split";
    public static final String FILTER_SYSTEM_APP = "system_app";

    public static final String FILTER_MODE_YES = "yes";
    public static final String FILTER_MODE_NO = "no";
    public static final String FILTER_MODE_WHATEVER = "whatever";

    public enum SimpleFilterMode {
        YES, NO, WHATEVER;

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
                switch (((SortFilterConfig) filterConfig).getSelectedOption().id()) {
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
