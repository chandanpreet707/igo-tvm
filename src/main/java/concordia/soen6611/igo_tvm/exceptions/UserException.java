package concordia.soen6611.igo_tvm.exceptions;

public class UserException extends AbstractCustomException {
    public UserException(String userMessage) {
        super(userMessage);
    }
    public UserException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
