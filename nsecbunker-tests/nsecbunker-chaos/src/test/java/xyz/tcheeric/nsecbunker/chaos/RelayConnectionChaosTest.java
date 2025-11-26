package xyz.tcheeric.nsecbunker.chaos;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.connection.ConnectionState;
import xyz.tcheeric.nsecbunker.connection.ReconnectionStrategy;
import xyz.tcheeric.nsecbunker.connection.RelayConnection;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Chaos-style tests for {@link RelayConnection} under abrupt network failures.
 */
class RelayConnectionChaosTest {

    private MockWebServer relay;

    @BeforeEach
    void setUp() throws IOException {
        relay = new MockWebServer();
    }

    @AfterEach
    void tearDown() throws IOException {
        relay.shutdown();
    }

    /**
     * Ensures connections attempt reconnection after an abrupt server drop.
     */
    @Test
    @DisplayName("Should attempt reconnect when server drops connection")
    void shouldAttemptReconnectWhenServerDrops() throws Exception {
        relay.enqueue(new MockResponse().withWebSocketUpgrade(new NoopWebSocketListener()));
        relay.enqueue(new MockResponse().withWebSocketUpgrade(new NoopWebSocketListener()));
        relay.start();

        String url = relay.url("/").toString().replace("http", "ws");
        RelayConnection connection = new RelayConnection(url);
        connection.setReconnectionStrategy(ReconnectionStrategy.fixedDelay(Duration.ofMillis(50), 3));
        connection.enableAutoReconnect();

        connection.connect();
        assertThat(connection.getState()).isEqualTo(ConnectionState.CONNECTED);

        // Simulate network partition by shutting down the server
        relay.shutdown();

        Awaitility.await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(connection.getReconnectionAttempt()).isGreaterThan(0));
    }

    /**
     * Ensures maxAttempts is honored when reconnection repeatedly fails.
     */
    @Test
    @DisplayName("Should stop reconnecting after max attempts")
    void shouldStopAfterMaxReconnectAttempts() throws Exception {
        relay.enqueue(new MockResponse().withWebSocketUpgrade(new NoopWebSocketListener()));
        relay.start();

        String url = relay.url("/").toString().replace("http", "ws");
        RelayConnection connection = new RelayConnection(url);
        connection.setReconnectionStrategy(ReconnectionStrategy.fixedDelay(Duration.ofMillis(50), 1));
        connection.enableAutoReconnect();

        connection.connect();
        assertThat(connection.getState()).isEqualTo(ConnectionState.CONNECTED);

        // Drop the server; no second upgrade enqueued, so reconnect must exhaust quickly
        relay.shutdown();

        Awaitility.await().atMost(2, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(connection.getReconnectionAttempt()).isEqualTo(1));
    }

    /**
     * No-op WebSocket listener for MockWebServer upgrades.
     */
    private static final class NoopWebSocketListener extends okhttp3.WebSocketListener {
    }
}
