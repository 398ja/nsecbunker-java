package xyz.tcheeric.nsecbunker.core.exception;

/**
 * Exception thrown when there are connection-related errors with nsecBunker.
 *
 * <p>This includes failures to establish connections, relay connectivity issues,
 * and unexpected disconnections.
 */
public class BunkerConnectionException extends BunkerException {

    private static final long serialVersionUID = 1L;

    private final String relay;

    /**
     * Creates a new BunkerConnectionException with the specified message.
     *
     * @param message the error message
     */
    public BunkerConnectionException(String message) {
        super(message);
        this.relay = null;
    }

    /**
     * Creates a new BunkerConnectionException with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public BunkerConnectionException(String message, Throwable cause) {
        super(message, cause);
        this.relay = null;
    }

    /**
     * Creates a new BunkerConnectionException for a specific relay.
     *
     * @param message the error message
     * @param relay   the relay URL that failed
     */
    public BunkerConnectionException(String message, String relay) {
        super(message + " (relay: " + relay + ")");
        this.relay = relay;
    }

    /**
     * Creates a new BunkerConnectionException for a specific relay with a cause.
     *
     * @param message the error message
     * @param relay   the relay URL that failed
     * @param cause   the underlying cause
     */
    public BunkerConnectionException(String message, String relay, Throwable cause) {
        super(message + " (relay: " + relay + ")", cause);
        this.relay = relay;
    }

    /**
     * Returns the relay URL associated with this error.
     *
     * @return the relay URL, or null if not specific to a relay
     */
    public String getRelay() {
        return relay;
    }
}
