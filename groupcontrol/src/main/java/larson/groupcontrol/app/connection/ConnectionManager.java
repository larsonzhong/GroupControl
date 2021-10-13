package larson.groupcontrol.app.connection;

import android.support.annotation.NonNull;

import larson.groupcontrol.app.SocketConfiguration;
import larson.groupcontrol.app.exception.UnconnectedException;
import larson.groupcontrol.app.filter.AndFilter;
import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.filter.MessageIdFilter;
import larson.groupcontrol.app.intf.IConnectionManager;
import larson.groupcontrol.app.intf.IStateChangeListener;
import larson.groupcontrol.app.listener.IMessageListener;
import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.util.BytesUtils;
import larson.groupcontrol.app.util.LogUtils;

import java.io.IOException;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

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
 * Connection manager for managing LocalSocket connections and for related resource management and calls
 * 连接管理器，用于管理LocalSocket连接以及进行相关资源管理及调用,包含了发送线程池的维护
 */
public class ConnectionManager implements IConnectionManager {
    /**
     * 能否连接
     * can not connect when connecting or disconnecting
     */
    private AtomicBoolean canConnect = new AtomicBoolean(true);
    /**
     * 套接字
     */
    private Connection mConnection;

    /**
     * 使用CopyOnWriteArrayList可以支持并发，避免{@link java.util.ConcurrentModificationException}
     */
    private CopyOnWriteArrayList<IStateChangeListener> connListeners = new CopyOnWriteArrayList<>();
    private ExecutorService mExecutor;
    private SocketConfiguration socketOption;
    /**
     * 重新连接管理器
     */
//    private ReconnectionManager mReconnectionManager = new ReconnectionManager();
    private NativeReconnectionManager mNativeReconnectionManager = new NativeReconnectionManager();
    private IMessageListener remoteDisconnectListener = new IMessageListener() {
        @Override
        public void processMessage(Message msg) {
            //启动native的重连管理
            LogUtils.w("启动native的重连管理");
            notifyConnectStateChange(IStateChangeListener.STATE_REMOTE_SOCKET_CLOSED, new UnconnectedException("远程服务器异常断开"));
        }
    };

    public ConnectionManager(SocketConfiguration configuration, IStateChangeListener connectionListener) {
        registerConnectListener(connectionListener);
        mExecutor = newExecutor();
        this.socketOption = configuration;
    }

    @Override
    public void connect(boolean isReconnect) {
        LogUtils.w("localSocket connect:>>>>>>> ");
        if (!canConnect.get()) {
            return;
        }
        if (isConnected()) {
            //如果已经连接上则不需要在此连接，这里需要保证isConnected状态必须正确
            return;
        }
        canConnect.set(false);
        if (socketOption == null) {
            throw new UnconnectedException("Connection parameter is empty, please check connection parameters !!");
        }

        mExecutor.execute(new ConnectTask(isReconnect));
    }

    @Override
    public boolean isConnected() {
        return mConnection != null && mConnection.isConnected();
    }

    @Override
    public void disConnect() {
        LogUtils.w("localSocket disconnect:>>>>>>> ");
//        mReconnectionManager.detach();
        mNativeReconnectionManager.detach();
        mExecutor.execute(new DisConnectTask(IStateChangeListener.STATE_SOCKET_CLOSE_SUCCESSFUL));
    }

    @Override
    public SocketConfiguration getConnectionConfiguration() {
        return socketOption;
    }

    /**
     * send message to server through connection，Does not block threads
     * 发送消息，阻塞线程
     *
     * @param msgDataBean the message to write
     * @param timeout     wait message the timeout is not 0
     */
    @Override
    public Message sendSyncMessage(Message msgDataBean, MessageFilter filter, long timeout) {
        return mConnection.sendSyncMessage(msgDataBean, filter, timeout);
    }

    @Override
    public Message sendSyncMessage(Message msgDataBean, long timeout) {
        return mConnection.sendSyncMessage(msgDataBean, timeout);
    }

