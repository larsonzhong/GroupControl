package larson.groupcontrol.app.connection;


import android.support.annotation.NonNull;

import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.packet.Packet;
import larson.groupcontrol.app.util.LogUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
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
 */
class PacketWriter {
    /**
     * 默认心跳时间间隔
     */
    private static final long DEFAULT_HEART_INTERVAL = 10 * 1000;
    private final BlockingQueue<Packet> mQueue;

    private Connection mConnection;
    private final OutputStream mOutputStream;
    private Thread mWriteThread;
    private ScheduledExecutorService mTimeExecutor;
    /**
     * writer是否停止工作
     */
    private boolean mShutdown;

    /**
     * Creates a new message writer with the specified connection.
     *
     * @param conn the connection
     */
    PacketWriter(Connection conn) {
        mQueue = new LinkedBlockingQueue<>();
        mConnection = conn;
        mOutputStream = mConnection.getOutputStream();
        init();
    }

    /**
     * 构造的时候或者因为其他错误断开的时候需要初始化
     * <p>
     * Initializes the writer in order to be used. It is called at the first connection and also is
     * invoked if the connection is disconnected by an error.
     */
    private void init() {
        LogUtils.d("init writer..");
        mShutdown = false;

        //开一个线程维护要往外写的数据，防止主线程阻塞
        mWriteThread = new WriteThread();
        mWriteThread.setName("Thread[Message Writer]");
        mWriteThread.setDaemon(true);
    }

    /**
     * 关闭writer，一旦调用则不在往外写数据
     * <p>
     * Shuts down the message writer. Once this method has been called, no further packets will be
     * written to the server.
     */
    public void shutdown() {
        mShutdown = true;
        synchronized (mQueue) {
            mQueue.notifyAll();
        }

        //larson add below
        try {
            //关闭心跳
            if (mTimeExecutor != null && !mTimeExecutor.isShutdown()) {
                mTimeExecutor.shutdownNow();
            }
            //关闭输出线程
            if (mWriteThread != null) {
                mWriteThread.interrupt();
            }
            //关闭输出流
            if (mOutputStream != null) {
                mOutputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * 开启writer线程，开启后保持往外写数据，直到线程被终止
     * <p>
     * Starts the packet write thread. The message writer will continue writing packets until {@link
     * #shutdown} or an error occurs.
     */
    public void startup() {
        mWriteThread.start();
    }

    /**
     * 开启一个守护线程防止线程挂掉，一旦挂了就没办法写数据了
     * <p>
     * Starts the keep alive process. An empty message (aka heartbeat) is going to be sent to the
     * server every 30 seconds (by default) since the last packet was sent to the server.
     */
    public void keepAlive(long keepAliveInterval) {
        // Schedule a keep-alive task to run if the feature is enabled, will write out a empty
        // message each time it runs to keep the TCP/IP connection open

        if (keepAliveInterval <= 0) {
            keepAliveInterval = DEFAULT_HEART_INTERVAL;
            LogUtils.e("read config error:keepAliveInterval=" + keepAliveInterval);
        }

        if (mTimeExecutor != null && !mTimeExecutor.isShutdown()) {
            mTimeExecutor.shutdownNow();
        }
        mTimeExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
            private final AtomicInteger integer = new AtomicInteger();

            @Override
            public Thread newThread(@NonNull Runnable r) {
                return new Thread(r, "Heartbeat thread: " + integer.getAndIncrement());
            }
        });
        mTimeExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sendHeartbeatMsg();
            }
        }, 0, keepAliveInterval, TimeUnit.MILLISECONDS);
    }

    /**
     * 将message写入到socket，如果message有多个packet则分多个packet发送
     *
     * @param msg 需要发送的消息
     */
    public void sendMessage(Message msg) {
        if (!mShutdown) {
            try {
                synchronized (this) {
                    for (Packet packet : msg.getPackets()) {
                        mQueue.put(packet);
                        //队列满时put会等待一段时间而offer直接返回false
                    }
                }
            } catch (Exception ie) {
                ie.printStackTrace();
                return;
            }

            synchronized (mQueue) {
                mQueue.notifyAll();
            }
        }
    }

    private class WriteThread extends Thread {

        @Override
        public void run() {
            super.run();
            LogUtils.w("start writer block thread ..");
            writePackets();
        }
    }

    /**
     * 不断读取队列中的packet并写入到connection中
     */
    private void writePackets() {
        try {
            while (!mShutdown) {
                Packet packet = nextPacket();
                synchronized (mOutputStream) {
                    if (packet != null) {
                        mOutputStream.write(packet.getBytes());
                        mOutputStream.flush();
                    }
                }
            }
            mQueue.clear();
        } catch (Exception e) {
            if (mConnection.isConnected()) {
                mConnection.onSocketCloseUnexpected(e);
            }
        }
    }

    /**
     * 取出下一个包，取出成功，然后让队列阻塞
     * fetch next packet if not empty and then block
     *
     * @return next packet
     */
    private Packet nextPacket() {
        Packet packet = null;
        synchronized (mQueue) {
            while (!mShutdown && (packet = mQueue.poll()) == null) {
                try {
                    mQueue.wait();
                } catch (InterruptedException e) {
                    LogUtils.i(e.getMessage());
                }
            }
        }
        return packet;
    }

    private void sendHeartbeatMsg() {
        try {
            Message msg = new Message.Builder((short) 0).setType(Message.MSG_TYPE_HEART).setAck(false).build();
            synchronized (mOutputStream) {
                for (Packet packet : msg.getPackets()) {
                    if (mOutputStream != null) {
                        mOutputStream.write(packet.getBytes());
                        mOutputStream.flush();
                    } else {
                        LogUtils.e("illegal current outputStream state:null !!!");
                    }
                }
            }
        } catch (Exception e) {
            if (mConnection.isConnected()) {
                mConnection.onSocketCloseUnexpected(e);
            }
        }
    }

}
