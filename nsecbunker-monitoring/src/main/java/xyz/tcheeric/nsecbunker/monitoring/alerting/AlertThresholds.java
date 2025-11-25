package xyz.tcheeric.nsecbunker.monitoring.alerting;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class AlertThresholds {
    @Builder.Default
    long failureThreshold = 0;
    @Builder.Default
    double errorRateThreshold = 0.1;
}
