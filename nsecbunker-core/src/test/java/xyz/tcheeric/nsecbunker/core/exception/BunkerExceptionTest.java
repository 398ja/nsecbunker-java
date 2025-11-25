package xyz.tcheeric.nsecbunker.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BunkerExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        BunkerException exception = new BunkerException("Test message");

        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        BunkerException exception = new BunkerException("Test message", cause);

        assertThat(exception.getMessage()).isEqualTo("Test message");
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void shouldCreateWithCause() {
        RuntimeException cause = new RuntimeException("Root cause");
        BunkerException exception = new BunkerException(cause);

        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getMessage()).contains("Root cause");
    }

    @Test
    void shouldBeCheckedException() {
        BunkerException exception = new BunkerException("Test");

        assertThat(exception).isInstanceOf(Exception.class);
        assertThat(exception).isNotInstanceOf(RuntimeException.class);
    }
}
