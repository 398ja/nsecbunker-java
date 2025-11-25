package xyz.tcheeric.nsecbunker.account.nip05;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import xyz.tcheeric.nsecbunker.account.registration.AccountManager;
import xyz.tcheeric.nsecbunker.account.registration.AccountRegistrationResult;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of {@link Nip05Manager}.
 */
public class DefaultNip05Manager implements Nip05Manager {

    private final AccountManager accountManager;
    private final ObjectMapper objectMapper;
    private final Map<String, Nip05Record> records = new ConcurrentHashMap<>();

    public DefaultNip05Manager(AccountManager accountManager) {
        this(accountManager, new ObjectMapper());
    }

    public DefaultNip05Manager(AccountManager accountManager, ObjectMapper objectMapper) {
        this.accountManager = Objects.requireNonNull(accountManager, "accountManager must not be null");
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public CompletableFuture<Nip05Record> setupNip05(String username, String domain) {
        Objects.requireNonNull(username, "username must not be null");
        Objects.requireNonNull(domain, "domain must not be null");
        return accountManager.createAccount(username, domain)
                .thenApply(this::toRecord)
                .thenApply(record -> {
                    records.put(record.getNip05(), record);
                    return record;
                });
    }

    @Override
    public CompletableFuture<Boolean> verifyNip05(String nip05) {
        return CompletableFuture.completedFuture(records.containsKey(nip05));
    }

    @Override
    public String generateWellKnown(Nip05Record record) {
        Objects.requireNonNull(record, "record must not be null");
        Map<String, Object> json = Map.of(
                "names", Map.of(record.getNip05().split("@")[0], record.getPubkey()),
                "relays", record.getRelaysJson() != null ? record.getRelaysJson() : "[]"
        );
        try {
            return objectMapper.writeValueAsString(json);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to generate NIP-05 JSON: " + e.getMessage(), e);
        }
    }

    private Nip05Record toRecord(AccountRegistrationResult result) {
        return Nip05Record.builder()
                .nip05(result.getNip05())
                .pubkey(result.getNpub())
                .relaysJson("[]")
                .build();
    }
}
