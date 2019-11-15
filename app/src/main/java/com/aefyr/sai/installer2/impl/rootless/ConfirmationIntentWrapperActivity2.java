package com.aefyr.sai.installer2.impl.rootless;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.aefyr.sai.utils.Logs;

public class ConfirmationIntentWrapperActivity2 extends AppCompatActivity {

    private static final String EXTRA_CONFIRMATION_INTENT = "confirmation_intent";
    public static final String EXTRA_SESSION_ID = "session_id";

    private static final int REQUEST_CODE_CONFIRM_INSTALLATION = 322;

    private boolean mFinishedProperly = false;

    private int mSessionId;
    private Intent mConfirmationIntent;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        mSessionId = intent.getIntExtra(EXTRA_SESSION_ID, -1);
        mConfirmationIntent = intent.getParcelableExtra(EXTRA_CONFIRMATION_INTENT);

        if (savedInstanceState == null) {
            try {
                startActivityForResult(mConfirmationIntent, REQUEST_CODE_CONFIRM_INSTALLATION);
            } catch (Exception e) {
                Logs.logException(e);
                sendErrorBroadcast(mSessionId, RootlessSaiPiBroadcastReceiver.STATUS_BAD_ROM);
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_CONFIRM_INSTALLATION) {
            mFinishedProperly = true;
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (isFinishing() && !mFinishedProperly)
            start(this, mSessionId, mConfirmationIntent); //Because if user doesn't confirm/cancel the installation, PackageInstaller session will hang

    }

    public static void start(Context c, int sessionId, Intent confirmationIntent) {
        Intent intent = new Intent(c, ConfirmationIntentWrapperActivity2.class);
        intent.putExtra(EXTRA_CONFIRMATION_INTENT, confirmationIntent);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        c.startActivity(intent);
    }

    private void sendErrorBroadcast(int sessionID, int status) {
        Intent statusIntent = new Intent(RootlessSaiPiBroadcastReceiver.ACTION_DELIVER_PI_EVENT);
        statusIntent.putExtra(PackageInstaller.EXTRA_STATUS, status);
        statusIntent.putExtra(PackageInstaller.EXTRA_SESSION_ID, sessionID);

        sendBroadcast(statusIntent);
    }

}
