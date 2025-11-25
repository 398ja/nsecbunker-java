package xyz.tcheeric.nsecbunker.monitoring.metrics;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Represents a basic metrics snapshot.
 */
@Value
@Builder(toBuilder = true)
public class MetricsRecord {
    String id;
    String keyName;
    String tokenId;
    String userPubkey;
    long count;
    long failures;
    Instant lastUpdated;
}
