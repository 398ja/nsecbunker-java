package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link RelayHealthMonitor} class.
 */
class RelayHealthMonitorTest {

    private static final String TEST_URL = "wss://relay.test.com";
    private RelayConnection connection;
    private RelayHealthMonitor monitor;

    @BeforeEach
    void setUp() {
        connection = new RelayConnection(TEST_URL);
        monitor = new RelayHealthMonitor(connection);
    }

    @AfterEach
    void tearDown() {
        if (monitor != null && monitor.isRunning()) {
            monitor.stop();
        }
    }

    @Test
    void constructorWithNullConnectionThrows() {
        assertThrows(NullPointerException.class, () -> new RelayHealthMonitor(null));
    }

    @Test
    void constructorCreatesMonitorInStoppedState() {
        assertFalse(monitor.isRunning());
    }

    @Test
    void startSetsRunningState() {
        monitor.start();
        assertTrue(monitor.isRunning());
    }

    @Test
    void stopClearsRunningState() {
        monitor.start();
        assertTrue(monitor.isRunning());

        monitor.stop();
        assertFalse(monitor.isRunning());
    }

    @Test
    void startMultipleTimesIsIdempotent() {
        monitor.start();
        monitor.start();
        monitor.start();

        assertTrue(monitor.isRunning());
    }

    @Test
    void stopMultipleTimesIsIdempotent() {
        monitor.start();
        monitor.stop();
        monitor.stop();
        monitor.stop();

        assertFalse(monitor.isRunning());
    }

    @Test
    void getHealthReturnsDisconnectedWhenNotConnected() {
        ConnectionHealth health = monitor.getHealth();

        assertNotNull(health);
        assertEquals(TEST_URL, health.getUrl());
        assertEquals(ConnectionState.DISCONNECTED, health.getState());
        assertFalse(health.isHealthy());
    }

    @Test
    void getConnectionReturnsConnection() {
        assertEquals(connection, monitor.getConnection());
    }

    @Test
    void setCheckIntervalWithValidDuration() {
        Duration interval = Duration.ofSeconds(60);
        monitor.setCheckInterval(interval);
        assertEquals(interval, monitor.getCheckInterval());
    }

    @Test
    void setCheckIntervalWithNullThrows() {
        assertThrows(NullPointerException.class, () -> monitor.setCheckInterval(null));
    }

    @Test
    void setCheckIntervalWithZeroThrows() {
        assertThrows(IllegalArgumentException.class, () -> monitor.setCheckInterval(Duration.ZERO));
    }

    @Test
    void setCheckIntervalWithNegativeThrows() {
        assertThrows(IllegalArgumentException.class, () ->
                monitor.setCheckInterval(Duration.ofSeconds(-1)));
    }

    @Test
    void setPingTimeoutWithValidDuration() {
        Duration timeout = Duration.ofSeconds(15);
        monitor.setPingTimeout(timeout);
        assertEquals(timeout, monitor.getPingTimeout());
    }

    @Test
    void setPingTimeoutWithNullThrows() {
        assertThrows(NullPointerException.class, () -> monitor.setPingTimeout(null));
    }

    @Test
    void setPingTimeoutWithZeroThrows() {
        assertThrows(IllegalArgumentException.class, () -> monitor.setPingTimeout(Duration.ZERO));
    }

    @Test
    void setUnhealthyThresholdWithValidValue() {
        monitor.setUnhealthyThreshold(5);
        assertEquals(5, monitor.getUnhealthyThreshold());
    }

    @Test
    void setUnhealthyThresholdWithZeroThrows() {
        assertThrows(IllegalArgumentException.class, () -> monitor.setUnhealthyThreshold(0));
    }

