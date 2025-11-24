package xyz.tcheeric.nsecbunker.connection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link ConnectionListener} implementation that logs connection events.
 *
 * <p>This is useful for debugging and monitoring connection lifecycle.
 */
public class LoggingConnectionListener implements ConnectionListener {

    private final Logger log;

    /**
     * Creates a LoggingConnectionListener with the specified logger name.
     *
     * @param loggerName the logger name
     */
    public LoggingConnectionListener(String loggerName) {
        this.log = LoggerFactory.getLogger(loggerName);
    }

    /**
     * Creates a LoggingConnectionListener with the specified logger.
     *
     * @param logger the logger to use
     */
    public LoggingConnectionListener(Logger logger) {
        this.log = logger;
    }

    @Override
    public void onConnected(String url) {
        log.info("Connected to relay: {}", url);
    }

    @Override
    public void onDisconnected(String url, int code, String reason) {
        log.info("Disconnected from relay: {} (code={}, reason={})", url, code, reason);
    }

    @Override
    public void onError(String url, Throwable error) {
        log.error("Connection error on {}: {}", url, error.getMessage(), error);
    }

    @Override
    public void onStateChanged(String url, ConnectionState oldState, ConnectionState newState) {
        log.debug("State change on {}: {} -> {}", url, oldState, newState);
    }

    @Override
    public void onReconnecting(String url, int attempt) {
        log.info("Reconnecting to {} (attempt {})", url, attempt);
    }
}
