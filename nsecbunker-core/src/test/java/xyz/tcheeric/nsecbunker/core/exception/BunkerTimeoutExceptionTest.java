package xyz.tcheeric.nsecbunker.core.exception;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class BunkerTimeoutExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        BunkerTimeoutException exception = new BunkerTimeoutException("Request timed out");

        assertThat(exception.getMessage()).isEqualTo("Request timed out");
        assertThat(exception.getTimeout()).isNull();
        assertThat(exception.getOperation()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithTimeoutDetails() {
        Duration timeout = Duration.ofSeconds(30);
        BunkerTimeoutException exception = new BunkerTimeoutException(
                "Request timed out",
                timeout,
                "sign_event"
        );

        assertThat(exception.getMessage()).isEqualTo("Request timed out");
        assertThat(exception.getTimeout()).isEqualTo(timeout);
        assertThat(exception.getOperation()).isEqualTo("sign_event");
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Socket timeout");
        BunkerTimeoutException exception = new BunkerTimeoutException("Request timed out", cause);

        assertThat(exception.getMessage()).isEqualTo("Request timed out");
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getTimeout()).isNull();
        assertThat(exception.getOperation()).isNull();
    }

    @Test
    void signingTimeoutShouldCreateCorrectException() {
        Duration timeout = Duration.ofMillis(5000);
        BunkerTimeoutException exception = BunkerTimeoutException.signingTimeout(timeout);

        assertThat(exception.getMessage()).isEqualTo("Signing request timed out after 5000ms");
        assertThat(exception.getTimeout()).isEqualTo(timeout);
        assertThat(exception.getOperation()).isEqualTo("sign_event");
    }

    @Test
    void connectionTimeoutShouldCreateCorrectException() {
        Duration timeout = Duration.ofMillis(10000);
        BunkerTimeoutException exception = BunkerTimeoutException.connectionTimeout(timeout);

        assertThat(exception.getMessage()).isEqualTo("Connection timed out after 10000ms");
        assertThat(exception.getTimeout()).isEqualTo(timeout);
        assertThat(exception.getOperation()).isEqualTo("connect");
    }

    @Test
    void requestTimeoutShouldCreateCorrectException() {
        Duration timeout = Duration.ofMillis(3000);
        BunkerTimeoutException exception = BunkerTimeoutException.requestTimeout("get_public_key", timeout);

        assertThat(exception.getMessage()).isEqualTo("Request 'get_public_key' timed out after 3000ms");
        assertThat(exception.getTimeout()).isEqualTo(timeout);
        assertThat(exception.getOperation()).isEqualTo("get_public_key");
    }

    @Test
    void shouldExtendBunkerException() {
        BunkerTimeoutException exception = new BunkerTimeoutException("Test");

        assertThat(exception).isInstanceOf(BunkerException.class);
    }
}
