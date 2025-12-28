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

    // Method names must match nsecbunkerd's admin interface
    static final String METHOD_GRANT_PERMISSION = "grant_permission";  // Not implemented in nsecbunkerd
    static final String METHOD_REVOKE_PERMISSION = "revoke_user";  // nsecbunkerd uses revoke_user
    static final String METHOD_LIST_KEY_USERS = "get_key_users";  // nsecbunkerd uses get_key_users
    static final String METHOD_GET_PERMISSIONS = "get_permissions";  // Not implemented in nsecbunkerd
    static final String METHOD_UPDATE_DESCRIPTION = "rename_key_user";  // nsecbunkerd uses rename_key_user

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

        // nsecbunkerd expects policyId (numeric string), not full policy JSON
        String policyId = policy.getId();
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("Policy must have an ID to grant permission");
        }

        // nsecbunkerd returns the KeyUser object directly (not ["ok"])
        return sendForResult(METHOD_GRANT_PERMISSION, List.of(keyName, userPubkey, policyId),
                "grant permission for " + keyName)
                .thenApply(result -> {
                    if (result == null || result.isBlank()) {
                        throw new AdminException("Empty response from grant_permission");
                    }
                    try {
                        // Try to parse the result as a KeyUser object directly
                        // nsecbunkerd returns: {"id":..., "key_name":..., "user_pubkey":..., ...}
                        return objectMapper.readValue(result, KeyUser.class);
                    } catch (JsonProcessingException e) {
                        // If parsing fails, return a minimal KeyUser
                        log.warn("Failed to parse grant_permission response as KeyUser: {}", result, e);
                        return KeyUser.builder()
                                .pubkeyHex(userPubkey)
                                .keyName(keyName)
                                .active(true)
                                .build();
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> revokePermission(String keyName, String userPubkey) {
        validateKeyName(keyName);
        validatePubkey(userPubkey);

        // nsecbunkerd's revoke_user expects [keyUserId] (numeric ID), not [keyName, userPubkey]
        // So we need to first query the key users to find the keyUserId
        return listKeyUsers(keyName)
                .thenCompose(users -> {
                    KeyUser user = users.stream()
                            .filter(u -> userPubkey.equals(u.getPublicKey()) || userPubkey.equals(u.getPubkeyHex()))
                            .findFirst()
                            .orElseThrow(() -> new AdminException("Key user not found for pubkey: " + userPubkey));

                    if (user.getId() == null) {
                        throw new AdminException("Key user ID not found for pubkey: " + userPubkey);
                    }

                    // Now call revoke_user with the keyUserId
                    return sendForResult(METHOD_REVOKE_PERMISSION, List.of(user.getId()),
                            "revoke permission for " + keyName);
                })
                .thenApply(result -> {
                    // Result is ["ok"]
                    return result != null && result.contains("ok");
                });
    }

    @Override
    public CompletableFuture<List<KeyUser>> listKeyUsers(String keyName) {
        validateKeyName(keyName);
        return sendForResult(METHOD_LIST_KEY_USERS, List.of(keyName), "list key users for " + keyName)
                .thenApply(this::parseUserList)
                .thenApply(users -> users.stream()
                        // Filter out inactive/revoked users - nsecbunkerd sets active: false on revoke
                        .filter(KeyUser::isActive)
                        .toList());
    }

    @Override
    public CompletableFuture<KeyUser> getPermissions(String keyName, String userPubkey) {
        validateKeyName(keyName);
        validatePubkey(userPubkey);

        // get_permissions is not implemented in nsecbunkerd
        // Use listKeyUsers to find the user instead
        return listKeyUsers(keyName)
                .thenApply(users -> users.stream()
                        .filter(u -> userPubkey.equals(u.getPublicKey()) || userPubkey.equals(u.getPubkeyHex()))
                        .findFirst()
                        .map(user -> {
                            // Ensure keyName is populated since listKeyUsers may not include it
                            if (user.getKeyName() == null) {
                                return user.toBuilder().keyName(keyName).build();
                            }
                            return user;
                        })
                        .orElseThrow(() -> new AdminException("Key user not found for pubkey: " + userPubkey)));
    }

    @Override
    public CompletableFuture<KeyUser> updateKeyUserDescription(String keyName, String userPubkey, String description) {
        validateKeyName(keyName);
        validatePubkey(userPubkey);
        if (description == null) {
            description = "";
        }

        // nsecbunkerd's rename_key_user expects [userPubkey, name], not [keyName, userPubkey, description]
        // and returns ["ok"], not the updated KeyUser
        final String finalDescription = description;
        return sendForResult(METHOD_UPDATE_DESCRIPTION, List.of(userPubkey, description),
                "update description for " + keyName)
                .thenCompose(result -> {
                    // Result is ["ok"] - now query to get the updated user
                    if (result != null && result.contains("ok")) {
                        return listKeyUsers(keyName)
                                .thenApply(users -> users.stream()
                                        .filter(u -> userPubkey.equals(u.getPublicKey()) || userPubkey.equals(u.getPubkeyHex()))
                                        .findFirst()
                                        // Return a user with the updated description if not found
                                        .orElse(KeyUser.builder()
                                                .pubkeyHex(userPubkey)
                                                .keyName(keyName)
                                                .description(finalDescription)
                                                .build()));
                    }
                    throw new AdminException("Failed to update key user description: " + result);
                });
    }

    private CompletableFuture<String> sendForResult(String method, List<String> params, String description) {
        Nip46Request request = Nip46Request.builder()
                .method(method)
                .params(params != null ? List.copyOf(params) : Collections.emptyList())
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
