package xyz.tcheeric.nsecbunker.core.exception;

import java.time.Duration;

/**
 * Exception thrown when an operation times out.
 *
 * <p>This includes request timeouts, connection timeouts, and
 * other time-based failures.
 */
public class BunkerTimeoutException extends BunkerException {

    private static final long serialVersionUID = 1L;

    private final Duration timeout;
    private final String operation;

    /**
     * Creates a new BunkerTimeoutException with the specified message.
     *
     * @param message the error message
     */
    public BunkerTimeoutException(String message) {
        super(message);
        this.timeout = null;
        this.operation = null;
    }

    /**
     * Creates a new BunkerTimeoutException with timeout details.
     *
     * @param message   the error message
     * @param timeout   the timeout duration
     * @param operation the operation that timed out
     */
    public BunkerTimeoutException(String message, Duration timeout, String operation) {
        super(message);
        this.timeout = timeout;
        this.operation = operation;
    }

    /**
     * Creates a new BunkerTimeoutException with message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public BunkerTimeoutException(String message, Throwable cause) {
        super(message, cause);
        this.timeout = null;
        this.operation = null;
    }

    /**
     * Returns the timeout duration.
     *
     * @return the timeout duration, or null
     */
    public Duration getTimeout() {
        return timeout;
    }

    /**
     * Returns the operation that timed out.
     *
     * @return the operation name, or null
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Creates a timeout exception for a signing request.
     *
     * @param timeout the timeout duration
     * @return a new BunkerTimeoutException
     */
    public static BunkerTimeoutException signingTimeout(Duration timeout) {
        return new BunkerTimeoutException(
                "Signing request timed out after " + timeout.toMillis() + "ms",
                timeout,
                "sign_event"
        );
    }

    /**
     * Creates a timeout exception for connection.
     *
     * @param timeout the timeout duration
     * @return a new BunkerTimeoutException
     */
    public static BunkerTimeoutException connectionTimeout(Duration timeout) {
        return new BunkerTimeoutException(
                "Connection timed out after " + timeout.toMillis() + "ms",
                timeout,
                "connect"
        );
    }

    /**
     * Creates a timeout exception for a generic request.
     *
     * @param method  the method that timed out
     * @param timeout the timeout duration
     * @return a new BunkerTimeoutException
     */
    public static BunkerTimeoutException requestTimeout(String method, Duration timeout) {
        return new BunkerTimeoutException(
                "Request '" + method + "' timed out after " + timeout.toMillis() + "ms",
                timeout,
                method
        );
    }
}
