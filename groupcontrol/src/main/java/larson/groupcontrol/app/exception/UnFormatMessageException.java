package larson.groupcontrol.app.exception;

/**
 * ......................-~~~~~~~~~-._       _.-~~~~~~~~~-.
 * ............... _ _.'              ~.   .~              `.__
 * ..............'//     NO           \./      BUG         \\`.
 * ............'//                     |                     \\`.
 * ..........'// .-~"""""""~~~~-._     |     _,-~~~~"""""""~-. \\`.
 * ........'//.-"                 `-.  |  .-'                 "-.\\`.
 * ......'//______.============-..   \ | /   ..-============.______\\`.
 * ....'______________________________\|/______________________________`.
 * ..larsonzhong@163.com      created in 2018/8/16     @author : larsonzhong
 */
public class UnFormatMessageException extends RuntimeException {
    public UnFormatMessageException() {
        super();
    }

    public UnFormatMessageException(String message) {
        super(message);
    }

    public UnFormatMessageException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnFormatMessageException(Throwable cause) {
        super(cause);
    }

}