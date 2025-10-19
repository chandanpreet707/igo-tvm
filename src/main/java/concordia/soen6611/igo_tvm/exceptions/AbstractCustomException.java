package concordia.soen6611.igo_tvm.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Arrays;

public abstract class AbstractCustomException extends RuntimeException {
    private final LocalDateTime timestamp;
    private final String exceptionType;
    private final String rootCause;
    private final String origin;         // stack frame where exception was triggered from
    private final String userMessage;

    private final Logger log = LoggerFactory.getLogger(getClass());

    protected AbstractCustomException(String userMessage) {
        this(userMessage, null);
    }

    protected AbstractCustomException(String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.timestamp = LocalDateTime.now();
        this.exceptionType = this.getClass().getSimpleName();
        this.rootCause = cause == null ? "None" : cause.getClass().getName() + ": " + safeMsg(cause);
        this.origin = computeOrigin();
        this.userMessage = userMessage;
        logException();
    }

    private String safeMsg(Throwable t) {
        return t.getMessage() == null ? "<no-message>" : t.getMessage();
    }

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

    private void logException() {
        // Structured single-line log; adjust level or format as needed
        log.error("exception.timestamp={} | exception.type={} | exception.rootCause={} | exception.origin={} | user.message={}",
                timestamp, exceptionType, rootCause, origin, userMessage);
    }

    // getters for callers/tests
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getExceptionType() { return exceptionType; }
    public String getRootCause() { return rootCause; }
    public String getOrigin() { return origin; }
    public String getUserMessage() { return userMessage; }
}
