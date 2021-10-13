package com.larson.remotedisplay.utils;

/**
 * 常量类
 * @author Administrator
 */
public class SocketUtils {

    /**
     * 端口号
     */
    public static final int PROT = 60000;

    /**
     * 超时时间
     */
    public static final int TIME_MILLIS = 3000;

    /**
     * 数据头的长度
     */
    public static final int DATA_HEADER_LENGTH = 20;

    /**
     * 没次数据前面是包头，后面是数据
     */
    public static final int ODD_EVEN = 2;

}
