package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link LoggingConnectionListener} class.
 */
class LoggingConnectionListenerTest {

    private static final String TEST_URL = "wss://relay.test.com";

    @Test
    void constructorWithLoggerNameCreatesListener() {
        LoggingConnectionListener listener = new LoggingConnectionListener("test.logger");
        assertNotNull(listener);
    }

    @Test
    void constructorWithLoggerCreatesListener() {
        Logger logger = LoggerFactory.getLogger("test.logger");
        LoggingConnectionListener listener = new LoggingConnectionListener(logger);
        assertNotNull(listener);
    }

    @Test
    void onConnectedDoesNotThrow() {
        LoggingConnectionListener listener = new LoggingConnectionListener("test.logger");
        assertDoesNotThrow(() -> listener.onConnected(TEST_URL));
    }

    @Test
    void onDisconnectedDoesNotThrow() {
        LoggingConnectionListener listener = new LoggingConnectionListener("test.logger");
        assertDoesNotThrow(() -> listener.onDisconnected(TEST_URL, 1000, "Normal closure"));
    }

    @Test
    void onErrorDoesNotThrow() {
        LoggingConnectionListener listener = new LoggingConnectionListener("test.logger");
        assertDoesNotThrow(() -> listener.onError(TEST_URL, new RuntimeException("Test error")));
    }

    @Test
    void onStateChangedDoesNotThrow() {
        LoggingConnectionListener listener = new LoggingConnectionListener("test.logger");
        assertDoesNotThrow(() -> listener.onStateChanged(TEST_URL,
            ConnectionState.DISCONNECTED, ConnectionState.CONNECTING));
    }

    @Test
    void onReconnectingDoesNotThrow() {
        LoggingConnectionListener listener = new LoggingConnectionListener("test.logger");
        assertDoesNotThrow(() -> listener.onReconnecting(TEST_URL, 3));
    }

    @Test
    void implementsConnectionListener() {
        LoggingConnectionListener listener = new LoggingConnectionListener("test.logger");
        assertInstanceOf(ConnectionListener.class, listener);
    }
}
