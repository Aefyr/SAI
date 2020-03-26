package com.aefyr.sai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.StyleRes;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;

import java.util.ArrayList;
import java.util.List;

public class Theme {
    private static final String THEME_TAG_CONCRETE = "concrete";
    private static final String THEME_TAG_LIGHT = "light";
    private static final String THEME_TAG_DARK = "dark";

    private static final int DEFAULT_LIGHT_THEME_ID = BuildConfig.DEFAULT_THEME;
    private static final int DEFAULT_DARK_THEME_ID = 1;

    public enum Mode {
        /**
         * Use a single selected theme
         */
        CONCRETE,
        /**
         * Choose between two selected light and dark themes depending on system theme (Android Q+)
         */
        AUTO_LIGHT_DARK
    }

    private static Theme sInstance;

    private Context mContext;

    private SharedPreferences mPrefs;

    private List<ThemeDescriptor> mThemes;

    private MutableLiveData<ThemeDescriptor> mLiveTheme = new MutableLiveData<>();

    private Mode mMode;

    public static Theme getInstance(Context c) {
        synchronized (Theme.class) {
            return sInstance != null ? sInstance : new Theme(c);
        }
    }

    private Theme(Context c) {
        mContext = c.getApplicationContext();

        mPrefs = PreferenceManager.getDefaultSharedPreferences(mContext);

        mMode = Mode.valueOf(mPrefs.getString(PreferencesKeys.THEME_MODE, Utils.apiIsAtLeast(Build.VERSION_CODES.Q) ? Mode.AUTO_LIGHT_DARK.name() : Mode.CONCRETE.name()));

        mThemes = new ArrayList<>();
        mThemes.add(new ThemeDescriptor(0, R.style.AppTheme_Light, false, R.string.theme_sai, false));
        mThemes.add(new ThemeDescriptor(1, R.style.AppTheme_Dark, true, R.string.theme_ruby, false));
        mThemes.add(new ThemeDescriptor(2, R.style.AppTheme_Rena, true, R.string.theme_rena, false));
        mThemes.add(new ThemeDescriptor(3, R.style.AppTheme_Rooter, false, R.string.theme_ukrrooter, false));
        mThemes.add(new ThemeDescriptor(4, R.style.AppTheme_Omelette, true, R.string.theme_amoled, false));
        mThemes.add(new ThemeDescriptor(5, R.style.AppTheme_Pixel, false, R.string.theme_pixel, false));
        mThemes.add(new ThemeDescriptor(6, R.style.AppTheme_FDroid, false, R.string.theme_sai_fdroid, false));
        mThemes.add(new ThemeDescriptor(7, R.style.AppTheme_Dark2, true, R.string.theme_dark, false));
        mThemes.add(new ThemeDescriptor(8, R.style.AppTheme_Gold, true, R.string.theme_gold, true));

        invalidateLiveTheme();

        sInstance = this;
    }

    public static ThemeDescriptor apply(Context c) {
        Theme theme = getInstance(c);
        ThemeDescriptor currentTheme = theme.getCurrentTheme();
        c.setTheme(currentTheme.getTheme());

        //In case system dark mode changes
        //TODO handle dark mode change better
        theme.invalidateLiveTheme();

        return currentTheme;
    }

    /**
     * Convenience method for {@link #getInstance(Context)}.{@link #getLiveTheme()}.{@link LiveData#observe(LifecycleOwner, Observer)}
     */
    public static void observe(Context c, @NonNull LifecycleOwner owner, @NonNull Observer<ThemeDescriptor> observer) {
        getInstance(c).getLiveTheme().observe(owner, observer);
    }

    public List<ThemeDescriptor> getThemes() {
        return mThemes;
    }

    public ThemeDescriptor getCurrentTheme() {
        switch (mMode) {
            case CONCRETE:
                return getConcreteTheme();
            case AUTO_LIGHT_DARK:
                if (shouldUseDarkThemeForAutoMode())
                    return getDarkTheme();

                return getLightTheme();
        }

        throw new IllegalStateException("Unknown mode");
    }

    public LiveData<ThemeDescriptor> getLiveTheme() {
        return mLiveTheme;
    }

    public Mode getThemeMode() {
        return mMode;
    }

    public void setMode(Mode mode) {
        if (mode == mMode)
            return;

        mPrefs.edit().putString(PreferencesKeys.THEME_MODE, mode.name()).apply();
        mMode = mode;

        invalidateLiveTheme();
    }

    public ThemeDescriptor getConcreteTheme() {
        return getThemeDescriptorById(getThemeId(THEME_TAG_CONCRETE, BuildConfig.DEFAULT_THEME));
    }

    public void setConcreteTheme(ThemeDescriptor theme) {
        saveThemeId(THEME_TAG_CONCRETE, theme.getId());

        if (getThemeMode() == Mode.CONCRETE)
            invalidateLiveTheme();
    }

    public ThemeDescriptor getLightTheme() {
        return getThemeDescriptorById(getThemeId(THEME_TAG_LIGHT, DEFAULT_LIGHT_THEME_ID));
    }

    public void setLightTheme(ThemeDescriptor theme) {
        saveThemeId(THEME_TAG_LIGHT, theme.getId());

        if (getThemeMode() == Mode.AUTO_LIGHT_DARK && !shouldUseDarkThemeForAutoMode())
            invalidateLiveTheme();
    }

    public ThemeDescriptor getDarkTheme() {
        return getThemeDescriptorById(getThemeId(THEME_TAG_DARK, DEFAULT_DARK_THEME_ID));
    }

    public void setDarkTheme(ThemeDescriptor theme) {
        saveThemeId(THEME_TAG_DARK, theme.getId());

        if (getThemeMode() == Mode.AUTO_LIGHT_DARK && shouldUseDarkThemeForAutoMode())
            invalidateLiveTheme();
    }

    private boolean shouldUseDarkThemeForAutoMode() {
        return (mContext.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    private ThemeDescriptor getThemeDescriptorById(int themeId) {
        if (themeId >= mThemes.size())
            return mThemes.get(0);

        return mThemes.get(themeId);
    }

    private int getThemeId(String themeTag, int defaultThemeId) {
        return mPrefs.getInt(PreferencesKeys.CURRENT_THEME + "." + themeTag, defaultThemeId);
    }

    private void saveThemeId(String themeTag, int themeId) {
        mPrefs.edit().putInt(PreferencesKeys.CURRENT_THEME + "." + themeTag, themeId).apply();
    }

    private void invalidateLiveTheme() {
        ThemeDescriptor currentTheme = getCurrentTheme();
        if (!currentTheme.equals(mLiveTheme.getValue()))
            mLiveTheme.setValue(currentTheme);
    }

    public static class ThemeDescriptor {
        private int mId;

        @StyleRes
        private int mTheme;
        private boolean mIsDark;

        @StringRes
        private int mNameStringRes;
        private boolean mDonationRequired;

        private ThemeDescriptor(int id, @StyleRes int theme, boolean isDark, @StringRes int nameStringRes, boolean donationRequired) {
            mId = id;
            mTheme = theme;
            mIsDark = isDark;
            mNameStringRes = nameStringRes;
            mDonationRequired = donationRequired;
        }

        public int getId() {
            return mId;
        }

        @StyleRes
        public int getTheme() {
            return mTheme;
        }

        public boolean isDark() {
            return mIsDark;
        }

        public String getName(Context c) {
            return c.getString(mNameStringRes);
        }

        public boolean isDonationRequired() {
            return mDonationRequired;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ThemeDescriptor && ((ThemeDescriptor) obj).getId() == getId();
        }
    }
}
