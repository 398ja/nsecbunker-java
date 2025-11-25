package xyz.tcheeric.nsecbunker.monitoring.alerting;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.monitoring.metrics.MetricsRecord;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAlertManagerTest {

    /**
     * Ensures alerts fire on failure threshold and error rate.
     */
    @Test
    void shouldTriggerAlerts() {
        MetricsRecord metrics = MetricsRecord.builder()
                .count(10)
                .failures(3)
                .lastUpdated(Instant.now())
                .build();

        DefaultAlertManager manager = new DefaultAlertManager();
        List<Alert> alerts = manager.evaluate(metrics, AlertThresholds.builder()
                .failureThreshold(1)
                .errorRateThreshold(0.1)
                .build());

        assertThat(alerts).extracting(Alert::getType)
                .contains("FAILURE_THRESHOLD", "ERROR_RATE");
    }
}
