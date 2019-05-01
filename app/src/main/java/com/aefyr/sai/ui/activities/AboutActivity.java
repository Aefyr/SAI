package com.aefyr.sai.ui.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;
import com.aefyr.sai.utils.Theme;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Theme.apply(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        ((TextView) findViewById(R.id.tv_about_app)).setText(String.format("%s %s", getString(R.string.app_name_full), BuildConfig.VERSION_NAME));
        findViewById(R.id.button_about_source).setOnClickListener((v) -> openLink(getString(R.string.about_source_link)));
        findViewById(R.id.button_about_donate).setOnClickListener((v) -> openLink(getString(R.string.about_donate_link)));
    }

    private void openLink(String link) {
        Intent openLinkIntent = new Intent(Intent.ACTION_VIEW);
        openLinkIntent.setData(Uri.parse(link));
        startActivity(openLinkIntent);
    }
}
