package com.larson.remotedisplay;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.projection.MediaProjection;
import android.os.Build;
import android.os.Build.VERSION_CODES;
import android.os.Handler;
import android.os.IBinder;
import android.view.Surface;
import android.widget.Toast;

import com.larson.remotedisplay.thread.SkExecutorService;
import com.larson.remotedisplay.utils.CodecUtils;
import com.larson.remotedisplay.utils.LogUtils;
import com.larson.remotedisplay.utils.SocketUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Administrator
 */
public class RemoteServer extends Service {
    public static final String START = "SERVER_START";
    public static final String STOP = "SERVER_STOP";
    private ServerSocket mServerSocket;
    private Socket mClientSocket = null;
    private MediaCodec mEncoder = null;
    private VirtualDisplay mVirtualDisplay;
    private ExecutorService mExecutor;
    private Handler mHandler;
    private AtomicBoolean bExit;
    private Future mEncoderFuture;
    private static MediaProjection mMediaProjection;
    private static int mPort;
    private static boolean mIsServerStarted;

    public static void setMediaProjection(MediaProjection mediaProjection) {
        mMediaProjection = mediaProjection;
    }

    public static void setProt(int port) {
        mPort = port;
    }

    private class ToastRunnable implements Runnable {
        String mText;

        private ToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run() {
            Toast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT).show();
        }
    }

    private void showToast(final String message) {
        mHandler.post(new ToastRunnable(message));
    }

    public RemoteServer() {
        mPort = SocketUtils.PROT;
        mExecutor = SkExecutorService.newExecutor();
        bExit = new AtomicBoolean(false);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public static boolean isServerStarted() {
        return mIsServerStarted;
    }

    /**
     * Main Entry Point of the mServerSocket code.
     * Create a WebSocket mServerSocket and start the mEncoder.
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null || STOP.equals(intent.getAction())) {
            LogUtils.d("RemoteServer STOP");
            mIsServerStarted = false;
            dispose();
            return START_NOT_STICKY;
        }

        if (mServerSocket == null && START.equals(intent.getAction())) {
            LogUtils.d("RemoteServer START");
            try {
                mServerSocket = new ServerSocket(mPort == 0 ? SocketUtils.PROT : mPort);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mExecutor.submit(new SocketServerWorker());
            mHandler = new Handler();
            mIsServerStarted = true;
        }
        return START_NOT_STICKY;
    }

    private void startEncoderWorker(Socket socket) {
        mClientSocket = socket;
        mEncoderFuture = mExecutor.submit(new EncoderWorker());
        showToast("connect client");
    }

    private class SocketServerWorker implements Runnable {
        @Override
        public void run() {
            while (!bExit.get()) {
                try {
                    if (mServerSocket != null) {
                        LogUtils.d("mServerSocket : " + SocketUtils.PROT);
                        Socket socket = mServerSocket.accept();
                        LogUtils.d("mServerSocket : " + socket);
                        closeClientSocket();
                        closeEncoderWorker();
                        startEncoderWorker(socket);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Create the display surface out of the mEncoder. The data to mEncoder will be fed from this
     * Surface itself.
     *
     * @return Surface
     * @throws IOException creat encoder failed
     *
     */
    @TargetApi(VERSION_CODES.LOLLIPOP)
    private Surface createDisplaySurface() throws IOException {
        MediaFormat mMediaFormat = MediaFormat.createVideoFormat(CodecUtils.MIME_TYPE, CodecUtils.WIDTH, CodecUtils.HEIGHT);
        mMediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, CodecUtils.BIT_RATE);
        mMediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, CodecUtils.FRAME_RATE);
        mMediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
        mMediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, CodecUtils.IFRAME_INTERVAL);
        mEncoder = MediaCodec.createEncoderByType(CodecUtils.MIME_TYPE);
        mEncoder.configure(mMediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        return mEncoder.createInputSurface();
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    public void startDisplayManager() {
        DisplayManager mDisplayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        if(mDisplayManager == null) {
            return;
        }
        Surface encoderInputSurface = null;
        try {
            encoderInputSurface = createDisplaySurface();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mMediaProjection != null) {
                mVirtualDisplay = mMediaProjection.createVirtualDisplay("skyruler minicap",
                        CodecUtils.WIDTH, CodecUtils.HEIGHT, 50,
                        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                        encoderInputSurface, null, null);
            } else {
                showToast("Something went wrong. Please restart the app.");
            }
        } else {
            mVirtualDisplay = mDisplayManager.createVirtualDisplay("skyruler minicap", CodecUtils.WIDTH, CodecUtils.HEIGHT, 50,
                    encoderInputSurface,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC | DisplayManager.VIRTUAL_DISPLAY_FLAG_SECURE);
        }
        LogUtils.d("Starting mEncoder");
        mEncoder.start();
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    private class EncoderWorker implements Runnable {
        @Override
        public void run() {

            startDisplayManager();

            ByteBuffer[] encoderOutputBuffers = mEncoder.getOutputBuffers();
            boolean encoderDone = false;
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            while (!encoderDone && !bExit.get()) {

                int encoderStatus;
                try {
                    encoderStatus = mEncoder.dequeueOutputBuffer(info, CodecUtils.TIMEOUT_USEC);
                } catch (IllegalStateException e) {
                    e.printStackTrace();
                    break;
                }

                if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                    //LogUtils.d("Encoder info try again later");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                    encoderOutputBuffers = mEncoder.getOutputBuffers();
                    LogUtils.d("Encoder output buffers changed");
                } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat newFormat = mEncoder.getOutputFormat();
                    LogUtils.d("Encoder output format changed: " + newFormat);
                } else if (encoderStatus < 0) {
                    LogUtils.e("Encoder output encoderStatus < 0");
                    break;
                } else {
                    ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                    if (encodedData == null) {
                        LogUtils.d("encoderOutputBuffers It's NULL. break!");
                        return;
                    }
                    sendDataToClient(info, encodedData);
                    encoderDone = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
                    try {
                        mEncoder.releaseOutputBuffer(encoderStatus, false);
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void sendDataToClient(MediaCodec.BufferInfo info, ByteBuffer encodedData) {
        try {
            OutputStream outputStream = mClientSocket.getOutputStream();

            //send header
            ByteBuffer buffer = ByteBuffer.allocate(SocketUtils.DATA_HEADER_LENGTH);
            buffer.putInt(info.offset);
            buffer.putInt(info.size);
            buffer.putLong(info.presentationTimeUs);
            buffer.putInt(info.flags);
            byte[] headerBytes = new byte[buffer.position()];
            buffer.flip();
            buffer.get(headerBytes);
            buffer.clear();
            outputStream.write(headerBytes);

            //send data
            byte[] dataBytes = new byte[info.size];
            if (info.size != 0) {
                encodedData.limit(info.offset + info.size);
                encodedData.position(info.offset);
                encodedData.get(dataBytes, info.offset, info.offset + info.size);
                outputStream.write(dataBytes);
            }

            //print
            printInfo(info);

        } catch (IOException e) {
            e.printStackTrace();
            closeClientSocket();
        } catch (BufferUnderflowException e) {
            e.printStackTrace();
        }
    }

    private void printInfo(MediaCodec.BufferInfo info) {
        String infoString;
        infoString = info.offset + ","
                + info.size + ","
                + info.presentationTimeUs + ","
                + info.flags;
        LogUtils.i("send :" + infoString);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dispose();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void dispose() {
        if(!bExit.get()) {
            return;
        }

        bExit.set(true);
        closeClientSocket();
        closeEncoderWorker();
        closeServerSocket();
        closeExecutorService();
        stopSelf();
    }

    @TargetApi(VERSION_CODES.LOLLIPOP)
    private void closeEncoderWorker() {

        if (mEncoderFuture != null) {
            mEncoderFuture.cancel(true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mVirtualDisplay != null) {
                mVirtualDisplay.release();
            }
        }
        if (mEncoder != null) {
            mEncoder.signalEndOfInputStream();
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
    }

    private void closeClientSocket() {
        try {
            if (mClientSocket != null) {
                mClientSocket.close();
                mClientSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeServerSocket() {

        try {
            if (mServerSocket != null) {
                mServerSocket.close();
                mServerSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeExecutorService() {

        if (mExecutor != null) {
            mExecutor.shutdown();
            try {
                if (!mExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                    mExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                mExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
