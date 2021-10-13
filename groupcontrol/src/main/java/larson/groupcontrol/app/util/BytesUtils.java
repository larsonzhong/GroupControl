package larson.groupcontrol.app.util;

import java.nio.ByteBuffer;

/**
 * ......................-~~~~~~~~~-._       _.-~~~~~~~~~-.
 * ............... _ _.'              ~.   .~              `.__
 * ..............'//     NO           \./      BUG         \\`.
 * ............'//                     |                     \\`.
 * ..........'// .-~"""""""~~~~-._     |     _,-~~~~"""""""~-. \\`.
 * ........'//.-"                 `-.  |  .-'                 "-.\\`.
 * ......'//______.============-..   \ | /   ..-============.______\\`.
 * ....'______________________________\|/______________________________`.
 * ..larsonzhong@163.com      created in 2018/7/27     @author : larsonzhong
 */
public class BytesUtils {
    /**
     * 小字节序
     * <p>
     * 将int数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToInt（）配套使用
     *
     * @param value 要转换的int值
     * @return byte数组
     */
    public static byte[] intToBytes(int value) {
        byte[] src = new byte[4];
        src[3] = (byte) ((value >> 24) & 0xFF);
        src[2] = (byte) ((value >> 16) & 0xFF);
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

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
     * 小字节序
     * <p>
     * 将short数值转换为占两个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToShort（）配套使用
     *
     * @param value 要转换的short值
     * @return byte数组
     */
    public static byte[] shortToBytes(short value) {
        byte[] src = new byte[2];
        src[1] = (byte) ((value >> 8) & 0xFF);
        src[0] = (byte) (value & 0xFF);
        return src;
    }

    /**
     * 小字节序
     * <p>
     * 将magic, type数值转换为占两个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。
     * 与bytesToType() bytesToMagic()配套使用
     * type 占高4位, magic占底12位
     *
     * @param magic type
     * @return byte数组
     */
    public static byte[] magicTypeToBytes(short magic, byte type) {
        byte[] src = new byte[2];
        src[1] = (byte) ((magic >> 8) & 0x0F);
        src[1] |= (byte) ((type << 4) & 0xF0);
        src[0] = (byte) (magic & 0xFF);
        return src;
    }

    /**
     * 小字节序
     * byte数组中取int数值，本方法适用于(低位在前，高位在后)的顺序，和和intToBytes（）配套使用
     *
     * @param src    byte数组
     * @param offset 从数组的第offset位开始
     * @return int数值
     */
    public static int bytesToInt(byte[] src, int offset) {
        int value;
        value = (src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8)
                | ((src[offset + 2] & 0xFF) << 16)
                | ((src[offset + 3] & 0xFF) << 24);
        return value;
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
     * 小字节序
     * byte数组中取short数值，本方法适用于(低位在前，高位在后)的顺序，和shortToBytes（）配套使用
     *
     * @param src    byte数组
     * @param offset 从数组的第offset位开始
     * @return short数值
     */
    public static short bytesToShort(byte[] src, int offset) {
        short value;
        value = (short) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0xFF) << 8));
        return value;
    }

    /**
     * 小字节序
     * byte数组中取short数值，本方法适用于(低位在前，高位在后)的顺序，和magicTypeToBytes（）配套使用
     *
     * @param src    byte数组
     * @param offset 从数组的第offset位开始
     * @return short数值
     * 注：type 占高4位, magic占低12位，他们共用short。
     */
    public static byte bytesToType(byte[] src, int offset) {
        byte value;
        value = (byte) ((src[offset + 1] & 0xF0) >> 4);
        return value;
    }

    /**
     * 小字节序
     * byte数组中取short数值，本方法适用于(低位在前，高位在后)的顺序，和magicTypeToBytes（）配套使用
     *
     * @param src    byte数组
     * @param offset 从数组的第offset位开始
     * @return short数值
     * 注：type 占高4位, magic占低12位，他们共用short。
     */
    public static short bytesToMagic(byte[] src, int offset) {
        short value;
        value = (short) ((src[offset] & 0xFF)
                | ((src[offset + 1] & 0x0F) << 8));
        return value;
    }

    /**
     * 生成打印16进制日志所需的字符串
     *
     * @param data 数据源
     * @return 字符串给日志使用
     */
    public static String toHexStringForLog(byte[] data) {
        StringBuilder sb = new StringBuilder();
        if (data != null) {
            for (int i = 0; i < data.length; i++) {
                String tempHexStr = Integer.toHexString(data[i] & 0xff) + " ";
                tempHexStr = tempHexStr.length() == 2 ? "0" + tempHexStr : tempHexStr;
                sb.append(tempHexStr);
            }
        }
        return sb.toString();
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

    /**
     * 将int转为ip字符串
     * @param ip
     * @return
     */
    public static String int2Ip(int ip){
        StringBuilder builder = new StringBuilder(String.valueOf(ip >>> 24));
        builder.append(".");
        builder.append(String.valueOf((ip & 0X00FFFFFF) >>> 16));
        builder.append(".");
        builder.append(String.valueOf((ip & 0X0000FFFF) >>> 8));
        builder.append(".");
        builder.append(String.valueOf(ip & 0X000000FF));
        return builder.toString();
    }

    /**
     * 小字节序
     * byte数组中取float数值，本方法适用于(低位在前，高位在后)的顺序，和floatToBytes（）配套使用
     *
     * @param src
     * @param offset
     * @return
     */
    public static float bytesToFloat(byte[] src, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put(src[offset + 3]);
        buffer.put(src[offset + 2]);
        buffer.put(src[offset + 1]);
        buffer.put(src[offset]);
        buffer.flip();
        return buffer.getFloat();
    }

    /**
     * 大字节序
     * byte数组中取float数值，本方法适用于(高位在前，低位在后)的顺序，和floatToBytes2（）配套使用
     *
     * @param src
     * @param offset
     * @return
     */
    public static float bytesToFloat2(byte[] src, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.put(src, offset, 4);
        buffer.flip();
        return buffer.getFloat();
    }

    /**
     * 小字节序
     * 将float数值转换为占四个字节的byte数组，本方法适用于(低位在前，高位在后)的顺序。 和bytesToFloat（）配套使用
     *
     * @param value
     * @return
     */
    public static byte[] floatToBytes(float value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putFloat(value);
        byte[] bytes = new byte[4];
        buffer.flip();
        bytes[0] = buffer.get(3);
        bytes[1] = buffer.get(2);
        bytes[2] = buffer.get(1);
        bytes[3] = buffer.get(0);
        return bytes;
    }

    /**
     * 大字节序
     * 将float数值转换为占四个字节的byte数组，本方法适用于(高位在前，低位在后)的顺序。 和bytesToFloat2（）配套使用
     *
     * @param value
     * @return
     */
    public static byte[] floatToBytes2(float value) {
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.putFloat(value);
        return buffer.array();
    }
}
