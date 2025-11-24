package xyz.tcheeric.nsecbunker.connection;

/**
 * Simple listener interface for connection lifecycle events.
 *
 * <p>This interface provides callbacks for basic connection state changes
 * without the Nostr-specific message handling of {@link RelayListener}.
 * Use this when you only need to track connection health.
 *
 * <p>For full Nostr message handling (EVENT, OK, EOSE, etc.),
 * use {@link RelayListener} instead.
 *
 * <p>Example usage:
 * <pre>{@code
 * relay.addConnectionListener(new ConnectionListener() {
 *     @Override
 *     public void onConnected(String url) {
 *         log.info("Connected to {}", url);
 *     }
 *
 *     @Override
 *     public void onDisconnected(String url, int code, String reason) {
 *         log.warn("Disconnected from {}: {} {}", url, code, reason);
 *     }
 *
 *     @Override
 *     public void onError(String url, Throwable error) {
 *         log.error("Error on {}: {}", url, error.getMessage());
 *     }
 * });
 * }</pre>
 *
 * @see RelayListener
 * @see RelayConnection
 */
public interface ConnectionListener {

    /**
     * Called when a connection is successfully established.
     *
     * @param url the relay URL that connected
     */
    default void onConnected(String url) {
        // Default no-op
    }

    /**
     * Called when a connection is closed.
     *
     * @param url    the relay URL that disconnected
     * @param code   the WebSocket close code
     * @param reason the close reason
     */
    default void onDisconnected(String url, int code, String reason) {
        // Default no-op
    }

    /**
     * Called when a connection error occurs.
     *
     * @param url   the relay URL where the error occurred
     * @param error the error
     */
    default void onError(String url, Throwable error) {
        // Default no-op
    }

    /**
     * Called when the connection state changes.
     *
     * @param url      the relay URL
     * @param oldState the previous state
     * @param newState the new state
     */
    default void onStateChanged(String url, ConnectionState oldState, ConnectionState newState) {
        // Default no-op
    }

    /**
     * Called when a reconnection attempt starts.
     *
     * @param url     the relay URL
     * @param attempt the attempt number (1-based)
     */
    default void onReconnecting(String url, int attempt) {
        // Default no-op
    }

    /**
     * Creates a ConnectionListener that logs events using the provided logger name.
     *
     * @param loggerName the logger name to use
     * @return a logging ConnectionListener
     */
    static ConnectionListener logging(String loggerName) {
        return new LoggingConnectionListener(loggerName);
    }

    /**
     * Creates a ConnectionListener that logs events using a default logger.
     *
     * @return a logging ConnectionListener
     */
    static ConnectionListener logging() {
        return logging(ConnectionListener.class.getName());
    }

    /**
     * Combines multiple ConnectionListeners into one.
     *
     * @param listeners the listeners to combine
     * @return a composite ConnectionListener
     */
    static ConnectionListener composite(ConnectionListener... listeners) {
        return new CompositeConnectionListener(listeners);
    }
}
