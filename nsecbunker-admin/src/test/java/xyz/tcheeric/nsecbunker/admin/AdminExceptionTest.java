package xyz.tcheeric.nsecbunker.admin;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Error;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for {@link AdminException}.
 */
class AdminExceptionTest {

    @Test
    @DisplayName("Constructor with message only")
    void constructorWithMessage() {
        AdminException ex = new AdminException("Test error");

        assertThat(ex.getMessage()).isEqualTo("Test error");
        assertThat(ex.getErrorCode()).isNull();
        assertThat(ex.getMethod()).isNull();
    }

    @Test
    @DisplayName("Constructor with message and cause")
    void constructorWithMessageAndCause() {
        RuntimeException cause = new RuntimeException("Cause");
        AdminException ex = new AdminException("Test error", cause);

        assertThat(ex.getMessage()).isEqualTo("Test error");
        assertThat(ex.getCause()).isEqualTo(cause);
        assertThat(ex.getErrorCode()).isNull();
    }

    @Test
    @DisplayName("Constructor with Nip46Error and method")
    void constructorWithNip46Error() {
        Nip46Error error = Nip46Error.of("UNAUTHORIZED", "Access denied");
        AdminException ex = new AdminException(error, "sign_event");

        assertThat(ex.getMessage()).contains("sign_event");
        assertThat(ex.getMessage()).contains("UNAUTHORIZED");
        assertThat(ex.getMessage()).contains("Access denied");
        assertThat(ex.getErrorCode()).isEqualTo("UNAUTHORIZED");
        assertThat(ex.getMethod()).isEqualTo("sign_event");
    }

    @Test
    @DisplayName("Constructor with null error")
    void constructorWithNullError() {
        AdminException ex = new AdminException(null, "sign_event");

        assertThat(ex.getMessage()).contains("sign_event");
        assertThat(ex.getErrorCode()).isNull();
    }

    @Test
    @DisplayName("isAuthenticationError returns true for UNAUTHORIZED")
    void isAuthenticationErrorForUnauthorized() {
        Nip46Error error = Nip46Error.of("UNAUTHORIZED", "Not authorized");
        AdminException ex = new AdminException(error, "connect");

        assertThat(ex.isAuthenticationError()).isTrue();
        assertThat(ex.isPermissionError()).isFalse();
        assertThat(ex.isNotFoundError()).isFalse();
    }

    @Test
    @DisplayName("isAuthenticationError returns true for AUTH_REQUIRED")
    void isAuthenticationErrorForAuthRequired() {
        Nip46Error error = Nip46Error.of("AUTH_REQUIRED", "Authentication required");
        AdminException ex = new AdminException(error, "connect");

        assertThat(ex.isAuthenticationError()).isTrue();
    }

    @Test
    @DisplayName("isPermissionError returns true for FORBIDDEN")
    void isPermissionErrorForForbidden() {
        Nip46Error error = Nip46Error.of("FORBIDDEN", "Access forbidden");
        AdminException ex = new AdminException(error, "sign_event");

        assertThat(ex.isPermissionError()).isTrue();
        assertThat(ex.isAuthenticationError()).isFalse();
    }

    @Test
    @DisplayName("isPermissionError returns true for PERMISSION_DENIED")
    void isPermissionErrorForPermissionDenied() {
        Nip46Error error = Nip46Error.of("PERMISSION_DENIED", "Permission denied");
        AdminException ex = new AdminException(error, "sign_event");

        assertThat(ex.isPermissionError()).isTrue();
    }

    @Test
    @DisplayName("isNotFoundError returns true for NOT_FOUND")
    void isNotFoundErrorForNotFound() {
        Nip46Error error = Nip46Error.of("NOT_FOUND", "Key not found");
        AdminException ex = new AdminException(error, "get_key");

        assertThat(ex.isNotFoundError()).isTrue();
        assertThat(ex.isAuthenticationError()).isFalse();
        assertThat(ex.isPermissionError()).isFalse();
    }

    @Test
    @DisplayName("Error flags are false for unknown error code")
    void errorFlagsForUnknownCode() {
        Nip46Error error = Nip46Error.of("CUSTOM_ERROR", "Custom error");
        AdminException ex = new AdminException(error, "custom");

        assertThat(ex.isAuthenticationError()).isFalse();
        assertThat(ex.isPermissionError()).isFalse();
        assertThat(ex.isNotFoundError()).isFalse();
    }
}
