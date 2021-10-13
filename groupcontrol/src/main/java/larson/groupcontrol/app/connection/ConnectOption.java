package larson.groupcontrol.app.connection;

import larson.groupcontrol.app.message.MessageSnBuilder;
import larson.groupcontrol.app.exception.UnconnectedException;

import java.nio.ByteOrder;

/**
 * ......................-~~~~~~~~~-._       _.-~~~~~~~~~-.
 * ............... _ _.'              ~.   .~              `.__
 * ..............'//     NO           \./      BUG         \\`.
 * ............'//                     |                     \\`.
 * ..........'// .-~"""""""~~~~-._     |     _,-~~~~"""""""~-. \\`.
 * ........'//.-"                 `-.  |  .-'                 "-.\\`.
 * ......'//______.============-..   \ | /   ..-============.______\\`.
 * ....'______________________________\|/______________________________`.
 * ..larsonzhong@163.com      created in 2018/8/13     @author : larsonzhong
 * 可选配置，如果没有配置，使用默认值
 */
public class ConnectOption {

    /**
     * 框架是否是调试模式
     */
    private boolean isDebug = true;
    /**
     * 写入Socket管道中给服务器的字节序
     */
    private ByteOrder mWriteOrder;
    /**
     * 从Socket管道中读取字节序时的字节序
     */
    private ByteOrder mReadByteOrder;
    /**
     * 脉搏频率单位是毫秒
     */
    private long mPulseFrequency;
    /**
     * 重连间隔，单位是毫秒
     */
    private final long reconnectInterval;
    /**
     * 脉搏丢失次数<br>
     * 大于或等于丢失次数时将断开该通道的连接<br>
     * 抛出{@link UnconnectedException}
     */
    private int mReconnectMaxAttemptTimes;
    /**
     * 连接超时时间(毫秒)
     */
    private int mSocketTimeout;

    private boolean isReconnectAllowed;

    private ConnectOption(Builder okOptions) {
        mPulseFrequency = okOptions.mPulseFrequency;
        mSocketTimeout = okOptions.mSocketTimeout;
        mWriteOrder = okOptions.mWriteOrder;
        mReadByteOrder = okOptions.mReadByteOrder;
        mReconnectMaxAttemptTimes = okOptions.mReconnectMaxAttemptTimes;
        isReconnectAllowed = okOptions.isReconnectAllowed;
        isDebug = okOptions.isDebug;
        reconnectInterval = okOptions.mReconnectInterval;
    }


    public boolean isReconnectAllowed() {
        return isReconnectAllowed;
    }

    public boolean isDebug() {
        return isDebug;
    }

    public ByteOrder getWriteOrder() {
        return mWriteOrder;
    }

    public ByteOrder getReadByteOrder() {
        return mReadByteOrder;
    }

    public long getPulseFrequency() {
        return mPulseFrequency;
    }

    public long getReconnectInterval() {
        return reconnectInterval;
    }

    public int getReconnectMaxAttemptTimes() {
        return mReconnectMaxAttemptTimes;
    }

    public int getSocketTimeout() {
        return mSocketTimeout;
    }

    public static class Builder {
        /**
         * 脉搏频率单位是毫秒
         */
        private long mPulseFrequency;
        /**
         * 重连间隔单位是毫秒
         */
        private long mReconnectInterval;
        /**
         * 连接超时时间(毫秒)
         */
        private int mSocketTimeout;
        /**
         * 写入Socket管道中给服务器的字节序
         */
        private ByteOrder mWriteOrder;
        /**
         * 从Socket管道中读取字节序时的字节序
         */
        private ByteOrder mReadByteOrder;
        /**
         * 最大重连次数，当大于该次数则停止重连
         * 大于或等于丢失次数时将断开该通道的连接<br>
         * 抛出{@link UnconnectedException}
         */
        private int mReconnectMaxAttemptTimes;
        /**
         * 是否容许自动重连
         */
        private boolean isReconnectAllowed;
        /**
         * 是否调试模式
         */
        private boolean isDebug = true;

        public Builder(String clientID) throws Exception {
            MessageSnBuilder.getInstance().setClientKey(clientID);
        }

        /**
         * 设置脉搏间隔频率<br>
         * 单位是毫秒<br>
         *
         * @param pulseFrequency 间隔毫秒数
         */

        public Builder setPulseFrequency(long pulseFrequency) {
            mPulseFrequency = pulseFrequency;
            return this;
        }

        /**
         * 设置重连间隔<br>
         *
         * @param interval 间隔毫秒数
         */
        public Builder setReconnectInterval(long interval) {
            mReconnectInterval = interval;
            return this;
        }


        /**
         * 脉搏丢失次数<br>
         * 大于或等于丢失次数时将断开该通道的连接<br>
         * 抛出{@link UnconnectedException}<br>
         * 默认是5次
         *
         * @param pulseFeedLoseTimes 丢失心跳ACK的次数,例如5,当丢失3次时,自动断开.
         */
        public Builder setReconnectMaxAttemptTimes(int pulseFeedLoseTimes) {
            mReconnectMaxAttemptTimes = pulseFeedLoseTimes;
            return this;
        }

        /**
         * 设置输出Socket管道中给服务器的字节序<br>
         * 默认是:大端字节序<br>
         *
         * @param writeOrder {@link ByteOrder} 字节序
         * @deprecated 请使用 {@link Builder#setWriteByteOrder(ByteOrder)}
         */
        public Builder setWriteOrder(ByteOrder writeOrder) {
            setWriteByteOrder(writeOrder);
            return this;
        }


        /**
         * 设置输出Socket管道中给服务器的字节序<br>
         * 默认是:大端字节序<br>
         *
         * @param writeOrder {@link ByteOrder} 字节序
         */
        public Builder setWriteByteOrder(ByteOrder writeOrder) {
            mWriteOrder = writeOrder;
            return this;
        }

        /**
         * 设置输入Socket管道中读取时的字节序<br>
         * 默认是:大端字节序<br>
         *
         * @param readByteOrder {@link ByteOrder} 字节序
         */
        public Builder setReadByteOrder(ByteOrder readByteOrder) {
            mReadByteOrder = readByteOrder;
            return this;
        }

        /**
         * 设置Socket超时时间,连接和读取的超时时间
         *
         * @param socketTimeout 注意单位是毫秒
         * @return
         */
        public Builder setSocketTimeout(int socketTimeout) {
            mSocketTimeout = socketTimeout;
            return this;
        }

        /**
         * 是否容许自动重连
         *
         * @param isAllow 是否容许
         */
        public Builder setReconnectAllowed(boolean isAllow) {
            isReconnectAllowed = isAllow;
            return this;
        }

        /**
         * 开启调试模式
         *
         * @param debug 调试模式开关
         */
        public Builder setDebug(boolean debug) {
            isDebug = debug;
            return this;
        }

        public ConnectOption build() {
            return new ConnectOption(this);
        }
    }
}
