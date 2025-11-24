package xyz.tcheeric.nsecbunker.admin.key;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.nsecbunker.admin.AdminException;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.core.model.BunkerKey;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link KeyManager} that uses {@link NsecBunkerAdminClient}
 * to issue NIP-46 admin requests.
 */
@Slf4j
public class DefaultKeyManager implements KeyManager {

    static final String METHOD_CREATE_NEW_KEY = "create_new_key";
    static final String METHOD_GET_KEYS = "get_keys";
    static final String METHOD_UNLOCK_KEY = "unlock_key";
    static final String METHOD_DELETE_KEY = "delete_key";
    static final String METHOD_GET_KEY = "get_key";
    static final String METHOD_ROTATE_KEY = "rotate_key";

    private final NsecBunkerAdminClient adminClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a new key manager with a default {@link ObjectMapper}.
     *
     * @param adminClient the admin client to use
     */
    public DefaultKeyManager(NsecBunkerAdminClient adminClient) {
        this(adminClient, createDefaultObjectMapper());
    }

    /**
     * Creates a new key manager with a custom {@link ObjectMapper}.
     *
     * @param adminClient the admin client to use
     * @param objectMapper the mapper used to decode bunker payloads
     */
    public DefaultKeyManager(NsecBunkerAdminClient adminClient, ObjectMapper objectMapper) {
        this.adminClient = Objects.requireNonNull(adminClient, "adminClient must not be null");
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
    }

    @Override
    public CompletableFuture<BunkerKey> createKey(String name, String nsec, String passphrase) {
        validateName(name);
        requirePassphrase(passphrase);
        if (nsec == null || nsec.isBlank()) {
            throw new IllegalArgumentException("nsec must not be null or blank");
        }

        return sendForKey(METHOD_CREATE_NEW_KEY, List.of(name, passphrase, nsec), name);
    }

    @Override
    public CompletableFuture<BunkerKey> createKey(String name, String passphrase) {
        validateName(name);
        requirePassphrase(passphrase);

        return sendForKey(METHOD_CREATE_NEW_KEY, List.of(name, passphrase), name);
    }

    @Override
    public CompletableFuture<List<BunkerKey>> listKeys() {
        return sendForResult(METHOD_GET_KEYS, Collections.emptyList(), "list keys")
                .thenApply(this::parseKeyList);
    }

    @Override
    public CompletableFuture<Boolean> unlockKey(String name, String passphrase) {
        validateName(name);
        requirePassphrase(passphrase);

        return sendForResult(METHOD_UNLOCK_KEY, List.of(name, passphrase), "unlock key " + name)
                .thenApply(ignored -> Boolean.TRUE);
    }

    @Override
    public CompletableFuture<Boolean> deleteKey(String name) {
        validateName(name);

        return sendForResult(METHOD_DELETE_KEY, List.of(name), "delete key " + name)
                .thenApply(ignored -> Boolean.TRUE);
    }

    @Override
    public CompletableFuture<BunkerKey> getKeyDetails(String name) {
        validateName(name);

        return sendForKey(METHOD_GET_KEY, List.of(name), name);
    }

    @Override
    public CompletableFuture<BunkerKey> rotateKey(String oldName, String newName, String passphrase) {
        validateName(oldName);
        validateName(newName);
        requirePassphrase(passphrase);

        return sendForKey(METHOD_ROTATE_KEY, List.of(oldName, newName, passphrase), newName);
    }

    private CompletableFuture<String> sendForResult(String method, List<String> params, String description) {
        Nip46Request request = Nip46Request.builder()
                .method(method)
                .params(params != null ? params : Collections.emptyList())
                .build();

        return adminClient.sendRequest(request)
                .thenApply(response -> unwrapResponse(response, method, description));
    }

    private CompletableFuture<BunkerKey> sendForKey(String method, List<String> params, String keyName) {
        return sendForResult(method, params, method)
                .thenApply(result -> parseKey(result, keyName));
    }

    private String unwrapResponse(Nip46Response response, String method, String description) {
        if (response == null) {
            throw new AdminException("Admin operation failed: " + description);
        }
        if (response.isError()) {
            throw new AdminException(response.getError(), method);
        }
        return response.getResult();
    }

    private BunkerKey parseKey(String result, String keyName) {
        if (result == null || result.isBlank()) {
            return BunkerKey.withName(keyName).build();
        }

        try {
            return objectMapper.readValue(result, BunkerKey.class);
        } catch (JsonProcessingException e) {
            log.debug("Falling back to minimal key for {} because parsing failed: {}", keyName, e.getMessage());
            return BunkerKey.withName(keyName)
                    .npub(result)
                    .build();
        }
    }

    private List<BunkerKey> parseKeyList(String result) {
        if (result == null || result.isBlank()) {
            return Collections.emptyList();
        }

        try {
            List<BunkerKey> keys = objectMapper.readValue(result, new TypeReference<List<BunkerKey>>() {});
            return List.copyOf(keys);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to parse key list: " + e.getMessage(), e);
        }
    }

    static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Key name must not be null or blank");
        }
    }

    private void requirePassphrase(String passphrase) {
        if (passphrase == null || passphrase.isBlank()) {
            throw new IllegalArgumentException("Passphrase must not be null or blank");
        }
    }
}
