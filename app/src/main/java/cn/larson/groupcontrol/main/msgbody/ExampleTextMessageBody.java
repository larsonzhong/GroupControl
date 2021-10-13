package cn.larson.groupcontrol.main.msgbody;

import larson.groupcontrol.app.message.intf.BaseMessageBody;

/**
 * ......................-~~~~~~~~~-._       _.-~~~~~~~~~-.
 * ............... _ _.'              ~.   .~              `.__
 * ..............'//     NO           \./      BUG         \\`.
 * ............'//                     |                     \\`.
 * ..........'// .-~"""""""~~~~-._     |     _,-~~~~"""""""~-. \\`.
 * ........'//.-"                 `-.  |  .-'                 "-.\\`.
 * ......'//______.============-..   \ | /   ..-============.______\\`.
 * ....'______________________________\|/______________________________`.
 * ..larsonzhong@163.com      created in 2018/8/17     @author : larsonzhong
 * 这是一个示例message body
 */
public class ExampleTextMessageBody  extends BaseMessageBody {
    private final String text;
    public static final short ID = 0x0027;
    public static final short ID_SYNC = 0x0028;

    ExampleTextMessageBody(Builder builder) {
        this.text = builder.text;
        this.bodyBytes = builder.bodyBytes;
    }

    @Override
    public String toString() {
        return text;
    }

    public String getText() {
        return text;
    }

    @Override
    public byte[] getBodyBytes() {
        return bodyBytes;
    }

    public static class Builder  extends BaseMessageBody.Builder{
        private String text;

        public Builder text(String text) {
            this.text = text;
            return this;
        }

        public ExampleTextMessageBody build() {
            this.bodyBytes = text.getBytes();
            //如果有多个字段，则使用数组组装把属性组装成数组封装到bodyBytes
            return new ExampleTextMessageBody(this);
        }

    }

}
