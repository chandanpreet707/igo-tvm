package concordia.soen6611.igo_tvm.exceptions;

/**
 * Exception representing database-related failures (e.g., connectivity issues,
 * timeouts, constraint violations). Carries a localized, user-facing message
 * via {@link AbstractCustomException}.
 */
public class DatabaseException extends AbstractCustomException {

    /**
     * Creates a {@code DatabaseException} with a user-facing message.
     *
     * @param userMessage localized, user-facing description of the error
     */
    public DatabaseException(String userMessage) {
        super(userMessage);
    }

    /**
     * Creates a {@code DatabaseException} with a user-facing message and a root cause.
     *
     * @param userMessage localized, user-facing description of the error
     * @param cause       underlying cause (e.g., SQL exception)
     */
    public DatabaseException(String userMessage, Throwable cause) {
        super(userMessage, cause);
    }
}
