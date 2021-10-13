package larson.groupcontrol.app.listener;


import larson.groupcontrol.app.connection.MessageCollector;
import larson.groupcontrol.app.message.Message;

/**
 * 提供一种通过选择器监听消息的机制，这样有利于基于事件编程，每一个新的消息到来时
 * 就会调用{@link #processMessage(Message)}这样有利于从{@link
 * MessageCollector}获取阻塞的结果
 * <p>
 * Provides a mechanism to listen for messages that pass a specified filter. This allows event-style
 * programming -- every time a new message is found, the {@link #processMessage(Message)} method
 * will be called. This is the opposite approach to the functionality provided by a {@link
 * MessageCollector} which lets you block while waiting for results.
 *
 * @author Larsonzhong (larsonzhong@163.com)
 * @since 2017-12-07 11:14
 */
public interface IMessageListener {
    /**
     * Process the next message sent to this message listener.
     * <p>
     * A single thread is responsible for invoking all listeners, so it's very important that
     * implementation of this method not block for any extended period of time.
     *
     * @param msg the message to process
     */
    void processMessage(Message msg);
}
