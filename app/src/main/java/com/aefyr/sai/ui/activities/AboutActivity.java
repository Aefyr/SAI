package com.aefyr.sai.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;
import com.aefyr.sai.ui.fragments.SuperSecretPreferencesFragment;

public class AboutActivity extends ThemedActivity {

    private static int sLogoClicksCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ((TextView) findViewById(R.id.tv_about_app)).setText(String.format("%s %s", getString(R.string.app_name_full), BuildConfig.VERSION_NAME));
        findViewById(R.id.button_about_source).setOnClickListener((v) -> openLink(getString(R.string.about_source_link)));
        findViewById(R.id.button_about_donate).setOnClickListener((v) -> openLink(getString(R.string.about_donate_link)));
        findViewById(R.id.button_about_licenses).setOnClickListener((v) -> startActivity(new Intent(this, LicensesActivity.class)));
        findViewById(R.id.button_about_translate).setOnClickListener(v -> openLink(getString(R.string.about_translate_link)));

        findViewById(R.id.iv_about_logo).setOnClickListener((v) -> sLogoClicksCount++);
        findViewById(R.id.iv_about_logo).setOnLongClickListener((v) -> {
            if (sLogoClicksCount >= 3)
                PreferencesActivity.open(this, SuperSecretPreferencesFragment.class, getString(R.string.sss));

            return sLogoClicksCount >= 3;
        });
    }

    private void openLink(String link) {
        Intent openLinkIntent = new Intent(Intent.ACTION_VIEW);
        openLinkIntent.setData(Uri.parse(link));
        startActivity(openLinkIntent);
    }
}
