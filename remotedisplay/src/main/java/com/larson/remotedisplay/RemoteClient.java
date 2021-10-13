package com.larson.remotedisplay;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.larson.remotedisplay.buffer.CircularEncoderBuffer;
import com.larson.remotedisplay.thread.SkExecutorService;
import com.larson.remotedisplay.utils.BytesUtils;
import com.larson.remotedisplay.utils.CodecUtils;
import com.larson.remotedisplay.utils.LogUtils;
import com.larson.remotedisplay.utils.SocketUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Administrator
 */
@SuppressLint("NewApi")
public class RemoteClient implements SurfaceHolder.Callback {
    private SurfaceView mSurfaceView;
    private MediaCodec mDecoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private CircularEncoderBuffer mEncoderBuffer;
    private Socket mSocket = null;
    private InputStream mInputStream = null;
    private String mIp;
    private int mPort;
    private ExecutorService mExecutor;
    private BlockingQueue<byte[]> mDataQueue;
    private AtomicBoolean bExit;
    private AtomicBoolean mIsDecoderConfigured;
    private AtomicBoolean mIsDecoderStarted;
    private Object mObject = new Object();
    private DataCollectorWorker mDataCollectorWorker;
    private DateHandleWorker mDateHandleWorker;
    private DecoderWorker mDecoderWorker;

    public void init(SurfaceView surfaceView, String address) {
        mSurfaceView = surfaceView;
        String[] addressArrary = address.split(":");
        if (addressArrary.length > 1) {
            mIp = addressArrary[0];
            mPort = Integer.parseInt(addressArrary[1]);
        } else if (addressArrary.length > 0) {
            mIp = addressArrary[0];
            mPort = SocketUtils.PROT;
        } else {
            LogUtils.e(new Exception("Please input Ip address!"));
        }

        mBufferInfo = new MediaCodec.BufferInfo();
        mEncoderBuffer = new CircularEncoderBuffer((int) (1024 * 1024 * 0.5), 30, 7);
        mDataQueue = new LinkedBlockingQueue<>();
    }

