package xyz.tcheeric.nsecbunker.it;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.monitoring.alerting.Alert;
import xyz.tcheeric.nsecbunker.monitoring.alerting.AlertThresholds;
import xyz.tcheeric.nsecbunker.monitoring.alerting.DefaultAlertManager;
import xyz.tcheeric.nsecbunker.monitoring.health.DefaultHealthChecker;
import xyz.tcheeric.nsecbunker.monitoring.health.HealthStatus;
import xyz.tcheeric.nsecbunker.monitoring.metrics.MetricsRecord;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

class MonitoringIntegrationTest {

    @Mock
    private NsecBunkerAdminClient adminClient;

    MonitoringIntegrationTest() {
        MockitoAnnotations.openMocks(this);
    }

    /**
     * Ensures health checker reports UP on pong.
     */
    @Test
    void shouldReportHealthUp() {
        when(adminClient.ping()).thenReturn(CompletableFuture.completedFuture("pong"));
        DefaultHealthChecker checker = new DefaultHealthChecker(adminClient);

        assertThat(checker.checkBunker().join().getStatus()).isEqualTo(HealthStatus.UP);
    }

    /**
     * Ensures alert manager triggers on thresholds.
     */
    @Test
    void shouldTriggerAlerts() {
        MetricsRecord metrics = MetricsRecord.builder()
                .count(10)
                .failures(3)
                .lastUpdated(Instant.now())
                .build();

        List<Alert> alerts = new DefaultAlertManager().evaluate(metrics, AlertThresholds.builder()
                .failureThreshold(1)
                .errorRateThreshold(0.1)
                .build());

        assertThat(alerts).isNotEmpty();
    }
}
