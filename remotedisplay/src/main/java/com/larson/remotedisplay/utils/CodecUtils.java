package com.larson.remotedisplay.utils;

/**
 *
 * @author omerjerk
 * @date 21/7/14
 */
public class CodecUtils {

    /**
     * 录制视频的宽
     */
    public static final int WIDTH = 1080 / 4;

    /**
     * 录制视频的高
     */
    public static final int HEIGHT = 1920 / 4;

    /**
     * 获取加密/解密缓冲区状态的延时时间
     */
    public static final int TIMEOUT_USEC = 10000;

    /**
     * 录制视频的比特率
     */
    public static final int BIT_RATE = (int)(1024*1024*0.5);

    /**
     * 录制视频的帧率
     */
    public static final int FRAME_RATE = 30;

    /**
     * 录制视频每间隔多长时间一个关键帧
     */
    public static final int IFRAME_INTERVAL = 1;

    /**
     * 录制视频的文件类型
     */
    public static final String MIME_TYPE = "video/avc";

}
