package xyz.tcheeric.nsecbunker.account.registration;

import java.util.concurrent.CompletableFuture;

/**
 * Manages account registration for nsecBunker.
 */
public interface AccountManager {

    /**
     * Creates an account with the given username and domain.
     *
     * @param username username
     * @param domain   domain
     * @return result of registration
     */
    CompletableFuture<AccountRegistrationResult> createAccount(String username, String domain);

    /**
     * Registers an account using the multi-step flow (create + key).
     *
     * @param username username
     * @param domain   domain
     * @return result
     */
    CompletableFuture<AccountRegistrationResult> registerAccount(String username, String domain);

    /**
     * Generates a bunker key name for the account.
     *
     * @param username username
     * @return generated key name
     */
    CompletableFuture<String> generateKeyForAccount(String username);
}
