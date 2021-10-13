package larson.groupcontrol.app.message;

import android.text.TextUtils;

import larson.groupcontrol.app.util.CrcUtils;

/**
 * @author Rony
 * @date 2018/8/20
 */

public class MessageSnBuilder {
    private static MessageSnBuilder instance;
    private int sn;
    private short mClientKey;

    private MessageSnBuilder() {
        sn = 0;
        mClientKey = 0;
    }

    public static MessageSnBuilder getInstance() {
        if (instance == null) {
            synchronized (MessageSnBuilder.class) {
                if (instance == null) {
                    instance = new MessageSnBuilder();
                }
            }
        }

        return instance;
    }

    /**
     * 获取命令序列号
     *
     * @return 命令序列号
     */
    short getNextSn() {
        sn = (sn + 1) % 0x10000;
        sn = (sn == 0 ? 1 : sn);
        return (short) sn;
    }

    /**
     * 获取客户端唯一标识，通过Imei+deviceid等字符进行CRC16计算得到一个唯一标识
     *
     * @return 客户端唯一标识
     */
    short getClientKey() throws Exception {
        if (mClientKey == 0) {
            throw new Exception("not client key!");
        }
        return mClientKey;
    }

    /**
     * 生成唯一标识，通过Imei+deviceid等字符进行CRC16计算
     *
     * @param clientKey Imei+deviceid+...
     * @throws Exception 返回异常
     */
    public void setClientKey(String clientKey) throws Exception {
        if (TextUtils.isEmpty(clientKey)) {
            throw new Exception("Client key is null!");
        }
        mClientKey = (short) CrcUtils.crc16Ccitt(CrcUtils.CRC16_CCITT, clientKey.getBytes());
    }
}
