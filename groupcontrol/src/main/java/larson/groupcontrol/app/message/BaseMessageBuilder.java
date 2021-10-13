package larson.groupcontrol.app.message;

/**
 * 所有MessageBuilder的父类
 * 通过MessageBuilder 构造具体的message并封装到具体的Message
 * <p>
 * MessageBuilder中的字段为通用字段，所有消息都有的字段（但是心跳消息只有固定头部，因此在转化成byte发出去的时候需要判空）
 * <p>
 *
 * @author larsonzhong
 * @date 2017/12/6
 */
public abstract class BaseMessageBuilder {
    private static final byte[] EMPTY_BODY = new byte[0];

    /**
     * 消息类型
     */
    protected byte type = 0;

    /**
     * 消息流水号，16个字节，当超过16个字节会自动清零
     */
    short sn = 0;


    /**
     * 消息设备唯一标识
     */
    short pid = 0;

    /**
     * 服务端标识客户端的设备id
     */
    protected short vid = 0;

    /**
     * 是否是命令返回消息
     */
    boolean isResponse = false;

    /**
     * 消息id，用于标识不同指令 的消息，14bit
     */
    short messageID = 0;
    /**
     * 确认回复标志
     */
    boolean isAck = false;
    /**
     * 消息体
     */
    protected byte[] body = EMPTY_BODY;

    BaseMessageBuilder(short messageID) throws Exception {
        this.messageID = messageID;
        this.sn = MessageSnBuilder.getInstance().getNextSn();
        this.pid = MessageSnBuilder.getInstance().getClientKey();
        this.isResponse = false;
    }

    BaseMessageBuilder(short messageID, short sn) throws Exception {
        //同步类型的消息id必须是高位置1
        this.messageID = messageID;
        this.sn = sn;
        this.isResponse = true;
        this.pid = MessageSnBuilder.getInstance().getClientKey();
    }

    BaseMessageBuilder() {
    }

    public BaseMessageBuilder setAck(boolean ack) {
        isAck = ack;
        return this;
    }

    public BaseMessageBuilder setBody(byte[] body) {
        this.body = body;
        return this;
    }

    public BaseMessageBuilder setType(byte type) {
        this.type = type;
        return this;
    }


    public BaseMessageBuilder setVid(short vid) {
        this.vid = vid;
        return this;
    }

    /**
     * 所有messageBuilder需要复写这个方法以构造对应的message
     *
     * @return Message
     */
    public abstract Message build();


}

