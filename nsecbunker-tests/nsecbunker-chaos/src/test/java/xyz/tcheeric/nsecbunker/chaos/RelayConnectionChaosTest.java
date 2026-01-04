package xyz.tcheeric.nsecbunker.chaos;

import okhttp3.WebSocket;
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
import java.lang.reflect.Field;
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
        relay.start();

        String url = relay.url("/").toString().replace("http", "ws");
        RelayConnection connection = new RelayConnection(url);
        connection.setReconnectionStrategy(ReconnectionStrategy.fixedDelay(Duration.ofMillis(50), 3));
        connection.enableAutoReconnect();

        connection.connect();
        assertThat(connection.getState()).isEqualTo(ConnectionState.CONNECTED);

        // Simulate server-side drop
        relay.shutdown();
        forceClientSideCancel(connection);

        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(connection.getReconnectionAttempt()).isGreaterThan(0));
    }

    /**
     * Ensures maxAttempts is honored when reconnection repeatedly fails.
     * With maxAttempts=1, the connection should transition to FAILED after exhausting retries.
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

        // Simulate server drop; with maxAttempts=1 we should stop after first retry
        relay.shutdown();
        forceClientSideCancel(connection);

        // Wait for connection to transition to FAILED after exhausting max attempts
        Awaitility.await().atMost(10, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(connection.getState()).isEqualTo(ConnectionState.FAILED));
    }

    /**
     * WebSocket listener for MockWebServer upgrades that properly handles lifecycle.
     *
     * <p>A completely empty listener causes NPE in OkHttp's RealWebSocket.loopReader
     * because the reader loop expects proper lifecycle handling. This implementation
     * handles onOpen, onMessage, and onClosing to prevent the MockWebServer crash.
     */
    private static final class NoopWebSocketListener extends okhttp3.WebSocketListener {

        @Override
        public void onOpen(okhttp3.WebSocket webSocket, okhttp3.Response response) {
            // Connection opened - no action needed for test
        }

        @Override
        public void onMessage(okhttp3.WebSocket webSocket, String text) {
            // Message received - no action needed for test
        }

        @Override
        public void onClosing(okhttp3.WebSocket webSocket, int code, String reason) {
            // Server closing - echo back close to complete handshake
            webSocket.close(code, reason);
        }

        @Override
        public void onFailure(okhttp3.WebSocket webSocket, Throwable t, okhttp3.Response response) {
            // Connection failed - no action needed for test
        }
    }

    /**
    * Forces the client WebSocket to cancel so onFailure triggers reconnection logic
    * when the mock server has already been shut down.
    */
    private void forceClientSideCancel(RelayConnection connection) throws Exception {
        Field wsField = RelayConnection.class.getDeclaredField("webSocket");
        wsField.setAccessible(true);
        WebSocket ws = (WebSocket) wsField.get(connection);
        if (ws != null) {
            ws.cancel();
        }
    }
}
