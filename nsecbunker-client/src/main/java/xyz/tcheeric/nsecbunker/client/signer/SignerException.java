package xyz.tcheeric.nsecbunker.client.signer;

/**
 * Runtime exception for signer operation failures.
 */
public class SignerException extends RuntimeException {
    public SignerException(String message) {
        super(message);
    }

    public SignerException(String message, Throwable cause) {
        super(message, cause);
    }
}
