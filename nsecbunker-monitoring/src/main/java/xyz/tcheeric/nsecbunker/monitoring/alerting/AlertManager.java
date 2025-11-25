package xyz.tcheeric.nsecbunker.monitoring.alerting;

import xyz.tcheeric.nsecbunker.monitoring.metrics.MetricsRecord;

import java.util.List;

/**
 * Evaluates metrics against thresholds to generate alerts.
 *
 * @see DefaultAlertManager
 */
public interface AlertManager {

    /**
     * Evaluates metrics against thresholds and returns any triggered alerts.
     *
     * @param metrics    the metrics to evaluate
     * @param thresholds the alert thresholds (null uses defaults)
     * @return list of alerts triggered, may be empty
     */
    List<Alert> evaluate(MetricsRecord metrics, AlertThresholds thresholds);
}
