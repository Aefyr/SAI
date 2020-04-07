package com.aefyr.sai.installerx.splitmeta.config;

import androidx.annotation.Nullable;

import com.aefyr.sai.utils.TextUtils;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class LocaleConfigSplitMeta extends ConfigSplitMeta {

    private Locale mLocale;

    public LocaleConfigSplitMeta(Map<String, String> manifestAttrs) {
        super(manifestAttrs);

        mLocale = Objects.requireNonNull(buildLocaleFromSplitName(TextUtils.requireNonEmpty(splitName())));
    }

    public Locale locale() {
        return mLocale;
    }

    public static boolean isLocaleSplit(String splitName) {
        return isLocaleValid(buildLocaleFromSplitName(splitName));
    }

    @Nullable
    private static Locale buildLocaleFromSplitName(String splitName) {
        int configPartIndex = splitName.lastIndexOf("config.");
        if (configPartIndex == -1 || (configPartIndex != 0 && splitName.charAt(configPartIndex - 1) != '.'))
            return null;

        String localeTag = splitName.substring(configPartIndex + ("config.".length()));
        return new Locale.Builder().setLanguageTag(localeTag).build();
    }

    private static boolean isLocaleValid(@Nullable Locale locale) {
        if (locale == null)
            return false;

        for (Locale validLocale : Locale.getAvailableLocales()) {
            if (validLocale.equals(locale))
                return true;
        }
        return false;
    }
}
