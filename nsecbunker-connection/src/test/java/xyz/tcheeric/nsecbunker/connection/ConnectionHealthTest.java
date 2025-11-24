package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link ConnectionHealth} class.
 */
class ConnectionHealthTest {

    private static final String TEST_URL = "wss://relay.test.com";

    @Test
    void builderCreatesValidHealth() {
        Instant now = Instant.now();
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .latency(Duration.ofMillis(50))
                .averageLatency(Duration.ofMillis(60))
                .minLatency(Duration.ofMillis(30))
                .maxLatency(Duration.ofMillis(100))
                .timestamp(now)
                .connectedSince(now.minusSeconds(60))
                .successfulPings(10)
                .failedPings(1)
                .consecutiveFailures(0)
                .messagesSent(100)
                .messagesReceived(150)
                .build();

        assertEquals(TEST_URL, health.getUrl());
        assertEquals(ConnectionState.CONNECTED, health.getState());
        assertTrue(health.isHealthy());
        assertEquals(Duration.ofMillis(50), health.getLatency());
        assertEquals(Duration.ofMillis(60), health.getAverageLatency());
        assertEquals(Duration.ofMillis(30), health.getMinLatency());
        assertEquals(Duration.ofMillis(100), health.getMaxLatency());
        assertEquals(now, health.getTimestamp());
        assertEquals(10, health.getSuccessfulPings());
        assertEquals(1, health.getFailedPings());
        assertEquals(0, health.getConsecutiveFailures());
        assertEquals(100, health.getMessagesSent());
        assertEquals(150, health.getMessagesReceived());
    }

    @Test
    void disconnectedCreatesDisconnectedHealth() {
        ConnectionHealth health = ConnectionHealth.disconnected(TEST_URL);

        assertEquals(TEST_URL, health.getUrl());
        assertEquals(ConnectionState.DISCONNECTED, health.getState());
        assertFalse(health.isHealthy());
        assertNotNull(health.getTimestamp());
    }

    @Test
    void connectedCreatesConnectedHealth() {
        ConnectionHealth health = ConnectionHealth.connected(TEST_URL);

        assertEquals(TEST_URL, health.getUrl());
        assertEquals(ConnectionState.CONNECTED, health.getState());
        assertTrue(health.isHealthy());
        assertNotNull(health.getTimestamp());
        assertNotNull(health.getConnectedSince());
        assertEquals(0, health.getSuccessfulPings());
        assertEquals(0, health.getFailedPings());
        assertEquals(0, health.getConsecutiveFailures());
    }

    @Test
    void getPingSuccessRateWithNoPings() {
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .successfulPings(0)
                .failedPings(0)
                .build();

        assertEquals(1.0, health.getPingSuccessRate());
    }

    @Test
    void getPingSuccessRateWithAllSuccessful() {
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .successfulPings(10)
                .failedPings(0)
                .build();

        assertEquals(1.0, health.getPingSuccessRate());
    }

    @Test
    void getPingSuccessRateWithSomeFailures() {
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .successfulPings(8)
                .failedPings(2)
                .build();

        assertEquals(0.8, health.getPingSuccessRate(), 0.001);
    }

    @Test
    void getPingSuccessRateWithAllFailures() {
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.DISCONNECTED)
                .healthy(false)
                .successfulPings(0)
                .failedPings(5)
                .build();

        assertEquals(0.0, health.getPingSuccessRate());
    }

    @Test
    void getUptimeWhenConnected() {
        Instant connectedAt = Instant.now().minusSeconds(120);
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .connectedSince(connectedAt)
                .build();

        Optional<Duration> uptime = health.getUptime();
        assertTrue(uptime.isPresent());
        assertTrue(uptime.get().toSeconds() >= 120);
    }

    @Test
    void getUptimeWhenDisconnected() {
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.DISCONNECTED)
                .healthy(false)
                .connectedSince(Instant.now().minusSeconds(60))
                .build();

        assertEquals(Optional.empty(), health.getUptime());
    }

    @Test
    void getUptimeWhenNeverConnected() {
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .connectedSince(null)
                .build();

        assertEquals(Optional.empty(), health.getUptime());
    }

    @Test
    void getTimeSinceLastPong() {
        Instant lastPong = Instant.now().minusSeconds(30);
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .lastPongTime(lastPong)
                .build();

        Optional<Duration> sinceLastPong = health.getTimeSinceLastPong();
        assertTrue(sinceLastPong.isPresent());
        assertTrue(sinceLastPong.get().toSeconds() >= 30);
    }

    @Test
    void getTimeSinceLastPongWhenNoPong() {
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .lastPongTime(null)
                .build();

        assertEquals(Optional.empty(), health.getTimeSinceLastPong());
    }

    @Test
    void toBuilderCreatesModifiableCopy() {
        ConnectionHealth original = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .latency(Duration.ofMillis(50))
                .build();

        ConnectionHealth modified = original.toBuilder()
                .latency(Duration.ofMillis(100))
                .build();

        assertEquals(Duration.ofMillis(50), original.getLatency());
        assertEquals(Duration.ofMillis(100), modified.getLatency());
        assertEquals(TEST_URL, modified.getUrl());
    }

    @Test
    void toStringContainsRelevantInfo() {
        ConnectionHealth health = ConnectionHealth.builder()
                .url(TEST_URL)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .latency(Duration.ofMillis(50))
                .averageLatency(Duration.ofMillis(60))
                .successfulPings(10)
                .failedPings(0)
                .build();

        String str = health.toString();
        assertTrue(str.contains("ConnectionHealth"));
        assertTrue(str.contains(TEST_URL));
        assertTrue(str.contains("CONNECTED"));
        assertTrue(str.contains("healthy=true"));
    }
}
