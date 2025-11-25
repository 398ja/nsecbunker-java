package xyz.tcheeric.nsecbunker.monitoring.alerting;

import xyz.tcheeric.nsecbunker.monitoring.metrics.MetricsRecord;

import java.util.List;

public interface AlertManager {

    List<Alert> evaluate(MetricsRecord metrics, AlertThresholds thresholds);
}
