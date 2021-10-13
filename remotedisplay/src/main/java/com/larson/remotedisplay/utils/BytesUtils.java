package com.larson.remotedisplay.utils;

import java.nio.ByteBuffer;

/**
 * @author Administrator
 */
public class BytesUtils {
    /**
     * 大字节序
     * <p>
     * 将int数值转换为占四个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。  和bytesToInt2（）配套使用
     */
    public static byte[] intToBytes2(int value) {
        byte[] src = new byte[4];
        src[0] = (byte) ((value >> 24) & 0xFF);
        src[1] = (byte) ((value >> 16) & 0xFF);
        src[2] = (byte) ((value >> 8) & 0xFF);
        src[3] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 大字节序
     * byte数组中取int数值，本方法适用于(低位在后，高位在前)的顺序。和intToBytes2（）配套使用
     */
    public static int bytesToInt2(byte[] src, int offset) {
        int value;
        value = ((src[offset] & 0xFF) << 24)
                | ((src[offset + 1] & 0xFF) << 16)
                | ((src[offset + 2] & 0xFF) << 8)
                | (src[offset + 3] & 0xFF);
        return value;
    }

    /**
     * 大字节序
     * byte数组中取long数值，本方法适用于(低位在后，高位在前)的顺序。和longToBytes（）配套使用
     */
    public static long bytesToLong(byte[] src, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.put(src, offset, 8);
        buffer.flip();
        return buffer.getLong();
    }

    /**
     * 大字节序
     * 将long数值转换为占八个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。和bytesToLong()配套使用
     */
    public static byte[] longToBytes(long value) {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(0, value);
        return buffer.array();
    }

    /**
     * Converts a primitive byte to an array of bytes
     * 把byte数据转成byte数组
     *
     * @param num a byte
     * @return a byte array
     */
    public static byte[] asBytes(byte num) {
        byte[] result = new byte[1];

        result[0] = num;

        return result;
    }
}
