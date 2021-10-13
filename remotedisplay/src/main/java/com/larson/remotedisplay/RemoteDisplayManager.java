package com.larson.remotedisplay;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.view.SurfaceView;

import com.larson.remotedisplay.utils.SocketUtils;

/**
 * 实现远程显示
 * 提供图像源的作为Socket服务端，通过Android L平台开始提供的MediaProject录制屏幕数据，通过MediaCodec加密，然后传给客户端
 * 显示远程图像的作为Socket客户端，接收数据，通过MediaCodec解密，提供给SurfaceView显示
 *
 * @author Administrator
 * @date 2018\9\12 0012
 **/
public class RemoteDisplayManager {
    private static RemoteClient mRemoteClient;

    /**
     * 绑定客户端的界面
     *
     * @param surfaceView 显示图像的控件
     * @param address     连接服务端的IP:prot
     * @return
     */
    public static boolean startClient(SurfaceView surfaceView, String address) {
        mRemoteClient = new RemoteClient();
        mRemoteClient.init(surfaceView, address);
        surfaceView.getHolder().addCallback(mRemoteClient);
        return true;
    }

    /**
     * 重置客户端
     * 释放Socket连接资源和数据解码相关的资源
     * @return
     */
    public static boolean resetClient() {
        if (mRemoteClient == null) {
            return false;
        }
        mRemoteClient.dispose();
        mRemoteClient.initWorkers();
        return true;
    }

    /**
     * 结束客户端
     * 释放Socket连接资源和数据解码相关的资源
     *
     * @return
     */
    public static boolean stopClient() {
        if (mRemoteClient == null) {
            return false;
        }
        mRemoteClient.dispose();
        return true;
    }

    /**
     * 启动服务端
     *
     * @param context         Activity上下文
     * @param mediaProjection 录屏需要的参数
     * @param port            Socket服务器监听的端口
     * @return
     */
    public static boolean startServer(Activity context, MediaProjection mediaProjection, int port) {
        if (!isServerStarted()) {
            RemoteServer.setMediaProjection(mediaProjection);
            RemoteServer.setProt(port);
            Intent startServerIntent = new Intent(context, RemoteServer.class);
            startServerIntent.setAction(RemoteServer.START);
            context.startService(startServerIntent);
        }
        return true;
    }

    /**
     * 启动服务端
     *
     * @param context         Activity上下文
     * @param mediaProjection 录屏需要的参数
     * @return
     */
    public static boolean startServer(Activity context, MediaProjection mediaProjection) {
        if (!isServerStarted()) {
            RemoteServer.setMediaProjection(mediaProjection);
            RemoteServer.setProt(SocketUtils.PROT);
            Intent startServerIntent = new Intent(context, RemoteServer.class);
            startServerIntent.setAction(RemoteServer.START);
            context.startService(startServerIntent);
        }
        return true;
    }

    /**
     * 停止服务端
     * 释放Socket服务资源和录屏的相关的资源
     *
     * @param context Activity上下文
     * @return
     */
    public static boolean stopServer(Activity context) {
        if (isServerStarted()) {
            Intent startServerIntent = new Intent(context, RemoteServer.class);
            startServerIntent.setAction(RemoteServer.STOP);
            context.startService(startServerIntent);
        }
        return true;
    }

    public static boolean isServerStarted() {
        return RemoteServer.isServerStarted();
    }
}
