package larson.groupcontrol.app.connection;

import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.listener.IMessageListener;
import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.packet.Packet;
import larson.groupcontrol.app.util.LogUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

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
 * 包分发器
 */
public class PacketRouter {
    /**
     * collection of MessageCollectors which collects messages for a specified filter and perform
     * blocking and polling operations on the result queue.
     */
    private final Collection<MessageCollector> mCollectors = new ConcurrentLinkedQueue<>();
    /**
     * List of MessageListeners that will be notified when a new message was received
     */
    private final Map<MessageFilter, ListenerWrapper> mRcvListeners = new ConcurrentHashMap<>();


    /**
     * Creates a new message collector for this connection. A message filter determines which messages
     * will be accumulated by the collector. A MessageCollector is more suitable to use than a {@link
     * IMessageListener} when you need to wait for a specific result.
     *
     * @param filter the message filter to use
     * @return a new message collector
     */
    public MessageCollector createMessageCollector(MessageFilter filter) {
        MessageCollector collector = new MessageCollector(this, filter);
        // Add the collector to the list of active collectors
        mCollectors.add(collector);
        return collector;
    }

    /**
     * Get the collection of all message collectors for this connection.
     *
     * @return a collection of message collectors for this connection
     */
    public Collection<MessageCollector> getCollectors() {
        return mCollectors;
    }

    /**
     * Removes a message collector of this connection.
     *
     * @param collector a message collector which was created for this connection
     */
    void removeMessageCollector(MessageCollector collector) {
        mCollectors.remove(collector);
    }


    /**
     * A wrapper class to associate a message filter with a listener.
     */
    public static class ListenerWrapper {

        private IMessageListener listener;
        private MessageFilter filter;

        /**
         * Creates a class which associates a message filter with a listener.
         *
         * @param listener the message listener
         * @param filter   the associated filter or {@code null} if it listen for all messages
         */
        public ListenerWrapper(MessageFilter filter, IMessageListener listener) {
            this.listener = listener;
            this.filter = filter;
        }

        /**
         * Notify and process the message listener if the filter matches the message .
         * if filter is null ,listener process all message
         *
         * @param msg the message which was sent or received
         */
        public void notifyListener(Message msg) {
            if (this.filter == null || this.filter.accept(msg)) {
                listener.processMessage(msg);
            }
        }
    }

    /**
     * Registers a message listener with this connection. A message filter determines which messages
     * will be delivered to the listener. If the same message listener is added again with a different
     * filter, only the new filter will be used.
     *
     * @param listener the message listener to notify of new received messages
     * @param filter   the message filter to use
     */
    public void addRcvListener(MessageFilter filter, IMessageListener listener) {
        if (listener == null) {
            throw new NullPointerException("Message listener is null.");
        }
        ListenerWrapper wrapper = new ListenerWrapper(filter, listener);
        mRcvListeners.put(filter, wrapper);
    }

    /**
     * Get a map of all message listeners for received messages of this connection.
     *
     * @return a map of all message listeners for received messages
     */
    Map<MessageFilter, ListenerWrapper> getRcvListeners() {
        return mRcvListeners;
    }

    /**
     * Removes a message listener for received messages from this connection.
     *
     * @param listener the message listener to remove
     */
    public void removeRcvListener(IMessageListener listener) {
        mRcvListeners.remove(listener);
    }

    private void handlerMessage(Message message) {
        LogUtils.d(Thread.currentThread().getName()+":handle message :" + message.toString());
        for (MessageCollector collector : mCollectors) {
            collector.processMessage(message);
        }

        if (message.isResponse()) {
            LogUtils.d("同步消息不需要listener处理");
            return;
        }

        for (ListenerWrapper wrapper : mRcvListeners.values()) {
            wrapper.notifyListener(message);
        }
    }

    private Map<String, List<Packet>> packetMap = new HashMap<>();

    public void onDataReceive(byte[] data, int packageSize) {
        Packet packet = new Packet(data);
        if (!packet.isLargeMsg()) {
            Packet[] packets = {packet};
            Message message;
            try {
                message = new Message.Builder(packets).build();
                handlerMessage(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            String packetKey = packet.getPacketHeader().getPid() + "_"
                    + packet.getSn() + "_" + packet.getMsgId() + "_"
                    + (packet.isResponseMsg() ? "response" : "normal");
            short count = packet.getSubCount();
            List<Packet> packets = packetMap.get(packetKey);
            if (packets == null || packets.size() == 0) {
                packets = new ArrayList<>(count);
                packetMap.put(packetKey, packets);
            }
            packets.add(packet);
            if (packets.size() >= count) {
                Packet[] packetArray = packets.toArray(new Packet[packets.size()]);
                Message message;
                try {
                    message = new Message.Builder(packetArray).build();
                    handlerMessage(message);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                packetMap.remove(packetKey);
            }
        }
    }

    /**
     * 清理资源
     */
    public void clear() {
        mCollectors.clear();
        mRcvListeners.clear();
        packetMap.clear();
    }
}
