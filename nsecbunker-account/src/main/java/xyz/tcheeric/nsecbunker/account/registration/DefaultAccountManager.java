package xyz.tcheeric.nsecbunker.account.registration;

import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link AccountManager}.
 */
@Slf4j
public class DefaultAccountManager implements AccountManager {

    private final Map<String, AccountRegistrationResult> accounts = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<AccountRegistrationResult> createAccount(String username, String domain) {
        validate(username, domain);
        String nip05 = username + "@" + domain;
        String npub = "npub_" + username + "_" + domain;
        AccountRegistrationResult result = AccountRegistrationResult.builder()
                .username(username)
                .domain(domain)
                .nip05(nip05)
                .npub(npub)
                .keyName("key-" + username)
                .createdAt(Instant.now())
                .build();
        accounts.put(nip05, result);
        log.debug("Created account {}", nip05);
        return CompletableFuture.completedFuture(result);
    }

    @Override
    public CompletableFuture<AccountRegistrationResult> registerAccount(String username, String domain) {
        return generateKeyForAccount(username)
                .thenCompose(keyName -> createAccount(username, domain)
                        .thenApply(res -> res.toBuilder().keyName(keyName).build()));
    }

    @Override
    public CompletableFuture<String> generateKeyForAccount(String username) {
        Objects.requireNonNull(username, "username must not be null");
        return CompletableFuture.completedFuture("key-" + username);
    }

    /**
     * Retrieves a stored account.
     *
     * @param nip05 nip05 identifier
     * @return account result or null
     */
    public AccountRegistrationResult find(String nip05) {
        return accounts.get(nip05);
    }

    private void validate(String username, String domain) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username must not be blank");
        }
        if (domain == null || domain.isBlank()) {
            throw new IllegalArgumentException("domain must not be blank");
        }
    }
}
