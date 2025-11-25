package xyz.tcheeric.nsecbunker.account.nip05;

import java.util.concurrent.CompletableFuture;

/**
 * Manages NIP-05 identifiers.
 */
public interface Nip05Manager {

    /**
     * Sets up a NIP-05 identifier for a username and domain.
     *
     * @param username username
     * @param domain   domain
     * @return record
     */
    CompletableFuture<Nip05Record> setupNip05(String username, String domain);

    /**
     * Verifies a NIP-05 identifier.
     *
     * @param nip05 nip05 string
     * @return true if known
     */
    CompletableFuture<Boolean> verifyNip05(String nip05);

    /**
     * Generates the .well-known JSON for the record.
     *
     * @param record nip05 record
     * @return json string
     */
    String generateWellKnown(Nip05Record record);
}
