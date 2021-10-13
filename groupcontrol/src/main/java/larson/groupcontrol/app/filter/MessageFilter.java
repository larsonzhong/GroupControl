package larson.groupcontrol.app.filter;

import larson.groupcontrol.app.listener.IMessageListener;
import larson.groupcontrol.app.message.Message;
import larson.groupcontrol.app.connection.MessageCollector;

/**
 * Defines a way to filter messages for particular attributes. Message filters are used when
 * constructing message listeners or collectors -- the filter defines what messages match the
 * criteria of the collector or listener for further message processing.
 * <p>
 * Several simple filters are pre-defined. These filters can be logically combined for more complex
 * message filtering by using the {@link AndFilter} and {@link OrFilter}. It's also possible to
 * define your own filters by implementing this interface. The code example below creates a trivial
 * filter for messages with a specific ID (real code should use {@link MessageIdFilter} instead).
 * <p>
 * <pre>
 *   // Use an an anonymous inner class to define a message filter that returns all messages that
 *   // have a message ID of 0x0000
 *   MessageFilter myFilter = new MessageFilter() {
 *     @Override
 *     public boolean accept(short id) {
 *       return 0x0000 == id;
 *     }
 *   };
 * </pre>
 *
 * @author Larsonzhong (larsonzhong@163.com)
 * @since 2017-12-07 11:14
 *
 * @see {@link MessageCollector}
 * @see {@link IMessageListener}
 */
public interface MessageFilter {

    /**
     * Tests whether or not the specified message should pass the filter.
     *
     * @param msg the message to test
     * @return true if and only if <tt>msg</tt> passes the filter
     */
    boolean accept(Message msg);
}