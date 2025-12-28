package xyz.tcheeric.nsecbunker.admin.policy;

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
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link PolicyManager} using {@link NsecBunkerAdminClient}.
 */
@Slf4j
public class DefaultPolicyManager implements PolicyManager {

    // Method names must match nsecbunkerd's admin interface
    static final String METHOD_CREATE_POLICY = "create_new_policy";
    static final String METHOD_LIST_POLICIES = "get_policies";
    static final String METHOD_GET_POLICY = "get_policy";  // Not implemented in nsecbunkerd
    static final String METHOD_DELETE_POLICY = "delete_policy";  // Not implemented in nsecbunkerd

    private final NsecBunkerAdminClient adminClient;
    private final ObjectMapper objectMapper;

    /**
     * Creates a policy manager with the default object mapper.
     *
     * @param adminClient the admin client
     */
    public DefaultPolicyManager(NsecBunkerAdminClient adminClient) {
        this(adminClient, createDefaultObjectMapper());
    }

    /**
     * Creates a policy manager with a custom object mapper.
     *
     * @param adminClient  the admin client
     * @param objectMapper mapper used for payload serialization
     */
    public DefaultPolicyManager(NsecBunkerAdminClient adminClient, ObjectMapper objectMapper) {
        this.adminClient = Objects.requireNonNull(adminClient, "adminClient must not be null");
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
    }

    @Override
    public CompletableFuture<BunkerPolicy> createPolicy(BunkerPolicy policy) {
        Objects.requireNonNull(policy, "policy must not be null");
        if (policy.getName() == null || policy.getName().isBlank()) {
            throw new IllegalArgumentException("Policy name must not be null or blank");
        }

        String payload = toJson(policy);
        // nsecbunkerd returns ["ok"] on success, not the created policy
        // So we query the list to get the policy with its ID
        return sendForResult(METHOD_CREATE_POLICY, List.of(payload), "create policy " + policy.getName())
                .thenCompose(result -> {
                    // Result is ["ok"] - check for success
                    if (result != null && result.contains("ok")) {
                        // Query the policies list to find the created policy with its ID
                        return listPolicies()
                                .thenApply(policies -> {
                                    BunkerPolicy found = policies.stream()
                                            .filter(p -> policy.getName().equals(p.getName()))
                                            .findFirst()
                                            .orElse(null);

                                    if (found == null) {
                                        return policy; // Fall back to input policy if not found
                                    }

                                    // nsecbunkerd may not return rules or active status accurately,
                                    // so preserve the original rules and ensure active is true
                                    BunkerPolicy.BunkerPolicyBuilder builder = found.toBuilder()
                                            .active(true)  // Ensure policy is considered valid
                                            .clearRules(); // Clear any incorrect rules from server

                                    if (policy.hasRules()) {
                                        builder.rules(policy.getRules());
                                    }

                                    return builder.build();
                                });
                    }
                    throw new AdminException("Failed to create policy: " + result);
                });
    }

    /**
     * Creates a policy using a builder.
     *
     * @param builder the policy builder
     * @return a future that completes with the created policy
     */
    public CompletableFuture<BunkerPolicy> createPolicy(PolicyBuilder builder) {
        Objects.requireNonNull(builder, "builder must not be null");
        return createPolicy(builder.build());
    }

    @Override
    public CompletableFuture<List<BunkerPolicy>> listPolicies() {
        return sendForResult(METHOD_LIST_POLICIES, Collections.emptyList(), "list policies")
                .thenApply(this::parsePolicyList);
    }

    @Override
    public CompletableFuture<BunkerPolicy> getPolicy(String policyId) {
        validateId(policyId);
        return sendForPolicy(METHOD_GET_POLICY, List.of(policyId), "get policy " + policyId);
    }

    @Override
    public CompletableFuture<Boolean> deletePolicy(String policyId) {
        validateId(policyId);
        return sendForResult(METHOD_DELETE_POLICY, List.of(policyId), "delete policy " + policyId)
                .thenApply(ignored -> Boolean.TRUE);
    }

    private CompletableFuture<BunkerPolicy> sendForPolicy(String method, List<String> params, String description) {
        return sendForResult(method, params, description)
                .thenApply(this::parsePolicy);
    }

    private CompletableFuture<String> sendForResult(String method, List<String> params, String description) {
        Nip46Request request = Nip46Request.builder()
                .method(method)
                .params(params != null ? List.copyOf(params) : Collections.emptyList())
                .build();

        return adminClient.sendRequest(request)
                .thenApply(response -> unwrapResponse(response, method, description));
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

    private BunkerPolicy parsePolicy(String result) {
        if (result == null || result.isBlank()) {
            throw new AdminException("Policy response was empty");
        }

        try {
            return objectMapper.readValue(result, BunkerPolicy.class);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to parse policy: " + e.getMessage(), e);
        }
    }

    private List<BunkerPolicy> parsePolicyList(String result) {
        if (result == null || result.isBlank()) {
            return Collections.emptyList();
        }

        try {
            List<BunkerPolicy> policies = objectMapper.readValue(result, new TypeReference<List<BunkerPolicy>>() {});
            return List.copyOf(policies);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to parse policy list: " + e.getMessage(), e);
        }
    }

    private String toJson(BunkerPolicy policy) {
        try {
            return objectMapper.writeValueAsString(policy);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to serialize policy: " + e.getMessage(), e);
        }
    }

    private void validateId(String policyId) {
        if (policyId == null || policyId.isBlank()) {
            throw new IllegalArgumentException("Policy id must not be null or blank");
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
