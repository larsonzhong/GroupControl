package larson.groupcontrol.app.exception;


/**
 * @author zhonglunshun
 */
public class UnconnectedException extends RuntimeException {
    public UnconnectedException() {
        super();
    }

    public UnconnectedException(String message) {
        super(message);
    }

    public UnconnectedException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnconnectedException(Throwable cause) {
        super(cause);
    }

}
