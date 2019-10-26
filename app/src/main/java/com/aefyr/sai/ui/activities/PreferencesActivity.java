package com.aefyr.sai.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceFragmentCompat;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;
import com.aefyr.sai.view.coolbar.Coolbar;

import java.lang.reflect.Constructor;
import java.util.Objects;

public class PreferencesActivity extends ThemedActivity {
    private static final String TAG_PREFERENCES_FRAGMENT = "preferences";
    private static final String EXTRA_PREF_FRAGMENT_CLASS = BuildConfig.APPLICATION_ID + ".extra.PreferencesActivity.PREF_FRAGMENT_CLASS";
    private static final String EXTRA_TITLE = BuildConfig.APPLICATION_ID + ".extra.PreferencesActivity.TITLE";

    private Coolbar mCoolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        mCoolbar = findViewById(R.id.coolbar);
        mCoolbar.setTitle(getIntent().getStringExtra(Intent.EXTRA_TITLE));

        FragmentManager fm = getSupportFragmentManager();
        PreferenceFragmentCompat mPreferencesFragment = (PreferenceFragmentCompat) fm.findFragmentByTag(TAG_PREFERENCES_FRAGMENT);
        if (mPreferencesFragment != null)
            return;

        mPreferencesFragment = createNewPrefsFragment();
        fm.beginTransaction().add(R.id.container, mPreferencesFragment, TAG_PREFERENCES_FRAGMENT).commit();
    }

    /**
     * Open PreferencesActivity that will display {@code prefFragmentClass} fragment.
     * Passed fragment class must be as descendant of PreferenceFragmentCompat and have a zero arguments constructor
     *
     * @param c                  context
     * @param prefsFragmentClass class of the fragment that will be wrapped in this PreferencesActivity
     */
    public static void open(Context c, Class<? extends PreferenceFragmentCompat> prefsFragmentClass, CharSequence title) {
        Intent intent = new Intent(c, PreferencesActivity.class);
        intent.putExtra(EXTRA_PREF_FRAGMENT_CLASS, Objects.requireNonNull(prefsFragmentClass.getCanonicalName()));
        intent.putExtra(Intent.EXTRA_TITLE, title);
        c.startActivity(intent);
    }

    private PreferenceFragmentCompat createNewPrefsFragment() {
        try {
            Class<? extends PreferenceFragmentCompat> prefsFragmentClass = (Class<? extends PreferenceFragmentCompat>) Class.forName(getIntent().getStringExtra(EXTRA_PREF_FRAGMENT_CLASS));
            Constructor zeroArgsConstructor = prefsFragmentClass.getConstructor();
            return (PreferenceFragmentCompat) zeroArgsConstructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
