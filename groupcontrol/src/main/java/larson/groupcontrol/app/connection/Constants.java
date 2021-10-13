package larson.groupcontrol.app.connection;

/**
 * ......................-~~~~~~~~~-._       _.-~~~~~~~~~-.
 * ............... _ _.'              ~.   .~              `.__
 * ..............'//     NO           \./      BUG         \\`.
 * ............'//                     |                     \\`.
 * ..........'// .-~"""""""~~~~-._     |     _,-~~~~"""""""~-. \\`.
 * ........'//.-"                 `-.  |  .-'                 "-.\\`.
 * ......'//______.============-..   \ | /   ..-============.______\\`.
 * ....'______________________________\|/______________________________`.
 * ..larsonzhong@163.com      created in 2018/8/30     @author : larsonzhong
 */
public class Constants {
    /**
     * 在登录前需要知道native层的server是否连接上，没有连接上需要等待该连接消息
     */
    static short MSG_SERVER_STATE_CHANGED = 0x0004;
    //////////////////////////////State Change////////////////////////////////
    /**
     * 连接状态改变
     */
    static short MSG_STATE_CHANGE = 0x0001;
    /**
     * 正在连接到远程服务器
     */
    static byte STATE_CONNECTING = 0x01;
    /**
     * 连接远程服务器成功
     */
    static byte STATE_CONNECTED = 0x02;
    /**
     * 连接服务器或者初始化流失败
     */
    static byte STATE_CONNECT_FAILED = 0x03;
    /**
     * 正在断开连接
     */
    static byte STATE_DISCONNECTING = 0x04;
    /**
     * 连接断开，可能是主动断开，可能是被动断开
     */
    static byte STATE_DISCONNECTED = 0x05;
    /**
     * 断开与远程服务器的连接失败
     */
    static byte STATE_DISCONNECT_FAILED = 0x06;

    ///////////////////////client change/////////////////////////
    /**
     * 客户端列表更新
     */
    public static final short MSG_CLIENT_UPDATE = 0x0002;

    public static final byte STATE_CLIENT_CONNECTING = 0x07;

    /**
     * 客户端连接成功
     */
    public static final byte STATE_CLIENT_CONNECTED = 0x08;
    /**
     * 连接远程服务器或者初始化远程流失败
     */
    public static final byte STATE_CLIENT_CONNECT_FAILED = 0x09;
    /**
     * 测试机APP退出
     */
    public static final byte STATE_CLIENT_CRASHED = 0x10;
    /**
     * 测试机正在断开？
     */
    public static final byte STATE_CLIENT_DISCONNECTING = 0x0A;
    /**
     * 远程客户端连接断开
     */
    public static final byte STATE_CLIENT_DISCONNECTED = 0x0B;
    /**
     * 测试机断开失败？？？
     */
    public static final byte STATE_CLIENT_DISCONNECT_FAILED = 0x0C;


    //////////////////////////////Login / Logout////////////////////////////////
    /**
     * 登入/登出 id
     */
    public static final short MSG_LOGIN_CONTROL = 0x0080;
    public static final byte CODE_LOGIN_REQUEST = 0x01;
    public static final byte CODE_LOGOUT_REQUEST = 0x02;
    public static final byte CODE_LOGIN_RESPONSE = STATE_CONNECTED;
    public static final byte CODE_LOGOUT_RESPONSE = STATE_DISCONNECTED;

    //////////////////////////////minicap&minitouch////////////////////////////////
    /**
     * miniCap相关
     */
    public static final short MSG_MINICAP_CONTROL = 0x0081;
    public static final byte CODE_REQUEST_MINICAP_OPEN = 0x01;
    public static final byte CODE_REQUEST_MINICAP_CLOSE = 0x02;
    public static final byte STATE_MINICAP_STARTED = 0x0D;
    public static final byte STATE_MINICAP_STOPED = 0x0E;
    public static final byte STATE_MINICAP_FAILED = 0x0F;


    /**
     * input 指令
     * tap:0x01 x:int y:int
     * swipe:0x02 x:int y:int x1:int y1:int ms:int
     * key event:0x03 keycode:int
     */
    public static final short MSG_MINICAP_INPUT = 0x0003;
    public static final byte CODE_REQUEST_TAP = 0x01;
    public static final byte CODE_REQUEST_SWIPE = 0x02;
    public static final byte CODE_REQUEST_KEYEVENT = 0x03;

    /**
     * 查找手机指令
     */
    public static final short MSG_REQUEST_FIND_PHONE = 0x0082;
    /**
     * 时间（s）最低5秒
     */
    public static final byte CODE_REQUEST_FIND_INTERVAL = 0x05;

    /**
     * 请求客户端列表
     */
    public static final short MSG_REQUEST_CLIENT_LIST = 0x0005;

    /**
     * 后台客户端连接远程服务器
     */
    public static final short MSG_RECONNECT_REMOTE_SERVER = 0x0006;
}
