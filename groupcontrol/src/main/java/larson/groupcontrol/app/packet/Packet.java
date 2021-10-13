package larson.groupcontrol.app.packet;

import larson.groupcontrol.app.util.BytesUtils;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Base class for JT/T808 Packets.
 * <p>
 * long=2int=42short=2byte=64BIT
 * <p>
 * Every Packet has a unique ID. Optionally, the "cipher", "phone", and "body" fields can be set.
 * <p>
 * ---------------------------------------------------------------------------------
 * | packetType  |     magic       |  pid    |  vid    |  length
 * ---------------------------------------------------------------------------------
 * | 4bit        |    12bit        | 16bit   |  16bit  |  16bit
 * ---------------------------------------------------------------------------------
 * <p>
 * ---------------------------------------------------------------------------------
 * | isLarge  |    ack   | isResponse   |  msgID    |  sn    |  subCount |  subIndex                  消息头
 * ---------------------------------------------------------------------------------
 * | 1bit     |    1bit  |    1bit      |  13bit    |  16bit |   16bit   |   16bit
 * ---------------------------------------------------------------------------------
 * <p>
 * ---------------------------------------------------------------------------------
 * | payload      |     crc       |
 * ---------------------------------------------------------------------------------
 * | byte[]       |    16bit      |
 * ---------------------------------------------------------------------------------
 * <p>
 * Ⅰ :fixed head
 * ( fixed head(1word) + PacketID(1word) )
 * 一、固定头部
 * 1、byte1: 1 0 1 0   0 1 0 ACK   A4 A5
 * 2、byte2: 消息ID, bit7终端默认0，服务默认为1
 * Ⅱ:changeable head
 * ( Packet Body Prosperity(2 word) +Packet serial number(2 word)+Packet Wrap Option(2 word) )
 * 二、可变头部
 * 1、消息体属性 2字节
 * -> bit15: SP 为1时，表示长消息，有消息封装项; bit14-12:加密方式，都为0不加密，bit12为1 消息体RSA加密，bit13为1消息体异或0x55加密; bit11-0:消息体长度
 * Ⅲ: 消息体，见各消息说明
 * Ⅳ: 检验码
 * 从消息头开始异或，并把异或结果同后一个字节异或，直到检验码前一个字节，占一个字节
 *
 * @author Larson Zhong (larsonzhong@163.com)
 * <p>
 * Created by dell on 2017/12/5.
 */
public class Packet {
    private static final short MAX_SOCKET_SIZE = 4096;
    public static final short MAX_SIZE = MAX_SOCKET_SIZE - PacketHeader.SIZE;
    private static final short LARGE_MASK = (short) 0x8000;
    private static final short ACK_MASK = (short) 0x4000;
    private static final short RESPONSE_MASK = (short) 0x2000;
    private static final short MSG_ID_MASK = (short) 0x1FFF;

    private final PacketHeader packetHeader;
    private final boolean isLargeMsg;
    private final boolean isAckMsg;
    private final boolean isResponseMsg;
    private final short msgId;
    private final short sn;
    private final short subCount;
    private final short subIndex;
    private final byte[] body;

    private Packet(Builder builder) {
        this.packetHeader = builder.packetHeader;
        this.isLargeMsg = builder.isLongMsg;
        this.isAckMsg = builder.isAckMsg;
        this.isResponseMsg = builder.isResponseMsg;
        this.msgId = builder.msgId;
        this.sn = builder.sn;
        this.subCount = builder.subCount;
        this.subIndex = builder.subIndex;
        this.body = builder.body;
    }

    public Packet(byte[] raw) {
        int index = 0;
        packetHeader = PacketHeader.parse(raw, index);
        index += PacketHeader.SIZE;
        short id = BytesUtils.bytesToShort(raw, index);
        isLargeMsg = (id & LARGE_MASK) != 0;
        isAckMsg = (id & ACK_MASK) != 0;
        isResponseMsg = (id & RESPONSE_MASK) != 0;
        msgId = (short) (id & MSG_ID_MASK);
        index += 2;
        sn = BytesUtils.bytesToShort(raw, index);
        index += 2;
        if (isLargeMsg) {
            subCount = BytesUtils.bytesToShort(raw, index);
            index += 2;
            subIndex = BytesUtils.bytesToShort(raw, index);
            index += 2;
        } else {
            subCount = 0;
            subIndex = 0;
        }
        int size = raw.length - index;
        body = new byte[size];
        System.arraycopy(raw, index, body, 0, size);
    }

