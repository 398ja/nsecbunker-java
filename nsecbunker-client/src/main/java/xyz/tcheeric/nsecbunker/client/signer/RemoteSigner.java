package xyz.tcheeric.nsecbunker.client.signer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Remote signer that performs NIP-46 operations against nsecBunker.
 */
public interface RemoteSigner extends AutoCloseable {

    /**
     * Establishes a connection to the bunker and performs the NIP-46 connect handshake.
     *
     * @return a future that completes when connected
     */
    CompletableFuture<Void> connect();

    /**
     * Disconnects and releases resources.
     *
     * @return a future that completes when disconnected
     */
    CompletableFuture<Void> disconnect();

    /**
     * Whether the signer is connected.
     *
     * @return true if connected
     */
    boolean isConnected();

    /**
     * Signs an event JSON remotely.
     *
     * @param eventJson unsigned event json
     * @return a future with signature or signed event (implementation-defined)
     */
    CompletableFuture<String> signEvent(String eventJson);

    /**
     * Gets the remote public key.
     *
     * @return a future with the public key (hex)
     */
    CompletableFuture<String> getPublicKey();

    /**
     * Encrypts plaintext using NIP-04 with the given recipient pubkey.
     *
     * @param pubkeyHex recipient pubkey hex
     * @param plaintext text to encrypt
     * @return a future with ciphertext
     */
    CompletableFuture<String> nip04Encrypt(String pubkeyHex, String plaintext);

    /**
     * Decrypts NIP-04 ciphertext from a sender.
     *
     * @param pubkeyHex sender pubkey hex
     * @param ciphertext ciphertext to decrypt
     * @return a future with plaintext
     */
    CompletableFuture<String> nip04Decrypt(String pubkeyHex, String ciphertext);

    /**
     * Encrypts plaintext using NIP-44 with the given recipient pubkey.
     *
     * @param pubkeyHex recipient pubkey hex
     * @param plaintext text to encrypt
     * @return a future with ciphertext
     */
    CompletableFuture<String> nip44Encrypt(String pubkeyHex, String plaintext);

    /**
     * Decrypts NIP-44 ciphertext from a sender.
     *
     * @param pubkeyHex sender pubkey hex
     * @param ciphertext ciphertext to decrypt
     * @return a future with plaintext
     */
    CompletableFuture<String> nip44Decrypt(String pubkeyHex, String ciphertext);

    /**
     * Pings the bunker for health check.
     *
     * @return a future with pong text
     */
    CompletableFuture<String> ping();

    /**
     * Requests signing permissions for the provided methods.
     *
     * @param methods methods to request (e.g., sign_event, nip04_encrypt)
     * @return a future with the bunker response (implementation-defined)
     */
    CompletableFuture<String> requestPermissions(List<String> methods);

    @Override
    void close();
}
