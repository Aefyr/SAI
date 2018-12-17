package com.aefyr.sai.ui.activities;

import android.os.Bundle;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.ui.fragments.PreferencesFragment;
import com.aefyr.sai.utils.Theme;

import java.io.File;
import java.util.List;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

public class PreferencesActivity extends AppCompatActivity implements FilePickerDialogFragment.OnFilesSelectedListener {
    public static final String FILE_PICKER_TAG_HOME_DIR = "home_dir";

    private static final String TAG_PREFERENCES_FRAGMENT = "preferences";

    private PreferencesFragment mPreferencesFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Theme.getInstance(this).apply(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        FragmentManager fm = getSupportFragmentManager();
        mPreferencesFragment = (PreferencesFragment) fm.findFragmentByTag(TAG_PREFERENCES_FRAGMENT);
        if (mPreferencesFragment != null)
            return;

        mPreferencesFragment = new PreferencesFragment();
        fm.beginTransaction().add(R.id.container, mPreferencesFragment, TAG_PREFERENCES_FRAGMENT).commit();
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        switch (tag) {
            case FILE_PICKER_TAG_HOME_DIR:
                mPreferencesFragment.onHomeDirSelected(files.get(0));
        }
    }
}
