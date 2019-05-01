package com.aefyr.sai.utils;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.aefyr.sai.ui.dialogs.SimpleAlertDialogFragment;

public class AlertsUtils {

    public static void showAlert(AppCompatActivity a, @StringRes int title, @StringRes int message) {
        showAlert(a, a.getText(title), a.getText(message));
    }

    public static void showAlert(AppCompatActivity a, CharSequence title, CharSequence message) {
        SimpleAlertDialogFragment.newInstance(title, message).show(a.getSupportFragmentManager(), "dialog_alert");
    }

    public static void showAlert(Fragment f, @StringRes int title, @StringRes int message) {
        showAlert(f, f.getText(title), f.getText(message));
    }

    public static void showAlert(Fragment f, CharSequence title, CharSequence message) {
        SimpleAlertDialogFragment.newInstance(title, message).show(f.getChildFragmentManager(), "dialog_alert");
    }
}
