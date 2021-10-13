package larson.groupcontrol.app.connection;

import larson.groupcontrol.app.filter.MessageFilter;
import larson.groupcontrol.app.listener.IMessageListener;
import larson.groupcontrol.app.message.Message;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * 在需要等待请求结果的时候，使用MessageCollector比使用{@link IMessageListener}要好
 *
 * Provides a mechanism to collect messages into a result queue that pass a specified filter. The
 * collector lets you perform blocking and polling operations on the result queue. So, a
 * MessageCollector is more suitable to use than a {@link IMessageListener} when you need to wait for
 * a specific result.
 * <p>
 * Each message collector will queue up a configured number of messages for processing before older
 * messages are automatically dropped.
 *
 * @author Larsonzhong (larsonzhong@163.com)
 * @since 2017-12-07 11:14
 */
public class MessageCollector {

    private final PacketRouter packetRouter;
    private final MessageFilter filter;
    private BlockingQueue<Message> mQueue;

    private boolean mCancelled = false;

    MessageCollector(PacketRouter router, MessageFilter filter) {
        this.packetRouter = router;
        this.filter = filter;
        mQueue = new ArrayBlockingQueue<>(500);
    }


    /**
     * Processes a message to see if it meets the criteria for this message collector. If so, the
     * message is added to the result queue.
     *
     * @param msg the message to process
     */
    public void processMessage(Message msg) {
        if (msg == null) {
            return;
        }

        if(filter==null||filter.accept(msg)){
            //offer方法在添加元素时，如果发现队列已满无法添加的话，会直接返回false
            //因此要及时清理队列以免真正阻塞，导致无法往队列添加新的结果（message，Collector是用来收集结果用的）
            while (!mQueue.offer(msg)) {
                // Since we know the queue is full, this poll should never actually block
                mQueue.poll();
            }
        }
    }


    /**
     * 超时没有获取到指定消息，则返回null
     *
     * Returns the next available message. The method call will block (not return) until a message is
     * available or the <tt>timeout</tt> has elapsed. If the time out elapses without a result, {@code
     * null} will be returned.
     *
     * @param timeout the amount of time to wait for the next packet (in milliseconds)
     * @return the next available message
     */
    public Message nextResult(long timeout) {
        try {
            return mQueue.poll(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 执行完成调用Cancel，因为这个Collector没用了，并从connection中移除这个collector
     *
     * Explicitly cancels the message collector so that no more results are queued up. Once a message
     * collector has been cancelled, it cannot be re-enabled. Instead, a new message collector must be
     * created.
     */
    public void cancel() {
        // If the message collector has already been cancelled, do nothing
        if (!mCancelled) {
            mCancelled = true;
            packetRouter.removeMessageCollector(this);
        }
    }
}

