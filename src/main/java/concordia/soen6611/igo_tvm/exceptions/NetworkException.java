package concordia.soen6611.igo_tvm.exceptions;

/**
 * Exception indicating network-related failures such as connectivity loss,
 * request timeouts, DNS resolution problems, or protocol errors.
 * <p>
 * Supports localized, user-facing messages and structured logging via
 * {@link AbstractCustomException}.
 */
public class NetworkException extends AbstractCustomException {

    /**
     * Constructs a {@code NetworkException} with a user-facing message.
     *
     * @param userMessage localized, user-facing description of the network error
     */
    public NetworkException(String userMessage) {
        super(userMessage);
    }

    /**
     * Constructs a {@code NetworkException} with a user-facing message and a root cause.
     *
     * @param userMessage localized, user-facing description of the network error
     * @param cause       underlying cause (e.g., {@code IOException})
     */
    public NetworkException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
