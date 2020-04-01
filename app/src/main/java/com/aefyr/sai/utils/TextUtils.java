package com.aefyr.sai.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TextUtils {

    @NonNull
    public static String requireNonEmpty(@Nullable String s) {
        if (android.text.TextUtils.isEmpty(s))
            throw new RuntimeException("String is empty");

        return s;
    }

    public static boolean isEmpty(@Nullable String s) {
        return android.text.TextUtils.isEmpty(s);
    }

    @Nullable
    public static String getNullIfEmpty(@Nullable String s) {
        if (isEmpty(s))
            return null;

        return s;
    }

}
