package com.aefyr.sai.ui.fragments.miui;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.dialogs.SimpleAlertDialogFragment;
import com.aefyr.sai.ui.fragments.SaiBaseFragment;
import com.aefyr.sai.utils.MiuiUtils;
import com.aefyr.sai.viewmodels.MiEntryViewModel;

public class MiEntryFragment extends SaiBaseFragment {

    private MiEntryViewModel mViewModel;

    @Override
    protected int layoutId() {
        return R.layout.fragment_mi_entry;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(MiEntryViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupMD3Animations();

        ((TextView) findViewById(R.id.tv_mi_miui_ver)).setText(getString(R.string.mi_miui_version, MiuiUtils.getMiuiVersionName(), MiuiUtils.getMiuiVersionCode()));

        findViewById(R.id.button_mi_open_dev_settings).setOnClickListener(v -> openDevSettings());

        Button continueButton = findViewById(R.id.button_mi_continue);
        continueButton.setOnClickListener(v -> doContinue());
        mViewModel.getCountdown().observe(getViewLifecycleOwner(), (countdown) -> {
            if (countdown == 0) {
                continueButton.setEnabled(true);
                continueButton.setText(R.string.mi_continue);
            } else {
                continueButton.setEnabled(false);
                continueButton.setText(getString(R.string.mi_continue_countdown, countdown));
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.setPaused(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        mViewModel.setPaused(false);
    }

    private void setupMD3Animations() {
        TextView title = findViewById(R.id.tv_mi_entry_title);

        MutableLiveData<Integer> titleColorLiveData = new MutableLiveData<>(title.getCurrentTextColor());
        MutableLiveData<Float> titleScaleLiveData = new MutableLiveData<>(1f);
        titleColorLiveData.observe(getViewLifecycleOwner(), title::setTextColor);
        titleScaleLiveData.observe(getViewLifecycleOwner(), scale -> {
            title.setScaleX(scale);
            title.setScaleY(scale);
        });

        ValueAnimator textColorAnimator = ValueAnimator.ofArgb(Color.parseColor("#FF0000"), Color.parseColor("#FF7F00"),
                Color.parseColor("#FFFF00"), Color.parseColor("#00FF00"), Color.parseColor("#0000FF"), Color.parseColor("#2E2B5F"), Color.parseColor("#8B00FF"), Color.parseColor("#FF0000"));
        textColorAnimator.setDuration(3000);
        textColorAnimator.setRepeatMode(ValueAnimator.RESTART);
        textColorAnimator.setRepeatCount(ValueAnimator.INFINITE);
        textColorAnimator.addUpdateListener(animation -> titleColorLiveData.setValue((Integer) animation.getAnimatedValue()));
        textColorAnimator.start();

        ValueAnimator titleScaleAnimator = ValueAnimator.ofFloat(1f, 0.75f);
        titleScaleAnimator.setDuration(400);
        titleScaleAnimator.setRepeatMode(ValueAnimator.REVERSE);
        titleScaleAnimator.setRepeatCount(ValueAnimator.INFINITE);
        titleScaleAnimator.addUpdateListener(animation -> titleScaleLiveData.setValue((float) animation.getAnimatedValue()));
        titleScaleAnimator.start();
    }

    private void openDevSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
        } catch (Exception e) {
            SimpleAlertDialogFragment.newInstance(getString(R.string.error), getString(R.string.installer_miui_warning_oof)).show(getChildFragmentManager(), "alert_oof");
        }
    }

    private void doContinue() {
        try {
            OnContinueListener listener;
            if (getParentFragment() != null)
                listener = (OnContinueListener) getParentFragment();
            else
                listener = (OnContinueListener) getActivity();

            if (listener != null)
                listener.onContinue();
        } catch (Exception e) {
            throw new IllegalStateException("OnContinueListener not implemented in host");
        }
    }

    public interface OnContinueListener {

        void onContinue();

    }
}
