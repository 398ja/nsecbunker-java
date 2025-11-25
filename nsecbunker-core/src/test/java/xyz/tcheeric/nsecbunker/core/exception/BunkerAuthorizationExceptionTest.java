package xyz.tcheeric.nsecbunker.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BunkerAuthorizationExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        BunkerAuthorizationException exception = new BunkerAuthorizationException("Not authorized");

        assertThat(exception.getMessage()).isEqualTo("Not authorized");
        assertThat(exception.getMethod()).isNull();
        assertThat(exception.getEventKind()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndMethod() {
        BunkerAuthorizationException exception = new BunkerAuthorizationException("Not authorized", "sign_event");

        assertThat(exception.getMessage()).isEqualTo("Not authorized");
        assertThat(exception.getMethod()).isEqualTo("sign_event");
        assertThat(exception.getEventKind()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndEventKind() {
        BunkerAuthorizationException exception = new BunkerAuthorizationException("Not authorized", 4);

        assertThat(exception.getMessage()).isEqualTo("Not authorized");
        assertThat(exception.getMethod()).isNull();
        assertThat(exception.getEventKind()).isEqualTo(4);
    }

    @Test
    void shouldCreateWithFullContext() {
        BunkerAuthorizationException exception = new BunkerAuthorizationException(
                "Not authorized",
                "sign_event",
                4
        );

        assertThat(exception.getMessage()).isEqualTo("Not authorized");
        assertThat(exception.getMethod()).isEqualTo("sign_event");
        assertThat(exception.getEventKind()).isEqualTo(4);
    }

    @Test
    void methodDeniedShouldCreateCorrectException() {
        BunkerAuthorizationException exception = BunkerAuthorizationException.methodDenied("encrypt");

        assertThat(exception.getMessage()).isEqualTo("Permission denied for method: encrypt");
        assertThat(exception.getMethod()).isEqualTo("encrypt");
    }

    @Test
    void eventKindDeniedShouldCreateCorrectException() {
        BunkerAuthorizationException exception = BunkerAuthorizationException.eventKindDenied(4);

        assertThat(exception.getMessage()).isEqualTo("Permission denied for event kind: 4");
        assertThat(exception.getEventKind()).isEqualTo(4);
    }

    @Test
    void policyViolationShouldCreateCorrectException() {
        BunkerAuthorizationException exception = BunkerAuthorizationException.policyViolation("restricted-policy");

        assertThat(exception.getMessage()).isEqualTo("Operation violates policy: restricted-policy");
    }

    @Test
    void shouldExtendBunkerException() {
        BunkerAuthorizationException exception = new BunkerAuthorizationException("Test");

        assertThat(exception).isInstanceOf(BunkerException.class);
    }
}
