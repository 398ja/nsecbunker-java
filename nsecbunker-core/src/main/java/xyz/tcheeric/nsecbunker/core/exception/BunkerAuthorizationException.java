package xyz.tcheeric.nsecbunker.core.exception;

/**
 * Exception thrown when an operation is not permitted by nsecBunker.
 *
 * <p>This is thrown when authentication succeeds but the user doesn't
 * have permission to perform the requested operation.
 */
public class BunkerAuthorizationException extends BunkerException {

    private static final long serialVersionUID = 1L;

    private final String method;
    private final Integer eventKind;

    /**
     * Creates a new BunkerAuthorizationException with the specified message.
     *
     * @param message the error message
     */
    public BunkerAuthorizationException(String message) {
        super(message);
        this.method = null;
        this.eventKind = null;
    }

    /**
     * Creates a new BunkerAuthorizationException for a denied method.
     *
     * @param message the error message
     * @param method  the method that was denied
     */
    public BunkerAuthorizationException(String message, String method) {
        super(message);
        this.method = method;
        this.eventKind = null;
    }

    /**
     * Creates a new BunkerAuthorizationException for a denied event kind.
     *
     * @param message   the error message
     * @param eventKind the event kind that was denied
     */
    public BunkerAuthorizationException(String message, int eventKind) {
        super(message);
        this.method = null;
        this.eventKind = eventKind;
    }

    /**
     * Creates a new BunkerAuthorizationException with full context.
     *
     * @param message   the error message
     * @param method    the method that was denied
     * @param eventKind the event kind that was denied
     */
    public BunkerAuthorizationException(String message, String method, Integer eventKind) {
        super(message);
        this.method = method;
        this.eventKind = eventKind;
    }

    /**
     * Returns the method that was denied.
     *
     * @return the denied method, or null
     */
    public String getMethod() {
        return method;
    }

    /**
     * Returns the event kind that was denied.
     *
     * @return the denied event kind, or null
     */
    public Integer getEventKind() {
        return eventKind;
    }

    /**
     * Creates an exception for denied method.
     *
     * @param method the method that was denied
     * @return a new BunkerAuthorizationException
     */
    public static BunkerAuthorizationException methodDenied(String method) {
        return new BunkerAuthorizationException(
                "Permission denied for method: " + method,
                method
        );
    }

    /**
     * Creates an exception for denied event kind.
     *
     * @param eventKind the event kind that was denied
     * @return a new BunkerAuthorizationException
     */
    public static BunkerAuthorizationException eventKindDenied(int eventKind) {
        return new BunkerAuthorizationException(
                "Permission denied for event kind: " + eventKind,
                eventKind
        );
    }

    /**
     * Creates an exception for policy violation.
     *
     * @param policyName the policy that was violated
     * @return a new BunkerAuthorizationException
     */
    public static BunkerAuthorizationException policyViolation(String policyName) {
        return new BunkerAuthorizationException(
                "Operation violates policy: " + policyName
        );
    }
}
