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
 *
 * <p>Log entries capture signing operations and other bunker activities
 * for audit and monitoring purposes.
 */
@Value
@Builder(toBuilder = true)
@Jacksonized
@JsonIgnoreProperties(ignoreUnknown = true)
public class LogEntry {

    /**
     * When the operation occurred.
     */
    @JsonProperty("timestamp")
    Instant timestamp;

    /**
     * Name of the key used for the operation.
     */
    @JsonProperty("key_name")
    String keyName;

    /**
     * Public key of the user who performed the operation.
     */
    @JsonProperty("user_pubkey")
    String userPubkey;

    /**
     * NIP-46 method that was called (e.g., "sign_event", "get_public_key").
     */
    @JsonProperty("method")
    String method;

    /**
     * Result of the operation ("success", "error", or error message).
     */
    @JsonProperty("result")
    String result;

    /**
     * Additional metadata about the operation.
     */
    @JsonProperty("metadata")
    Map<String, Object> metadata;
}
