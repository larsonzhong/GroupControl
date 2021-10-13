package larson.groupcontrol.app.message.intf;

/**
 * ......................-~~~~~~~~~-._       _.-~~~~~~~~~-.
 * ............... _ _.'              ~.   .~              `.__
 * ..............'//     NO           \./      BUG         \\`.
 * ............'//                     |                     \\`.
 * ..........'// .-~"""""""~~~~-._     |     _,-~~~~"""""""~-. \\`.
 * ........'//.-"                 `-.  |  .-'                 "-.\\`.
 * ......'//______.============-..   \ | /   ..-============.______\\`.
 * ....'______________________________\|/______________________________`.
 * ..larsonzhong@163.com      created in 2018/8/22     @author : larsonzhong
 */
public abstract class BaseMessageBody {
    protected byte[] bodyBytes;

    /**
     * 返回改body组装成的byte数组
     *
     * @return body
     */
    public abstract byte[] getBodyBytes();

    public static class Builder {
        public byte[] bodyBytes;
    }
}
