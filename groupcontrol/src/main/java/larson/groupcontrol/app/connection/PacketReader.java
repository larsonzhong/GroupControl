package larson.groupcontrol.app.connection;

import android.support.annotation.NonNull;

import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.packet.Packet;
import larson.groupcontrol.app.packet.PacketHeader;
import larson.groupcontrol.app.util.LogUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
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
 */
public class PacketReader {

    private final OnCallbackListener mCallbackListener;
    private final ExecutorService mExecutorService;
    private ByteBuffer mByteBuffer;
    private BlockingQueue<byte[]> mQueue;
    private ReadRunnable mReadRunnable;
    private DataRunnable mDataRunnable;
    private Future mReadRunnableFuture;
    /**
     * is PacketReader shutdown
     */
    private AtomicBoolean bExit;
    private final Connection mConnection;
    private InputStream mInputStream;

    /**
     * Creates a new MessageReader with the special connection
     *
     * @param conn the connection
     */
    PacketReader(Connection conn, OnCallbackListener listener) {
        mConnection = conn;
        mInputStream = mConnection.getInputStream();
        mCallbackListener = listener;
        mExecutorService = newExecutor();
        mByteBuffer = ByteBuffer.allocate(8192);
        mByteBuffer.clear();
        init();
    }

    /**
     * initialize the reader inorder to be use.the reader is initialized
     * during the first connection  and when reconnecting due to abruptly disconnection
     */
    private void init() {
        LogUtils.d("init reader..");
        bExit = new AtomicBoolean(false);
        mQueue = new LinkedBlockingQueue<>();
        mReadRunnable = new ReadRunnable();
        mDataRunnable = new DataRunnable();
    }

    private ExecutorService newExecutor() {
        //设置核心池大小
        int corePoolSize = 10;
        //设置线程池最大能接受多少线程
        int maxPoolSize = 500;
        //当前线程数大于corePoolSize、小于maximumPoolSize时，超出corePoolSize的线程数的生命周期
        long keepActiveTime = 200;
        //设置时间单位，秒
        TimeUnit timeUnit = TimeUnit.SECONDS;
        //设置线程池缓存队列的排队策略为FIFO，并且指定缓存队列大小为1
        BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(1);
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

    /**
     * Starts the packet read thread.
     */
    public synchronized void startup() {
        mReadRunnableFuture = mExecutorService.submit(mReadRunnable);
        mExecutorService.execute(mDataRunnable);
    }

    private class ReadRunnable implements Runnable {

        @Override
        public void run() {
            while (!bExit.get()) {
                try {
                    byte[] buffer = new byte[4096];
                    int num = mInputStream.read(buffer);
                    if (num <= 0) {
                        continue;
                    }
                    byte[] bytes = new byte[num];
                    System.arraycopy(buffer, 0, bytes, 0, num);
                    mQueue.add(bytes);
                } catch (IOException e) {
                    //LogUtils.d(e.toString());
                }
            }
            LogUtils.e("ReadRunnable exit!");
        }
    }

    private class DataRunnable implements Runnable {
        public void stop() {
            try {
                Packet packet = new Packet.Builder(Message.MSG_TYPE_ACK, (short) 0, (short) 0, (short) 0).build();
                mQueue.add(packet.getBytes());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            LogUtils.d("start read block thread ..");
            while (!bExit.get()) {
                try {
                    byte[] bytes = mQueue.take();
                    if (bytes == null) {
                        continue;
                    }
                    parseData(bytes);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void parseData(byte[] data) {
        PacketHeader header;
        int len = data.length + mByteBuffer.position();
        mByteBuffer.put(data);
        mByteBuffer.flip();
        byte[] bytes = new byte[len];
        mByteBuffer.get(bytes);
        mByteBuffer.clear();
        int index = 0;
        while (len > 0) {
            /*
             * 保存非完整的头
             */
            if (len < PacketHeader.SIZE) {
                mByteBuffer.put(bytes, index, len);
                break;
            }
            header = PacketHeader.parse(bytes, index);
            if ((header.getMagic() & PacketHeader.MAGIC_MASK) != PacketHeader.DEFAULT_MAGIC) {
                LogUtils.e("Error package, error magic index:" + index);
                index += 1;
                len -= 1;
                continue;
            }
            /*
             * 检查是否一个完整包
             */
            int packetSize = header.getPaddingSize() + PacketHeader.SIZE;
            if (len >= packetSize) {
                //发送数据给ui显示
                if (header.getType() != PacketHeader.TYPE_ACK) {
                    if (mCallbackListener != null) {
                        byte[] packet = new byte[packetSize];
                        System.arraycopy(bytes, index, packet, 0, packetSize);
                        mCallbackListener.onDataReceive(packet, packetSize);
                    }
                }
                index += packetSize;
                len -= packetSize;
            } else {
                //保存非完整数据,已经有包头
                mByteBuffer.put(bytes, index, len);
                break;
            }
        }
    }

    public interface OnCallbackListener {
        /**
         * 获取回调数据
         *
         * @param data 数据
         * @param len  长度
         */
        void onDataReceive(byte[] data, int len);
    }

    public void shutdown() {
        try {
            bExit.set(true);
            if (mInputStream != null) {
                mInputStream.close();
                mInputStream = null;
            }
            mDataRunnable.stop();
            mReadRunnableFuture.cancel(true);
            if (mExecutorService != null) {
                mExecutorService.shutdown();
                try {
                    if (!mExecutorService.awaitTermination(1, TimeUnit.SECONDS)) {
                        mExecutorService.shutdownNow();
                    }
                } catch (InterruptedException e) {
                    mExecutorService.shutdownNow();
                    Thread.currentThread().interrupt();
                }
            }
            if (mQueue != null) {
                mQueue.clear();
                mQueue = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
