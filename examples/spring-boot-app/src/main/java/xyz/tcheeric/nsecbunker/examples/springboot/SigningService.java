package xyz.tcheeric.nsecbunker.examples.springboot;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import xyz.tcheeric.nsecbunker.client.signer.NsecBunkerSigner;
import xyz.tcheeric.nsecbunker.starter.metrics.SigningMetrics;

import java.util.concurrent.CompletableFuture;

/**
 * Service layer for nsecBunker signing operations.
 *
 * <p>This service wraps the NsecBunkerSigner and SigningMetrics to provide
 * a clean API for signing operations with automatic metrics collection.
 */
@Service
public class SigningService {

    private static final Logger log = LoggerFactory.getLogger(SigningService.class);

    private final NsecBunkerSigner signer;
    private final SigningMetrics metrics;

    public SigningService(NsecBunkerSigner signer, SigningMetrics metrics) {
        this.signer = signer;
        this.metrics = metrics;
        log.info("SigningService initialized with signer pubkey: {}",
                signer.getCommunicationIdentity().getPublicKey());
    }

    /**
     * Signs a Nostr event using the remote bunker.
     *
     * @param eventJson the unsigned event JSON
     * @return a future containing the signed event JSON
     */
    public CompletableFuture<String> signEvent(String eventJson) {
        log.debug("Signing event: {}", eventJson.substring(0, Math.min(50, eventJson.length())));
        return metrics.recordSignEvent(eventJson);
    }

    /**
     * Gets the signing public key from the bunker.
     *
     * @return a future containing the public key
     */
    public CompletableFuture<String> getPublicKey() {
        log.debug("Getting public key");
        return metrics.recordGetPublicKey();
    }

    /**
     * Encrypts a message using NIP-44.
     *
     * @param recipientPubkey the recipient's public key
     * @param plaintext       the message to encrypt
     * @return a future containing the ciphertext
     */
    public CompletableFuture<String> encrypt(String recipientPubkey, String plaintext) {
        log.debug("Encrypting message for: {}", recipientPubkey.substring(0, 8));
        return metrics.recordEncrypt(recipientPubkey, plaintext, true);
    }

    /**
     * Decrypts a message using NIP-44.
     *
     * @param senderPubkey the sender's public key
     * @param ciphertext   the encrypted message
     * @return a future containing the plaintext
     */
    public CompletableFuture<String> decrypt(String senderPubkey, String ciphertext) {
        log.debug("Decrypting message from: {}", senderPubkey.substring(0, 8));
        return metrics.recordDecrypt(senderPubkey, ciphertext, true);
    }

    /**
     * Pings the bunker to check connectivity.
     *
     * @return a future containing the pong response
     */
    public CompletableFuture<String> ping() {
        log.debug("Pinging bunker");
        return metrics.recordPing();
    }

    /**
     * Checks if the signer is connected to the bunker.
     *
     * @return true if connected
     */
    public boolean isConnected() {
        return signer.isConnected();
    }

    /**
     * Gets the current active operation count.
     *
     * @return the number of operations in progress
     */
    public long getActiveOperations() {
        return metrics.getActiveOperations();
    }
}
