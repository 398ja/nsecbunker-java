package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for the {@link ConnectionListener} interface.
 */
class ConnectionListenerTest {

    @Test
    void defaultMethodsDoNothing() {
        // Create a default implementation
        ConnectionListener listener = new ConnectionListener() {};

        // All default methods should execute without error
        assertDoesNotThrow(() -> listener.onConnected("wss://relay.test.com"));
        assertDoesNotThrow(() -> listener.onDisconnected("wss://relay.test.com", 1000, "Normal closure"));
        assertDoesNotThrow(() -> listener.onError("wss://relay.test.com", new RuntimeException("Test error")));
        assertDoesNotThrow(() -> listener.onStateChanged("wss://relay.test.com",
            ConnectionState.DISCONNECTED, ConnectionState.CONNECTING));
        assertDoesNotThrow(() -> listener.onReconnecting("wss://relay.test.com", 1));
    }

    @Test
    void loggingFactoryCreatesLoggingListener() {
        ConnectionListener listener = ConnectionListener.logging("test.logger");

        assertNotNull(listener);
        assertInstanceOf(LoggingConnectionListener.class, listener);
    }

    @Test
    void loggingFactoryWithDefaultLoggerCreatesListener() {
        ConnectionListener listener = ConnectionListener.logging();

        assertNotNull(listener);
        assertInstanceOf(LoggingConnectionListener.class, listener);
    }

    @Test
    void compositeFactoryCreatesCompositeListener() {
        ConnectionListener l1 = new ConnectionListener() {};
        ConnectionListener l2 = new ConnectionListener() {};

        ConnectionListener composite = ConnectionListener.composite(l1, l2);

        assertNotNull(composite);
        assertInstanceOf(CompositeConnectionListener.class, composite);
    }

    @Test
    void compositeFactoryWithEmptyArrayCreatesEmptyComposite() {
        ConnectionListener composite = ConnectionListener.composite();

        assertNotNull(composite);
        assertInstanceOf(CompositeConnectionListener.class, composite);
    }
}