    public void initWorkers() {
        mDataQueue.clear();
        mIsDecoderConfigured = new AtomicBoolean(false);
        mIsDecoderStarted = new AtomicBoolean(false);
        bExit = new AtomicBoolean(false);
        mExecutor = SkExecutorService.newExecutor();
        mDataCollectorWorker = new DataCollectorWorker();
        mDateHandleWorker = new DateHandleWorker();
        mDecoderWorker = new DecoderWorker();
        try {
            mDecoder = MediaCodec.createDecoderByType(CodecUtils.MIME_TYPE);
            mExecutor.execute(mDecoderWorker);
            mExecutor.submit(mDateHandleWorker);
            mExecutor.submit(mDataCollectorWorker);
        } catch (IOException e) {
            LogUtils.e(e.toString());
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        initWorkers();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i2, int i3) {
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        dispose();
    }

    /**
     * Main mDecoder function which reads the encoded frames from the CircularBuffer and renders them
     * on to the Surface
     */
    class DecoderWorker implements Runnable {
        @Override
        public void run() {
            LogUtils.d("DecoderWorker");
            synchronized (mObject) {
                try {
                    mObject.wait();
                    mIsDecoderStarted.set(true);
                } catch (InterruptedException e) {
                    LogUtils.e(e.toString());
                }
            }
            LogUtils.d("Main Body");

            int index = mEncoderBuffer.getFirstIndex();
            if (index < 0) {
                LogUtils.e("CircularBuffer Error");
                return;
            }

            ByteBuffer encodedFrames;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (!bExit.get()) {
                encodedFrames = mEncoderBuffer.getChunk(index, info);
                encodedFrames.limit(info.size + info.offset);
                encodedFrames.position(info.offset);

                try {
                    index = mEncoderBuffer.getNextIntCustom(index);
                } catch (InterruptedException e) {
                    LogUtils.e(e.toString());
                    e.printStackTrace();
                }

                int inputBufIndex = -1;
                try {
                    if(null != mDecoder) {
                        inputBufIndex = mDecoder.dequeueInputBuffer(-1);
                    }
                } catch (IllegalStateException e){
                    LogUtils.e(e.toString());
                    e.printStackTrace();
                }

                if (inputBufIndex >= 0 && null != mDecoder) {
                    ByteBuffer inputBuf = mDecoder.getInputBuffer(inputBufIndex);
                    if (inputBuf != null) {
                        inputBuf.clear();
                        inputBuf.put(encodedFrames);
                    }
                    mDecoder.queueInputBuffer(inputBufIndex, 0, info.size,
                            info.presentationTimeUs, info.flags);
                }

                if (mIsDecoderConfigured.get() && null != mDecoder) {
                    int decoderStatus = mDecoder.dequeueOutputBuffer(info, CodecUtils.TIMEOUT_USEC);
                    if (decoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                        LogUtils.d("no output from mDecoder available");
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                        LogUtils.d("mDecoder output buffers changed");
                    } else if (decoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                        // this happens before the first frame is returned
                        MediaFormat decoderOutputFormat = mDecoder.getOutputFormat();
                        LogUtils.d("mDecoder output format changed: " +
                                decoderOutputFormat);
                    } else {
                        mDecoder.releaseOutputBuffer(decoderStatus, true);
                    }
                }
            }
        }
    }

    private int readStream(InputStream in, byte[] data, int size) {
        int readCount = 0;
        try {
            while (readCount < size) {
                readCount += in.read(data, readCount, size - readCount);
            }
        } catch (IOException e) {
            LogUtils.e(e.toString());
            e.printStackTrace();
        }
        return readCount;
    }

    class DataCollectorWorker implements Runnable {
        int mIndex = 0;

        @Override
        public void run() {
            try {
                mSocket = new Socket();
                InetSocketAddress address = new InetSocketAddress(mIp, mPort);
                mSocket.connect(address, SocketUtils.TIME_MILLIS);
                mInputStream = mSocket.getInputStream();
                int length = SocketUtils.DATA_HEADER_LENGTH;
                while (!bExit.get()) {
                    byte[] bytes = new byte[length];
                    if (length != readStream(mInputStream, bytes, length)) {
                        LogUtils.e("readStream length !=" + length);
                        break;
                    }
                    //printInfo(bytes);
                    length = getLength(SocketUtils.DATA_HEADER_LENGTH, BytesUtils.bytesToInt2(bytes, 4));
                    mDataQueue.add(bytes);
                }
            } catch (UnknownHostException e) {
                LogUtils.e( "UnknownHostException:" + e.toString());
            } catch (SocketTimeoutException e) {
                LogUtils.e( "TimeoutException:" + e.getMessage());
            } catch (IOException e) {
                LogUtils.e( "IOException:" + e.toString());
            } finally {
                if (mSocket != null && mSocket.isConnected()) {
                    try {
                        mSocket.close();
                        mSocket = null;
                    } catch (IOException e) {
                        LogUtils.e( "IOException " + e.toString());
                    }
                }
                if (mInputStream != null) {
                    try {
                        mInputStream.close();
                        mInputStream = null;
                    } catch (IOException e) {
                        LogUtils.e( "IOException " + e.toString());
                    }
                }
            }
        }

        private int getLength(int headerLength, int dataLength) {
            ++mIndex;
            if (mIndex % SocketUtils.ODD_EVEN == 0) {
                return headerLength;
            } else {
                return dataLength;
            }
        }

        private void printInfo(byte[] bytes) {
            if (mIndex % SocketUtils.ODD_EVEN == 0) {
                String infoString;
                infoString = BytesUtils.bytesToInt2(bytes, 0) + ","
                        + BytesUtils.bytesToInt2(bytes, 4) + ","
                        + BytesUtils.bytesToLong(bytes, 8) + ","
                        + BytesUtils.bytesToInt2(bytes, 16);
                LogUtils.i("receive :" + infoString);
            }
        }

        private void stop() {
            try {
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }
            } catch (IOException e) {
                LogUtils.e(e.toString());
                e.printStackTrace();
            }

            if (mInputStream != null) {
                try {
                    mInputStream.close();
                    mInputStream = null;
                } catch (IOException e) {
                    LogUtils.e(e.toString());
                    e.printStackTrace();
                }
            }

            try {
                byte[] stopSignal = {0};
                mDataQueue.add(stopSignal);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Add a new frame to the CircularBuffer
     *
     * @param encodedFrame The new frame to be added to the CircularBuffer
     * @param info         The BufferInfo object for the encodedFrame
     */
    private void setData(ByteBuffer encodedFrame, MediaCodec.BufferInfo info) {
        if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
            LogUtils.d("Configuring Decoder");
            MediaFormat format = MediaFormat.createVideoFormat(CodecUtils.MIME_TYPE, CodecUtils.WIDTH, CodecUtils.HEIGHT);
            format.setByteBuffer("csd-0", encodedFrame);
            mDecoder.configure(format, mSurfaceView.getHolder().getSurface(), null, 0);
            mDecoder.start();
            mIsDecoderConfigured.set(true);
            LogUtils.d("mDecoder configured (" + info.size + " bytes)");
            return;
        }

        mEncoderBuffer.add(encodedFrame, info.flags, info.presentationTimeUs);
        if ((info.flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0) {
            synchronized (mObject) {
                if (mIsDecoderConfigured.get() && !mIsDecoderStarted.get()) {
                    mObject.notify();
                }
            }
            LogUtils.d("First I-Frame added");
        }
    }

    class DateHandleWorker implements Runnable {
        private int mIndexData = -1;

        @Override
        public void run() {
            while (!bExit.get()) {
                try {
                    byte[] bytes = mDataQueue.take();
                    if (bytes.length == 1 && bytes[0] == 0) {
                        continue;
                    }
                    ++mIndexData;
                    if (mIndexData % SocketUtils.ODD_EVEN == 0) {
                        setBufferInfo(bytes);
                    } else {
                        ByteBuffer buffer = ByteBuffer.allocate(bytes.length);
                        buffer.put(bytes);
                        buffer.flip();
                        setData(buffer, mBufferInfo);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mDataQueue.clear();
        }

        private void printInfo(MediaCodec.BufferInfo info) {
            String infoString;
            infoString = info.offset + ","
                    + info.size + ","
                    + info.presentationTimeUs + ","
                    + info.flags;
            LogUtils.i("handle :" + infoString);
        }

        private void setBufferInfo(byte[] bytes) {
            mBufferInfo.set(BytesUtils.bytesToInt2(bytes, 0),
                    BytesUtils.bytesToInt2(bytes, 4),
                    BytesUtils.bytesToLong(bytes, 8),
                    BytesUtils.bytesToInt2(bytes, 16));
            printInfo(mBufferInfo);
        }

        private void stop() {
            try {
                byte[] stopSignal = {0, 1};
                ByteBuffer buffer = ByteBuffer.allocate(stopSignal.length);
                buffer.put(stopSignal);
                buffer.flip();
                setData(buffer, mBufferInfo);
            } catch (Exception e) {
                LogUtils.e(e.toString());
                e.printStackTrace();
            }
        }
    }

    public void dispose() {
        if (bExit.get()) {
            return;
        }
        bExit.set(true);
        mIsDecoderConfigured.set(false);
        mIsDecoderStarted.set(false);
        mDataCollectorWorker.stop();
        mDateHandleWorker.stop();

        if (mDecoder != null) {
            mDecoder.stop();
            mDecoder.release();
            mDecoder = null;
        }
        if (mExecutor != null) {
            mExecutor.shutdown();
            try {
                if (!mExecutor.awaitTermination(SocketUtils.TIME_MILLIS, TimeUnit.MILLISECONDS)) {
                    mExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                mExecutor.shutdownNow();
                Thread.currentThread().interrupt();
                LogUtils.e(e.toString());
            }
        }
    }
}
