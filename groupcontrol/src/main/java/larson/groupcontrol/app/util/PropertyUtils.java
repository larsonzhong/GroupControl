package larson.groupcontrol.app.util;

import java.lang.reflect.Method;

/**
 * @author Rony
 * @date 2018/8/22
 */

public class PropertyUtils {

    public static String getProperty(String key) {
        String value = null;
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("get", String.class);
            value = (String) (get.invoke(clazz, key));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static String getProperty(String key, String defaultValue) {
        String value = defaultValue;
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("get", String.class, String.class);
            value = (String) (get.invoke(clazz, key, defaultValue));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static int getIntProperty(String key, int defaultValue) {
        int value = defaultValue;
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("getInt", String.class, String.class);
            value = (int) (get.invoke(clazz, key, defaultValue));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static long getLongProperty(String key, long defaultValue) {
        long value = defaultValue;
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("getLong", String.class, String.class);
            value = (long) (get.invoke(clazz, key, defaultValue));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static boolean getBooleanProperty(String key, boolean defaultValue) {
        boolean value = defaultValue;
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method get = clazz.getMethod("getBoolean", String.class, String.class);
            value = (boolean) (get.invoke(clazz, key, defaultValue));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return value;
    }

    public static void setProperty(String key, String value) {
        try {
            Class<?> clazz = Class.forName("android.os.SystemProperties");
            Method set = clazz.getMethod("set", String.class, String.class);
            set.invoke(clazz, key, value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
