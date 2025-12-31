package xyz.tcheeric.nsecbunker.core.exception;

import java.io.Serial;

/**
 * Exception thrown when there are key-related errors in nsecBunker.
 *
 * <p>This includes key not found, key locked, key already exists, and
 * other key management failures.
 */
public class BunkerKeyException extends BunkerException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * The reason for the key error.
     */
    public enum Reason {
        /**
         * The key was not found.
         */
        NOT_FOUND,

        /**
         * The key is locked and requires a passphrase.
         */
        LOCKED,

        /**
         * A key with this name already exists.
         */
        ALREADY_EXISTS,

        /**
         * The passphrase is incorrect.
         */
        INVALID_PASSPHRASE,

        /**
         * The key format is invalid.
         */
        INVALID_FORMAT,

        /**
         * The key cannot be deleted (e.g., has active users).
         */
        CANNOT_DELETE,

        /**
         * Unknown key error.
         */
        UNKNOWN
    }

    private final String keyName;
    private final Reason reason;

    /**
     * Creates a new BunkerKeyException with the specified message.
     *
     * @param message the error message
     */
    public BunkerKeyException(String message) {
        super(message);
        this.keyName = null;
        this.reason = Reason.UNKNOWN;
    }

    /**
     * Creates a new BunkerKeyException with key name and reason.
     *
     * @param message the error message
     * @param keyName the key name
     * @param reason  the reason for the error
     */
    public BunkerKeyException(String message, String keyName, Reason reason) {
        super(message);
        this.keyName = keyName;
        this.reason = reason;
    }

    /**
     * Creates a new BunkerKeyException with message and cause.
     *
     * @param message the error message
     * @param cause   the underlying cause
     */
    public BunkerKeyException(String message, Throwable cause) {
        super(message, cause);
        this.keyName = null;
        this.reason = Reason.UNKNOWN;
    }

    /**
     * Returns the key name associated with this error.
     *
     * @return the key name, or null
     */
    public String getKeyName() {
        return keyName;
    }

    /**
     * Returns the reason for the error.
     *
     * @return the error reason
     */
    public Reason getReason() {
        return reason;
    }

    /**
     * Creates an exception for key not found.
     *
     * @param keyName the key name that was not found
     * @return a new BunkerKeyException
     */
    public static BunkerKeyException notFound(String keyName) {
        return new BunkerKeyException(
                "Key not found: " + keyName,
                keyName,
                Reason.NOT_FOUND
        );
    }

    /**
     * Creates an exception for locked key.
     *
     * @param keyName the key name that is locked
     * @return a new BunkerKeyException
     */
    public static BunkerKeyException locked(String keyName) {
        return new BunkerKeyException(
                "Key is locked and requires passphrase: " + keyName,
                keyName,
                Reason.LOCKED
        );
    }

    /**
     * Creates an exception for key already exists.
     *
     * @param keyName the key name that already exists
     * @return a new BunkerKeyException
     */
    public static BunkerKeyException alreadyExists(String keyName) {
        return new BunkerKeyException(
                "Key already exists: " + keyName,
                keyName,
                Reason.ALREADY_EXISTS
        );
    }

    /**
     * Creates an exception for invalid passphrase.
     *
     * @param keyName the key name
     * @return a new BunkerKeyException
     */
    public static BunkerKeyException invalidPassphrase(String keyName) {
        return new BunkerKeyException(
                "Invalid passphrase for key: " + keyName,
                keyName,
                Reason.INVALID_PASSPHRASE
        );
    }
}
