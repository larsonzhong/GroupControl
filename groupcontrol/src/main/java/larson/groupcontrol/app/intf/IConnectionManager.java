package larson.groupcontrol.app.intf;

import larson.groupcontrol.app.SocketConfiguration;
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
 * ..larsonzhong@163.com      created in 2018/8/20     @author : larsonzhong
 */
public interface IConnectionManager {

    /**
     * Open the connection, different from reconnect, the reconnect method is only used for the first login,
     * and the network is reconnected using reconnect.
     * 开启连接 ，区别于reconnect，reconnect方法只在首次登陆使用，断网重连使用reconnect
     * @param isReconnect 是否重连
     */
    void connect(boolean isReconnect);

    /**
     * 发送登录消息
     *
     * @param isReconnecting 是否正在重连
     */
    void sendLoginMessage(boolean isReconnecting);

    /**
     * Return localSocket connection status
     * 返回localSocket连接状态
     *
     * @return true 已连接,false 未连接
     */
    boolean isConnected();

    /**
     * disconnect the socket
     * 主动断开socket连接
     */
    void disConnect();

    /**
     * Get the connection configuration
     * 获得连接配置
     *
     * @return 连接信息
     */
    SocketConfiguration getConnectionConfiguration();

    /**
     * Destroy the connection and release all relevant resources
     * 摧毁连接，释放所有相关资源
     */
    void onDestroy();

    /**
     * Add listener
     * 添加监听器
     *
     * @param listener 连接监听器
     */
    void registerConnectListener(IStateChangeListener listener);

    /**
     * Remove listener
     * 移除监听器
     *
     * @param listener 连接监听器
     */
    void unRegisterConnectListener(IStateChangeListener listener);

    /**
     * Reach the maximum number of reconnections
     * 达到最大重连次数
     *
     * @param e 抛出异常
     */
    void onMaxReconnectTimeReached(Exception e);

    /**
     * Connection failure callback method
     * 连接失败回调该方法
     *
     * @param e 异常说明
     */
    void onConnectFailed(Exception e);

    /**
     * Connection successful callback method
     * 连接成功回调该方法
     */
    void onConnectSuccess();

    /**
     * * Add a listener, add a custom Listener after successful login
     * 添加监听器,登录成功才添加自定义Listener
     *
     * @param listener 消息监听器
     * @param filter   对应消息的过滤器
     */
    void addMsgListener(IMessageListener listener, MessageFilter filter);

    /**
     * Remove the listener and add a custom Listener if the login is successful.
     * 移除监听器,登录成功才添加自定义Listener
     *
     * @param listener 消息监听器
     */
    void removeMsgListener(IMessageListener listener);

    /**
     * 发送消息
     *
     * @param msgDataBean 发送的消息
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
     * 发送同步类消息，该类型消息需要等到符合指定选择器的消息到达后才表示完成
     *
     * @param msgDataBean 发送的消息
     * @param timeout     等待超时时间
     * @param filter      消息选择器，只有符合该选择器的消息才会被返回
     * @return 需要的返回消息
     */
    Message sendSyncMessage(Message msgDataBean, MessageFilter filter, long timeout);


}
