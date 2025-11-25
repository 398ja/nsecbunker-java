package xyz.tcheeric.nsecbunker.monitoring.metrics;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Represents a metrics snapshot for usage tracking.
 *
 * <p>Contains counts of operations and failures for a specific key,
 * token, or user.
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MetricsRecord.MetricsRecordBuilder.class)
public class MetricsRecord {

    /**
     * Unique identifier for this metrics record.
     */
    String id;

    /**
     * Name of the bunker key these metrics apply to.
     */
    String keyName;

    /**
     * Token ID if metrics are token-scoped, null otherwise.
     */
    String tokenId;

    /**
     * User public key if metrics are user-scoped, null otherwise.
     */
    String userPubkey;

    /**
     * Total number of operations.
     */
    long count;

    /**
     * Number of failed operations.
     */
    long failures;

    /**
     * When these metrics were last updated.
     */
    Instant lastUpdated;

    @JsonPOJOBuilder(withPrefix = "")
    public static class MetricsRecordBuilder {
    }
}
