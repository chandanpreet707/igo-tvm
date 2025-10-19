package concordia.soen6611.igo_tvm.exceptions;

public class DatabaseException extends AbstractCustomException {
    public DatabaseException(String userMessage) {
        super(userMessage);
    }
    public DatabaseException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
