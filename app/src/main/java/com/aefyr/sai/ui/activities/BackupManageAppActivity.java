package com.aefyr.sai.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.fragments.BackupManageAppFragment;

public class BackupManageAppActivity extends ThemedActivity implements BackupManageAppFragment.DismissDelegate {

    private static final String EXTRA_PKG = "pkg";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_manage_app);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_backup_app_details, BackupManageAppFragment.newInstance(getIntent().getStringExtra(EXTRA_PKG)))
                    .commitNow();
        }
    }

    public static void start(Context context, String pkg) {
        Intent intent = new Intent(context, BackupManageAppActivity.class);
        intent.putExtra(EXTRA_PKG, pkg);
        context.startActivity(intent);
    }


    @Override
    public void dismiss(BackupManageAppFragment fragment) {
        finish();
    }
}
