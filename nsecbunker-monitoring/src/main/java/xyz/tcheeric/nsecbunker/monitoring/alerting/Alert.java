package xyz.tcheeric.nsecbunker.monitoring.alerting;

import lombok.Builder;
import lombok.Value;

/**
 * Represents an alert triggered by the monitoring system.
 *
 * <p>Alerts are generated when thresholds are exceeded or health checks fail.
 */
@Value
@Builder(toBuilder = true)
public class Alert {

    /**
     * Type of alert (e.g., "error_rate", "connection_failure", "health_check").
     */
    String type;

    /**
     * Human-readable description of the alert condition.
     */
    String message;
}
