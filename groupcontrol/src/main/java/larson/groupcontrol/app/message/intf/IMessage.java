package larson.groupcontrol.app.message.intf;

import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.packet.Packet;

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
 */
public interface IMessage {
    /**
     * 获取Message生成的所有Packet
     *
     * @return Message生成的包
     */
    Packet[] getPackets();

    /**
     * 每一条消息都有一个独立的messageID
     *
     * @return 获取消息id
     */
    short getMsgId();

    /**
     * When the message is too long, it will be divided into multiple packets to be sent.
     * When unpacking, it needs to be identified through this field.
     * 当消息过长，会被分隔成多个packet发送,在解包的时候需要通过该字段判别
     *
     * @return 是否是长消息
     */
    boolean isLongMsg();

    /**
     * Get the message body
     *
     * @return 该message下的所有packet组成的消息体
     */
    byte[] getBody();

    /**
     * Whether the receiver needs feedback Ack package
     *
     * @return 是否需要接收方反馈Ack包
     */
    boolean isAckRequire();

    /**
     * 当这条消息是为了回复某一条消息的时候发送消息isResponse = true
     *
     * @return 是否为response消息
     */
    boolean isResponse();

    /**
     * 参考{@link Message#MSG_TYPE_DATA,Message#MSG_TYPE_HEART
     * ,Message#MSG_TYPE_ACK,Message#MSG_TYPE_CONTROL}
     *
     * @return 消息类型
     */
    short getType();

    /**
     * 连接上Socket之后，本地Server会分配一个SocketID
     *
     * @return SocketID
     */
    int getPid();

    /**
     * 客户端的唯一标识
     *
     * @return clientID
     */
    short getVid();
}
