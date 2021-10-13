package cn.larson.groupcontrol.main.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.util.List;

/**
 * @author Rony
 * @date 2018/8/28
 */

public class ServiceUtils {
    /**
     * 获取服务是否运行
     *
     * @param context     上下文
     * @param serviceName 服务名
     * @return 是否运行
     */
    public static boolean isServiceNotWork(Context context, String serviceName) {
        boolean isWork = false;
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> serviceInfos = activityManager != null ? activityManager.getRunningServices(100) : null;
        if (serviceInfos == null || serviceInfos.size() <= 0) {
            return true;
        }
        for (int i = 0; i < serviceInfos.size(); i++) {
            String name = serviceInfos.get(i).service.getClassName();
            if (name.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return !isWork;
    }
}
