package larson.groupcontrol.app.connection;

import android.net.LocalSocket;
import android.net.LocalSocketAddress;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import larson.groupcontrol.app.SocketConfiguration;
import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.filter.MessageIdFilter;
import larson.groupcontrol.app.listener.IMessageListener;
import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.util.LogUtils;


/**
 * Creates a socket connection to  skyRuler server.
 * <p>
 * Connection can be reused between connections. This means that a Connection may be connected,
 * disconnected, and then connected again. Listeners of the Connection will be retained across
 * connections.
 * <p>
 * If a connected Connection gets disconnected abruptly and automatic reconnection is enabled
 * ({@link ConnectOption#isReconnectAllowed()} , the default), then it will try to
 * reconnect again. To stop the reconnection process, . Once stopped you
 * can use {@link #connect()} to manually connect to the server.
 *
 * @author Larsonzhong (larsonzhong@163.com)
 * @since 2017-12-07 11:14
 */
public class Connection {
    private boolean mConnected = false;
    /**
     * connect to server config
     */
    private SocketConfiguration mConfig;
    /**
     * The socket which is used for this connection
     */
    private LocalSocket mSocket;
    private InputStream mInputStream;
    private OutputStream mOutputStream;
    private PacketReader mReader;
    private PacketWriter mWriter;
    private IConnectionListener connListener;
    /**
     * 包分发器，因为控制端会收到不同的客户端发过来的包，需要对这些包进行分包路由，
     * 为了避免代码臃肿，新开一类专门用来处理packet路由
     */
    private PacketRouter packetRouter;

    /**
     * Creates a new JT/T808 connection using the specified connection configuration.
     * <p>
     * Note that Connection constructors do not establish a connection to the server and you must call
     * {@link #connect()}.
     *
     * @param cfg the configuration which is used to establish the connection
     */
    Connection(SocketConfiguration cfg) {
        mConfig = cfg;
        packetRouter = new PacketRouter();
    }


    /**
     * Establishes a connection to the JT/T808 server and performs an automatic login only if the
     * previous connection state was logged (authenticated). It basically creates and maintains a
     * connection to the server.
     * <p>
     * Listeners will be preserved from a previous connection.
     */
    public void connect() throws IOException {
        mSocket = new LocalSocket();
        //int timeout =  mConfig.getSkSocketOption().getConnectTimeoutSecond()*1000;
        mSocket.connect(new LocalSocketAddress(mConfig.getSocketName()));
        mSocket.setSoTimeout(3000);

        // Set the input stream and output stream instance variables
        try {
            mInputStream = mSocket.getInputStream();
            mOutputStream = mSocket.getOutputStream();
        } catch (IOException ioe) {
            // An exception occurred in setting up the connection. Make sure we shut down the input
            // stream and output stream and close the socket
            resetConnection();
            setConnected(false);
            throw ioe;
        }

        mWriter = new PacketWriter(this);
        mReader = new PacketReader(this, new PacketReader.OnCallbackListener() {
            @Override
            public void onDataReceive(byte[] data, int len) {
                packetRouter.onDataReceive(data, len);
            }
        });

        // Start the message writer
        mWriter.startup();
        // Start the message reader, the startup() method will block until we get a packet from server
        mReader.startup();

        mWriter.keepAlive(mConfig.getSkSocketOption().getPulseFrequency());

        // Make note of the fact that we're now connected
        setConnected(true);

        if (connListener != null) {
            connListener.onConnectSuccessful();
        }

        addCustomerReceiveListeners();
    }

    /**
     * 登录成功才添加自定义Listener
     */
    private void addCustomerReceiveListeners() {
        for (MessageFilter filter : mConfig.getWrappers().keySet()) {
            packetRouter.addRcvListener(filter, mConfig.getWrappers().get(filter));
        }
    }

    private void resetConnection() {
        if (mWriter != null) {
            mWriter.shutdown();
            mWriter = null;
        }
        if (mReader != null) {
            mReader.shutdown();
            mReader = null;
        }
        if (mInputStream != null) {
            try {
                mInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mInputStream = null;
        }
        if (mOutputStream != null) {
            try {
                mOutputStream.close();
            } catch (IOException e) {
                // Ignore
            }
            mOutputStream = null;
        }
        if (mSocket != null) {
            try {
                mSocket.close();
            } catch (IOException e) {
                // Ignore
            }
            mSocket = null;
        }

        if (packetRouter != null) {
            packetRouter.clear();
            packetRouter = null;
        }
        connListener = null;
    }

    public void deInit() {
        resetConnection();
    }

    public InputStream getInputStream() {
        return mInputStream;
    }

    public OutputStream getOutputStream() {
        return mOutputStream;
    }

    public boolean isConnected() {
        return mConnected;
    }

    private void setConnected(boolean isConnect) {
        this.mConnected = isConnect;
    }


    public void setConnectListener(IConnectionListener connListener) {
        this.connListener = connListener;
    }

    void onSocketCloseUnexpected(Exception e) {
        if (connListener != null) {
            connListener.onSocketClosed(e);
        }
    }


    /**
     * 通过writer将消息写出去
     * write message to server
     *
     * @param msg 要发送的消息
     */
    public void sendMessage(Message msg) {
        if (!isConnected()) {
            LogUtils.e("Not connected to server...");
            return;
        }
        if (msg == null) {
            LogUtils.e("Message is null.");
            return;
        }
        mWriter.sendMessage(msg);
    }


    public Message sendSyncMessage(final Message msg, long timeOut) {
        if (!isConnected()) {
            LogUtils.e("Not connected to server...");
            return null;
        }
        if (msg == null) {
            LogUtils.e("Message is null.");
            return null;
        }

        mWriter.sendMessage(msg);

        //创造一个filter过滤不属于该ClientID的消息
        MessageFilter idFilter = new MessageIdFilter(msg.getMsgId());
        MessageCollector collector = packetRouter.createMessageCollector(idFilter);
        Message retMsg = collector.nextResult(timeOut);
        collector.cancel();
        return retMsg;
    }

    public Message sendSyncMessage(final Message msg, MessageFilter filter, long timeOut) {
        if (!isConnected()) {
            LogUtils.e("Not connected to server...");
            return null;
        }
        if (msg == null) {
            LogUtils.e("Message is null.");
            return null;
        }
        mWriter.sendMessage(msg);
        MessageCollector collector = packetRouter.createMessageCollector(filter);
        Message retMsg = collector.nextResult(timeOut);
        collector.cancel();
        return retMsg;
    }

    public void addMsgListener(IMessageListener listener, MessageFilter filter) {
        packetRouter.addRcvListener(filter, listener);
    }

    public void removeMsgListener(IMessageListener listener) {
        packetRouter.removeRcvListener(listener);
    }

    /**
     * 添加监听等待服务端发过来的消息
     *
     * @param filter  filter
     * @param timeOut timeout
     * @return msg from server
     */
    public Message waitForMessage(MessageFilter filter, long timeOut) {
        if (null == packetRouter) {
            return null;
        }
        MessageCollector collector = packetRouter.createMessageCollector(filter);
        Message retMsg = collector.nextResult(timeOut);
        //if (retMsg != null) {
        collector.cancel();
        //}
        return retMsg;
    }

    interface IConnectionListener {
        /**
         * 连接成功
         */
        void onConnectSuccessful();

        /**
         * 连接断开，可能是主动断开，可能是被动断开
         *
         * @param e 当e为null表示是主动断开，重连机制不需要处理
         */
        void onSocketClosed(Exception e);
    }
}
