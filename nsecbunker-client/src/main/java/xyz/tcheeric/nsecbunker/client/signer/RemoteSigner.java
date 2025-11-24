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
     * Requests signing permissions for the provided methods.
     *
     * @param methods methods to request (e.g., sign_event, nip04_encrypt)
     * @return a future with the bunker response (implementation-defined)
     */
    CompletableFuture<String> requestPermissions(List<String> methods);

    @Override
    void close();
}
