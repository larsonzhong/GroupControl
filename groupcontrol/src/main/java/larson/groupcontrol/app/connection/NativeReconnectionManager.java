package larson.groupcontrol.app.connection;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.atomic.AtomicBoolean;

import larson.groupcontrol.app.exception.UnconnectedException;
import larson.groupcontrol.app.filter.AndFilter;
import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.filter.MessageIdFilter;
import larson.groupcontrol.app.intf.IConnectionManager;
import larson.groupcontrol.app.intf.IStateChangeListener;
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
 * ..larsonzhong@163.com      created in 2018/8/15     @author : larsonzhong
 * <p>
 * 本地重连管理器，区别于{@link ReconnectionManager}
 */
public class NativeReconnectionManager implements IStateChangeListener {
    /**
     * 默认重连时间(后面会以指数次增加)
     */
    private static final long DEFAULT_RECONNECT_INTERVAL = 5 * 1000;
    /**
     * 最大连接失败次数,不包括断开异常
     */
    private static final int DEFAULT_MAX_CONNECTION_FAILED_TIMES = 12;
    /**
     * 延时连接时间
     */
    private long mReconnectTimeDelay = DEFAULT_RECONNECT_INTERVAL;
    /**
     * 最大重连次数
     */
    private int mMaxConnectionFailedTimes;
    /**
     * 当前连接失败次数,不包括断开异常
     */
    private int mCurrentTimes = 0;
    /**
     * 是否需要重连管理器
     */
    private boolean isReconnectEnable = false;
    /**
     * 连接管理器
     */
    private IConnectionManager mConnectionManager;

    private AtomicBoolean isReconnecting = new AtomicBoolean(false);

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            LogUtils.e("Handler reconnect :" + msg.what);
            isReconnecting.set(false);
            //首先启动本地服务器
            reconnectRemoteServer(mReconnectTimeDelay);
        }
    };

    /**
     * 关联到某一个连接管理器
     *
     * @param manager 当前连接管理器
     */
    public void attach(ConnectionManager manager) {
        detach();
        mConnectionManager = manager;
        ConnectOption option = manager.getConnectionConfiguration().getSkSocketOption();
        this.isReconnectEnable = option.isReconnectAllowed();
        this.mMaxConnectionFailedTimes = option.getReconnectMaxAttemptTimes() == 0 ?
                DEFAULT_MAX_CONNECTION_FAILED_TIMES : option.getReconnectMaxAttemptTimes();
        this.mReconnectTimeDelay = option.getReconnectInterval() == 0 ?
                DEFAULT_RECONNECT_INTERVAL : option.getReconnectInterval();

        mConnectionManager.registerConnectListener(this);
    }

    /**
     * Unlink the current connection manager
     * 解除连接当前的连接管理器
     */
    public void detach() {
        reset();
        if (mConnectionManager != null) {
            mConnectionManager.unRegisterConnectListener(this);
        }
        mConnectionManager = null;
        isReconnectEnable = false;
        mMaxConnectionFailedTimes = 0;
        mReconnectTimeDelay = 0;
    }

    /**
     * The reset is introduced because there is no need to continue reconnecting after the connection is successful,
     * then all the states of the reconnect manager need to be reset to handle the new reconnection operation.
     * 引入reset是因为当连接成功后就不需要继续重连，则需要把重连管理器所有状态重置以处理新的重连操作
     */
    private void reset() {
        isReconnecting.set(false);
        mHandler.removeCallbacksAndMessages(null);
        mCurrentTimes = 0;
    }

    /**
     * 发起重连
     */
    private void reconnectDelay() {
        isReconnecting.set(true);
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessageDelayed(0, mReconnectTimeDelay);
        LogUtils.i("Reconnect after " + mReconnectTimeDelay + " mills ...");
    }

    /**
     * 连接到远程的服务器，这个用在远程服务器断开后，客户端做重连
     *
     * @param mReconnectTimeDelay 连接间隔
     */
    private void reconnectRemoteServer(long mReconnectTimeDelay) {
        try {
            final Message message = new Message.Builder(Constants.MSG_RECONNECT_REMOTE_SERVER)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .build();

            MessageFilter serverStateFilter = new MessageIdFilter(Constants.MSG_SERVER_STATE_CHANGED);
            MessageFilter connectedFilter = new MessageFilter() {
                @Override
                public boolean accept(Message msg) {
                    byte state = msg.getBody()[0];
                    //远程服务器断开
                    return state == Constants.STATE_CONNECTED;
                }
            };

            Message retMsg = mConnectionManager
                    .sendSyncMessage(message, new AndFilter(connectedFilter, serverStateFilter), mReconnectTimeDelay);

            if (retMsg != null) {
                mConnectionManager.sendLoginMessage(true);
            } else {
                onConnectStateChange(STATE_RECONNECT_REMOTE_FAILED,
                        new UnconnectedException("send reconnect message timeout !!!"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onConnectStateChange(int stateCode, Exception e) {
        if (!isReconnectEnable) {
            LogUtils.w("reconnect is not enable  !!!");
            detach();
            return;
        }

        if (isReconnecting.get()) {
            LogUtils.d("reconnecting ,stateCode= " + stateCode);
            return;
        }

        switch (stateCode) {
            //主动断开,不需要重连
            case STATE_SOCKET_CLOSE_SUCCESSFUL:
                LogUtils.w("logout success & detach !!!");
                detach();
                break;
            //连接成功,清除消息
            case STATE_RECONNECT_REMOTE_SUCCESS:
                LogUtils.d("reconnect & login success !!!");
                reset();
                break;
            case STATE_RECONNECT_REMOTE_FAILED:
            case STATE_REMOTE_SOCKET_CLOSED:
                LogUtils.e("reconnect & login failed ,stateCode= " + stateCode + " mCurrentTimes:" + mCurrentTimes);
                mCurrentTimes++;
                if (mCurrentTimes >= mMaxConnectionFailedTimes) {
                    mConnectionManager.onMaxReconnectTimeReached(e);
                    detach();
                } else {
                    reconnectDelay();
                }
                break;
            default:
                LogUtils.d("reconnect ,stateCode= " + stateCode);
                break;
        }
    }
}
