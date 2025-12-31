package xyz.tcheeric.nsecbunker.core.exception;

import java.io.Serial;

/**
 * Exception thrown when authentication with nsecBunker fails.
 *
 * <p>This includes invalid credentials, expired tokens, and failed
 * connection handshakes.
 */
public class BunkerAuthenticationException extends BunkerException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The reason for the authentication failure.
     */
    public enum Reason {
        /**
         * Invalid credentials provided.
         */
        INVALID_CREDENTIALS,

        /**
         * The token has expired.
         */
        TOKEN_EXPIRED,

        /**
         * The token has been revoked.
         */
        TOKEN_REVOKED,

        /**
         * The secret is invalid.
         */
        INVALID_SECRET,

        /**
         * The admin key is not authorized.
         */
        UNAUTHORIZED_ADMIN,

        /**
         * The connection handshake failed.
         */
        HANDSHAKE_FAILED,

        /**
         * Unknown authentication error.
         */
        UNKNOWN
    }

    private final Reason reason;

    /**
     * Creates a new BunkerAuthenticationException with the specified message.
     *
     * @param message the error message
     */
    public BunkerAuthenticationException(String message) {
        super(message);
        this.reason = Reason.UNKNOWN;
    }

    /**
     * Creates a new BunkerAuthenticationException with the specified message and reason.
     *
     * @param message the error message
     * @param reason  the reason for the failure
     */
    public BunkerAuthenticationException(String message, Reason reason) {
        super(message);
        this.reason = reason;
    }

    /**
     * Creates a new BunkerAuthenticationException with the specified message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public BunkerAuthenticationException(String message, Throwable cause) {
        super(message, cause);
        this.reason = Reason.UNKNOWN;
    }

    /**
     * Creates a new BunkerAuthenticationException with message, reason, and cause.
     *
     * @param message the error message
     * @param reason  the reason for the failure
     * @param cause   the underlying cause
     */
    public BunkerAuthenticationException(String message, Reason reason, Throwable cause) {
        super(message, cause);
        this.reason = reason;
    }

    /**
     * Returns the reason for the authentication failure.
     *
     * @return the failure reason
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * Creates an exception for expired token.
     *
     * @return a new BunkerAuthenticationException
     */
    public static BunkerAuthenticationException tokenExpired() {
        return new BunkerAuthenticationException("Access token has expired", Reason.TOKEN_EXPIRED);
    }

    /**
     * Creates an exception for revoked token.
     *
     * @return a new BunkerAuthenticationException
     */
    public static BunkerAuthenticationException tokenRevoked() {
        return new BunkerAuthenticationException("Access token has been revoked", Reason.TOKEN_REVOKED);
    }

    /**
     * Creates an exception for invalid secret.
     *
     * @return a new BunkerAuthenticationException
     */
    public static BunkerAuthenticationException invalidSecret() {
        return new BunkerAuthenticationException("Invalid secret provided", Reason.INVALID_SECRET);
    }
}
