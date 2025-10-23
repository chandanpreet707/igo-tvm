package concordia.soen6611.igo_tvm.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * Base class for all custom, localized runtime exceptions in the application.
 * <p>
 * This abstract exception captures and logs structured diagnostic data at
 * construction time, including:
 * <ul>
 *   <li>{@link #timestamp} – when the exception was created</li>
 *   <li>{@link #exceptionType} – simple class name of the concrete exception</li>
 *   <li>{@link #rootCause} – type/message of the underlying cause if present</li>
 *   <li>{@link #origin} – the first meaningful stack frame outside infrastructure code</li>
 *   <li>{@link #userMessage} – a user-facing, localized message suitable for UI</li>
 * </ul>
 * Subclasses should provide domain-specific context while relying on this class
 * for consistent logging and metadata extraction.
 */
public abstract class AbstractCustomException extends RuntimeException {
    /** Creation time of this exception instance. */
    private final LocalDateTime timestamp;
    /** Simple name of the concrete exception class. */
    private final String exceptionType;
    /** Type and message of the root cause, or "None" if absent. */
    private final String rootCause;
    /** First meaningful stack frame where the exception originated. */
    private final String origin;         // stack frame where exception was triggered from
    /** Localized, user-facing message that can be shown in the UI. */
    private final String userMessage;

    /** Logger bound to the concrete subclass type. */
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Constructs the exception with a user-facing message and no underlying cause.
     *
     * @param userMessage localized, user-facing message
     */
    protected AbstractCustomException(String userMessage) {
        this(userMessage, null);
    }

    /**
     * Constructs the exception with a user-facing message and an underlying cause.
     * Also initializes diagnostic fields and emits a structured error log entry.
     *
     * @param userMessage localized, user-facing message
     * @param cause       underlying cause (nullable)
     */
    protected AbstractCustomException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.timestamp = LocalDateTime.now();
        this.exceptionType = this.getClass().getSimpleName();
        this.rootCause = cause == null ? "None" : cause.getClass().getName() + ": " + safeMsg(cause);
        this.origin = computeOrigin();
        this.userMessage = userMessage;
        logException();
    }

    /**
     * Safely extracts a message from a throwable; falls back to a placeholder when null.
     *
     * @param t throwable to inspect
     * @return non-null message string
     */
    private String safeMsg(Throwable t) {
        return t.getMessage() == null ? "<no-message>" : t.getMessage();
    }

    /**
     * Determines the first meaningful stack frame outside this base class and
     * common JVM internals (e.g., {@code Thread}, reflection).
     *
     * @return a string representation of the stack frame, or {@code "unknown"} if not found
     */
    // Find first meaningful stack frame outside the exception classes and java internals
    private String computeOrigin() {
        StackTraceElement[] frames = Thread.currentThread().getStackTrace();
        // skip frames for Thread.getStackTrace, this constructor and base classes
        return Arrays.stream(frames)
                .filter(f -> !f.getClassName().contains(AbstractCustomException.class.getName()))
                .filter(f -> !f.getClassName().startsWith("java.lang.Thread"))
                .filter(f -> !f.getClassName().startsWith("java.lang.reflect"))
                .findFirst()
                .map(StackTraceElement::toString)
                .orElse("unknown");
    }

    /**
     * Emits a single structured error-line suitable for log aggregation/search.
     * Fields include timestamp, type, root cause, origin, and user message.
     */
    private void logException() {
        // Structured single-line log; adjust level or format as needed
        log.error("exception.timestamp={} | exception.type={} | exception.rootCause={} | exception.origin={} | user.message={}",
                timestamp, exceptionType, rootCause, origin, userMessage);
    }

    // getters for callers/tests

    /**
     * @return creation time of this exception
     */
    public LocalDateTime getTimestamp() { return timestamp; }

    /**
     * @return simple class name of the concrete exception
     */
    public String getExceptionType() { return exceptionType; }

    /**
     * @return description of the underlying cause or {@code "None"} if absent
     */
    public String getRootCause() { return rootCause; }

    /**
     * @return first meaningful origin stack frame as a string
     */
    public String getOrigin() { return origin; }

    /**
     * @return localized, user-facing message
     */
    public String getUserMessage() { return userMessage; }
}
