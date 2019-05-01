package com.aefyr.sai.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

import java.util.Objects;

public class PermissionsUtils {
    public static final int REQUEST_CODE_STORAGE_PERMISSIONS = 322;

    public static boolean checkAndRequestStoragePermissions(Activity a) {
        if (Build.VERSION.SDK_INT >= 23 && (ActivityCompat.checkSelfPermission(a, Manifest.permission.READ_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_DENIED) {
            a.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSIONS);
            return false;
        }
        return true;
    }

    public static boolean checkAndRequestStoragePermissions(Fragment f) {
        if (Build.VERSION.SDK_INT >= 23 && (ActivityCompat.checkSelfPermission(Objects.requireNonNull(f.getActivity()), Manifest.permission.READ_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_DENIED) {
            f.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSIONS);
            return false;
        }
        return true;
    }

}
