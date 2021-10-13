package larson.groupcontrol.app.util;


import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Operations on arrays, primitive arrays (like {@code byte[]} and primitive wrapper arrays (like
 * {@code Byte[]}).
 * <p>
 * This class tries to handle {@code null} input gracefully. An exception will not be thrown for a
 * {@code null}  array input. Each method documents its behavior.
 *
 * @author larsonzhong (larsonzhong@163.com)
 */
public class ArrayUtils {

    /**
     * An empty immutable byte array.
     */
    public static final byte[] EMPTY_BYTE_ARRAY = new byte[0];
    /**
     * An empty immutable short array.
     */
    public static final short[] EMPTY_SHORT_ARRAY = new short[0];
    /**
     * An empty immutable int array.
     */
    public static final int[] EMPTY_INT_ARRAY = new int[0];
    /**
     * An empty immutable long array.
     */
    public static final long[] EMPTY_LONG_ARRAY = new long[0];

    /**
     * Checks if an array of primitive bytes is null or empty.
     *
     * @param arr the array to test
     * @return {@code true} if the array is null or empty
     */
    public static boolean isEmpty(byte[] arr) {
        return arr == null || arr.length <= 0;
    }

    /**
     * Checks if an array of primitive shorts is null or empty.
     *
     * @param arr the array to test
     * @return {@code true} if the array is null or empty
     */
    public static boolean isEmpty(short[] arr) {
        return arr == null || arr.length <= 0;
    }

    /**
     * Checks if an array of primitive ints is null or empty.
     *
     * @param arr the array to test
     * @return {@code true} if the array is null or empty
     */
    public static boolean isEmpty(int[] arr) {
        return arr == null || arr.length <= 0;
    }

    /**
     * Checks if an array of primitive longs is null or empty.
     *
     * @param arr the array to test
     * @return {@code true} if the array is null or empty
     */
    public static boolean isEmpty(long[] arr) {
        return arr == null || arr.length <= 0;
    }

    /**
     * Checks if an array of Objects is null or empty.
     *
     * @param arr the array to test
     * @return {@code true} if the array is null or empty
     */
    public static <T> boolean isEmpty(T[] arr) {
        return arr == null || arr.length <= 0;
    }


    /**

     */
    public static byte[] verifyCode(byte[] in) {
        if (isEmpty(in)) {
            return EMPTY_BYTE_ARRAY;
        }

        List<Byte> checkedData = new LinkedList<>();
        int lastRes = -1;
        for (int i = 0; i < in.length - 1; i++) {
            if (lastRes == -1) {
                lastRes = (in[i] >> 7 & 0x1) ^ (in[i] >> 6 & 0x1) ^ (in[i] >> 5 & 0x1) ^ (in[i] >> 4 & 0x1) ^ (in[i] >> 3 & 0x1) ^ (in[i] >> 2 & 0x1) ^ (in[i] >> 1 & 0x1 ^ (in[i] & 0x1));
            } else {
                lastRes ^= (in[i] >> 7 & 0x1) ^ (in[i] >> 6 & 0x1) ^ (in[i] >> 5 & 0x1) ^ (in[i] >> 4 & 0x1) ^ (in[i] >> 3 & 0x1) ^ (in[i] >> 2 & 0x1) ^ (in[i] >> 1 & 0x1 ^ (in[i] & 0x1));
            }
            checkedData.add(in[i]);
        }

        return toPrimitives(checkedData);
    }


    /**
     * Unescape any JT/T808 pattern found in the byte array.
     * <p>
     * 解密消息，得到消息体数据
     */
    public static byte[] unescape(byte[] bytes) {
        if (isEmpty(bytes)) {
            return EMPTY_BYTE_ARRAY;
        }

        List<Byte> out = new LinkedList<>();

        //// TODO: 2017/12/6 解密消息，暂时不做

        //  return ListUtils.toPrimitives(out);
        return bytes;
    }

    /**
     * Escapes the bytes in a byte array using JT/T808 rules.
     * <p>
     * Escapes any value it finds into their JT/T808 form.
     * <p>
     */
    public static byte[] escape(byte[] in) {
        if (isEmpty(in)) {
            return EMPTY_BYTE_ARRAY;
        }

        List<Byte> out = new LinkedList<>();
        for (byte b : in) {
            out.add(b);
        }
        return in;
    }

    /**
     * 获取校验码
     * <p>
     * larsonzhong write
     *
     * @param bs
     * @param start
     * @param end
     * @return
     */
    public static byte getCheckSum4JT808(byte[] bs, int start, int end) {
        if (start < 0 || end > bs.length) {
            throw new ArrayIndexOutOfBoundsException("getCheckSum4JT808 error : index out of bounds(start=" + start
                    + ",end=" + end + ",bytes length=" + bs.length + ")");
        }
        byte cs = 0;
        for (int i = start; i < end; i++) {
            cs ^= bs[i];
        }
        return cs;
    }

    /**
     * 最后一位为校验位，第一个it与后一个bit与的值得到校验结果，与校验位比对，如果相等则校验成功返回原始数据，否则返回null数据
     *
     * @param data the byte array to verifyCode, may be {@code null}
     * @return a new unescaped byte array, {@link #EMPTY_BYTE_ARRAY} if null or empty array input
     */
    public static byte xorCheck(byte[] data) {
        if (isEmpty(data)) {
            return 0;
        }

        byte checksum = 0;

        for (byte b : data) {
            checksum ^= b;
        }

        return checksum;
    }

    @SuppressWarnings("unchecked")
    public static byte[] ensureLength(byte[] arr, int len) {
        if (isEmpty(arr)) {
            return new byte[len];
        }
        if (arr.length < len) {
            return concatenate(new byte[len - arr.length], arr);
        }
        if (arr.length > len) {
            return Arrays.copyOf(arr, len);
        }

        return arr;
    }

    /**
     * 把多个byte数组连接成一个byte数组
     *
     * @param parts
     * @return
     */
    public static byte[] concatenate(byte[]... parts) {
        List<Byte> result = new LinkedList<>();

        for (byte[] part : parts) {
            if (isEmpty(part)) {
                continue;
            }
            for (byte b : part) {
                result.add(b);
            }
        }

        return toPrimitives(result);
    }

    /**
     * 把集合类型byte数组连接成一个byte数组
     *
     * @param parts
     * @return
     */
    public static byte[] concatenate(List<byte[]> parts) {
        List<Byte> result = new LinkedList<>();

        for (byte[] part : parts) {
            if (isEmpty(part)) {
                continue;
            }
            for (byte b : part) {
                result.add(b);
            }
        }

        return toPrimitives(result);
    }

    /**
     * davide message to send
     *
     * @param entire the large message body
     * @param len    max length
     * @return the split message list
     */
    public static List<byte[]> divide(byte[] entire, int len) {
        List<byte[]> result = new LinkedList<>();

        if (isEmpty(entire)) {
            result.add(EMPTY_BYTE_ARRAY);
            return result;
        }
        if (len <= 0 || len >= entire.length) {
            result.add(entire);
            return result;
        }

        int head = 0;
        while (head + len < entire.length) {
            result.add(Arrays.copyOfRange(entire, head, head += len));
        }
        result.add(Arrays.copyOfRange(entire, head, entire.length));

        return result;
    }

    /**
     * 把byte转为字符串的bit
     */
    public static String byteToBit(byte[] bytes) {
        StringBuilder stringBuilder = new StringBuilder();
        for (byte b : bytes) {
            stringBuilder.append("")
                    .append((byte) ((b >> 7) & 0x1))
                    .append((byte) ((b >> 6) & 0x1))
                    .append((byte) ((b >> 5) & 0x1))
                    .append((byte) ((b >> 4) & 0x1))
                    .append((byte) ((b >> 3) & 0x1))
                    .append((byte) ((b >> 2) & 0x1))
                    .append((byte) ((b >> 1) & 0x1))
                    .append((byte) ((b) & 0x1));
        }
        return stringBuilder.toString();
    }


    public static byte[] subBytes(byte[] src, int begin, int count) {
        byte[] bs = new byte[count];
        System.arraycopy(src, begin, bs, 0, count);
        return bs;
    }


    /**
     * 合并字节数组
     *
     * @param rest
     * @return
     */
    public static byte[] concatBytes(List<byte[]> rest) {
        int totalLength = 0;
        for (byte[] array : rest) {
            if (array != null) {
                totalLength += array.length;
            }
        }
        byte[] result = new byte[totalLength];
        int offset = 0;
        for (byte[] array : rest) {
            if (array != null) {
                System.arraycopy(array, 0, result, offset, array.length);
                offset += array.length;
            }
        }
        return result;
    }

    /**
     * 多个数组合并
     *
     * @param first 开始数组
     * @param rest  须要合并的数组集
     * @return 合并的数组
     */
    public static byte[] concatBytes(byte[] first, byte[]... rest) {
        int totalLength = first.length;
        for (byte[] array : rest) {
            totalLength += array.length;
        }
        byte[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (byte[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }


    /**
     * Converts a list of wrappers to an array of primitives.
     * <p>
     * Any unspecified element will be ignored.
     *
     * @param wrappers a Byte list, may be {@code null}
     * @return a byte array, {@link ArrayUtils#EMPTY_BYTE_ARRAY} if null or empty list input
     */
    private static byte[] toPrimitives(List<Byte> wrappers) {
        if (wrappers == null || wrappers.size() <= 0) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }

        byte[] primitives = new byte[wrappers.size()];

        int i = 0;
        for (Byte b : wrappers) {
            if (b == null) {
                continue;
            }
            primitives[i++] = b;
        }

        return primitives;
    }
}
