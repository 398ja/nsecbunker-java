package xyz.tcheeric.nsecbunker.monitoring.alerting;

import lombok.Builder;
import lombok.Value;

/**
 * Configuration for alert thresholds.
 *
 * <p>Defines when alerts should be triggered based on failure counts
 * and error rates.
 */
@Value
@Builder(toBuilder = true)
public class AlertThresholds {

    /**
     * Number of failures before triggering an alert.
     * A value of 0 means no failure-based alerting.
     */
    @Builder.Default
    long failureThreshold = 0;

    /**
     * Error rate (0.0 to 1.0) that triggers an alert.
     * Default is 0.1 (10% error rate).
     */
    @Builder.Default
    double errorRateThreshold = 0.1;
}