    public byte[] getBytes() {
        byte[] bytes = new byte[packetHeader.getPaddingSize() + PacketHeader.SIZE];
        ByteBuffer byteBuffer = ByteBuffer.allocate(MAX_SOCKET_SIZE);
        byteBuffer.put(packetHeader.getBytes());
        byteBuffer.put(BytesUtils.shortToBytes(getFullMsgId()));
        byteBuffer.put(BytesUtils.shortToBytes(sn));
        if (isLargeMsg) {
            byteBuffer.put(BytesUtils.shortToBytes(subCount));
            byteBuffer.put(BytesUtils.shortToBytes(subIndex));
        }
        byteBuffer.put(body);
        byteBuffer.flip();
        byteBuffer.get(bytes);
        return bytes;
    }

    private short getFullMsgId() {
        short id = 0;
        if (isLargeMsg) {
            id |= LARGE_MASK;
        }
        if (isAckMsg) {
            id |= 0x4000;
        }
        if (isResponseMsg) {
            id |= 0x2000;
        }
        id |= msgId & 0x1fff;
        return id;
    }

    @Override
    public String toString() {
        return "Packet{" +
                "packetHeader=" + packetHeader +
                ", isLargeMsg=" + isLargeMsg +
                ", isAckMsg=" + isAckMsg +
                ", isResponseMsg=" + isResponseMsg +
                ", msgId=" + msgId +
                ", sn=" + sn +
                ", subCount=" + subCount +
                ", subIndex=" + subIndex +
                ", body=" + Arrays.toString(body) +
                '}';
    }

    public PacketHeader getPacketHeader() {
        return packetHeader;
    }

    public boolean isLargeMsg() {
        return isLargeMsg;
    }

    public boolean isAckMsg() {
        return isAckMsg;
    }

    public boolean isResponseMsg() {
        return isResponseMsg;
    }

    public short getMsgId() {
        return msgId;
    }

    public short getSn() {
        return sn;
    }

    public short getSubCount() {
        return subCount;
    }

    public short getSubIndex() {
        return subIndex;
    }

    public byte[] getBody() {
        return body;
    }

    public static class Builder {
        private final PacketHeader packetHeader;
        private boolean isLongMsg;
        private boolean isAckMsg;
        private boolean isResponseMsg;
        private short msgId;
        private short sn;
        private short subCount;
        private short subIndex;
        private byte[] body;

        public Builder(byte type, short pid, short vid, short paddingSize) {
            this.packetHeader = new PacketHeader.Builder()
                    .setType(type)
                    .setPid(pid)
                    .setVid(vid)
                    .setPaddingSize(paddingSize)
                    .build();
            isLongMsg = false;
            isAckMsg = false;
            isResponseMsg = false;
            subCount = 0;
            subIndex = 0;
            body = new byte[0];
        }

        public Builder setLongMsg(boolean longMsg) {
            isLongMsg = longMsg;
            return this;
        }

        public Builder setAckMsg(boolean ackMsg) {
            isAckMsg = ackMsg;
            return this;
        }

        public Builder setResponseMsg(boolean responseMsg) {
            isResponseMsg = responseMsg;
            return this;
        }

        public Builder setMsgId(short msgId) {
            this.msgId = msgId;
            return this;
        }

        public Builder setSn(short sn) {
            this.sn = sn;
            return this;
        }

        public Builder setSubCount(short subCount) {
            this.subCount = subCount;
            return this;
        }

        public Builder setSubIndex(short subIndex) {
            this.subIndex = subIndex;
            return this;
        }

        public Builder setBody(byte[] body) {
            this.body = body;
            return this;
        }

        public Packet build() {
            return new Packet(this);
        }
    }
}
