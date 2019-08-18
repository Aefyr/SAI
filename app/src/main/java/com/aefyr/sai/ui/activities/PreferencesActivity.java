package com.aefyr.sai.ui.activities;

import android.os.Bundle;

import androidx.fragment.app.FragmentManager;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.fragments.PreferencesFragment;

public class PreferencesActivity extends ThemedActivity {
    private static final String TAG_PREFERENCES_FRAGMENT = "preferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        FragmentManager fm = getSupportFragmentManager();
        PreferencesFragment mPreferencesFragment = (PreferencesFragment) fm.findFragmentByTag(TAG_PREFERENCES_FRAGMENT);
        if (mPreferencesFragment != null)
            return;

        mPreferencesFragment = new PreferencesFragment();
        fm.beginTransaction().add(R.id.container, mPreferencesFragment, TAG_PREFERENCES_FRAGMENT).commit();
    }
}
