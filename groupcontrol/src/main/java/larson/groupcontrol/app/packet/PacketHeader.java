package larson.groupcontrol.app.packet;

import larson.groupcontrol.app.util.BytesUtils;

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
 * ..larsonzhong@163.com      created in 2018/8/15     @author : larsonzhong
 * 一个完整包的头部，任何一个包都包含一个头部
 */
public class PacketHeader {
    public static final int SIZE = 8;
    public static final short MAGIC_MASK = 0x0FFF;
    public static final short DEFAULT_MAGIC = 0x534;
    public static final byte TYPE_MASK = 0x0F;
    public static final byte TYPE_CTRL = 0;
    public static final byte TYPE_ACK = 1;
    public static final byte TYPE_HEART = 2;
    public static final byte TYPE_DATA = 3;

    private byte type;
    private short magic;
    private short pid;
    private short vid;
    private short paddingSize;

    public PacketHeader() {
        this.type = TYPE_ACK;
        this.magic = 0;
        this.pid = 0;
        this.vid = 0;
        this.paddingSize = 0;
    }

    public PacketHeader(Builder builder) {
        this.type = builder.type;
        this.magic = builder.magic;
        this.pid = builder.pid;
        this.vid = builder.vid;
        this.paddingSize = builder.paddingSize;
    }

    public PacketHeader(byte type, short pid, short vid, short paddingSize) {
        this.type = type;
        this.magic = DEFAULT_MAGIC;
        this.pid = pid;
        this.vid = vid;
        this.paddingSize = paddingSize;
    }

    public short getMagic() {
        return magic;
    }

    public short getPid() {
        return pid;
    }

    public short getVid() {
        return vid;
    }

    public byte getType() {
        return type;
    }

    public short getPaddingSize() {
        return paddingSize;
    }

    public byte[] getBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE);
        buffer.put(BytesUtils.magicTypeToBytes(magic,type));
        buffer.put(BytesUtils.shortToBytes(pid));
        buffer.put(BytesUtils.shortToBytes(vid));
        buffer.put(BytesUtils.shortToBytes(paddingSize));
        buffer.flip();
        return buffer.array();
    }

    @Override
    public String toString() {
        return "PacketHeader{" +
                "type=" + type +
                ", magic=" + magic +
                ", pid=" + pid +
                ", vid=" + vid +
                ", paddingSize=" + paddingSize +
                '}';
    }

    public static PacketHeader parse(byte[] data, int index) {
        PacketHeader header = new PacketHeader();
        header.type = BytesUtils.bytesToType(data, index);
        header.magic = BytesUtils.bytesToMagic(data, index);
        header.pid = BytesUtils.bytesToShort(data, index + 2);
        header.vid = BytesUtils.bytesToShort(data, index + 4);
        header.paddingSize = BytesUtils.bytesToShort(data, index + 6);
        return header;
    }

    public static class Builder {
        private byte type;
        private short magic;
        private short pid;
        private short vid;
        private short paddingSize;

        public Builder() {
            this.type = TYPE_ACK;
            this.magic = DEFAULT_MAGIC;
            this.pid = 0;
            this.vid = 0;
            this.paddingSize = 0;
        }

        public Builder setType(byte type) {
            this.type = type;
            return this;
        }

        public Builder setMagic(short magic) {
            this.magic = magic;
            return this;
        }

        public Builder setPid(short pid) {
            this.pid = pid;
            return this;
        }

        public Builder setVid(short vid) {
            this.vid = vid;
            return this;
        }

        public Builder setPaddingSize(short paddingSize) {
            this.paddingSize = paddingSize;
            return this;
        }

        public PacketHeader build() {
            return new PacketHeader(this);
        }
    }

}
