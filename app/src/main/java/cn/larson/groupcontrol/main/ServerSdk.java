package cn.larson.groupcontrol.main;

import java.util.ArrayList;
import java.util.List;

import larson.groupcontrol.app.connection.Constants;
import larson.groupcontrol.app.GroupControlManager;
import larson.groupcontrol.app.SocketConfiguration;
import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.filter.MessageIdFilter;
import larson.groupcontrol.app.intf.IStateChangeListener;
import larson.groupcontrol.app.listener.IMessageListener;
import cn.larson.groupcontrol.main.bean.ClientInfo;
import cn.larson.groupcontrol.main.msgbody.MiniTouchMsgBody;
import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.util.BytesUtils;
import larson.groupcontrol.app.util.LogUtils;

/**
 * ......................-~~~~~~~~~-._       _.-~~~~~~~~~-.
 * ............... _ _.'              ~.   .~              `.__
 * ..............'//     NO           \./      BUG         \\`.
 * ............'//                     |                     \\`.
 * ..........'// .-~"""""""~~~~-._     |     _,-~~~~"""""""~-. \\`.
 * ........'//.-"                 `-.  |  .-'                 "-.\\`.
 * ......'//______.============-..   \ | /   ..-============.______\\`.
 * ....'______________________________\|/______________________________`.
 * ..larsonzhong@163.com      created in 2018/8/28     @author : larsonzhong
 * <p>
 * 客户机状态维护，其他状态上报,提供接口
 */
public class ServerSdk implements IStateChangeListener {

    private static ServerSdk instance;
    private GroupControlManager skSocket;
    private boolean isInit = false;

    private ServerSdk() {
    }

    public void setup(SocketConfiguration configuration) {
        skSocket = new GroupControlManager(configuration, this);
        isInit = true;
    }


    public static ServerSdk getInstance() {
        if (instance == null) {
            synchronized (ServerSdk.class) {
                if (instance == null) {
                    instance = new ServerSdk();
                }
            }
        }
        return instance;
    }

    public boolean isInit() {
        return isInit;
    }

    /**
     * @param stateCode 状态码，参照上面
     * @param e         当异常断开会抛出异常
     */
    @Override
    public void onConnectStateChange(int stateCode, Exception e) {

        if (stateCode == STATE_CONNECT_SUCCESSFUL) {
            //连接状态的改变开发者不需要知道，只需要知道登录状态改变就行了
            addMessageListener(clientUpdateListener, new MessageIdFilter(Constants.MSG_CLIENT_UPDATE));
        }

        if (connListeners != null) {
            for (IStateChangeListener listener : connListeners) {
                listener.onConnectStateChange(stateCode, e);
            }
        }
    }


    public void addMessageListener(IMessageListener listener, MessageFilter messageFilter) {
        skSocket.addMessageListener(listener, messageFilter);
    }

    /**
     * Make sure the connection status is correct, if not,
     * it will not connect and disconnect normally.
     *
     * @return isConnectedToLocalSocket
     */
    public boolean isConnected() {
        return skSocket != null && skSocket.isConnected();
    }

    public void connect(boolean isReconnect) {
        skSocket.connect(isReconnect);
    }

    public void disConnect() {
        skSocket.disConnect();
    }

    public void sendMessage(Message message) {
        skSocket.sendMessage(message);
    }

    public Message sendSyncMessage(final Message msg, MessageFilter filter, long timeout) {
        return skSocket.sendSyncMessage(msg, filter, timeout);
    }

    public Message sendSyncMessage(final Message msg, long timeout) {
        return skSocket.sendSyncMessage(msg, timeout);
    }


    /**
     * The connection state change listener provided to the developer, because the developer
     * does not need to care about reconnection and login related information, so a layer of encapsulation
     * 给开发者提供的连接状态改变监听，因为开发者不需要关心重连
     * 以及登录相关的信息，所以做了一层封装
     */
    private List<IStateChangeListener> connListeners = new ArrayList<>();
    /**
     * 客户端状态改变监听
     */
    private List<IClientStateCallback> clientStateCallbacks = new ArrayList<>();

