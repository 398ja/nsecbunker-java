package xyz.tcheeric.nsecbunker.monitoring.logging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.nsecbunker.admin.AdminException;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link LogReader} using the admin client.
 */
@Slf4j
public class DefaultLogReader implements LogReader {

    private static final String METHOD_GET_LOGS = "get_logs";

    private final NsecBunkerAdminClient adminClient;
    private final ObjectMapper objectMapper;

    public DefaultLogReader(NsecBunkerAdminClient adminClient) {
        this(adminClient, createDefaultObjectMapper());
    }

    public DefaultLogReader(NsecBunkerAdminClient adminClient, ObjectMapper objectMapper) {
        this.adminClient = Objects.requireNonNull(adminClient, "adminClient must not be null");
        this.objectMapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();
    }

    @Override
    public CompletableFuture<List<LogEntry>> getLogs(String keyName, LogFilter filter) {
        Objects.requireNonNull(keyName, "keyName must not be null");
        List<String> params = List.of(keyName, serializeFilter(filter));
        Nip46Request request = Nip46Request.builder()
                .method(METHOD_GET_LOGS)
                .params(params)
                .build();

        return adminClient.sendRequest(request)
                .thenApply(response -> unwrap(response, METHOD_GET_LOGS))
                .thenApply(this::parseLogs);
    }

    @Override
    public CompletableFuture<String> exportLogs(String keyName, LogFilter filter, Format format) {
        return getLogs(keyName, filter).thenApply(logs -> {
            if (format == Format.JSON) {
                return toJson(logs);
            }
            return toCsv(logs);
        });
    }

    @Override
    public CompletableFuture<Void> streamLogs(String keyName, LogFilter filter, Consumer<List<LogEntry>> consumer) {
        return getLogs(keyName, filter).thenAccept(consumer);
    }

    private String serializeFilter(LogFilter filter) {
        if (filter == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(filter);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize filter: " + e.getMessage(), e);
        }
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

    private List<LogEntry> parseLogs(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<LogEntry> entries = objectMapper.readValue(json, new TypeReference<List<LogEntry>>() {});
            return List.copyOf(entries);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to parse logs: " + e.getMessage(), e);
        }
    }

    private String toJson(List<LogEntry> logs) {
        try {
            return objectMapper.writeValueAsString(logs);
        } catch (JsonProcessingException e) {
            throw new AdminException("Failed to export logs as JSON: " + e.getMessage(), e);
        }
    }

    private String toCsv(List<LogEntry> logs) {
        String header = "timestamp,key_name,user_pubkey,method,result";
        String body = logs.stream()
                .map(l -> String.join(",",
                        safe(l.getTimestamp()),
                        safe(l.getKeyName()),
                        safe(l.getUserPubkey()),
                        safe(l.getMethod()),
                        safe(l.getResult())))
                .collect(Collectors.joining("\n"));
        return header + (body.isEmpty() ? "" : "\n" + body);
    }

    private String safe(Object value) {
        return value == null ? "" : value.toString();
    }

    public static ObjectMapper createDefaultObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
