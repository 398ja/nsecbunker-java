package xyz.tcheeric.nsecbunker.admin.key;

import xyz.tcheeric.nsecbunker.core.model.BunkerKey;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages bunker keys through the admin NIP-46 interface.
 *
 * <p>Implementations provide async-first operations for creating, listing,
 * unlocking, deleting, and rotating keys stored in nsecBunker.</p>
 */
public interface KeyManager {

    /**
     * Imports an existing nsec into the bunker.
     *
     * @param name       the key name to register
     * @param nsec       the bech32 encoded private key to import
     * @param passphrase the passphrase used to encrypt the key in the bunker
     * @return a future that completes with the created key metadata
     */
    CompletableFuture<BunkerKey> createKey(String name, String nsec, String passphrase);

    /**
     * Generates a new key in the bunker.
     *
     * @param name       the key name to register
     * @param passphrase the passphrase used to encrypt the generated key
     * @return a future that completes with the created key metadata
     */
    CompletableFuture<BunkerKey> createKey(String name, String passphrase);

    /**
     * Lists all keys registered in the bunker.
     *
     * @return a future that completes with the list of keys (possibly empty)
     */
    CompletableFuture<List<BunkerKey>> listKeys();

    /**
     * Unlocks an encrypted key inside the bunker.
     *
     * @param name       the key name
     * @param passphrase the passphrase used to decrypt the key
     * @return a future that completes with true when the operation succeeds
     */
    CompletableFuture<Boolean> unlockKey(String name, String passphrase);

    /**
     * Deletes a key from the bunker.
     *
     * @param name the key name
     * @return a future that completes with true when the operation succeeds
     */
    CompletableFuture<Boolean> deleteKey(String name);

    /**
     * Retrieves metadata for a single key.
     *
     * @param name the key name
     * @return a future that completes with the key metadata
     */
    CompletableFuture<BunkerKey> getKeyDetails(String name);

    /**
     * Rotates a key by creating a new key and migrating permissions.
     *
     * @param oldName    the existing key name
     * @param newName    the new key name
     * @param passphrase the passphrase to encrypt the rotated key
     * @return a future that completes with the new key metadata
     */
    CompletableFuture<BunkerKey> rotateKey(String oldName, String newName, String passphrase);
}
