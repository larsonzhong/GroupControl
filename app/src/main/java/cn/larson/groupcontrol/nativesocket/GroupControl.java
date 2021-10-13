package cn.larson.groupcontrol.nativesocket;

/**
 * @author Tag
 * @date 2018/11/07
 */
public class GroupControl {
    private volatile static GroupControl mInstance;

    static {
        System.loadLibrary("GroupControlJni");
    }

    public static GroupControl getInstance() {
        if (mInstance == null) {
            synchronized (GroupControl.class) {
                if (mInstance == null) {
                    mInstance = new GroupControl();
                }
            }
        }
        return mInstance;
    }

    private GroupControl(){}

    /**
     * 启动控制端
     * localServerName  --- 本地Socket服务名字
     * remoteServerPort --- 远程Socket服务端口
     * isRoot           --- 手机是否ROOT
     * @return 1：启动成功；-1：启动失败，本地服务失败；-2：启动失败，远程服务失败
     */
    public native int startServer(String localServerName, int remoteServerPort, boolean isRoot);

    /**
     * 停止控制端
     * @return 0：停止成功
     */
    public native int stopServer();

    /**
     * 控制端是否已经启动
     * @return 1：启动；-1：没有启动
     */
    public native int isServerStart();

    /**
     * 启动测试端
     * localServerName  --- 本地Socket服务名字
     * remoteServerIp   --- 远程Socket服务IP
     * remoteServerPort --- 远程Socket服务端口
     * isRoot           --- 手机是否ROOT
     * @return 1：启动成功；-1：启动失败，本地服务失败；-2：启动失败，远程客户端失败
     */
    public native int startClient(String localServerName, String remoteServerIp, int remoteServerPort, boolean isRoot);

    /**
     * 停止测试端
     * @return 0：停止成功；-1：表示正在连接，停止失败
     */
    public native int stopClient();

    /**
     * 测试端是否已经启动
     * @return 1：启动成功，并连接远程；2：启动，但没有连接远程；-1：没有启动
     */
    public native int isClientStart();
}
