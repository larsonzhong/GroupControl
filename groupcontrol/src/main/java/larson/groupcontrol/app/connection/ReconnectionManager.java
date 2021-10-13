package larson.groupcontrol.app.connection;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.CallSuper;

import larson.groupcontrol.app.intf.IConnectionManager;
import larson.groupcontrol.app.intf.IStateChangeListener;
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
 */
public class ReconnectionManager implements IStateChangeListener {
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


    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(android.os.Message msg) {
            super.handleMessage(msg);
            LogUtils.e("Handler reconnect :" + msg.what);
            if (!mConnectionManager.isConnected()) {
                mConnectionManager.connect(true);
            }
        }
    };

    /**
     * 关联到某一个连接管理器
     *
     * @param manager 当前连接管理器
     */
    @CallSuper
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
        mHandler.removeCallbacksAndMessages(null);
        mCurrentTimes = 0;
    }

    private void reconnectDelay() {
        mHandler.removeCallbacksAndMessages(null);
        mHandler.sendEmptyMessageDelayed(0, mReconnectTimeDelay);
        LogUtils.i("Reconnect after " + mReconnectTimeDelay + " mills ...");
    }

    @Override
    public void onConnectStateChange(int stateCode, Exception e) {
        if (!isReconnectEnable) {
            LogUtils.w("reconnect is not enable  !!!");
            detach();
            return;
        }

        switch (stateCode) {
            //主动断开,不需要重连
            case STATE_SOCKET_CLOSE_SUCCESSFUL:
                detach();
                break;
            //连接失败导致的重连
            case STATE_CONNECT_FAILED:
                LogUtils.e("reconnect start : " + mCurrentTimes);
                mCurrentTimes++;
                if (mCurrentTimes >= mMaxConnectionFailedTimes) {
                    mConnectionManager.onMaxReconnectTimeReached(e);
                    detach();
                } else {
                    reconnectDelay();
                }
                break;
            //被动断开
            case STATE_SOCKET_CLOSE_FAILED:
                if (isReconnectEnable && e != null) {
                    reconnectDelay();
                } else {
                    detach();
                }
                break;
            case STATE_CONNECT_SUCCESSFUL:
                reset();
                break;
            default:
                break;
        }
    }


}
