package com.aefyr.sai.installerx.common;

import androidx.annotation.Nullable;

import java.util.List;

public interface SplitCategory {

    Category category();

    String id();

    String name();

    @Nullable
    String description();

    List<SplitPart> parts();

}
