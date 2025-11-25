package xyz.tcheeric.nsecbunker.monitoring.metrics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import xyz.tcheeric.nsecbunker.admin.AdminException;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link MetricsCollector} using the admin client.
 */
public class DefaultMetricsCollector implements MetricsCollector {

    private static final String METHOD_KEY_STATS = "get_key_statistics";
    private static final String METHOD_TOKEN_STATS = "get_token_statistics";
    private static final String METHOD_USER_STATS = "get_user_statistics";
    private static final String METHOD_POLICY_STATS = "get_policy_statistics";

    private final NsecBunkerAdminClient adminClient;
    private final ObjectMapper objectMapper;

    public DefaultMetricsCollector(NsecBunkerAdminClient adminClient) {
        this(adminClient, createDefaultObjectMapper());
    }

    public DefaultMetricsCollector(NsecBunkerAdminClient adminClient, ObjectMapper objectMapper) {
        this.adminClient = Objects.requireNonNull(adminClient, "adminClient must not be null");
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
    }

    @Override
    public CompletableFuture<MetricsRecord> getKeyStatistics(String keyName) {
        return query(METHOD_KEY_STATS, keyName);
    }

    @Override
    public CompletableFuture<MetricsRecord> getTokenStatistics(String tokenId) {
        return query(METHOD_TOKEN_STATS, tokenId);
    }

    @Override
    public CompletableFuture<MetricsRecord> getUserStatistics(String userPubkey) {
        return query(METHOD_USER_STATS, userPubkey);
    }

    @Override
    public CompletableFuture<MetricsRecord> getPolicyStatistics(String policyId) {
        return query(METHOD_POLICY_STATS, policyId);
    }

    private CompletableFuture<MetricsRecord> query(String method, String id) {
        Nip46Request request = Nip46Request.builder()
                .method(method)
                .params(java.util.List.of(id))
                .build();

        return adminClient.sendRequest(request)
                .thenApply(response -> unwrap(response, method))
                .thenApply(this::parseRecord);
    }

    private String unwrap(Nip46Response response, String method) {
        if (response == null) {
            throw new AdminException("Null response for " + method);
        }
        if (response.isError()) {
            throw new AdminException(response.getError(), method);
        }
        return response.getResult();
    }

    private MetricsRecord parseRecord(String json) {
        if (json == null || json.isBlank()) {
            return MetricsRecord.builder().build();
        }
        try {
            return objectMapper.readValue(json, MetricsRecord.class);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to parse metrics: " + e.getMessage(), e);
        }
    }

    public static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
