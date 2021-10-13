package larson.groupcontrol.app;

import larson.groupcontrol.app.connection.ConnectionManager;
import larson.groupcontrol.app.exception.UnFormatMessageException;
import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.intf.IConnectionManager;
import larson.groupcontrol.app.intf.IStateChangeListener;
import larson.groupcontrol.app.intf.ISocket;
import larson.groupcontrol.app.listener.IMessageListener;
import larson.groupcontrol.app.message.Message;
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
 * ..larsonzhong@163.com      created in 2018/8/13     @author : larsonzhong
 * <p>
 * Provides interface calls and management classes to the app development layer
 * so that developers don't need to care too much about the underlying business logic.
 * <p>
 * To create a connection to skyRuler socket a simple usage of this API might looks like the
 * following:
 * <p>
 * <pre>
 *   // Create a configuration for new connection
 *   SocketConfiguration configuration = new SocketConfiguration.Builder()
 *          .setSkSocketOption(option).setSocketName("GroupControl").build();
 *   // Create a connection to the JT/T808 server
 *   GroupControlManager skSocket = new GroupControlManager(configuration);
 *   // Connect to the server
 *    skSocket.connect(mContext);
 *   // Create a message to send
 *   Message msg = new Message.Builder().setXXX(ooo).build();
 *   // Send the server a message
 *   skSocket.sendMessage(msg);
 *   // Disconnect from the server
 *   skSocket.disconnect();
 *   // Destroy then when exit app
 *   skSocket.onDestroy();
 * </pre>
 * <p>
 */
public class GroupControlManager implements ISocket {

    private SocketConfiguration option;
    private IConnectionManager mConnMgr;

    public GroupControlManager(SocketConfiguration option,IStateChangeListener mStateChangeListener) {
        this.option = option;
        mConnMgr = new ConnectionManager(option, mStateChangeListener);
        LogUtils.setDebugMode(option.getSkSocketOption().isDebug());
    }

    @Override
    public void connect(boolean isReconnect) {
        LogUtils.d("start: ");
        mConnMgr.connect(isReconnect);
    }

    @Override
    public void disConnect() {
        LogUtils.d("disConnect");
        mConnMgr.disConnect();
    }

    @Override
    public void onDestroy() {
        LogUtils.d("onDestroy");
        this.option = null;

        if (mConnMgr != null) {
            mConnMgr.onDestroy();
            mConnMgr = null;
        }
    }

    @Override
    public boolean isConnected() {
        return mConnMgr != null && mConnMgr.isConnected();
    }

    @Override
    public void sendMessage(Message msgDataBean) {
        mConnMgr.sendMessage(msgDataBean);
    }

    @Override
    public Message sendSyncMessage(final Message msg, long timeout) {
        if (timeout == 0) {
            throw new UnFormatMessageException("Unable to send a message with timeout 0 in this method !!");
        }
        return mConnMgr.sendSyncMessage(msg, timeout);
    }

    @Override
    public Message sendSyncMessage(final Message msg, MessageFilter filter, long timeout) {
        if (timeout == 0) {
            throw new UnFormatMessageException("Unable to send a message with timeout 0 in this method !!");
        }
        return mConnMgr.sendSyncMessage(msg, filter, timeout);
    }

    @Override
    public void addMessageListener(IMessageListener listener, MessageFilter filter) {
        if (mConnMgr != null) {
            mConnMgr.addMsgListener(listener, filter);
        } else {
            LogUtils.e("mConnMgr is null ");
        }
    }

    @Override
    public void removeMessageListener(IMessageListener listener) {
        if (mConnMgr != null) {
            mConnMgr.removeMsgListener(listener);
        } else {
            LogUtils.e("mConnMgr is null ");
        }
    }
}
