package larson.groupcontrol.app.message;

import larson.groupcontrol.app.exception.UnFormatMessageException;
import larson.groupcontrol.app.message.intf.IMessage;
import larson.groupcontrol.app.packet.Packet;
import larson.groupcontrol.app.packet.PacketHeader;
import larson.groupcontrol.app.util.ArrayUtils;
import larson.groupcontrol.app.util.CrcUtils;

import java.util.Arrays;
import java.util.List;

/**
 * * Base class for GroupControl messages.
 * <p>
 * Every message has a unique ID. Optionally, the "mIsAck", and "body" fields can be set.
 *
 * @author Larson zhong (larsonzhong@163.com)
 * @since on 2017/12/6.
 */
public class Message implements IMessage {
    public static final byte MSG_TYPE_CONTROL = PacketHeader.TYPE_CTRL;
    public static final byte MSG_TYPE_ACK = PacketHeader.TYPE_ACK;
    public static final byte MSG_TYPE_HEART = PacketHeader.TYPE_HEART;
    public static final byte MSG_TYPE_DATA = PacketHeader.TYPE_DATA;

    private static final int MSG_CRC_TYPE = CrcUtils.CRC16_CCITT;
    private static final int MSG_SHORT_HEADER_SIZE = 4;
    private static final int MSG_LONG_HEADER_SIZE = 8;
    private static final short MSG_SHORT_MAX_SIZE = Packet.MAX_SIZE - MSG_SHORT_HEADER_SIZE;
    private static final short MSG_LONG_MAX_SIZE = Packet.MAX_SIZE - MSG_LONG_HEADER_SIZE;


    private final short msgId;
    private final boolean isLongMsg;
    private final byte[] body;
    private final boolean isAckRequire;
    private final byte type;
    private final boolean isResponse;
    private final short sn;
    /**
     * ClientID
     */
    private final short pid;
    /**
     * SocketID
     */
    private final short vid;

    protected Message(Builder builder) {
        this.msgId = builder.messageID;
        this.isLongMsg = builder.body.length > MSG_SHORT_MAX_SIZE;
        this.body = builder.body;
        this.isAckRequire = builder.isAck;
        this.type = builder.type;
        this.isResponse = builder.isResponse;
        this.sn = builder.sn;
        this.pid = builder.pid;
        this.vid = builder.vid;
    }

    @Override
    public Packet[] getPackets() {
        byte[] payload = CrcUtils.getCrc16Bytes(body, MSG_CRC_TYPE);
        if (payload.length > MSG_SHORT_MAX_SIZE) {
            List<byte[]> payloads = ArrayUtils.divide(payload, MSG_LONG_MAX_SIZE);
            Packet[] packets = new Packet[payloads.size()];
            for (short i = 0; i < payloads.size(); i++) {
                packets[i] = new Packet.Builder(type, pid, vid, (short) (payloads.get(i).length + MSG_LONG_HEADER_SIZE))
                        .setLongMsg(isLongMsg)
                        .setAckMsg(isAckRequire)
                        .setResponseMsg(isResponse)
                        .setSn(sn)
                        .setMsgId(msgId)
                        .setSubCount((short) payloads.size())
                        .setSubIndex((short) (i + 1))
                        .setBody(payloads.get(i))
                        .build();
            }
            return packets;
        }
        Packet packet = new Packet.Builder(type, pid, vid, (short) (payload.length + MSG_SHORT_HEADER_SIZE))
                .setLongMsg(isLongMsg)
                .setAckMsg(isAckRequire)
                .setResponseMsg(isResponse)
                .setMsgId(msgId)
                .setSn(sn)
                .setBody(payload)
                .build();
        return new Packet[]{packet};
    }


    @Override
    public short getMsgId() {
        return msgId;
    }

    @Override
    public boolean isLongMsg() {
        return isLongMsg;
    }

    @Override
    public byte[] getBody() {
        return body;
    }

    @Override
    public boolean isAckRequire() {
        return isAckRequire;
    }

    @Override
    public boolean isResponse() {
        return isResponse;
    }

    @Override
    public short getType() {
        return type;
    }

    @Override
    public int getPid() {
        return pid & 0XFFFF;
    }

    @Override
    public short getVid() {
        return vid;
    }

    @Override
    public String toString() {
        return "Message{" +
                "msgId=" + getMsgId() +
                ", isLongMsg=" + isLongMsg() +
                ", body=" + Arrays.toString(body) +
                ", isAckRequire=" + isAckRequire() +
                ", type=" + getType() +
                ", isResponse=" + isResponse() +
                ", sn=" + sn +
                ", pid=" + getPid() +
                ", vid=" + getVid() +
                '}';
    }

    public static class Builder extends BaseMessageBuilder {
        /**
         * 用于发送时的消息构造
         *
         * @param msgId 每个消息都有独特的id
         */
        public Builder(short msgId) throws Exception {
            super(msgId);
        }

        /**
         * 用于回复同步类的消息构造,
         */
        public Builder(short msgId, short sn) throws Exception {
            super(msgId, sn);
        }

        /**
         * 用于解析收到的消息的构造
         * 构造Builder的时候传入该message对应的packet
         *
         * @param packets 收到的包，一个或多个
         */
        public Builder(Packet[] packets) {
            super();
            Packet packet = packets[0];
            if (packets.length == 1) {
                if (packet.isLargeMsg()) {
                    throw new UnFormatMessageException("Only one packet, but packet is large msg!");
                }
                parsePacket(packet);
            } else {
                byte[] bytes = new byte[packets.length * MSG_LONG_MAX_SIZE];
                int msgSize = 0;
                if (packets.length != packet.getSubCount()) {
                    throw new UnFormatMessageException("Not full packets!");
                }

                for (Packet pkt : packets) {
                    int start = (pkt.getSubIndex() - 1) * MSG_LONG_MAX_SIZE;
                    int size = pkt.getBody().length;
                    //获取第一个包
                    if (pkt.getSubIndex() == 1) {
                        parsePacket(packet);
                    }
                    System.arraycopy(pkt.getBody(), 0, bytes, start, size);
                    msgSize += size;
                }

                byte[] data = new byte[msgSize];
                System.arraycopy(bytes, 0, data, 0, msgSize);
                byte[] dataBytes = CrcUtils.getDataBytes(data, MSG_CRC_TYPE);
                if (dataBytes == null) {
                    throw new UnFormatMessageException("data bytes crc16 check failure!");
                }
                this.body = dataBytes;
            }
        }

        /**
         * 组包时用于获取消息头和进行消息crc16校验
         *
         * @param packet 数据包
         */
        private void parsePacket(Packet packet) {
            this.messageID = packet.getMsgId();
            this.isAck = packet.isAckMsg();
            this.isResponse = packet.isResponseMsg();
            this.type = packet.getPacketHeader().getType();
            this.pid = packet.getPacketHeader().getPid();
            this.vid = packet.getPacketHeader().getVid();
            this.sn = packet.getSn();
            if (!packet.isLargeMsg()) {
                byte[] dataBytes = CrcUtils.getDataBytes(packet.getBody(), MSG_CRC_TYPE);
                if (dataBytes == null) {
                    throw new UnFormatMessageException("data bytes crc16 check failure!");
                } else {
                    this.body = dataBytes;
                }
            }
        }

        @Override
        public Message build() {
            return new Message(this);
        }
    }
}
