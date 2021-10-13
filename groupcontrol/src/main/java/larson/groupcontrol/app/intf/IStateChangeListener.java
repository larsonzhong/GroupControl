package larson.groupcontrol.app.intf;

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
public interface IStateChangeListener {
    /**
     * 连接成功
     */
    int STATE_CONNECT_SUCCESSFUL = 1001;
    /**
     * 连接服务器或者初始化流失败
     */
    int STATE_CONNECT_FAILED = 1002;
    /**
     * 主动断开
     */
    int STATE_SOCKET_CLOSE_SUCCESSFUL = 1003;
    /**
     * 被动断开,异常断开
     */
    int STATE_SOCKET_CLOSE_FAILED = 1004;
    /**
     * 达到最大重连次数
     */
    int STATE_REACH_MAX_RECONNECT_TIME = 1005;

    /**
     * 远程服务器正在连接
     */
    int STATE_REMOTE_SOCKET_CONNECTING = 1008;

    /**
     * 连接超时
     */
    int STATE_CONNECT_TIMEOUT = 1009;

    /**
     * 连接成功但是登录超时
     */
    int STATE_LOGIN_TIMEOUT = 1010;
    /**
     * 连接成功但是被拒绝了
     */
    int STATE_LOGIN_REFUSED = 1011;

    /**
     * 远程服务器与本地的连接断开了或者不可用了
     */
    int STATE_REMOTE_SOCKET_CLOSED = 1012;

    /**
     * 远程服务器连接失败
     */
    int STATE_CONNECT_REMOTE_FAILED = 1013;

    /**
     * 正在连接到远程服务器
     */
    int STATE_CONNECTING_REMOTE = 1014;

    /**
     * 正在断开到远程服务器的连接
     */
    int STATE_DISCONNECTING_REMOTE = 1015;
    /**
     * 断开到远程服务器的连接失败
     */
    int STATE_DISCONNECTING_REMOTE_FAILED = 1016;

    /**
     * 重连远程服务器失败
     */
    int STATE_RECONNECT_REMOTE_FAILED = 1017;
    /**
     * 当连接成功后异常断开，然后本地客户端又连接上了远程服务器,回调该接口
     */
    int STATE_RECONNECT_REMOTE_SUCCESS = 1018;

    /**
     * Callback method when the connection state changes
     * 当连接状态改变的时候回回调该方法
     *
     * @param stateCode 状态码，参照上面
     * @param e         异常断开会抛出异常
     */
    void onConnectStateChange(int stateCode, Exception e);

}
