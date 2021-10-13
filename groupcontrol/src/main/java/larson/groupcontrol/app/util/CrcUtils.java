package larson.groupcontrol.app.util;

import java.util.Arrays;

/**
 * @author Rony
 * @date 2018/8/15
 * CRC数组处理工具类及数组合并
 */

public class CrcUtils {
    private static final int CRC16_BUF_MIN_SIZE = 3;
    public static int CRC16_XMODEM = 1;
    public static int CRC16_CCITT = 2;
    public static int CRC16_CCITT_FALSE = 3;

    /**
     * 生成带crc校验核的数组
     *
     * @param buf     数据
     * @param crcType crc校验类型
     * @return 末尾带crc校验核的数组, null表示失败
     */
    public static byte[] getCrc16Bytes(byte[] buf, int crcType) {
        if (buf == null) {
            return new byte[0];
        }
        if (buf.length == 0) {
            return buf;
        }
        byte[] crcBytes = new byte[2];
        int crc16 = crc16Ccitt(crcType, buf);
        crcBytes[0] = (byte) (crc16 & 0xff);
        crcBytes[1] = (byte) ((crc16 >> 8) & 0xff);
        // 将新生成的byte数组添加到原数据结尾并返回
        return ArrayUtils.concatBytes(buf, crcBytes);
    }

    /**
     * 从带crc校验核的数组中获取数据，并进行crc校验。
     *
     * @param srcBuf  末尾带crc校验核的数组
     * @param crcType crc校验类型
     * @return 数据， null表示crc验证失败
     */
    public static byte[] getDataBytes(byte[] srcBuf, int crcType) {
        if (srcBuf == null || srcBuf.length < CRC16_BUF_MIN_SIZE) {
            return new byte[0];
        }
        byte[] data = Arrays.copyOf(srcBuf, srcBuf.length - 2);
        int crc16 = crc16Ccitt(crcType, data);
        int crc16Source = ((srcBuf[srcBuf.length - 2] & 0xff)
                        | ((srcBuf[srcBuf.length - 1] & 0xff) << 8)) & 0xffff;
        if (crc16 != crc16Source) {
            return null;
        }
        return data;
    }

    /**
     * CRC-CCITT(XModem)
     * CRC-CCITT
     * CRC-CCITT_FALSE
     * 校验模式
     *
     * @param crcType CRC16_XMODEM:1, CRC16_CCITT:2, CRC16_CCITT_FALSE:3
     * @param bytes 数据
     * @return crc16
     */
    public static int crc16Ccitt(int crcType, byte[] bytes) {
        int crc = 0;
        int polynomial = 0x1021;

        if (crcType == CRC16_CCITT_FALSE) {
            crc = 0xFFFF;
        }

        for (byte b : bytes) {
            if (crcType == CRC16_CCITT) {
                b = reversalByte(b);
            }
            for (int i = 0; i < Byte.SIZE; i++) {
                boolean bit = ((b >> (7 - i) & 1) == 1);
                boolean c15 = ((crc >> 15 & 1) == 1);
                crc <<= 1;
                if (c15 ^ bit) {
                    crc ^= polynomial;
                }
            }
        }
        if (crcType == CRC16_CCITT) {
            crc = reversalShort((short) crc);
        }
        crc &= 0xffff;

        return crc;
    }

    /**
     * Byte数据高低位交换
     *
     * @param data 数据
     * @return 高低位交换的数据
     */
    public static byte reversalByte(byte data) {
        byte result = 0;
        for (int i = 0; i < Byte.SIZE; i++) {
            if ((data & (1 << i)) != 0) {
                result |= 1 << (7 - i);
            }
        }
        return result;
    }

    /**
     * Short数据高低位交换
     *
     * @param data 数据
     * @return 高低位交换的数据
     */
    public static short reversalShort(short data) {
        short result = 0;
        for (int i = 0; i < Short.SIZE; i++) {
            if ((data & (1 << i)) != 0) {
                result |= 1 << (15 - i);
            }
        }
        return result;
    }
}
