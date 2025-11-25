package xyz.tcheeric.nsecbunker.admin.permission;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.nsecbunker.admin.AdminException;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.core.model.BunkerPolicy;
import xyz.tcheeric.nsecbunker.core.model.KeyUser;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link PermissionManager} using {@link NsecBunkerAdminClient}.
 */
@Slf4j
public class DefaultPermissionManager implements PermissionManager {

    static final String METHOD_GRANT_PERMISSION = "grant_permission";
    static final String METHOD_REVOKE_PERMISSION = "revoke_permission";
    static final String METHOD_LIST_KEY_USERS = "list_key_users";
    static final String METHOD_GET_PERMISSIONS = "get_permissions";
    static final String METHOD_UPDATE_DESCRIPTION = "update_key_user_description";

    private final NsecBunkerAdminClient adminClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a permission manager with the default object mapper.
     *
     * @param adminClient the admin client for bunker communication
     */
    public DefaultPermissionManager(NsecBunkerAdminClient adminClient) {
        this(adminClient, createDefaultObjectMapper());
    }

    /**
     * Creates a permission manager with a custom object mapper.
     *
     * @param adminClient  the admin client for bunker communication
     * @param objectMapper custom mapper for JSON serialization
     */
    public DefaultPermissionManager(NsecBunkerAdminClient adminClient, ObjectMapper objectMapper) {
        this.adminClient = Objects.requireNonNull(adminClient, "adminClient must not be null");
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
    }

    @Override
    public CompletableFuture<KeyUser> grantPermission(String keyName, String userPubkey, BunkerPolicy policy) {
        validateKeyName(keyName);
        validatePubkey(userPubkey);
        Objects.requireNonNull(policy, "policy must not be null");

        String policyJson = toJson(policy);
        return sendForUser(METHOD_GRANT_PERMISSION, List.of(keyName, userPubkey, policyJson),
                "grant permission for " + keyName);
    }

    @Override
    public CompletableFuture<Boolean> revokePermission(String keyName, String userPubkey) {
        validateKeyName(keyName);
        validatePubkey(userPubkey);
        return sendForResult(METHOD_REVOKE_PERMISSION, List.of(keyName, userPubkey),
                "revoke permission for " + keyName)
                .thenApply(ignored -> Boolean.TRUE);
    }

    @Override
    public CompletableFuture<List<KeyUser>> listKeyUsers(String keyName) {
        validateKeyName(keyName);
        return sendForResult(METHOD_LIST_KEY_USERS, List.of(keyName), "list key users for " + keyName)
                .thenApply(this::parseUserList);
    }

    @Override
    public CompletableFuture<KeyUser> getPermissions(String keyName, String userPubkey) {
        validateKeyName(keyName);
        validatePubkey(userPubkey);
        return sendForUser(METHOD_GET_PERMISSIONS, List.of(keyName, userPubkey),
                "get permissions for " + keyName);
    }

    @Override
    public CompletableFuture<KeyUser> updateKeyUserDescription(String keyName, String userPubkey, String description) {
        validateKeyName(keyName);
        validatePubkey(userPubkey);
        if (description == null) {
            description = "";
        }
        return sendForUser(METHOD_UPDATE_DESCRIPTION, List.of(keyName, userPubkey, description),
                "update description for " + keyName);
    }

    private CompletableFuture<String> sendForResult(String method, List<String> params, String description) {
        Nip46Request request = Nip46Request.builder()
                .method(method)
                .params(params != null ? params : Collections.emptyList())
                .build();

        return adminClient.sendRequest(request)
                .thenApply(response -> unwrapResponse(response, method, description));
    }

    private CompletableFuture<KeyUser> sendForUser(String method, List<String> params, String description) {
        return sendForResult(method, params, description)
                .thenApply(this::parseUser);
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

    private KeyUser parseUser(String result) {
        if (result == null || result.isBlank()) {
            throw new AdminException("Permission response was empty");
        }
        try {
            return objectMapper.readValue(result, KeyUser.class);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to parse key user: " + e.getMessage(), e);
        }
    }

    private List<KeyUser> parseUserList(String result) {
        if (result == null || result.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<KeyUser> users = objectMapper.readValue(result, new TypeReference<List<KeyUser>>() {});
            return List.copyOf(users);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to parse key user list: " + e.getMessage(), e);
        }
    }

    private String toJson(BunkerPolicy policy) {
        try {
            return objectMapper.writeValueAsString(policy);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to serialize policy: " + e.getMessage(), e);
        }
    }

    private void validateKeyName(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            throw new IllegalArgumentException("Key name must not be null or blank");
        }
    }

    private void validatePubkey(String pubkey) {
        if (pubkey == null || pubkey.isBlank()) {
            throw new IllegalArgumentException("User pubkey must not be null or blank");
        }
    }

    static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
