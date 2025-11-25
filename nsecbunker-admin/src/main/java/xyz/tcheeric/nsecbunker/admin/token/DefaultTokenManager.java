package xyz.tcheeric.nsecbunker.admin.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.nsecbunker.admin.AdminException;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.core.model.AccessToken;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link TokenManager} using {@link NsecBunkerAdminClient}.
 */
@Slf4j
public class DefaultTokenManager implements TokenManager {

    static final String METHOD_CREATE_TOKEN = "create_token";
    static final String METHOD_LIST_TOKENS = "list_tokens";
    static final String METHOD_GET_TOKEN = "get_token";
    static final String METHOD_REVOKE_TOKEN = "revoke_token";
    static final String METHOD_VALIDATE_TOKEN = "validate_token";

    private final NsecBunkerAdminClient adminClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a token manager with the default object mapper.
     *
     * @param adminClient the admin client for bunker communication
     */
    public DefaultTokenManager(NsecBunkerAdminClient adminClient) {
        this(adminClient, createDefaultObjectMapper());
    }

    /**
     * Creates a token manager with a custom object mapper.
     *
     * @param adminClient  the admin client for bunker communication
     * @param objectMapper custom mapper for JSON serialization
     */
    public DefaultTokenManager(NsecBunkerAdminClient adminClient, ObjectMapper objectMapper) {
        this.adminClient = Objects.requireNonNull(adminClient, "adminClient must not be null");
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
    }

    @Override
    public CompletableFuture<AccessToken> createToken(String keyName, String clientName, String policyId, Duration lifetime) {
        validateKeyName(keyName);
        validateClientName(clientName);

        List<String> params = new ArrayList<>();
        params.add(keyName);
        params.add(clientName);
        if (policyId != null && !policyId.isBlank()) {
            params.add(policyId);
        }
        if (lifetime != null) {
            params.add(Long.toString(lifetime.getSeconds()));
        }

        return sendForToken(METHOD_CREATE_TOKEN, params, "create token for " + keyName);
    }

    @Override
    public CompletableFuture<List<AccessToken>> listTokens(String keyName) {
        validateKeyName(keyName);
        return sendForResult(METHOD_LIST_TOKENS, List.of(keyName), "list tokens for " + keyName)
                .thenApply(this::parseTokenList);
    }

    @Override
    public CompletableFuture<AccessToken> getToken(String tokenId) {
        validateId(tokenId);
        return sendForToken(METHOD_GET_TOKEN, List.of(tokenId), "get token " + tokenId);
    }

    @Override
    public CompletableFuture<Boolean> revokeToken(String tokenId) {
        validateId(tokenId);
        return sendForResult(METHOD_REVOKE_TOKEN, List.of(tokenId), "revoke token " + tokenId)
                .thenApply(ignored -> Boolean.TRUE);
    }

    @Override
    public CompletableFuture<Boolean> validateToken(String token) {
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Token must not be null or blank");
        }
        return sendForResult(METHOD_VALIDATE_TOKEN, List.of(token), "validate token")
                .thenApply(result -> Boolean.parseBoolean(result));
    }

    private CompletableFuture<String> sendForResult(String method, List<String> params, String description) {
        Nip46Request request = Nip46Request.builder()
                .method(method)
                .params(params != null ? params : Collections.emptyList())
                .build();

        return adminClient.sendRequest(request)
                .thenApply(response -> unwrapResponse(response, method, description));
    }

    private CompletableFuture<AccessToken> sendForToken(String method, List<String> params, String description) {
        return sendForResult(method, params, description)
                .thenApply(this::parseToken);
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

    private AccessToken parseToken(String result) {
        if (result == null || result.isBlank()) {
            throw new AdminException("Token response was empty");
        }

        try {
            return objectMapper.readValue(result, AccessToken.class);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to parse access token: " + e.getMessage(), e);
        }
    }

    private List<AccessToken> parseTokenList(String result) {
        if (result == null || result.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<AccessToken> tokens = objectMapper.readValue(result, new TypeReference<List<AccessToken>>() {});
            return List.copyOf(tokens);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to parse token list: " + e.getMessage(), e);
        }
    }

    private void validateKeyName(String keyName) {
        if (keyName == null || keyName.isBlank()) {
            throw new IllegalArgumentException("Key name must not be null or blank");
        }
    }

    private void validateClientName(String clientName) {
        if (clientName == null || clientName.isBlank()) {
            throw new IllegalArgumentException("Client name must not be null or blank");
        }
    }

    private void validateId(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            throw new IllegalArgumentException("Token id must not be null or blank");
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
