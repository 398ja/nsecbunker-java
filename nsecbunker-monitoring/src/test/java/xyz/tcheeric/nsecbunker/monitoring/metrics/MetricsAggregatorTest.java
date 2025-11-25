package xyz.tcheeric.nsecbunker.monitoring.metrics;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MetricsAggregatorTest {

    /**
     * Ensures records aggregate into buckets summing counts and failures.
     */
    @Test
    void shouldAggregateByBucket() {
        MetricsRecord r1 = MetricsRecord.builder()
                .count(5)
                .failures(1)
                .lastUpdated(Instant.ofEpochSecond(60))
                .build();
        MetricsRecord r2 = MetricsRecord.builder()
                .count(3)
                .failures(2)
                .lastUpdated(Instant.ofEpochSecond(90))
                .build();

        Map<Instant, MetricsRecord> result = MetricsAggregator.aggregate(
                List.of(r1, r2), Duration.ofMinutes(1));

        assertThat(result).hasSize(1);
        MetricsRecord agg = result.values().iterator().next();
        assertThat(agg.getCount()).isEqualTo(8);
        assertThat(agg.getFailures()).isEqualTo(3);
    }
}
