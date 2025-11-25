package xyz.tcheeric.nsecbunker.client.signer.exception;

/**
 * Exception thrown when a signer adapter operation fails.
 *
 * <p>This exception wraps errors that occur when adapting between different
 * signer interfaces, such as converting between nostr-java's Signer interface
 * and the NsecBunkerSigner.
 *
 * @see xyz.tcheeric.nsecbunker.client.signer.NostrJavaSignerAdapter
 */
public class SignerAdapterException extends RuntimeException {

    /**
     * Creates a new signer adapter exception.
     *
     * @param message the error message describing what failed
     * @param cause   the underlying cause of the failure
     */
    public SignerAdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
