package com.aefyr.sai.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;

public class PermissionsUtils {
    public static final int REQUEST_CODE_STORAGE_PERMISSIONS = 322;
    public static final int REQUEST_CODE_SHIZUKU = 1337;

    public static boolean checkAndRequestStoragePermissions(Activity a) {
        return checkAndRequestPermissions(a, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSIONS);
    }

    public static boolean checkAndRequestStoragePermissions(Fragment f) {
        return checkAndRequestPermissions(f, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_STORAGE_PERMISSIONS);
    }

    public static boolean checkAndRequestShizukuPermissions(Activity a) {
        return checkAndRequestPermissions(a, new String[]{"moe.shizuku.manager.permission.API_V23"}, REQUEST_CODE_SHIZUKU);
    }

    public static boolean checkAndRequestShizukuPermissions(Fragment f) {
        return checkAndRequestPermissions(f, new String[]{"moe.shizuku.manager.permission.API_V23"}, REQUEST_CODE_SHIZUKU);
    }

    private static boolean checkAndRequestPermissions(Activity a, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT < 23)
            return true;

        for (String permission : permissions) {
            if ((ActivityCompat.checkSelfPermission(a, permission)) == PackageManager.PERMISSION_DENIED) {
                a.requestPermissions(permissions, requestCode);
                return false;
            }
        }
        return true;
    }

    private static boolean checkAndRequestPermissions(Fragment f, String[] permissions, int requestCode) {
        if (Build.VERSION.SDK_INT < 23)
            return true;

        for (String permission : permissions) {
            if ((ActivityCompat.checkSelfPermission(f.requireContext(), permission)) == PackageManager.PERMISSION_DENIED) {
                f.requestPermissions(permissions, requestCode);
                return false;
            }
        }
        return true;
    }

}
