package cn.larson.groupcontrol.main;

import android.annotation.TargetApi;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.Binder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.vivo.api.zxwy.interfaces.DeviceAssistant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import cn.larson.groupcontrol.main.utils.AssetUtils;
import cn.larson.groupcontrol.main.utils.PackageVersion;
import larson.groupcontrol.app.util.ExecUtils;
import larson.groupcontrol.app.util.LogUtils;
import cn.larson.groupcontrol.nativesocket.GroupControl;

/**
 * @author Rony
 * @date 2018/8/27
 */

public class GroupControlService extends Service {
    private static final String GROUP_CONTROL_SERVER = "groupControlServer";
    private static final String GROUP_CONTROL_CLIENT = "groupControlClient";
    private static final int FIND_PHONE_FLASH_TIMES = 30;
    private boolean isVivo;
    private boolean isRoot;
    private boolean isNewVersion;
    private ExecutorService mExecutor;
    private String mServerPath;
    private String mClientPath;
    private ExecUtils mExecUtils;
    private boolean mInitCacheFile;
    private static FindPhoneBroadcastReceiver mFindPhoneBroadcastReceiver;
    private ScheduledExecutorService mTimeExecutor;
    private int mCounts;

    private class FindPhoneBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            LogUtils.d("cn.larson.groupcontrol.FIND_PHONE");

            mCounts = 0;
            if (mTimeExecutor != null && !mTimeExecutor.isShutdown()) {
                mTimeExecutor.shutdownNow();
            }

            mTimeExecutor = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
                private final AtomicInteger integer = new AtomicInteger();

