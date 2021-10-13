package larson.groupcontrol.app.intf;

import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.listener.IMessageListener;
import larson.groupcontrol.app.message.Message;

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
 */
public interface ISocket {

    /**
     * Connect to the local socket, you need to configure the connection options before this
     * 连接到本地socket，在此之前需要先配置好连接选项
     */
    void connect(boolean isReconnect);

    /**
     * Disconnected, the client is called when it is manually disconnected,
     * and there is no need to reconnect at this time.
     * 断开连接，客户端手动断开的时候调用,此时不需要重新连接
     */
    void disConnect();

    /**
     * Need to free resources when you don't need to use a socket connection or process destruction
     * 当不需要使用socket连接或者进程销毁的时候需要释放资源
     */
    void onDestroy();

    /**
     * Connection status, whether the current socket is connected
     * 连接状态，当前socket是否连接
     *
     * @return isSocket Connected 当前socket是否连接
     */
    boolean isConnected();

    /**
     * send wrapped message
     * 发送消息
     *
     * @param msgDataBean 需要发送的消息，一般情况下是{@link Message}的子类
     */
    void sendMessage(Message msgDataBean);

    /**
     * 发送同步类消息，返回的消息是messageID的高位置1
     *
     * @param msgDataBean 发送的消息
     * @param timeout     等待超时时间
     * @return 需要的返回消息
     */
    Message sendSyncMessage(Message msgDataBean, long timeout);

    /**
     * Send a wait message, that is to say, the message sent needs to wait for the server to give back.
     * If the server does not respond, it indicates that the message is sent out.
     * It should be noted that this time blocking the current process (call process)
     * 发送等待消息，也就是说发送的这个消息需要等待服务器回馈，如果服务器没有回应，表示消息发送超时
     * 需要注意的是，这回阻塞当前进程（调用进程）
     *
     * @param msgDataBean 需要发送的消息
     * @param timeout     超时接收时间
     * @param filter      符合接收的消息规则
     * @return 服务器返回的消息
     */
    Message sendSyncMessage(Message msgDataBean, MessageFilter filter, long timeout);

    /**
     * Adds a message listener of the specified type, and when it receives a
     * message of the specified type, it will call back the interface.
     * 添加指定类型的消息监听，当收到指定类型的消息时，会回调该接口
     *
     * @param listener 添加的监听器
     * @param filter   过滤器
     */
    void addMessageListener(IMessageListener listener, MessageFilter filter);

    /**
     * 移除一个MessageListener
     *
     * @param listener 要移除的listener
     */
    void removeMessageListener(IMessageListener listener);
}
