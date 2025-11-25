package xyz.tcheeric.nsecbunker.monitoring.health;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

/**
 * Aggregate health check result for all components.
 */
@Value
@Builder(toBuilder = true)
public class AggregateHealthResult {

    /**
     * Overall health status.
     */
    HealthStatus overallStatus;

    /**
     * Health check result for the bunker.
     */
    HealthCheckResult bunkerHealth;

    /**
     * Health check results for all relays.
     */
    Map<String, RelayHealthResult> relayHealth;

    /**
     * Total number of relays.
     */
    int totalRelays;

    /**
     * Number of healthy relays.
     */
    int healthyRelays;

    /**
     * Number of connected relays.
     */
    int connectedRelays;

    /**
     * Summary message.
     */
    String summary;

    /**
     * When the check was performed.
     */
    Instant checkedAt;

    /**
     * Returns whether all components are healthy.
     *
     * @return true if everything is healthy
     */
    public boolean isFullyHealthy() {
        return overallStatus == HealthStatus.UP
                && bunkerHealth != null
                && bunkerHealth.getStatus() == HealthStatus.UP
                && healthyRelays == totalRelays;
    }

    /**
     * Returns the relay health percentage (0.0 to 1.0).
     *
     * @return the health percentage
     */
    public double getRelayHealthPercentage() {
        if (totalRelays == 0) {
            return 1.0;
        }
        return (double) healthyRelays / totalRelays;
    }

    /**
     * Creates a builder pre-configured with current timestamp.
     *
     * @return a new builder
     */
    public static AggregateHealthResultBuilder builderWithTimestamp() {
        return builder().checkedAt(Instant.now());
    }
}
