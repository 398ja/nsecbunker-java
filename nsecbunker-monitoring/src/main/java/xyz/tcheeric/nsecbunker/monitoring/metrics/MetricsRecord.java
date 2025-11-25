package xyz.tcheeric.nsecbunker.monitoring.metrics;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Represents a basic metrics snapshot.
 */
@Value
@Builder(toBuilder = true)
@JsonDeserialize(builder = MetricsRecord.MetricsRecordBuilder.class)
public class MetricsRecord {
    String id;
    String keyName;
    String tokenId;
    String userPubkey;
    long count;
    long failures;
    Instant lastUpdated;

    @JsonPOJOBuilder(withPrefix = "")
    public static class MetricsRecordBuilder {
    }
}
