package xyz.tcheeric.nsecbunker.core.exception;

/**
 * Exception thrown when there are NIP-46 protocol-related errors.
 *
 * <p>This includes malformed messages, unsupported methods, and
 * protocol version mismatches.
 */
public class BunkerProtocolException extends BunkerException {

    private static final long serialVersionUID = 1L;

    private final String requestId;
    private final String errorCode;

    /**
     * Creates a new BunkerProtocolException with the specified message.
     *
     * @param message the error message
     */
    public BunkerProtocolException(String message) {
        super(message);
        this.requestId = null;
        this.errorCode = null;
    }

    /**
     * Creates a new BunkerProtocolException with message and request ID.
     *
     * @param message   the error message
     * @param requestId the ID of the failed request
     */
    public BunkerProtocolException(String message, String requestId) {
        super(message);
        this.requestId = requestId;
        this.errorCode = null;
    }

    /**
     * Creates a new BunkerProtocolException with full context.
     *
     * @param message   the error message
     * @param requestId the ID of the failed request
     * @param errorCode the error code from the response
     */
    public BunkerProtocolException(String message, String requestId, String errorCode) {
        super(message);
        this.requestId = requestId;
        this.errorCode = errorCode;
    }

    /**
     * Creates a new BunkerProtocolException with message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public BunkerProtocolException(String message, Throwable cause) {
        super(message, cause);
        this.requestId = null;
        this.errorCode = null;
    }

    /**
     * Returns the request ID associated with this error.
     *
     * @return the request ID, or null
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Returns the error code from the response.
     *
     * @return the error code, or null
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Creates an exception for malformed response.
     *
     * @param details the details about what was malformed
     * @return a new BunkerProtocolException
     */
    public static BunkerProtocolException malformedResponse(String details) {
        return new BunkerProtocolException("Malformed protocol response: " + details);
    }

    /**
     * Creates an exception for unsupported method.
     *
     * @param method the unsupported method
     * @return a new BunkerProtocolException
     */
    public static BunkerProtocolException unsupportedMethod(String method) {
        return new BunkerProtocolException("Unsupported NIP-46 method: " + method);
    }

    /**
     * Creates an exception for decryption failure.
     *
     * @param cause the underlying cause
     * @return a new BunkerProtocolException
     */
    public static BunkerProtocolException decryptionFailed(Throwable cause) {
        return new BunkerProtocolException("Failed to decrypt NIP-46 message", cause);
    }

    /**
     * Creates an exception for encryption failure.
     *
     * @param cause the underlying cause
     * @return a new BunkerProtocolException
     */
    public static BunkerProtocolException encryptionFailed(Throwable cause) {
        return new BunkerProtocolException("Failed to encrypt NIP-46 message", cause);
    }
}
