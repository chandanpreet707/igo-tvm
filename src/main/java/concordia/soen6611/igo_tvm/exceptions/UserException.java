package concordia.soen6611.igo_tvm.exceptions;

/**
 * Exception representing user-related errors, such as invalid input,
 * unauthorized actions, or incorrect operations initiated by the user.
 * <p>
 * Provides localized, user-friendly error messages and structured logging
 * through its {@link AbstractCustomException} superclass.
 */
public class UserException extends AbstractCustomException {

    /**
     * Constructs a {@code UserException} with a user-facing message.
     *
     * @param userMessage localized, user-facing description of the user error
     */
    public UserException(String userMessage) {
        super(userMessage);
    }

    /**
     * Constructs a {@code UserException} with a user-facing message and a root cause.
     *
     * @param userMessage localized, user-facing description of the user error
     * @param cause       underlying cause (e.g., validation failure)
     */
    public UserException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
