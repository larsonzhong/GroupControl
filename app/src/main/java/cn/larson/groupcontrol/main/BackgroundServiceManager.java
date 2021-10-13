package cn.larson.groupcontrol.main;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

import larson.groupcontrol.app.util.LogUtils;

/**
 * @author Rony
 * @date 2018/8/29
 */

public class BackgroundServiceManager {

    public static final int BINDER_FAILED = 0;
    public static final int BINDER_SUCCEED = 1;
    private final Context mContext;
    private final GroupControlServerListener mListener;
    private GroupControlService mService;
    private boolean isBinder;

    private ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            GroupControlService.LocalBinder binder = (GroupControlService.LocalBinder) service;
            mService = binder.getService();
            if (mListener != null) {
                mListener.onBinderState(BINDER_SUCCEED);
            }
            LogUtils.d("onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mService = null;
            if (mListener != null) {
                mListener.onBinderState(BINDER_FAILED);
            }
            LogUtils.d("onServiceDisconnected");
        }
    };

    public BackgroundServiceManager(Context context, GroupControlServerListener listener) {
        mContext = context;
        isBinder = false;
        mListener = listener;
    }

    public boolean bindService() {
        final Intent intent = new Intent(mContext, GroupControlService.class);
        isBinder = mContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LogUtils.d("bindService " + isBinder);
        return isBinder;
    }

    public void unbind() {
        if (isBinder) {
            mContext.unbindService(mServiceConnection);
        }
    }

    public int startServer(String localServerName, int remoteServerPort) {
        if (mService != null) {
            try {
                int pid = mService.isClientStart();
                if (pid > 0 || pid == -2) {
                    mService.stopClient(pid);
                }
                pid = mService.isServerStart();
                if (pid > 0) {
                    return pid;
                }
                return mService.startServer(localServerName, remoteServerPort);
            } catch (RemoteException e) {
                e.printStackTrace();
                return -1;
            }
        }
        return -1;
    }

    public int stopServer(int pid) {
        if (mService != null) {
            try {
                mService.stopServer(pid);
            } catch (RemoteException e) {
                e.printStackTrace();
                return -1;
            }
        }
        return 0;
    }

    public int startClient(String localServerName, String remoteServerIp, int remoteServerPort) {
        if (mService != null) {
            try {
                int pid = mService.isServerStart();
                if (pid > 0) {
                    mService.stopServer(pid);
                }
                pid = mService.isClientStart();
                if (pid > 0) {
                    return pid;
                }
                return mService.startClient(localServerName, remoteServerIp, remoteServerPort);
            } catch (RemoteException e) {
                e.printStackTrace();
                return -1;
            }
        }
        return 0;
    }

    public int stopClient(int pid) {
        if (mService != null) {
            try {
                mService.stopClient(pid);
            } catch (RemoteException e) {
                e.printStackTrace();
                return -1;
            }
        }
        LogUtils.d("stopClient finish ");
        return 0;
    }

    public interface GroupControlServerListener {
        /**
         * 返回服务状态
         *
         * @param state 0---绑定失败， 1---绑定成功
         */
        void onBinderState(int state);
    }
}
