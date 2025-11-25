package xyz.tcheeric.nsecbunker.monitoring.logging;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a single bunker log entry.
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEntry {

    @JsonProperty("timestamp")
    Instant timestamp;

    @JsonProperty("key_name")
    String keyName;

    @JsonProperty("user_pubkey")
    String userPubkey;

    @JsonProperty("method")
    String method;

    @JsonProperty("result")
    String result;

    @JsonProperty("metadata")
    Map<String, Object> metadata;
}
