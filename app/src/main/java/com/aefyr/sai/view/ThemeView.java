package com.aefyr.sai.view;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.appcompat.widget.LinearLayoutCompat;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.Theme;
import com.aefyr.sai.utils.Utils;
import com.google.android.material.card.MaterialCardView;

public class ThemeView extends MaterialCardView {

    private TextView mThemeTitle;
    private TextView mThemeMessage;

    public ThemeView(Context context) {
        super(context);
        init();
    }

    public ThemeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ThemeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        LinearLayoutCompat container = new LinearLayoutCompat(getContext());
        container.setOrientation(LinearLayoutCompat.VERTICAL);
        MaterialCardView.LayoutParams containerLayoutParams = new MaterialCardView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        containerLayoutParams.gravity = Gravity.CENTER;
        addView(container, containerLayoutParams);

        mThemeTitle = new AppCompatTextView(getContext());
        mThemeTitle.setGravity(Gravity.CENTER);
        mThemeTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        LinearLayoutCompat.LayoutParams titleLayoutParams = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        container.addView(mThemeTitle, titleLayoutParams);

        mThemeMessage = new AppCompatTextView(getContext());
        mThemeMessage.setGravity(Gravity.CENTER);
        mThemeMessage.setVisibility(GONE);
        LinearLayoutCompat.LayoutParams messageLayoutParams = new LinearLayoutCompat.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        container.addView(mThemeMessage, messageLayoutParams);
    }

    public void setTheme(Theme.ThemeDescriptor theme) {
        mThemeTitle.setText(theme.getName(getContext()));

        Context themedContext = new ContextThemeWrapper(getContext(), theme.getTheme());
        setCardBackgroundColor(Utils.getThemeColor(themedContext, R.attr.colorPrimary));

        int accentColor = Utils.getThemeColor(themedContext, R.attr.colorAccent);
        setStrokeColor(accentColor);
        mThemeTitle.setTextColor(accentColor);

        if (Utils.apiIsAtLeast(Build.VERSION_CODES.M)) {
            setRippleColor(themedContext.getColorStateList(R.color.selector_theme_card_ripple));
        }

        mThemeMessage.setTextColor(Utils.getThemeColor(themedContext, android.R.attr.textColorPrimary));
    }

    public void setMessage(@Nullable CharSequence message) {
        if (message == null) {
            mThemeMessage.setVisibility(GONE);
        } else {
            mThemeMessage.setVisibility(VISIBLE);
            mThemeMessage.setText(message);
        }
    }

    public void setMessage(@StringRes int message) {
        mThemeMessage.setVisibility(VISIBLE);
        mThemeMessage.setText(message);
    }
}
