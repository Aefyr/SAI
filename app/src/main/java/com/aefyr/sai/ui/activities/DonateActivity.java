package com.aefyr.sai.ui.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.aefyr.sai.R;
import com.aefyr.sai.billing.BillingManager;
import com.aefyr.sai.billing.DefaultBillingManager;
import com.aefyr.sai.ui.fragments.DonateFragment;

public class DonateActivity extends ThemedActivity {

    private BillingManager mBillingManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        mBillingManager = DefaultBillingManager.getInstance(this);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_donate, new DonateFragment())
                    .commitNow();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mBillingManager.refresh();
    }

    public static void start(Context context) {
        context.startActivity(new Intent(context, DonateActivity.class));
    }
}
