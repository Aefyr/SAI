package com.aefyr.sai.view.coolbar;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Outline;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.TextView;

import androidx.annotation.AttrRes;

import com.aefyr.sai.R;

public class Coolbar extends ViewGroup {
    private static final int DEFAULT_TITLE_TEXT_SIZE_SP = 24;
    private static final int DEFAULT_TITLE_COLOR = 0xff212121;

    TextView mTitle;
    private String mTitleText = "";
    private int mHeight;
    private int mWidth;
    private int mTitleColor = 0xff212121;

    public Coolbar(Context context) {
        super(context);
        init();
    }

    public Coolbar(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttrs(attrs);
        init();
    }

    public Coolbar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAttrs(attrs);
        init();
    }

    public Coolbar(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        parseAttrs(attrs);
        init();
    }

    private void parseAttrs(AttributeSet attrs) {
        TypedArray a = getResources().obtainAttributes(attrs, R.styleable.Coolbar);
        mTitleText = a.getString(R.styleable.Coolbar_title);
        mTitleColor = a.getColor(R.styleable.Coolbar_titleColor, getThemeColor(R.attr.titleTextColor, DEFAULT_TITLE_COLOR));
        a.recycle();
    }

    private void init() {
        mTitle = new TextView(getContext());
        mTitle.setText(mTitleText);
        mTitle.setBackgroundColor(Color.TRANSPARENT);
        mTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, DEFAULT_TITLE_TEXT_SIZE_SP);
        mTitle.setTextColor(mTitleColor);
        mTitle.setGravity(Gravity.CENTER);
        mTitle.setSingleLine();
        mTitle.setEllipsize(TextUtils.TruncateAt.MARQUEE);
        mTitle.setMarqueeRepeatLimit(-1);
        mTitle.setSelected(true);
        addView(mTitle);

        if (getBackground() == null)
            setBackgroundColor(getThemeColor(R.attr.colorPrimary, Color.WHITE));
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        setOutlineProvider(new CoolbarOutlineProvider(w, h));
    }

    @Override
    public ViewOutlineProvider getOutlineProvider() {
        return super.getOutlineProvider();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth;
        if (getParent() instanceof ViewGroup)
            desiredWidth = ((ViewGroup) getParent()).getWidth();
        else
            desiredWidth = getResources().getDisplayMetrics().widthPixels;

        int desiredHeight = dpToPx(56);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width;
        int height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        //Children
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE || child == mTitle)
                continue;

            LayoutParams params = (LayoutParams) child.getLayoutParams();

            int heightMS;
            int widthMS;

            if (params.height == ViewGroup.LayoutParams.MATCH_PARENT)
                heightMS = MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY);
            else if (params.height == LayoutParams.WRAP_CONTENT) {
                heightMS = MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.AT_MOST);
            } else {
                heightMS = MeasureSpec.makeMeasureSpec(clamp(params.height, 0, mHeight), MeasureSpec.EXACTLY);
            }

            if (params.width == ViewGroup.LayoutParams.MATCH_PARENT)
                widthMS = MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY);
            else if (params.width == LayoutParams.WRAP_CONTENT) {
                widthMS = MeasureSpec.makeMeasureSpec(mHeight * 2, MeasureSpec.AT_MOST);
            } else {
                widthMS = MeasureSpec.makeMeasureSpec(clamp(params.width, 0, mHeight * 2), MeasureSpec.EXACTLY);
            }

            child.measure(widthMS, heightMS);
        }

        mHeight = height;
        mWidth = width;
        setMeasuredDimension(width, height);
    }

    public void setTitle(String title) {
        mTitleText = title;
        mTitle.setText(title);
    }

    public void setTitleColor(int color) {
        mTitle.setTextColor(color);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        int startOffset = 0;
        int endOffset = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE || child == mTitle)
                continue;

            LayoutParams params = (LayoutParams) child.getLayoutParams();

            int childHeight = child.getMeasuredHeight();
            int childWidth = child.getMeasuredWidth();

            int top = 0;
            int bottom = mHeight;
            if (childHeight < mHeight) {
                top = (mHeight - childHeight) / 2;
                bottom = top + childHeight;
            }

            if (params.getGravity() == Gravity.START) {
                child.layout(startOffset, top, startOffset + childWidth, bottom);
                startOffset += childWidth;
            } else {
                int a = getMeasuredWidth() - endOffset;
                child.layout(a - childWidth, top, a, bottom);
                endOffset += childWidth;
            }

        }


        if (startOffset > endOffset) {
            mTitle.measure(MeasureSpec.makeMeasureSpec((getMeasuredWidth() - startOffset * 2), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
            mTitle.layout(startOffset, 0, getMeasuredWidth() - endOffset - (startOffset - endOffset), getMeasuredHeight());
        } else {
            mTitle.measure(MeasureSpec.makeMeasureSpec((getMeasuredWidth() - endOffset * 2), MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(mHeight, MeasureSpec.EXACTLY));
            mTitle.layout(startOffset + (endOffset - startOffset), 0, getMeasuredWidth() - endOffset, getMeasuredHeight());
        }

    }

    @Override
    protected ViewGroup.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected ViewGroup.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    private int dpToPx(int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private int clamp(int a, int min, int max) {
        if (a < min)
            return min;
        if (a > max)
            return max;
        return a;
    }

    private int getThemeColor(@AttrRes int attr, int defaultColor) {
        Resources.Theme theme = getContext().getTheme();
        TypedValue color = new TypedValue();
        if (theme.resolveAttribute(attr, color, true))
            return color.data;
        else
            return defaultColor;
    }

    public static class LayoutParams extends ViewGroup.LayoutParams {
        private int mGravity = Gravity.END;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.Coolbar_Layout);
            mGravity = a.getInt(R.styleable.Coolbar_Layout_coolbar_gravity, 0) == 0 ? Gravity.END : Gravity.START;
            a.recycle();
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams source) {
            super(source);
        }

        public int getGravity() {
            return mGravity;
        }

        public void setGravity(int gravity) {
            mGravity = gravity;

        }
    }

    private class CoolbarOutlineProvider extends ViewOutlineProvider {
        int mWidth;
        int mHeight;

        CoolbarOutlineProvider(int width, int height) {
            mWidth = width;
            mHeight = height;
        }

        @Override
        public void getOutline(View view, Outline outline) {
            outline.setRect(0, 0, mWidth, mHeight);
        }
    }
}
