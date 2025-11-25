package xyz.tcheeric.nsecbunker.core.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BunkerAuthenticationExceptionTest {

    @Test
    void shouldCreateWithMessage() {
        BunkerAuthenticationException exception = new BunkerAuthenticationException("Auth failed");

        assertThat(exception.getMessage()).isEqualTo("Auth failed");
        assertThat(exception.getReason()).isEqualTo(BunkerAuthenticationException.Reason.UNKNOWN);
        assertThat(exception.getCause()).isNull();
    }

    @Test
    void shouldCreateWithMessageAndReason() {
        BunkerAuthenticationException exception = new BunkerAuthenticationException(
                "Token expired",
                BunkerAuthenticationException.Reason.TOKEN_EXPIRED
        );

        assertThat(exception.getMessage()).isEqualTo("Token expired");
        assertThat(exception.getReason()).isEqualTo(BunkerAuthenticationException.Reason.TOKEN_EXPIRED);
    }

    @Test
    void shouldCreateWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Decryption failed");
        BunkerAuthenticationException exception = new BunkerAuthenticationException("Auth failed", cause);

        assertThat(exception.getMessage()).isEqualTo("Auth failed");
        assertThat(exception.getCause()).isEqualTo(cause);
        assertThat(exception.getReason()).isEqualTo(BunkerAuthenticationException.Reason.UNKNOWN);
    }

    @Test
    void shouldCreateWithMessageReasonAndCause() {
        RuntimeException cause = new RuntimeException("Invalid signature");
        BunkerAuthenticationException exception = new BunkerAuthenticationException(
                "Handshake failed",
                BunkerAuthenticationException.Reason.HANDSHAKE_FAILED,
                cause
        );

        assertThat(exception.getMessage()).isEqualTo("Handshake failed");
        assertThat(exception.getReason()).isEqualTo(BunkerAuthenticationException.Reason.HANDSHAKE_FAILED);
        assertThat(exception.getCause()).isEqualTo(cause);
    }

    @Test
    void tokenExpiredShouldCreateCorrectException() {
        BunkerAuthenticationException exception = BunkerAuthenticationException.tokenExpired();

        assertThat(exception.getMessage()).isEqualTo("Access token has expired");
        assertThat(exception.getReason()).isEqualTo(BunkerAuthenticationException.Reason.TOKEN_EXPIRED);
    }

    @Test
    void tokenRevokedShouldCreateCorrectException() {
        BunkerAuthenticationException exception = BunkerAuthenticationException.tokenRevoked();

        assertThat(exception.getMessage()).isEqualTo("Access token has been revoked");
        assertThat(exception.getReason()).isEqualTo(BunkerAuthenticationException.Reason.TOKEN_REVOKED);
    }

    @Test
    void invalidSecretShouldCreateCorrectException() {
        BunkerAuthenticationException exception = BunkerAuthenticationException.invalidSecret();

        assertThat(exception.getMessage()).isEqualTo("Invalid secret provided");
        assertThat(exception.getReason()).isEqualTo(BunkerAuthenticationException.Reason.INVALID_SECRET);
    }

    @Test
    void shouldSupportAllReasons() {
        for (BunkerAuthenticationException.Reason reason : BunkerAuthenticationException.Reason.values()) {
            BunkerAuthenticationException exception = new BunkerAuthenticationException("Test", reason);
            assertThat(exception.getReason()).isEqualTo(reason);
        }
    }

    @Test
    void shouldExtendBunkerException() {
        BunkerAuthenticationException exception = new BunkerAuthenticationException("Test");

        assertThat(exception).isInstanceOf(BunkerException.class);
    }
}
