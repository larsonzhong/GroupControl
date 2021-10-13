package cn.larson.groupcontrol.main.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

/**
 * 获取Package版本，并校验是否为新版本
 *
 * @author Rony
 * @date 2018/3/13
 */

public class PackageVersion {
    private static final String GROUP_CONTROL_SETTINGS_NAME = "GroupControlSettings";
    private static final String KEY_VERSION_NAME = "keyVersionName";
    private static final String KEY_VERSION_CODE = "keyVersionCode";
    private static final String KEY_LAST_UPDATE_TIME = "keyLastUpdateTime";

    private static String mVersionName;
    private static int mVersionCode;
    private static long mLastUpdateTime;

    private static void getSharedPrefVersion(Context context) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(GROUP_CONTROL_SETTINGS_NAME, 0);
        mVersionName = sharedPreferences.getString(KEY_VERSION_NAME, "");
        mVersionCode = sharedPreferences.getInt(KEY_VERSION_CODE, 0);
        mLastUpdateTime = sharedPreferences.getLong(KEY_LAST_UPDATE_TIME, 0);
    }

    private static void saveSharedPrefVersion(Context context, PackageInfo pi) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(GROUP_CONTROL_SETTINGS_NAME, 0);
        SharedPreferences.Editor edit = sharedPreferences.edit();
        edit.putString(KEY_VERSION_NAME, pi.versionName);
        edit.putInt(KEY_VERSION_CODE, pi.versionCode);
        edit.putLong(KEY_LAST_UPDATE_TIME, pi.lastUpdateTime);
        edit.apply();
    }

    public static boolean isNewVersion(Context context) {
        getSharedPrefVersion(context);
        PackageInfo pi;
        try {
            PackageManager pm = context.getPackageManager();
            pi = pm.getPackageInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return true;
        }

        if (pi.versionName.equals(mVersionName)
                && pi.versionCode == mVersionCode
                && pi.lastUpdateTime == mLastUpdateTime) {
            return false;
        }
        saveSharedPrefVersion(context, pi);
        return true;
    }
}
