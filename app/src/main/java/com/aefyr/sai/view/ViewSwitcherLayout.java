package com.aefyr.sai.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ViewSwitcherLayout extends FrameLayout {


    public ViewSwitcherLayout(@NonNull Context context) {
        super(context);
    }

    public ViewSwitcherLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public ViewSwitcherLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public ViewSwitcherLayout(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Show the view with {@code viewId} id and hide all others
     */
    public void setShownView(@IdRes int viewId) {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.setVisibility(child.getId() == viewId ? VISIBLE : GONE);
        }
    }


}
