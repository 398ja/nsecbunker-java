package xyz.tcheeric.nsecbunker.monitoring.alerting;

import xyz.tcheeric.nsecbunker.monitoring.metrics.MetricsRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DefaultAlertManager implements AlertManager {

    @Override
    public List<Alert> evaluate(MetricsRecord metrics, AlertThresholds thresholds) {
        Objects.requireNonNull(metrics, "metrics must not be null");
        AlertThresholds th = thresholds != null ? thresholds : AlertThresholds.builder().build();

        List<Alert> alerts = new ArrayList<>();
        if (metrics.getFailures() > th.getFailureThreshold()) {
            alerts.add(Alert.builder()
                    .type("FAILURE_THRESHOLD")
                    .message("Failures " + metrics.getFailures() + " exceeded threshold " + th.getFailureThreshold())
                    .build());
        }
        if (metrics.getCount() > 0) {
            double rate = (double) metrics.getFailures() / (double) metrics.getCount();
            if (rate > th.getErrorRateThreshold()) {
                alerts.add(Alert.builder()
                        .type("ERROR_RATE")
                        .message("Error rate " + rate + " exceeded threshold " + th.getErrorRateThreshold())
                        .build());
            }
        }
        return alerts;
    }
}
