package xyz.tcheeric.nsecbunker.monitoring.metrics;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Aggregates metrics into time buckets.
 */
public final class MetricsAggregator {

    private MetricsAggregator() {
        // Utility
    }

    public static Map<Instant, MetricsRecord> aggregate(List<MetricsRecord> records, Duration bucketSize) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyMap();
        }
        Duration bucket = bucketSize != null ? bucketSize : Duration.ofMinutes(1);
        Map<Instant, MetricsRecord> result = new TreeMap<>();
        for (MetricsRecord record : records) {
            Instant ts = record.getLastUpdated() != null ? record.getLastUpdated() : Instant.EPOCH;
            long bucketSeconds = (ts.getEpochSecond() / bucket.getSeconds()) * bucket.getSeconds();
            Instant bucketStart = Instant.ofEpochSecond(bucketSeconds);
            MetricsRecord existing = result.get(bucketStart);
            if (existing == null) {
                result.put(bucketStart, record);
            } else {
                result.put(bucketStart, existing.toBuilder()
                        .count(existing.getCount() + record.getCount())
                        .failures(existing.getFailures() + record.getFailures())
                        .lastUpdated(ts.isAfter(existing.getLastUpdated()) ? ts : existing.getLastUpdated())
                        .build());
            }
        }
        return result;
    }
}
