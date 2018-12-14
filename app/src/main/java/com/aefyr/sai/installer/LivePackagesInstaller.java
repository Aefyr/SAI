package com.aefyr.sai.installer;

import android.content.Context;

import java.io.File;
import java.util.List;

import androidx.lifecycle.LiveData;

public class LivePackagesInstaller extends LiveData<LivePackagesInstaller.Status> {
    public enum Status {
        IDLE, INSTALLING, INSTALLED, FAILED
    }

    private Context mContext;

    public LivePackagesInstaller(Context c) {
        mContext = c;
        setValue(Status.IDLE);
    }

    public void installPackages(List<File> apkFiles) {
        SAIPackageInstaller.getInstance(mContext).installApks(apkFiles, (status -> {
            switch (status) {
                case INSTALLING:
                    setValue(Status.INSTALLING);
                    break;
                case AWAITING_USER_CONFIRMATION:
                    setValue(Status.INSTALLING);
                    break;
                case INSTALLATION_SUCCEED:
                    setValue(Status.INSTALLED);
                    break;
                case INSTALLATION_FAILED:
                    setValue(Status.FAILED);
                    break;
                case UNKNOWN:
                    break;
            }
        }));
    }

    public void reset() {
        setValue(Status.IDLE);
    }
}
