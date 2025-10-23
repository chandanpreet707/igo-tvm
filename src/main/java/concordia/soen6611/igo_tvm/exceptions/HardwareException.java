package concordia.soen6611.igo_tvm.exceptions;

/**
 * Exception indicating hardware-related errors such as device malfunction,
 * communication failure with peripherals, or sensor read issues.
 * <p>
 * This exception supports localization and automatic structured logging
 * via its {@link AbstractCustomException} superclass.
 */
public class HardwareException extends AbstractCustomException {

    /**
     * Constructs a {@code HardwareException} with a user-facing message.
     *
     * @param userMessage localized, user-facing description of the hardware error
     */
    public HardwareException(String userMessage) {
        super(userMessage);
    }

    /**
     * Constructs a {@code HardwareException} with a user-facing message and a root cause.
     *
     * @param userMessage localized, user-facing description of the hardware error
     * @param cause       underlying cause (e.g., {@code IOException})
     */
    public HardwareException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
