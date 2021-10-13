package larson.groupcontrol.app.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.TextUtils;

/**
 * @author Rony
 * @date 2018/8/22
 */

public class DeviceInfoUtils {

    @SuppressLint("MissingPermission")
    public static String getDeviceId(Context context) {
        try {
            if (context == null) {
                return null;
            }
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            String deviceId;
            if (telephonyManager == null) {
                return null;
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                deviceId = telephonyManager.getImei();
            } else {
                deviceId = telephonyManager.getDeviceId();
            }
            if (TextUtils.isEmpty(deviceId)) {
                deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
            }
            return deviceId;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getAndroidId(Context context) {
        return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    public static String getSerialNo() {
        return PropertyUtils.getProperty("ro.serialno", "");
    }

    public static String getDeviceInfo(Context context) {
        String deviceInfo = "";
        String deviceId = getDeviceId(context);
        if (!TextUtils.isEmpty(deviceId)) {
            deviceInfo = deviceId;
        }
        String androidId = getAndroidId(context);
        if (!TextUtils.isEmpty(androidId)) {
            deviceInfo = deviceInfo + "_" + androidId;
        }
        String serialNo = getSerialNo();
        if (!TextUtils.isEmpty(serialNo)) {
            deviceInfo = deviceInfo + "_" + serialNo;
        }
        return deviceInfo;
    }
}
