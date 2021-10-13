package larson.groupcontrol.app;

import android.net.LocalSocketAddress;

import larson.groupcontrol.app.connection.ConnectOption;
import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.listener.IMessageListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
public class SocketConfiguration {
    /**
     * 连接本地socket（{@link android.net.LocalSocket}）需要用到的Socket名字
     */
    private final String socketName;
    /**
     * 连接本地socket的命名空间
     */
    private final LocalSocketAddress.Namespace nameSpace;
    /**
     * 是控制機還是測試機
     */
    private boolean isServer;

    private final ConnectOption skSocketOption;

    /**
     * 静态消息监听器
     */
    private final Map<MessageFilter, IMessageListener> mWrappers;

    SocketConfiguration(Builder builder) {
        this.nameSpace = builder.nameSpace == null ? LocalSocketAddress.Namespace.ABSTRACT : builder.nameSpace;
        this.socketName = builder.socketName;
        this.skSocketOption = builder.skSocketOption;
        this.isServer = builder.isServer;
        this.mWrappers = builder.wrappers;
    }

    public String getSocketName() {
        return socketName;
    }

    public boolean isServer() {
        return isServer;
    }

    public boolean isClient() {
        return !isServer;
    }

    public LocalSocketAddress.Namespace getNameSpace() {
        return nameSpace;
    }

    public Map<MessageFilter, IMessageListener> getWrappers() {
        return mWrappers;
    }

    public ConnectOption getSkSocketOption() {
        return skSocketOption;
    }

    public static class Builder {
        String socketName;
        LocalSocketAddress.Namespace nameSpace;
        Map<MessageFilter, IMessageListener> wrappers = new ConcurrentHashMap<>();
        ConnectOption skSocketOption;
        boolean isServer;

        public Builder(boolean isServer, String socketName) {
            this.isServer = isServer;
            this.socketName = socketName;
        }

        public Builder setNameSpace(LocalSocketAddress.Namespace nameSpace) {
            this.nameSpace = nameSpace;
            return this;
        }


        public Builder addMessageListener( MessageFilter messageFilter,IMessageListener listener) {
            if (wrappers == null) {
                throw new NullPointerException("Message listener is null.");
            }
            wrappers.put(messageFilter, listener);
            return this;
        }


        public Builder setSkSocketOption(ConnectOption skSocketOption) {
            this.skSocketOption = skSocketOption;
            return this;
        }

        public SocketConfiguration build() {
            return new SocketConfiguration(this);
        }
    }

    @Override
    public String toString() {
        return "socketName=" + socketName + ",nameSpace=" + nameSpace;
    }
}