    public void addConnectListener(IStateChangeListener listener) {
        //当登录状态改变则会回调该方法
        connListeners.add(listener);
    }

    public void removeConnectListener(IStateChangeListener listener) {
        connListeners.remove(listener);
    }

    public void addClientStateCallback(IClientStateCallback listener) {
        //当登录状态改变则会回调该方法
        clientStateCallbacks.add(listener);
    }

    public void removeClientStateCallback(IClientStateCallback listener) {
        clientStateCallbacks.remove(listener);
    }


    public void onDestroy() {
        if (connListeners != null) {
            //這裡不需要制空
            connListeners.clear();
        }

        if (skSocket != null) {
            skSocket.onDestroy();
            //skSocket = null;
        }

       /* mDeviceWidth = -1;
        mDeviceHeight = -1;*/

        instance = null;
        isInit = false;
    }

    /**
     * 客户端列表改变监听
     */
    private IMessageListener clientUpdateListener = new IMessageListener() {
        @Override
        public void processMessage(Message msg) {
            if (clientStateCallbacks != null) {
                ClientInfo info = ClientInfo.parse(msg);
                LogUtils.d(String.format("%s:%s", info.getIp(), info.toString()));
                for (IClientStateCallback callback : clientStateCallbacks) {
                    callback.onClientStateChange(info);
                }
            }
        }
    };

    /**
     * 开启或者关闭远程控制
     *
     * @param isOpen 开关
     * @return 打开或者关闭结果
     */
    public boolean toggleScreenControl(boolean isOpen, short vid) {
        try {
            byte reqCode = isOpen ? Constants.CODE_REQUEST_MINICAP_OPEN : Constants.CODE_REQUEST_MINICAP_CLOSE;
            byte resCode = isOpen ? Constants.STATE_MINICAP_STARTED : Constants.STATE_MINICAP_STOPED;

            final Message message = new Message.Builder(Constants.MSG_MINICAP_CONTROL)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .setBody(BytesUtils.asBytes(reqCode))
                    .setVid(vid)
                    .build();
            Message msg = skSocket.sendSyncMessage(message, new MessageFilter() {
                @Override
                public boolean accept(Message msg) {
                    return msg.isResponse() && msg.getMsgId() == message.getMsgId();
                }
            }, 5000);

            if (msg != null) {
                byte[] data = msg.getBody();
                byte responseCode = data[0];
                LogUtils.e(isOpen ? "open" : "close" + " control  reply :" + responseCode);
                //通知开发者结果
                return resCode == responseCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 回复控制机打开/关闭 miniCap
     *
     * @param stateMiniCapStarted 執行狀態
     * @param vid                 客户端标识
     */
    public void sendMiniCapControlReply(byte stateMiniCapStarted, short vid, short sn) {
        try {
            final Message message = new Message.Builder(Constants.MSG_MINICAP_CONTROL, sn)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .setBody(BytesUtils.asBytes(stateMiniCapStarted))
                    .setVid(vid)
                    .build();
            sendMessage(message);
        } catch (Exception e) {
            LogUtils.e("回复miniCap控制失败 :" + stateMiniCapStarted);
            e.printStackTrace();
        }
    }

    /**
     * 发送minitouch指令
     *
     * @param messageBody body
     */
    public void sendMiniTouchEvent(MiniTouchMsgBody messageBody, short vid) {
        try {
            final Message message = new Message.Builder(MiniTouchMsgBody.ID)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .setVid(vid)
                    .setBody(messageBody.getBodyBytes())
                    .build();
            LogUtils.e("[content]:" + messageBody.toString());
            sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送查找手机指令
     *
     * @param time 查找时长
     */
    public boolean findPhone(byte time, short vid) {
        try {
            final Message message = new Message.Builder(Constants.MSG_REQUEST_FIND_PHONE)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .setBody(BytesUtils.asBytes(time))
                    .setVid(vid)
                    .build();
            skSocket.sendMessage(message);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 请求客户端列表
     */
    public void fetchClientInfos() {
        try {
            final Message message = new Message.Builder(Constants.MSG_REQUEST_CLIENT_LIST)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .build();
            skSocket.sendMessage(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
