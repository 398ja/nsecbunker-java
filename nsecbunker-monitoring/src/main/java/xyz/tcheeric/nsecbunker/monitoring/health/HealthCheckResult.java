package xyz.tcheeric.nsecbunker.monitoring.health;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

/**
 * Result of a health check operation.
 *
 * <p>Contains the status, response latency, and any diagnostic message.
 */
@Value
@Builder(toBuilder = true)
public class HealthCheckResult {

    /**
     * Health status (UP or DOWN).
     */
    HealthStatus status;

    /**
     * Response latency in milliseconds, or null if unavailable.
     */
    Long latencyMs;

    /**
     * Diagnostic message, typically an error description when DOWN.
     */
    String message;

    /**
     * When the health check was performed.
     */
    Instant checkedAt;
}
