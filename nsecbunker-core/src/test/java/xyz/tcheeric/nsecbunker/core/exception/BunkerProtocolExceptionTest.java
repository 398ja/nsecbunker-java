package xyz.tcheeric.nsecbunker.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BunkerProtocolExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        BunkerProtocolException exception = new BunkerProtocolException("Protocol error");

        assertThat(exception.getMessage()).isEqualTo("Protocol error");
        assertThat(exception.getRequestId()).isNull();
        assertThat(exception.getErrorCode()).isNull();
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndRequestId() {
        BunkerProtocolException exception = new BunkerProtocolException("Protocol error", "req-123");

        assertThat(exception.getMessage()).isEqualTo("Protocol error");
        assertThat(exception.getRequestId()).isEqualTo("req-123");
        assertThat(exception.getErrorCode()).isNull();
    }

    @Test
    void shouldCreateWithFullContext() {
        BunkerProtocolException exception = new BunkerProtocolException(
                "Protocol error",
                "req-123",
                "ERR_INVALID"
        );

        assertThat(exception.getMessage()).isEqualTo("Protocol error");
        assertThat(exception.getRequestId()).isEqualTo("req-123");
        assertThat(exception.getErrorCode()).isEqualTo("ERR_INVALID");
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("JSON parse error");
        BunkerProtocolException exception = new BunkerProtocolException("Protocol error", cause);

        assertThat(exception.getMessage()).isEqualTo("Protocol error");
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getRequestId()).isNull();
        assertThat(exception.getErrorCode()).isNull();
    }

    @Test
    void malformedResponseShouldCreateCorrectException() {
        BunkerProtocolException exception = BunkerProtocolException.malformedResponse("missing result field");

        assertThat(exception.getMessage()).isEqualTo("Malformed protocol response: missing result field");
    }

    @Test
    void unsupportedMethodShouldCreateCorrectException() {
        BunkerProtocolException exception = BunkerProtocolException.unsupportedMethod("unknown_method");

        assertThat(exception.getMessage()).isEqualTo("Unsupported NIP-46 method: unknown_method");
    }

    @Test
    void decryptionFailedShouldCreateCorrectException() {
        RuntimeException cause = new RuntimeException("Invalid ciphertext");
        BunkerProtocolException exception = BunkerProtocolException.decryptionFailed(cause);

        assertThat(exception.getMessage()).isEqualTo("Failed to decrypt NIP-46 message");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void encryptionFailedShouldCreateCorrectException() {
        RuntimeException cause = new RuntimeException("Key error");
        BunkerProtocolException exception = BunkerProtocolException.encryptionFailed(cause);

        assertThat(exception.getMessage()).isEqualTo("Failed to encrypt NIP-46 message");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldExtendBunkerException() {
        BunkerProtocolException exception = new BunkerProtocolException("Test");

        assertThat(exception).isInstanceOf(BunkerException.class);
    }
}
