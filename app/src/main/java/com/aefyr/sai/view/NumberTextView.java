/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package com.aefyr.sai.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.Utils;

import java.util.ArrayList;
import java.util.Locale;

public class NumberTextView extends View {

    private ArrayList<StaticLayout> letters = new ArrayList<>();
    private ArrayList<StaticLayout> oldLetters = new ArrayList<>();
    private TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private ObjectAnimator animator;
    private float progress = 0.0f;
    private int currentNumber = 0;

    public NumberTextView(Context context) {
        super(context);
    }

    public NumberTextView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        parseAttrs(attrs);
    }

    public NumberTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        parseAttrs(attrs);
    }

    public NumberTextView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        parseAttrs(attrs);
    }

    private void parseAttrs(@Nullable AttributeSet attrs) {
        if (attrs == null)
            return;

        TypedArray typedArray = getResources().obtainAttributes(attrs, R.styleable.NumberTextView);
        textPaint.setTextSize(typedArray.getDimensionPixelSize(R.styleable.NumberTextView_textSize, Utils.spToPx(getContext(), 14)));

        int colorAttrType = typedArray.getType(R.styleable.NumberTextView_textColor);
        if (colorAttrType == TypedValue.TYPE_ATTRIBUTE) {
            textPaint.setColor(Utils.getThemeColor(getContext(), typedArray.getResourceId(R.styleable.NumberTextView_textColor, android.R.attr.textColorPrimary)));
        } else {
            textPaint.setColor(typedArray.getColor(R.styleable.NumberTextView_textColor, Color.BLACK));
        }
        typedArray.recycle();
    }

    public void setProgress(float value) {
        if (progress == value) {
            return;
        }
        progress = value;
        invalidate();
    }

    public float getProgress() {
        return progress;
    }

    public void setNumber(int number, boolean animated) {
        if (currentNumber == number && animated) {
            return;
        }
        if (animator != null) {
            animator.cancel();
            animator = null;
        }
        oldLetters.clear();
        oldLetters.addAll(letters);
        letters.clear();
        String oldText = String.format(Locale.US, "%d", currentNumber);
        String text = String.format(Locale.US, "%d", number);
        boolean forwardAnimation = number > currentNumber;
        currentNumber = number;
        progress = 0;
        for (int a = 0; a < text.length(); a++) {
            String ch = text.substring(a, a + 1);
            String oldCh = !oldLetters.isEmpty() && a < oldText.length() ? oldText.substring(a, a + 1) : null;
            if (oldCh != null && oldCh.equals(ch)) {
                letters.add(oldLetters.get(a));
                oldLetters.set(a, null);
            } else {
                StaticLayout layout = new StaticLayout(ch, textPaint, (int) Math.ceil(textPaint.measureText(ch)), Layout.Alignment.ALIGN_NORMAL, 1.0f, 0.0f, false);
                letters.add(layout);
            }
        }
        if (animated && !oldLetters.isEmpty()) {
            animator = ObjectAnimator.ofFloat(this, "progress", forwardAnimation ? -1 : 1, 0);
            animator.setDuration(150);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    animator = null;
                    oldLetters.clear();
                }
            });
            animator.start();
        }
        invalidate();
    }

    public void setTextSize(int size) {
        textPaint.setTextSize(Utils.spToPx(getContext(), size));
        oldLetters.clear();
        letters.clear();
        setNumber(currentNumber, false);
    }

    public void setTextColor(int value) {
        textPaint.setColor(value);
        invalidate();
    }

    public void setTypeface(Typeface typeface) {
        textPaint.setTypeface(typeface);
        oldLetters.clear();
        letters.clear();
        setNumber(currentNumber, false);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (letters.isEmpty()) {
            return;
        }
        float height = letters.get(0).getHeight();
        canvas.save();
        canvas.translate(getPaddingLeft(), (getMeasuredHeight() - height) / 2);
        int count = Math.max(letters.size(), oldLetters.size());
        for (int a = 0; a < count; a++) {
            canvas.save();
            StaticLayout old = a < oldLetters.size() ? oldLetters.get(a) : null;
            StaticLayout layout = a < letters.size() ? letters.get(a) : null;
            if (progress > 0) {
                if (old != null) {
                    textPaint.setAlpha((int) (255 * progress));
                    canvas.save();
                    canvas.translate(0, (progress - 1.0f) * height);
                    old.draw(canvas);
                    canvas.restore();
                    if (layout != null) {
                        textPaint.setAlpha((int) (255 * (1.0f - progress)));
                        canvas.translate(0, progress * height);
                    }
                } else {
                    textPaint.setAlpha(255);
                }
            } else if (progress < 0) {
                if (old != null) {
                    textPaint.setAlpha((int) (255 * -progress));
                    canvas.save();
                    canvas.translate(0, (1.0f + progress) * height);
                    old.draw(canvas);
                    canvas.restore();
                }
                if (layout != null) {
                    if (a == count - 1 || old != null) {
                        textPaint.setAlpha((int) (255 * (1.0f + progress)));
                        canvas.translate(0, progress * height);
                    } else {
                        textPaint.setAlpha(255);
                    }
                }
            } else if (layout != null) {
                textPaint.setAlpha(255);
            }
            if (layout != null) {
                layout.draw(canvas);
            }
            canvas.restore();
            canvas.translate(layout != null ? layout.getLineWidth(0) : old.getLineWidth(0) + Utils.dpToPx(getContext(), 1), 0);
        }
        canvas.restore();
    }
}