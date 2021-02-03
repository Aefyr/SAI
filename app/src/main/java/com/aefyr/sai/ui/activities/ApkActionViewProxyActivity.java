package com.aefyr.sai.ui.activities;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.Nullable;

public class ApkActionViewProxyActivity extends ThemedActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!Intent.ACTION_VIEW.equals(getIntent().getAction()) || getIntent().getData() == null) {
            finish();
            return;
        }

        Intent mainActivityIntent = new Intent(this, MainActivity.class);
        mainActivityIntent.setAction(getIntent().getAction());
        mainActivityIntent.setData(getIntent().getData());
        startActivity(mainActivityIntent);

        finish();
    }

    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context, ApkActionViewProxyActivity.class);
    }
}
