package xyz.tcheeric.nsecbunker.account.registration;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Manages account registration for nsecBunker.
 *
 * <p>This interface defines operations for creating and managing user accounts
 * with associated NIP-05 identifiers. Implementations may store accounts in
 * memory or use persistent storage.</p>
 *
 * <p>For pluggable implementations, see
 * {@link xyz.tcheeric.nsecbunker.account.registration.spi.AccountManagerProvider}.</p>
 */
public interface AccountManager {

    /**
     * Creates an account with the given username and domain.
     *
     * @param username username (e.g., "alice")
     * @param domain   domain (e.g., "example.com")
     * @return result of registration including NIP-05 identifier
     */
    CompletableFuture<AccountRegistrationResult> createAccount(String username, String domain);

    /**
     * Registers an account using the multi-step flow (create + key).
     *
     * <p>This method first generates a bunker key for the account, then
     * creates the account with the generated key name.</p>
     *
     * @param username username
     * @param domain   domain
     * @return result including key information
     */
    CompletableFuture<AccountRegistrationResult> registerAccount(String username, String domain);

    /**
     * Generates a bunker key name for the account.
     *
     * @param username username
     * @return generated key name (e.g., "key-alice")
     */
    CompletableFuture<String> generateKeyForAccount(String username);

    /**
     * Finds an account by its NIP-05 identifier.
     *
     * <p>Default implementation returns empty. Persistent implementations
     * should override this method to provide lookup functionality.</p>
     *
     * @param nip05 NIP-05 identifier (e.g., "alice@example.com")
     * @return the account if found, empty otherwise
     */
    default CompletableFuture<Optional<AccountRegistrationResult>> findAccount(String nip05) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Finds an account by username.
     *
     * <p>Default implementation returns empty. Persistent implementations
     * should override this method to provide username-based lookup.</p>
     *
     * @param username username to search for
     * @return the account if found, empty otherwise
     */
    default CompletableFuture<Optional<AccountRegistrationResult>> findByUsername(String username) {
        return CompletableFuture.completedFuture(Optional.empty());
    }

    /**
     * Finds all accounts for a given domain.
     *
     * <p>Default implementation returns empty list. Persistent implementations
     * should override this method to provide domain-based queries.</p>
     *
     * @param domain domain name (e.g., "example.com")
     * @return list of accounts for the domain
     */
    default CompletableFuture<List<AccountRegistrationResult>> findByDomain(String domain) {
        return CompletableFuture.completedFuture(List.of());
    }

    /**
     * Checks if an account exists for the given NIP-05 identifier.
     *
     * <p>Default implementation returns false. Persistent implementations
     * should override this method for efficient existence checks.</p>
     *
     * @param nip05 NIP-05 identifier
     * @return true if the account exists
     */
    default CompletableFuture<Boolean> exists(String nip05) {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Deletes an account by its NIP-05 identifier.
     *
     * <p>Default implementation does nothing. Persistent implementations
     * should override this method to provide deletion functionality.</p>
     *
     * @param nip05 NIP-05 identifier to delete
     * @return true if the account was deleted, false if not found
     */
    default CompletableFuture<Boolean> deleteAccount(String nip05) {
        return CompletableFuture.completedFuture(false);
    }

    /**
     * Counts the total number of accounts.
     *
     * <p>Default implementation returns 0. Persistent implementations
     * should override this method to provide accurate counts.</p>
     *
     * @return total number of accounts
     */
    default CompletableFuture<Long> count() {
        return CompletableFuture.completedFuture(0L);
    }
}
