package com.aefyr.sai.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.aefyr.sai.R;
import com.aefyr.sai.backup.BackupRepository;
import com.aefyr.sai.ui.dialogs.MiuiWarningDialogFragment;
import com.aefyr.sai.ui.fragments.BackupFragment;
import com.aefyr.sai.ui.fragments.InstallerFragment;
import com.aefyr.sai.utils.FragmentNavigator;
import com.aefyr.sai.utils.PreferencesKeys;
import com.aefyr.sai.utils.Utils;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends ThemedActivity implements BottomNavigationView.OnNavigationItemSelectedListener, FragmentNavigator.FragmentFactory {

    private BottomNavigationView mBottomNavigationView;

    private FragmentNavigator mFragmentNavigator;

    private InstallerFragment mInstallerFragment;
    private BackupFragment mBackupFragment;

    private boolean mIsNavigationEnabled = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //TODO is this ok?
        BackupRepository.getInstance(this);


        showMiuiWarning();


        mBottomNavigationView = findViewById(R.id.bottomnav_main);
        mBottomNavigationView.setOnNavigationItemSelectedListener(this);

        mFragmentNavigator = new FragmentNavigator(savedInstanceState, getSupportFragmentManager(), R.id.container_main, this);
        mInstallerFragment = mFragmentNavigator.findFragmentByTag("installer");
        mBackupFragment = mFragmentNavigator.findFragmentByTag("backup");
        if (savedInstanceState == null)
            mFragmentNavigator.switchTo("installer");

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            deliverActionViewUri(intent.getData());
            getIntent().setData(null);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            deliverActionViewUri(intent.getData());
        }
    }

    private void deliverActionViewUri(Uri uri) {
        if (!mIsNavigationEnabled) {
            Toast.makeText(this, R.string.main_navigation_disabled, Toast.LENGTH_SHORT).show();
            return;
        }
        mBottomNavigationView.getMenu().getItem(0).setChecked(true);
        mFragmentNavigator.switchTo("installer");
        getInstallerFragment().handleActionView(uri);
    }

    private void showMiuiWarning() {
        if (Utils.isMiui() && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PreferencesKeys.MIUI_WARNING_SHOWN, false))
            new MiuiWarningDialogFragment().show(getSupportFragmentManager(), "miui_warning_dialog");
    }

    public void setNavigationEnabled(boolean enabled) {
        mIsNavigationEnabled = enabled;

        for (int i = 0; i < mBottomNavigationView.getMenu().size(); i++) {
            mBottomNavigationView.getMenu().getItem(i).setEnabled(enabled);
        }
        mBottomNavigationView.animate()
                .alpha(enabled ? 1f : 0.4f)
                .setDuration(300)
                .start();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_installer:
                mFragmentNavigator.switchTo("installer");
                break;
            case R.id.menu_backup:
                mFragmentNavigator.switchTo("backup");
                break;
        }

        return true;
    }

    @Override
    public Fragment createFragment(String tag) {
        switch (tag) {
            case "installer":
                return getInstallerFragment();
            case "backup":
                return getBackupFragment();
        }

        throw new IllegalArgumentException("Unknown fragment tag: " + tag);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        mFragmentNavigator.writeStateToBundle(outState);
    }

    private InstallerFragment getInstallerFragment() {
        if (mInstallerFragment == null)
            mInstallerFragment = new InstallerFragment();
        return mInstallerFragment;
    }

    private BackupFragment getBackupFragment() {
        if (mBackupFragment == null)
            mBackupFragment = new BackupFragment();
        return mBackupFragment;
    }
}
