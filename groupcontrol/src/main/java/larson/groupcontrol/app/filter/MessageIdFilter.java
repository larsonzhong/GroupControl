package larson.groupcontrol.app.filter;


import larson.groupcontrol.app.message.Message;

/**
 * 通过MessageID过滤消息类型
 *
 * Filters for messages with a particular message ID.
 *
 * @author Larsonzhong (larsonzhong@163.com)
 * @since 2017-12-07 11:14
 */
public class MessageIdFilter implements MessageFilter{
    private short mId;

    /**
     * Creates a new message ID filter using the specified message ID.
     *
     * @param id the message ID to filter for
     */
    public MessageIdFilter(short id) {
        mId = id;
    }

    @Override
    public boolean accept(Message msg) {
        return mId == msg.getMsgId();
    }

    @Override
    public String toString() {
        return "MessageIdFilter by ID: " + mId;
    }
}
