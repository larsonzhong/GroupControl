package cn.larson.groupcontrol.main;

import larson.groupcontrol.app.connection.Constants;
import cn.larson.groupcontrol.main.bean.ClientInfo;

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
public interface IClientStateCallback {
    /**
     * The client state changes will call back this interface, the connection state reference:
     * 客户端状态改变就会回调这个接口,连接状态参考:
     * Client connection客户端连接 {@link Constants#STATE_CLIENT_CONNECTED}
     * Client exit客户端退出 {@link Constants#STATE_CLIENT_DISCONNECTED}
     * Client abnormal disconnection客户端异常断开 {@link Constants#STATE_CLIENT_DISCONNECT_FAILED}
     *
     * @param info 客户端信息
     */
    void onClientStateChange(ClientInfo info);
}