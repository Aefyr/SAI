package com.aefyr.sai.ui.activities;

import android.os.Bundle;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.fragments.PreferencesFragment;
import com.aefyr.sai.utils.Theme;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

public class PreferencesActivity extends AppCompatActivity {
    private static final String TAG_PREFERENCES_FRAGMENT = "preferences";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Theme.getInstance(this).apply(this);

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
