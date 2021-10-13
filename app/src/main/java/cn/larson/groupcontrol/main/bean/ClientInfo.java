package cn.larson.groupcontrol.main.bean;

import java.io.Serializable;

import larson.groupcontrol.app.connection.Constants;
import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.util.BytesUtils;

/**
 * ......................-~~~~~~~~~-._       _.-~~~~~~~~~-.
 * ............... _ _.'              ~.   .~              `.__
 * ..............'//     NO           \./      BUG         \\`.
 * ............'//                     |                     \\`.
 * ..........'// .-~"""""""~~~~-._     |     _,-~~~~"""""""~-. \\`.
 * ........'//.-"                 `-.  |  .-'                 "-.\\`.
 * ......'//______.============-..   \ | /   ..-============.______\\`.
 * ....'______________________________\|/______________________________`.
 * ..larsonzhong@163.com      created in 2018/8/29     @author : larsonzhong
 */
public class ClientInfo implements Serializable{
    /**
     * socket id
     */
    private short vid;
    /**
     * clientID 从message里面解析得出
     */
    private int pid;
    /**
     * 远程客户端的端口号
     */
    private int port;
    /**
     * 远程客户端的ip地址
     */
    private String ip;
    /**
     * 连接状态，see{@link Constants#STATE_CLIENT_CONNECTED}
     */
    private byte connectState;

    public short getVid() {
        return vid;
    }

    public int getPid() {
        return pid;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public int getConnectState() {
        return connectState;
    }

    public String getStateString() {
        String stateStr = "unknown";
        switch (connectState) {
            case Constants.STATE_CLIENT_CONNECTING:
                stateStr = "connecting";
                break;
            case Constants.STATE_CLIENT_CONNECTED:
                stateStr = "connected";
                break;
            case Constants.STATE_CLIENT_CONNECT_FAILED:
                stateStr = "disconnected";
                break;
            case Constants.STATE_CLIENT_DISCONNECTING:
                stateStr = "disconnecting";
                break;
            case Constants.STATE_CLIENT_DISCONNECTED:
                stateStr = "remove";
                break;
            case Constants.STATE_CLIENT_DISCONNECT_FAILED:
                stateStr = "disconnect failed";
                break;
            default:
        }
        return stateStr;
    }

    @Override
    public String toString() {
        return "{ip=" + ip
                + ",port=" + port
                + ",pid=" + pid
                + ",vid=" + vid
                + ",connectState=" + connectState
                + "}";
    }

    public static ClientInfo parse(Message msg) {
        ClientInfo info = new ClientInfo();
        info.vid = msg.getVid();
        info.pid = msg.getPid();
        byte[] body = msg.getBody();
        info.connectState = body[0];
        info.ip = BytesUtils.int2Ip(BytesUtils.bytesToInt(body, 1));
        info.port = BytesUtils.bytesToShort(body, 5) & 0xFFFF;
        return info;
    }
}
