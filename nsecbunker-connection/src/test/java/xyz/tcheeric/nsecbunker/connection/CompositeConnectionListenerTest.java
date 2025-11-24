package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link CompositeConnectionListener} class.
 */
class CompositeConnectionListenerTest {

    private static final String TEST_URL = "wss://relay.test.com";
    private List<String> eventLog;

    @BeforeEach
    void setUp() {
        eventLog = new ArrayList<>();
    }

    @Test
    void constructorWithVarargsCreatesComposite() {
        ConnectionListener l1 = new ConnectionListener() {};
        ConnectionListener l2 = new ConnectionListener() {};

        CompositeConnectionListener composite = new CompositeConnectionListener(l1, l2);

        assertEquals(2, composite.size());
    }

    @Test
    void constructorWithEmptyVarargsCreatesEmptyComposite() {
        CompositeConnectionListener composite = new CompositeConnectionListener();
        assertEquals(0, composite.size());
    }

    @Test
    void addListenerIncreasesSize() {
        CompositeConnectionListener composite = new CompositeConnectionListener();
        assertEquals(0, composite.size());

        composite.addListener(new ConnectionListener() {});
        assertEquals(1, composite.size());

        composite.addListener(new ConnectionListener() {});
        assertEquals(2, composite.size());
    }

    @Test
    void addNullListenerIsIgnored() {
        CompositeConnectionListener composite = new CompositeConnectionListener();
        composite.addListener(null);
        assertEquals(0, composite.size());
    }

    @Test
    void addSelfIsIgnored() {
        CompositeConnectionListener composite = new CompositeConnectionListener();
        composite.addListener(composite);
        assertEquals(0, composite.size());
    }

    @Test
    void removeListenerDecreasesSize() {
        ConnectionListener l1 = new ConnectionListener() {};
        ConnectionListener l2 = new ConnectionListener() {};

        CompositeConnectionListener composite = new CompositeConnectionListener(l1, l2);
        assertEquals(2, composite.size());

        assertTrue(composite.removeListener(l1));
        assertEquals(1, composite.size());

        assertTrue(composite.removeListener(l2));
        assertEquals(0, composite.size());
    }

    @Test
    void removeNonexistentListenerReturnsFalse() {
        CompositeConnectionListener composite = new CompositeConnectionListener();
        assertFalse(composite.removeListener(new ConnectionListener() {}));
    }

    @Test
    void onConnectedDelegatesToAllListeners() {
        ConnectionListener l1 = new ConnectionListener() {
            @Override
            public void onConnected(String url) {
                eventLog.add("l1:connected:" + url);
            }
        };
        ConnectionListener l2 = new ConnectionListener() {
            @Override
            public void onConnected(String url) {
                eventLog.add("l2:connected:" + url);
            }
        };

        CompositeConnectionListener composite = new CompositeConnectionListener(l1, l2);
        composite.onConnected(TEST_URL);

        assertEquals(2, eventLog.size());
        assertTrue(eventLog.contains("l1:connected:" + TEST_URL));
        assertTrue(eventLog.contains("l2:connected:" + TEST_URL));
    }

    @Test
    void onDisconnectedDelegatesToAllListeners() {
        ConnectionListener l1 = new ConnectionListener() {
            @Override
            public void onDisconnected(String url, int code, String reason) {
                eventLog.add("l1:disconnected:" + url + ":" + code);
            }
        };
        ConnectionListener l2 = new ConnectionListener() {
            @Override
            public void onDisconnected(String url, int code, String reason) {
                eventLog.add("l2:disconnected:" + url + ":" + code);
            }
        };

        CompositeConnectionListener composite = new CompositeConnectionListener(l1, l2);
        composite.onDisconnected(TEST_URL, 1000, "Normal closure");

        assertEquals(2, eventLog.size());
        assertTrue(eventLog.contains("l1:disconnected:" + TEST_URL + ":1000"));
        assertTrue(eventLog.contains("l2:disconnected:" + TEST_URL + ":1000"));
    }

    @Test
    void onErrorDelegatesToAllListeners() {
        AtomicInteger l1Called = new AtomicInteger(0);
        AtomicInteger l2Called = new AtomicInteger(0);

        ConnectionListener l1 = new ConnectionListener() {
            @Override
            public void onError(String url, Throwable error) {
                l1Called.incrementAndGet();
            }
        };
        ConnectionListener l2 = new ConnectionListener() {
            @Override
            public void onError(String url, Throwable error) {
                l2Called.incrementAndGet();
            }
        };

        CompositeConnectionListener composite = new CompositeConnectionListener(l1, l2);
        composite.onError(TEST_URL, new RuntimeException("Test"));

        assertEquals(1, l1Called.get());
        assertEquals(1, l2Called.get());
    }

