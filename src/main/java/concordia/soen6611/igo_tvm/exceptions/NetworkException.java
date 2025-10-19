package concordia.soen6611.igo_tvm.exceptions;

public class NetworkException extends AbstractCustomException {
    public NetworkException(String userMessage) {
        super(userMessage);
    }
    public NetworkException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
