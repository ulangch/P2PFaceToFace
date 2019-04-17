package com.ulangch.p2pface2face.utils;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * author : chengwuliang
 * time   : 2019/4/16
 */
public class PermissionUtils {

    private static final int DEFAULT_REQUEST_CODE = 0xff01;

    public static boolean checkPermission(Activity context, String permission) {
        return checkPermissions(context, new String[] {permission});
    }

    public static boolean checkPermissions(Activity context, String[] permissions) {
        return getDeniedPermissions(context, permissions).isEmpty();
    }

    public static boolean requestPermission(Activity context, String permission, int requestCode) {
        return requestPermissions(context, new String[] {permission}, requestCode);
    }

    public static boolean requestPermissions(Activity context, String[] permissions, int requestCode) {
        List<String> deniedPermissions = getDeniedPermissions(context, permissions);
        if (deniedPermissions.isEmpty()) {
            return true;
        } else {
            ActivityCompat.requestPermissions(context, permissions, requestCode);
            return false;
        }
    }

    public static boolean requestPermission(Fragment context, String permission, int requestCode) {
        return requestPermissions(context, new String[] {permission}, requestCode);
    }


    public static boolean requestPermissions(Fragment context, String[] permissions, int requestCode) {
        List<String> deniedPermissions = getDeniedPermissions(context.getActivity(), permissions);
        if (deniedPermissions.isEmpty()) {
            return true;
        } else {
            context.requestPermissions(permissions, requestCode);
            return false;
        }
    }

    public static List<String> getDeniedPermissions(Activity context, String[] permissions) {
        List<String> deniedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_DENIED) {
                deniedPermissions.add(permission);
            }
        }
        return deniedPermissions;
    }
}
