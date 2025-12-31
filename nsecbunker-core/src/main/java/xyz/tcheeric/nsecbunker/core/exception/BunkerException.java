package xyz.tcheeric.nsecbunker.core.exception;

import java.io.Serial;

/**
 * Base exception for all nsecBunker-related errors.
 *
 * <p>This exception serves as the root of the nsecBunker exception hierarchy.
 * All specific bunker exceptions extend this class.
 */
public class BunkerException extends Exception {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new BunkerException with the specified message.
     *
     * @param message the error message
     */
    public BunkerException(String message) {
        super(message);
    }

    /**
     * Creates a new BunkerException with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public BunkerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new BunkerException with the specified cause.
     *
     * @param cause the underlying cause
     */
    public BunkerException(Throwable cause) {
        super(cause);
    }
}
