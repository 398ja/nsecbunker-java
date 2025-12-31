package xyz.tcheeric.nsecbunker.account.nip05;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Manages NIP-05 identifiers.
 *
 * <p>This interface defines operations for creating, querying, and managing
 * NIP-05 identifiers. Implementations may store records in memory or use
 * persistent storage.</p>
 *
 * <p>For pluggable implementations, see
 * {@link xyz.tcheeric.nsecbunker.account.nip05.spi.Nip05ManagerProvider}.</p>
 */
public interface Nip05Manager {

    /**
     * Sets up a NIP-05 identifier for a username and domain.
     *
     * @param username username (e.g., "alice")
     * @param domain   domain (e.g., "example.com")
     * @return the created NIP-05 record
     */
    CompletableFuture<Nip05Record> setupNip05(String username, String domain);

    /**
     * Verifies whether a NIP-05 identifier exists and is valid.
     *
     * @param nip05 NIP-05 identifier (e.g., "alice@example.com")
     * @return true if the identifier is registered and valid
     */
    CompletableFuture<Boolean> verifyNip05(String nip05);

    /**
     * Generates the .well-known/nostr.json content for a record.
     *
     * @param record NIP-05 record
     * @return JSON string compliant with NIP-05 specification
     */
    String generateWellKnown(Nip05Record record);

    /**
     * Finds a NIP-05 record by its identifier.
     *
     * <p>Default implementation returns empty. Persistent implementations
     * should override this method to provide lookup functionality.</p>
     *
     * @param nip05 NIP-05 identifier (e.g., "alice@example.com")
     * @return the record if found, empty otherwise
     */
    default CompletableFuture<Optional<Nip05Record>> findByNip05(String nip05) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Finds all NIP-05 records for a given domain.
     *
     * <p>Default implementation returns empty list. Persistent implementations
     * should override this method to provide domain-based queries.</p>
     *
     * @param domain domain name (e.g., "example.com")
     * @return list of records for the domain
     */
    default CompletableFuture<List<Nip05Record>> findByDomain(String domain) {
        return CompletableFuture.completedFuture(List.of());
    }

    /**
     * Finds a NIP-05 record by public key.
     *
     * <p>Default implementation returns empty. Persistent implementations
     * should override this method to provide pubkey-based lookup.</p>
     *
     * @param pubkey public key in hex format (64 characters)
     * @return the record if found, empty otherwise
     */
    default CompletableFuture<Optional<Nip05Record>> findByPubkey(String pubkey) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Deletes a NIP-05 record by its identifier.
     *
     * <p>Default implementation does nothing. Persistent implementations
     * should override this method to provide deletion functionality.</p>
     *
     * @param nip05 NIP-05 identifier to delete
     * @return true if the record was deleted, false if not found
     */
    default CompletableFuture<Boolean> deleteNip05(String nip05) {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Updates the relay list for a NIP-05 record.
     *
     * <p>Default implementation does nothing. Persistent implementations
     * should override this method to provide update functionality.</p>
     *
     * @param nip05  NIP-05 identifier
     * @param relays list of relay URLs
     * @return the updated record if found, empty otherwise
     */
    default CompletableFuture<Optional<Nip05Record>> updateRelays(String nip05, List<String> relays) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Counts the total number of NIP-05 records.
     *
     * <p>Default implementation returns 0. Persistent implementations
     * should override this method to provide accurate counts.</p>
     *
     * @return total number of records
     */
    default CompletableFuture<Long> count() {
        return CompletableFuture.completedFuture(0L);
    }

    /**
     * Counts the number of NIP-05 records for a given domain.
     *
     * <p>Default implementation returns 0. Persistent implementations
     * should override this method to provide accurate counts.</p>
     *
     * @param domain domain name
     * @return number of records for the domain
     */
    default CompletableFuture<Long> countByDomain(String domain) {
        return CompletableFuture.completedFuture(0L);
    }
}
