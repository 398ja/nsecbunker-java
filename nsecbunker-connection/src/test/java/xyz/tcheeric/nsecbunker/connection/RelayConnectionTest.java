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
}