                @Override
                public Thread newThread(@NonNull Runnable r) {
                    return new Thread(r, "FindPhone thread: " + integer.getAndIncrement());
                }
            });
            mTimeExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    if (++mCounts > FIND_PHONE_FLASH_TIMES) {
                        if (mTimeExecutor != null && !mTimeExecutor.isShutdown()) {
                            mTimeExecutor.shutdownNow();
                        }
                    }
                    if (isVivo) {
                        DeviceAssistant deviceAssistant = new DeviceAssistant();
                        deviceAssistant.runCmdAsRoot("input keyevent KEYCODE_POWER");
                    }
                }
            }, 0, 1, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onCreate() {
        isVivo = Build.MODEL.startsWith("vivo");
        isRoot = ExecUtils.isCanRoot();
        if (isRoot) {
            mExecUtils = ExecUtils.getInstance();
        }
        isNewVersion = PackageVersion.isNewVersion(getApplicationContext());
        mExecutor = newExecutor();
        mInitCacheFile = false;
        super.onCreate();
        LogUtils.d("server onCreate! isNewVersion:" + isNewVersion);

        // 注册查找手机的Receiver用来接收来自C层的查找指令
        mFindPhoneBroadcastReceiver = new FindPhoneBroadcastReceiver();
        registerReceiver(mFindPhoneBroadcastReceiver,
                new IntentFilter("cn.larson.groupcontrol.FIND_PHONE"));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtils.d("server onStartCommand!");
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mFindPhoneBroadcastReceiver);
        if (mTimeExecutor != null && !mTimeExecutor.isShutdown()) {
            mTimeExecutor.shutdownNow();
        }
        mExecutor.shutdown();
        if (isRoot && mExecUtils != null) {
            mExecUtils.shutdown();
        }

        LogUtils.d("server onDestroy!");
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtils.d("server onUnbind!");
        return super.onUnbind(intent);
    }

    void initCacheFile(boolean isServer) throws Exception {
        File serverFile = new File(getCacheDir(), GROUP_CONTROL_SERVER);
        File clientFile = new File(getCacheDir(), GROUP_CONTROL_CLIENT);
        if (isNewVersion) {
            stopThreadByName(serverFile.getAbsolutePath());
            stopThreadByName(clientFile.getAbsolutePath());
        }
        mServerPath = AssetUtils.getAssetsCacheFile(getApplicationContext(), GROUP_CONTROL_SERVER, isNewVersion);
        mClientPath = AssetUtils.getAssetsCacheFile(getApplicationContext(), GROUP_CONTROL_CLIENT, isNewVersion);
        mInitCacheFile = true;
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
    private String execCommand(String cmd) throws RemoteException {
        String result;
        if (isVivo) {
            try {
                result = ExecUtils.exec(cmd);
            } catch (IOException e) {
                return "";
            }
        } else if (isRoot) {
            try {
                result = mExecUtils.execRoot(cmd);
            } catch (IOException e) {
                return "";
            } catch (InterruptedException e) {
                return "";
            }
        } else {
            throw new RemoteException("Phone is not Vivo or not root");
        }
        return result;
    }

    private ArrayList<Integer> getProcessPids(String serverName) {
        String result;
        ArrayList<Integer> pids = new ArrayList<>();
        int startIndex = serverName.lastIndexOf('/') + 1;
        String simpleServerName = serverName.substring(startIndex, serverName.length());
        try {
            String prefix = "bad pid";
            result = execCommand("ps | grep " + simpleServerName);
            if (!TextUtils.isEmpty(result) || result.startsWith(prefix)) {
                result = execCommand("ps");
            }
            if (TextUtils.isEmpty(result) || !result.contains(simpleServerName)) {
                result = execCommand("ps -A | grep " + simpleServerName);
                if (!TextUtils.isEmpty(result) || result.startsWith(prefix)) {
                    result = execCommand("ps -A");
                }
            }
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
        }
        if (TextUtils.isEmpty(result) || !result.contains(simpleServerName)) {
            return null;
        }
        String[] strings = result.split("[\r]|[\n]");
        for (String string : strings) {
            if (string.contains(simpleServerName)) {
                String[] split = string.split("[ ]|[\t]");
                int index = 0;
                for (String s : split) {
                    if (TextUtils.isEmpty(s)) {
                        continue;
                    }
                    if (++index == 2) {
                        pids.add(Integer.parseInt(s));
                    }
                }
            }
        }
        return pids;

    }

    private int getProcessPid(String serverName) {
        ArrayList<Integer> pids = getProcessPids(serverName);
        if (pids == null || pids.size() == 0) {
            return -1;
        }
        return pids.get(0);
    }

    private void stopThreadByName(String name) throws RemoteException {
        ArrayList<Integer> pids = getProcessPids(name);
        if (pids != null && pids.size() != 0) {
            for (Integer serverPid : pids) {
                if (serverPid > 0) {
                    execCommand("kill -9 " + serverPid);
                }
            }
        }
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

    void runGroupControl(final String cmd) {
        if (TextUtils.isEmpty(cmd)) {
            return;
        }
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    execCommand(cmd);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private LocalBinder binder = new LocalBinder();

    /**
     * 创建Binder对象，返回给客户端即Activity使用，提供数据交换的接口
     */
    public class LocalBinder extends Binder {
        GroupControlService getService() {
            return GroupControlService.this;
        }
    }

    /**
     * 把Binder类返回给客户端
     */
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public int startServer(String localServerName, int remoteServerPort) throws RemoteException {
        if (isVivo) {
            return GroupControl.getInstance().startServer(localServerName, remoteServerPort, isRoot);
        }
        if (!mInitCacheFile) {
            try {
                initCacheFile(true);
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
        if (TextUtils.isEmpty(mServerPath)) {
            return -1;
        }
        int pid = getProcessPid(mServerPath);
        if (pid < 0) {
            String cmd = mServerPath + " -n " + localServerName + " -p " + remoteServerPort;
            if (isRoot) {
                cmd = cmd + " -b";
            }
            runGroupControl(cmd);
            SystemClock.sleep(100);
            pid = getProcessPid(mServerPath);
        }
        return pid;
    }

    public int stopServer(int pid) throws RemoteException {
        if (isVivo) {
            return GroupControl.getInstance().stopServer();
        }
        if (pid > 0) {
            execCommand("kill -9 " + pid);
        }
        if (TextUtils.isEmpty(mServerPath)) {
            return -1;
        }
        stopThreadByName(mServerPath);
        return 0;
    }

    public int isServerStart() {
        if (isVivo) {
            return GroupControl.getInstance().isServerStart();
        }
        if (TextUtils.isEmpty(mServerPath)) {
            return -1;
        }
        return getProcessPid(mServerPath);
    }

    public int startClient(String localServerName, String remoteServerIp, int remoteServerPort) throws RemoteException {
        if (isVivo) {
            return GroupControl.getInstance().startClient(localServerName, remoteServerIp, remoteServerPort, isRoot);
        }
        if (!mInitCacheFile) {
            try {
                initCacheFile(false);
            } catch (Exception e) {
                e.printStackTrace();
                return -1;
            }
        }
        if (TextUtils.isEmpty(mClientPath)) {
            return -1;
        }
        int pid = getProcessPid(mClientPath);
        if (pid < 0) {
            String cmd = mClientPath + " -n " + localServerName + " -s " + remoteServerIp + " -p " + remoteServerPort;
            if (isRoot) {
                cmd = cmd + " -b";
            }
            runGroupControl(cmd);
            SystemClock.sleep(100);
            pid = getProcessPid(mClientPath);
        }
        return pid;
    }

    public int stopClient(int pid) throws RemoteException {
        if (isVivo) {
            return GroupControl.getInstance().stopClient();
        }
        if (pid > 0) {
            execCommand("kill -9 " + pid);
        }
        if (TextUtils.isEmpty(mClientPath)) {
            return -1;
        }
        stopThreadByName(mClientPath);
        return 0;
    }

    public int isClientStart() throws RemoteException {
        if (isVivo) {
            return GroupControl.getInstance().isClientStart();
        }
        if (TextUtils.isEmpty(mClientPath)) {
            return -1;
        }
        return getProcessPid(mClientPath);
    }

}