    /**
     * Send a message, each message will be sent through the child thread
     * to avoid blocking the child thread
     * 发送消息，每一个消息都会通过子线程发送以免阻塞子线程
     *
     * @param msgDataBean 要发送的消息
     */
    @Override
    public void sendMessage(final Message msgDataBean) {
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                mConnection.sendMessage(msgDataBean);
            }
        });
    }

    /**
     * 销毁需要清理callback和配置
     */
    @Override
    public void onDestroy() {
        canConnect.set(false);
//        mReconnectionManager.detach();
        mNativeReconnectionManager.detach();
        mExecutor.execute(new DisConnectTask(IStateChangeListener.STATE_SOCKET_CLOSE_SUCCESSFUL));

        if (connListeners != null) {
            //初始化状态为size=0，不需要置空
            connListeners.clear();
        }
        if (mExecutor != null) {
            mExecutor.shutdown();
            mExecutor = null;
        }
        if (socketOption != null) {
            socketOption = null;
        }
    }

    /**
     * A runnable task to connect to the server.
     */
    private class ConnectTask implements Runnable {
        private boolean mIsReconnecting;
        ConnectTask(boolean isReconnecting) {
            this.mIsReconnecting = isReconnecting;
        }
        @Override
        public void run() {
            LogUtils.d("run: Connecting...");
            if (mConnection != null && mConnection.isConnected()) {
                return;
            }
            try {
                if (!isConnected()) {
                    // Create a new connection
                    mConnection = new Connection(socketOption);
                    mConnection.setConnectListener(connListenerImpl);
                    if (socketOption.isClient()) {
                        mExecutor.execute(new WaitForConnectTask(mIsReconnecting));
                    }
                    // Connect to the server
                    mConnection.connect();
                } else {
                    LogUtils.w("run: Connected already.");
                }
                //连接完成，取出下一个任务执行
                LogUtils.d("run: Connect successful.");
            } catch (IOException ioe) {
                LogUtils.e("Socket server " + socketOption.toString() + " connect failed! error msg:" + ioe.getMessage());
                if (mIsReconnecting){
                    mExecutor.submit(new DisConnectTask(IStateChangeListener.STATE_SOCKET_CLOSE_FAILED));
                } else {
                    onConnectFailed(ioe);
                }
                canConnect.set(true);
            }
        }
    }


    /**
     * 删除连接资源
     */
    private class DisConnectTask implements Runnable {
        private int mCurrentState;

        DisConnectTask(int mCurrentState) {
            this.mCurrentState = mCurrentState;
        }

        @Override
        public void run() {
            canConnect.set(false);
            LogUtils.d("disconnect thread currentState: " + mCurrentState);
            if (mCurrentState == IStateChangeListener.STATE_SOCKET_CLOSE_SUCCESSFUL) {
                if (socketOption == null) {
                    LogUtils.e("disconnect exception !!!");
                    return;
                }
                if (socketOption.isClient()) {
                    boolean isSuccess = sendLogoutMessage();
                    LogUtils.e("send logout message :" + isSuccess);
                }
                //null表示登出成功
                notifyConnectStateChange(mCurrentState, null);
            } else if (mCurrentState == IStateChangeListener.STATE_LOGIN_REFUSED) {
                notifyConnectStateChange(mCurrentState, new IllegalAccessException("登录被远程服务器拒绝"));
            } else if (mCurrentState == IStateChangeListener.STATE_LOGIN_TIMEOUT) {
                notifyConnectStateChange(mCurrentState, new IllegalAccessException("登录超时,stateCode =" + mCurrentState));
            } else if (mCurrentState == IStateChangeListener.STATE_SOCKET_CLOSE_FAILED) {
                notifyConnectStateChange(mCurrentState, new UnconnectedException("本地连接异常断开，重启底层服务"));
            } else if (mCurrentState == IStateChangeListener.STATE_CONNECT_TIMEOUT) {
                notifyConnectStateChange(mCurrentState, new IllegalAccessException("登录失败，连接超时"));
            } else if (mCurrentState == IStateChangeListener.STATE_REMOTE_SOCKET_CLOSED) {
                notifyConnectStateChange(mCurrentState, new UnconnectedException("登录失败，远程服务器断开"));
            } else if (mCurrentState == IStateChangeListener.STATE_CONNECTING_REMOTE) {
                notifyConnectStateChange(mCurrentState, new UnconnectedException("登录失败，本地与远程连接未就绪"));
            } else if (mCurrentState == IStateChangeListener.STATE_REACH_MAX_RECONNECT_TIME) {
                notifyConnectStateChange(mCurrentState, new UnconnectedException("达到最大重连次数，重启底层服务"));
            }

            if (mConnection != null) {
                mConnection.deInit();
                mConnection = null;
            }
            canConnect.set(true);
        }
    }

    private ExecutorService newExecutor() {
        //设置核心池大小
        int corePoolSize = 50;
        //设置线程池最大能接受多少线程
        int maxPoolSize = 500;
        //当前线程数大于corePoolSize、小于maximumPoolSize时，超出corePoolSize的线程数的生命周期
        long keepActiveTime = 200;
        //设置时间单位，秒
        TimeUnit timeUnit = TimeUnit.SECONDS;
        //设置线程池缓存队列的排队策略为FIFO，并且指定缓存队列大小为1
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(50);
        //创建ThreadPoolExecutor线程池对象，并初始化该对象的各种参数
        ThreadFactory factory = new ThreadFactory() {
            private final AtomicInteger integer = new AtomicInteger();

            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "ConnectionManager thread: " + integer.getAndIncrement());
            }
        };
        return new ThreadPoolExecutor(corePoolSize, maxPoolSize, keepActiveTime, timeUnit, workQueue, factory);
    }

    @Override
    public void registerConnectListener(IStateChangeListener listener) {
        if (listener != null) {
            connListeners.add(listener);
        } else {
            throw new IllegalArgumentException("listener can not be null !!!");
        }
    }

    @Override
    public void unRegisterConnectListener(IStateChangeListener listener) {
        if (connListeners != null && connListeners.contains(listener)) {
            connListeners.remove(listener);
        }
    }

    @Override
    public void addMsgListener(IMessageListener listener, MessageFilter filter) {
        if (mConnection != null && mConnection.isConnected()) {
            mConnection.addMsgListener(listener, filter);
        } else {
            throw new UnconnectedException("can not add message listener when connection unconnected !!!");
        }
    }

    @Override
    public void removeMsgListener(IMessageListener listener) {
        if (mConnection != null && mConnection.isConnected()) {
            mConnection.removeMsgListener(listener);
        } else {
            throw new UnconnectedException("can not unregister message listener when connection is not initialed !!!");
        }
    }

    @Override
    public void onMaxReconnectTimeReached(Exception e) {
        mExecutor.execute(new DisConnectTask(IStateChangeListener.STATE_REACH_MAX_RECONNECT_TIME));
    }

    @Override
    public void onConnectFailed(Exception e) {
        //连接本地socket出现异常
        notifyConnectStateChange(IStateChangeListener.STATE_CONNECT_FAILED, e);
    }

    @Override
    public void onConnectSuccess() {
        if (socketOption.isClient()) {
            attachNativeReconnection();
        }
        notifyConnectStateChange(IStateChangeListener.STATE_CONNECT_SUCCESSFUL, null);
    }

    public void attachNativeReconnection() {
        LogUtils.d("attachNativeReconnection");
        mNativeReconnectionManager.attach(this);
        MessageFilter serverStateFilter = new MessageIdFilter(Constants.MSG_SERVER_STATE_CHANGED);
        MessageFilter disconnectFilter = new MessageFilter() {
            @Override
            public boolean accept(Message msg) {
                byte state = msg.getBody()[0];
                //远程服务器断开
                if (state == Constants.STATE_DISCONNECTED) {
                    return true;
                } else if (state == Constants.STATE_DISCONNECTING) {
                    LogUtils.w("remote socket is disconnecting...");
                } else if (state == Constants.STATE_DISCONNECT_FAILED) {
                    LogUtils.e("remote socket disconnect failed !!!");
                }
                return false;
            }
        };
        addMsgListener(remoteDisconnectListener, new AndFilter(disconnectFilter, serverStateFilter));
    }

    private void notifyConnectStateChange(int state, Exception e) {
        for (IStateChangeListener listener : connListeners) {
            listener.onConnectStateChange(state, e);
        }
    }

    private Connection.IConnectionListener connListenerImpl = new Connection.IConnectionListener() {
        @Override
        public void onConnectSuccessful() {
            canConnect.set(false);
            //这里需要发送登录消息
            if (socketOption.isServer()) {
                onConnectSuccess();
            }
            //LogUtils.i("因为服务端可能在已连接上就发送连接成功的消息，所以添连接成功应该在初始化connection的时候");
            // mExecutor.execute(new WaitForConnectTask());
        }

        @Override
        public void onSocketClosed(Exception e) {
            canConnect.set(true);
            mExecutor.submit(new DisConnectTask(IStateChangeListener.STATE_SOCKET_CLOSE_FAILED));
        }
    };

    /**
     * 发送登录消息
     *
     * @param isReconnecting 是否是在重连
     */
    @Override
    public void sendLoginMessage(boolean isReconnecting) {
        try {
            final Message message = new Message.Builder(Constants.MSG_LOGIN_CONTROL)
                    .setAck(false)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .setBody(BytesUtils.asBytes(Constants.CODE_LOGIN_REQUEST))
                    .build();

            Message msg = sendSyncMessage(message, new MessageFilter() {
                @Override
                public boolean accept(Message msg) {
                    return msg.isResponse() && msg.getMsgId() == message.getMsgId();
                }
            }, 5000);

            if (msg != null) {
                byte[] data = msg.getBody();
                byte responseCode = data[0];
                LogUtils.e("login state :" + responseCode);

                //通知开发者登录结果
                if (isReconnecting) {
                    if (Constants.STATE_CONNECTED == responseCode) {
                        attachNativeReconnection();
                        notifyConnectStateChange(IStateChangeListener.STATE_RECONNECT_REMOTE_SUCCESS, new UnconnectedException("重连成功") );
                    } else {
                        notifyConnectStateChange(IStateChangeListener.STATE_RECONNECT_REMOTE_FAILED, null);
                    }
                } else {
                    if (Constants.STATE_CONNECTED == responseCode) {
                        onConnectSuccess();
                    } else {
                        mExecutor.execute(new DisConnectTask(IStateChangeListener.STATE_LOGIN_REFUSED));
                    }
                }
            } else {
                LogUtils.e("login timeout ,response is null !!");
                if (isReconnecting) {
                    notifyConnectStateChange(IStateChangeListener.STATE_RECONNECT_REMOTE_FAILED, null);
                } else {
                    mExecutor.execute(new DisConnectTask(IStateChangeListener.STATE_LOGIN_TIMEOUT));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 发送登出信息
     */
    private boolean sendLogoutMessage() {
        try {
            final Message message = new Message.Builder(Constants.MSG_LOGIN_CONTROL)
                    .setAck(false)
                    .setType(Message.MSG_TYPE_CONTROL)
                    .setBody(BytesUtils.asBytes(Constants.CODE_LOGOUT_REQUEST))
                    .build();
            Message msg = sendSyncMessage(message, new MessageFilter() {
                @Override
                public boolean accept(Message msg) {
                    return msg.isResponse() && msg.getMsgId() == message.getMsgId();
                }
            }, 5000);

            if (msg != null) {
                byte[] data = msg.getBody();
                byte responseCode = data[0];
                LogUtils.e("login state :" + responseCode);
                return Constants.STATE_DISCONNECTED == responseCode;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 客户端等待LoacalServer Socket返回远程服务器的状态，在发送登录信息
     */
    private class WaitForConnectTask implements Runnable {
        private boolean mIsReconnecting;

        WaitForConnectTask(boolean isReconnecting) {
            this.mIsReconnecting = isReconnecting;
        }

        @Override
        public void run() {
            //这样做的目的是为了处理STATE_CONNECTING_REMOTE中情况
            MessageFilter idFilter = new MessageIdFilter(Constants.MSG_SERVER_STATE_CHANGED);
            MessageFilter stateFilter = new MessageFilter() {
                @Override
                public boolean accept(Message msg) {
                    //这里需要过滤掉正在连接的状态,正在连接不能视为连接失败，所以在这里处理
                    if (Constants.STATE_CONNECTING == msg.getBody()[0]) {
                        //还没连上，就通知上层，并等待直到超时
                        notifyConnectStateChange(IStateChangeListener.STATE_CONNECTING_REMOTE,
                                new UnconnectedException("正在连接远程服务器"));
                        return false;
                    }
                    return true;
                }
            };
            Message retMsg = mConnection.waitForMessage(new AndFilter(stateFilter, idFilter), 5000);
            if (retMsg == null) {
                if (mIsReconnecting) {
                    notifyConnectStateChange(IStateChangeListener.STATE_RECONNECT_REMOTE_FAILED, null);
                } else {
                    mExecutor.execute(new DisConnectTask(IStateChangeListener.STATE_CONNECT_TIMEOUT));
                }
                return;
            }

            //过滤失败结果
            byte[] data = retMsg.getBody();
            byte responseCode = data[0];
            if (Constants.STATE_CONNECTED == responseCode) {
                sendLoginMessage(mIsReconnecting);
            } else {
                if (mIsReconnecting) {
                    notifyConnectStateChange(IStateChangeListener.STATE_RECONNECT_REMOTE_FAILED, null);
                } else {
                    int state = parseLocalState(responseCode);
                    //其他的情况都由DisconnectThread处理
                    mExecutor.execute(new DisConnectTask(state));
                    LogUtils.e(isConnected() + ":wait state :" + state);
                }
            }
        }
    }


    /**
     * 将服务器发过来的状态转换为上层{@link IStateChangeListener}可以识别的状态
     *
     * @param state c层上报的状态
     * @return java state
     */
    private int parseLocalState(byte state) {
        if (Constants.STATE_CONNECTED == state) {
            return IStateChangeListener.STATE_CONNECT_SUCCESSFUL;
        } else if (Constants.STATE_DISCONNECTED == state) {
            return IStateChangeListener.STATE_REMOTE_SOCKET_CLOSED;
        } else if (Constants.STATE_CONNECTING == state) {
            return IStateChangeListener.STATE_CONNECTING_REMOTE;
        } else if (Constants.STATE_CONNECT_FAILED == state) {
            return IStateChangeListener.STATE_CONNECT_REMOTE_FAILED;
        } else if (Constants.STATE_DISCONNECTING == state) {
            return IStateChangeListener.STATE_DISCONNECTING_REMOTE;
        } else if (Constants.STATE_DISCONNECT_FAILED == state) {
            return IStateChangeListener.STATE_DISCONNECTING_REMOTE_FAILED;
        } else {
            return state;
        }
    }

}
