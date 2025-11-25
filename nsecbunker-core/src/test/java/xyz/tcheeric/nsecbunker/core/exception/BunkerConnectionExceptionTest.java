package xyz.tcheeric.nsecbunker.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BunkerConnectionExceptionTest {

    private static final String TEST_RELAY = "wss://relay.example.com";

    @Test
    void shouldCreateWithMessage() {
        BunkerConnectionException exception = new BunkerConnectionException("Connection failed");

        assertThat(exception.getMessage()).isEqualTo("Connection failed");
        assertThat(exception.getRelay()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Socket error");
        BunkerConnectionException exception = new BunkerConnectionException("Connection failed", cause);

        assertThat(exception.getMessage()).isEqualTo("Connection failed");
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getRelay()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndRelay() {
        BunkerConnectionException exception = new BunkerConnectionException("Connection failed", TEST_RELAY);

        assertThat(exception.getMessage()).isEqualTo("Connection failed (relay: " + TEST_RELAY + ")");
        assertThat(exception.getRelay()).isEqualTo(TEST_RELAY);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessageRelayAndCause() {
        RuntimeException cause = new RuntimeException("Socket error");
        BunkerConnectionException exception = new BunkerConnectionException("Connection failed", TEST_RELAY, cause);

        assertThat(exception.getMessage()).isEqualTo("Connection failed (relay: " + TEST_RELAY + ")");
        assertThat(exception.getRelay()).isEqualTo(TEST_RELAY);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldExtendBunkerException() {
        BunkerConnectionException exception = new BunkerConnectionException("Test");

        assertThat(exception).isInstanceOf(BunkerException.class);
    }
}
