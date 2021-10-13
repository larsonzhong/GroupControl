// IGroupControlInterface.aidl
package cn.skyruler.groupcontrol;

// Declare any non-default types here with import statements

interface IGroupControlInterface {
    int startServer(String localServerName, int remoteServerPort);
    int stopServer(int pid);
    int isServerStart();
    int startClient(String localServerName, String remoteServerIp, int remoteServerPort);
    int stopClient(int pid);
    int isClientStart();
}
