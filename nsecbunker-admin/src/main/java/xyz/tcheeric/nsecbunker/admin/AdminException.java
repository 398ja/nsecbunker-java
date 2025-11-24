package xyz.tcheeric.nsecbunker.admin;

import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Error;

/**
 * Exception thrown for admin operation failures.
 *
 * <p>This exception encapsulates errors from the bunker as well as
 * client-side errors during admin operations.
 */
public class AdminException extends RuntimeException {

    private final String errorCode;
    private final String method;

    /**
     * Creates a new AdminException with a message.
     *
     * @param message the error message
     */
    public AdminException(String message) {
        super(message);
        this.errorCode = null;
        this.method = null;
    }

    /**
     * Creates a new AdminException with a message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public AdminException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = null;
        this.method = null;
    }

    /**
     * Creates a new AdminException from a NIP-46 error.
     *
     * @param error  the NIP-46 error
     * @param method the method that failed
     */
    public AdminException(Nip46Error error, String method) {
        super(formatErrorMessage(error, method));
        this.errorCode = error != null ? error.getCode() : null;
        this.method = method;
    }

    /**
     * Gets the error code from the bunker (if available).
     *
     * @return the error code, or null
     */
    public String getErrorCode() {
        return errorCode;
    }

    /**
     * Gets the method that failed (if available).
     *
     * @return the method name, or null
     */
    public String getMethod() {
        return method;
    }

    /**
     * Checks if this is an authentication error.
     *
     * @return true if authentication failed
     */
    public boolean isAuthenticationError() {
        return "UNAUTHORIZED".equals(errorCode) || "AUTH_REQUIRED".equals(errorCode);
    }

    /**
     * Checks if this is a permission error.
     *
     * @return true if permission was denied
     */
    public boolean isPermissionError() {
        return "FORBIDDEN".equals(errorCode) || "PERMISSION_DENIED".equals(errorCode);
    }

    /**
     * Checks if this is a not found error.
     *
     * @return true if the requested resource was not found
     */
    public boolean isNotFoundError() {
        return "NOT_FOUND".equals(errorCode);
    }

    private static String formatErrorMessage(Nip46Error error, String method) {
        if (error == null) {
            return "Admin operation failed: " + method;
        }
        StringBuilder sb = new StringBuilder();
        if (method != null) {
            sb.append(method).append(": ");
        }
        if (error.getCode() != null) {
            sb.append("[").append(error.getCode()).append("] ");
        }
        sb.append(error.getMessage());
        return sb.toString();
    }
}