    @Test
    void setUnhealthyThresholdWithNegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> monitor.setUnhealthyThreshold(-1));
    }

    @Test
    void recordMessageSentTracksCount() {
        monitor.start();
        monitor.recordMessageSent();
        monitor.recordMessageSent();
        monitor.recordMessageSent();
        // Trigger a health update
        monitor.checkNow();

        ConnectionHealth health = monitor.getHealth();
        assertEquals(3, health.getMessagesSent());
    }

    @Test
    void recordMessageReceivedTracksCount() {
        monitor.start();
        monitor.recordMessageReceived();
        monitor.recordMessageReceived();
        // Trigger a health update
        monitor.checkNow();

        ConnectionHealth health = monitor.getHealth();
        assertEquals(2, health.getMessagesReceived());
    }

    @Test
    void recordLatencyUpdatesHealth() {
        monitor.start();
        monitor.recordLatency(Duration.ofMillis(50));

        ConnectionHealth health = monitor.getHealth();
        assertNotNull(health.getLatency());
        assertEquals(1, health.getSuccessfulPings());
    }

    @Test
    void recordLatencyWithNullIsIgnored() {
        monitor.start();
        assertDoesNotThrow(() -> monitor.recordLatency(null));
    }

    @Test
    void recordLatencyWithNegativeIsIgnored() {
        monitor.start();
        assertDoesNotThrow(() -> monitor.recordLatency(Duration.ofMillis(-10)));
        assertEquals(0, monitor.getHealth().getSuccessfulPings());
    }

    @Test
    void recordPingFailureTracksCount() {
        monitor.start();
        monitor.recordPingFailure();
        monitor.recordPingFailure();

        ConnectionHealth health = monitor.getHealth();
        assertEquals(2, health.getFailedPings());
    }

    @Test
    void addHealthListenerCanBeAdded() {
        AtomicReference<ConnectionHealth> receivedHealth = new AtomicReference<>();

        monitor.addHealthListener(health -> {
            receivedHealth.set(health);
        });

        // Verify listener was added (doesn't throw)
        monitor.start();
        // Listener mechanism is tested through state changes during actual connection
    }

    @Test
    void removeHealthListenerRemovesListener() {
        AtomicReference<ConnectionHealth> receivedHealth = new AtomicReference<>();

        var listener = new java.util.function.Consumer<ConnectionHealth>() {
            @Override
            public void accept(ConnectionHealth health) {
                receivedHealth.set(health);
            }
        };

        monitor.addHealthListener(listener);
        monitor.removeHealthListener(listener);

        // Verify removal doesn't throw
        monitor.start();
    }

    @Test
    void checkNowTriggersImmediateCheck() {
        monitor.start();

        // Should not throw
        assertDoesNotThrow(() -> monitor.checkNow());
    }

    @Test
    void checkNowWhenNotRunningDoesNothing() {
        // Should not throw
        assertDoesNotThrow(() -> monitor.checkNow());
    }

    @Test
    void isHealthyDelegatesToHealth() {
        // Disconnected connection is not healthy
        assertFalse(monitor.isHealthy());
    }

    @Test
    void latencyStatisticsAreCalculated() {
        monitor.start();

        // Record multiple latency samples
        monitor.recordLatency(Duration.ofMillis(50));
        monitor.recordLatency(Duration.ofMillis(100));
        monitor.recordLatency(Duration.ofMillis(75));

        ConnectionHealth health = monitor.getHealth();

        assertNotNull(health.getLatency());
        assertNotNull(health.getAverageLatency());
        assertNotNull(health.getMinLatency());
        assertNotNull(health.getMaxLatency());

        // Min should be 50ms
        assertEquals(50, health.getMinLatency().toMillis());
        // Max should be 100ms
        assertEquals(100, health.getMaxLatency().toMillis());
    }

    @Test
    void defaultCheckIntervalIs30Seconds() {
        assertEquals(Duration.ofSeconds(30), monitor.getCheckInterval());
    }

    @Test
    void defaultPingTimeoutIs10Seconds() {
        assertEquals(Duration.ofSeconds(10), monitor.getPingTimeout());
    }

    @Test
    void defaultUnhealthyThresholdIs3() {
        assertEquals(3, monitor.getUnhealthyThreshold());
    }

    @Test
    void implementsHealthMonitor() {
        assertInstanceOf(HealthMonitor.class, monitor);
    }
}