    @Test
    void onStateChangedDelegatesToAllListeners() {
        ConnectionListener l1 = new ConnectionListener() {
            @Override
            public void onStateChanged(String url, ConnectionState oldState, ConnectionState newState) {
                eventLog.add("l1:stateChanged:" + oldState + "->" + newState);
            }
        };
        ConnectionListener l2 = new ConnectionListener() {
            @Override
            public void onStateChanged(String url, ConnectionState oldState, ConnectionState newState) {
                eventLog.add("l2:stateChanged:" + oldState + "->" + newState);
            }
        };

        CompositeConnectionListener composite = new CompositeConnectionListener(l1, l2);
        composite.onStateChanged(TEST_URL, ConnectionState.DISCONNECTED, ConnectionState.CONNECTING);

        assertEquals(2, eventLog.size());
        assertTrue(eventLog.contains("l1:stateChanged:DISCONNECTED->CONNECTING"));
        assertTrue(eventLog.contains("l2:stateChanged:DISCONNECTED->CONNECTING"));
    }

    @Test
    void onReconnectingDelegatesToAllListeners() {
        ConnectionListener l1 = new ConnectionListener() {
            @Override
            public void onReconnecting(String url, int attempt) {
                eventLog.add("l1:reconnecting:" + attempt);
            }
        };
        ConnectionListener l2 = new ConnectionListener() {
            @Override
            public void onReconnecting(String url, int attempt) {
                eventLog.add("l2:reconnecting:" + attempt);
            }
        };

        CompositeConnectionListener composite = new CompositeConnectionListener(l1, l2);
        composite.onReconnecting(TEST_URL, 3);

        assertEquals(2, eventLog.size());
        assertTrue(eventLog.contains("l1:reconnecting:3"));
        assertTrue(eventLog.contains("l2:reconnecting:3"));
    }

    @Test
    void exceptionInOneListenerDoesNotAffectOthers() {
        AtomicInteger successCount = new AtomicInteger(0);

        ConnectionListener throwingListener = new ConnectionListener() {
            @Override
            public void onConnected(String url) {
                throw new RuntimeException("Test exception");
            }
        };
        ConnectionListener normalListener = new ConnectionListener() {
            @Override
            public void onConnected(String url) {
                successCount.incrementAndGet();
            }
        };

        // Put throwing listener first
        CompositeConnectionListener composite = new CompositeConnectionListener(throwingListener, normalListener);

        // Should not throw, and second listener should still be called
        assertDoesNotThrow(() -> composite.onConnected(TEST_URL));
        assertEquals(1, successCount.get());
    }

    @Test
    void exceptionInListenerIsCaughtForAllMethods() {
        ConnectionListener throwingListener = new ConnectionListener() {
            @Override
            public void onConnected(String url) {
                throw new RuntimeException("onConnected exception");
            }
            @Override
            public void onDisconnected(String url, int code, String reason) {
                throw new RuntimeException("onDisconnected exception");
            }
            @Override
            public void onError(String url, Throwable error) {
                throw new RuntimeException("onError exception");
            }
            @Override
            public void onStateChanged(String url, ConnectionState oldState, ConnectionState newState) {
                throw new RuntimeException("onStateChanged exception");
            }
            @Override
            public void onReconnecting(String url, int attempt) {
                throw new RuntimeException("onReconnecting exception");
            }
        };

        CompositeConnectionListener composite = new CompositeConnectionListener(throwingListener);

        assertDoesNotThrow(() -> composite.onConnected(TEST_URL));
        assertDoesNotThrow(() -> composite.onDisconnected(TEST_URL, 1000, "reason"));
        assertDoesNotThrow(() -> composite.onError(TEST_URL, new RuntimeException()));
        assertDoesNotThrow(() -> composite.onStateChanged(TEST_URL,
            ConnectionState.DISCONNECTED, ConnectionState.CONNECTING));
        assertDoesNotThrow(() -> composite.onReconnecting(TEST_URL, 1));
    }

    @Test
    void implementsConnectionListener() {
        CompositeConnectionListener composite = new CompositeConnectionListener();
        assertInstanceOf(ConnectionListener.class, composite);
    }
}
