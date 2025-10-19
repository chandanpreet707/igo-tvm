package concordia.soen6611.igo_tvm.exceptions;

public class HardwareException extends AbstractCustomException {
    public HardwareException(String userMessage) {
        super(userMessage);
    }
    public HardwareException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
