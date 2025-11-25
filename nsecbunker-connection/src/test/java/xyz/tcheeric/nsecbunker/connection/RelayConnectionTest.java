package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RelayConnectionTest {

    private static final String VALID_WSS_URL = "wss://relay.example.com";
    private static final String VALID_WS_URL = "ws://relay.example.com";

    @Test
    void constructor_shouldAcceptValidWssUrl() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        assertThat(relay.getUrl()).isEqualTo(VALID_WSS_URL);
        assertThat(relay.getState()).isEqualTo(ConnectionState.DISCONNECTED);
    }

    @Test
    void constructor_shouldAcceptValidWsUrl() {
        RelayConnection relay = new RelayConnection(VALID_WS_URL);

        assertThat(relay.getUrl()).isEqualTo(VALID_WS_URL);
    }

    @Test
    void constructor_shouldThrowOnNullUrl() {
        assertThatThrownBy(() -> new RelayConnection(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("must not be null");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://example.com",
            "https://example.com",
            "ftp://example.com",
            "example.com",
            ""
    })
    void constructor_shouldThrowOnInvalidUrl(String invalidUrl) {
        assertThatThrownBy(() -> new RelayConnection(invalidUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("wss://");
    }

    @Test
    void constructor_shouldUseDefaultTimeout() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        assertThat(relay.getConnectTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    void constructor_shouldAcceptCustomTimeout() {
        Duration customTimeout = Duration.ofSeconds(60);

        RelayConnection relay = new RelayConnection(VALID_WSS_URL, null, customTimeout);

        assertThat(relay.getConnectTimeout()).isEqualTo(customTimeout);
    }

    @Test
    void isConnected_shouldReturnFalseWhenDisconnected() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        assertThat(relay.isConnected()).isFalse();
    }

    @Test
    void addListener_shouldNotThrowOnNullListener() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        // Should not throw
        relay.addListener(null);
    }

    @Test
    void addListener_shouldAcceptListener() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);
        TestRelayListener listener = new TestRelayListener();

        relay.addListener(listener);
        // If we could trigger events, the listener would receive them
    }

    @Test
    void removeListener_shouldRemoveListener() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);
        TestRelayListener listener = new TestRelayListener();

        relay.addListener(listener);
        relay.removeListener(listener);
        // Listener should no longer receive events
    }

    @Test
    void send_shouldThrowWhenNotConnected() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        assertThatThrownBy(() -> relay.send("[\"REQ\",\"test\",{}]"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Not connected");
    }

    @Test
    void close_shouldNotThrowWhenNotConnected() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        // Should not throw
        relay.close();

        assertThat(relay.getState()).isEqualTo(ConnectionState.CLOSED);
    }

    @Test
    void close_shouldTransitionToClosedState() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        relay.close();

        assertThat(relay.getState()).isEqualTo(ConnectionState.CLOSED);
    }

    @Test
    void close_shouldBeIdempotent() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        relay.close();
        relay.close(); // Should not throw

        assertThat(relay.getState()).isEqualTo(ConnectionState.CLOSED);
    }

    @Test
    void toString_shouldContainUrlAndState() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        String result = relay.toString();

        assertThat(result).contains(VALID_WSS_URL);
        assertThat(result).contains("DISCONNECTED");
    }

    @Test
    void setReconnectionStrategy_shouldSetStrategy() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);
        ReconnectionStrategy strategy = ExponentialBackoffStrategy.builder().build();

        relay.setReconnectionStrategy(strategy);

        assertThat(relay.getReconnectionStrategy()).isEqualTo(strategy);
        assertThat(relay.isAutoReconnectEnabled()).isTrue();
    }

    @Test
    void setReconnectionStrategy_shouldHandleNullByUsingNone() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);
        relay.setReconnectionStrategy(ExponentialBackoffStrategy.builder().build());

        relay.setReconnectionStrategy(null);

        assertThat(relay.getReconnectionStrategy()).isNotNull();
        assertThat(relay.isAutoReconnectEnabled()).isFalse();
    }

    @Test
    void enableAutoReconnect_shouldEnableReconnection() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        relay.enableAutoReconnect();

        assertThat(relay.isAutoReconnectEnabled()).isTrue();
    }

    @Test
    void disableAutoReconnect_shouldDisableReconnection() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);
        relay.enableAutoReconnect();

        relay.disableAutoReconnect();

        assertThat(relay.isAutoReconnectEnabled()).isFalse();
    }

    @Test
    void getReconnectionAttempt_shouldReturnZeroInitially() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        assertThat(relay.getReconnectionAttempt()).isEqualTo(0);
    }

    @Test
    void getHealthMonitor_shouldReturnHealthMonitor() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        RelayHealthMonitor monitor = relay.getHealthMonitor();

        assertThat(monitor).isNotNull();
        // Should return the same instance
        assertThat(relay.getHealthMonitor()).isSameAs(monitor);
    }

    @Test
    void getHealth_shouldReturnDisconnectedHealthWhenDisconnected() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        ConnectionHealth health = relay.getHealth();

        assertThat(health).isNotNull();
        assertThat(health.isHealthy()).isFalse();
        assertThat(health.getUrl()).isEqualTo(VALID_WSS_URL);
    }

    @Test
    void addConnectionListener_shouldAcceptListener() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);
        TestConnectionListener listener = new TestConnectionListener();

        relay.addConnectionListener(listener);
        // Listener is added successfully
    }

    @Test
    void addConnectionListener_shouldIgnoreNull() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        relay.addConnectionListener(null);
        // Should not throw
    }

    @Test
    void removeConnectionListener_shouldRemoveListener() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);
        TestConnectionListener listener = new TestConnectionListener();

        relay.addConnectionListener(listener);
        relay.removeConnectionListener(listener);
        // Listener is removed successfully
    }

    @Test
    void constructorWithClient_shouldUseProvidedClient() {
        okhttp3.OkHttpClient client = new okhttp3.OkHttpClient.Builder().build();

        RelayConnection relay = new RelayConnection(VALID_WSS_URL, client);

        assertThat(relay.getUrl()).isEqualTo(VALID_WSS_URL);
    }

    @Test
    void setReconnectionStrategy_shouldReturnThisForChaining() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        RelayConnection result = relay.setReconnectionStrategy(ReconnectionStrategy.none());

        assertThat(result).isSameAs(relay);
    }

    @Test
    void enableAutoReconnect_shouldReturnThisForChaining() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        RelayConnection result = relay.enableAutoReconnect();

        assertThat(result).isSameAs(relay);
    }

    @Test
    void disableAutoReconnect_shouldReturnThisForChaining() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        RelayConnection result = relay.disableAutoReconnect();

        assertThat(result).isSameAs(relay);
    }

    @Test
    void getReconnectionStrategy_shouldReturnDefaultNoReconnectStrategy() {
        RelayConnection relay = new RelayConnection(VALID_WSS_URL);

        ReconnectionStrategy strategy = relay.getReconnectionStrategy();

        assertThat(strategy).isNotNull();
        assertThat(strategy.getMaxAttempts()).isEqualTo(0);
    }

    /**
     * Test listener implementation for capturing events.
     */
    private static class TestRelayListener implements RelayListener {
        private boolean connected;
        private boolean disconnected;
        private Throwable error;

        @Override
        public void onConnect(RelayConnection relay) {
            connected = true;
        }

        @Override
        public void onDisconnect(RelayConnection relay, int code, String reason) {
            disconnected = true;
        }

        @Override
        public void onError(RelayConnection relay, Throwable throwable) {
            error = throwable;
        }
    }

    /**
     * Test connection listener implementation for capturing connection events.
     */
    private static class TestConnectionListener implements ConnectionListener {
        @Override
        public void onConnected(String relayUrl) {
        }

        @Override
        public void onDisconnected(String relayUrl, int code, String reason) {
        }

        @Override
        public void onError(String relayUrl, Throwable error) {
        }
    }
}
