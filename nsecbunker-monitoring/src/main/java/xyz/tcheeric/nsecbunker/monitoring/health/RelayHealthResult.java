package xyz.tcheeric.nsecbunker.monitoring.health;

import lombok.Builder;
import lombok.Value;

import java.time.Duration;
import java.time.Instant;

/**
 * Health check result for a single relay connection.
 */
@Value
@Builder(toBuilder = true)
public class RelayHealthResult {

    /**
     * The relay URL.
     */
    String relayUrl;

    /**
     * Whether the relay is connected.
     */
    boolean connected;

    /**
     * Whether the relay is considered healthy.
     */
    boolean healthy;

    /**
     * Current latency measurement.
     */
    Duration latency;

    /**
     * Average latency over recent measurements.
     */
    Duration averageLatency;

    /**
     * Ping success rate (0.0 to 1.0).
     */
    double successRate;

    /**
     * Number of consecutive failures.
     */
    int consecutiveFailures;

    /**
     * Error message if unhealthy.
     */
    String errorMessage;

    /**
     * When the check was performed.
     */
    Instant checkedAt;

    /**
     * Creates a healthy result for a relay.
     *
     * @param relayUrl the relay URL
     * @param latency the measured latency
     * @param avgLatency the average latency
     * @param successRate the success rate
     * @return a healthy result
     */
    public static RelayHealthResult healthy(String relayUrl, Duration latency,
                                            Duration avgLatency, double successRate) {
        return RelayHealthResult.builder()
                .relayUrl(relayUrl)
                .connected(true)
                .healthy(true)
                .latency(latency)
                .averageLatency(avgLatency)
                .successRate(successRate)
                .consecutiveFailures(0)
                .checkedAt(Instant.now())
                .build();
    }

    /**
     * Creates an unhealthy result for a relay.
     *
     * @param relayUrl the relay URL
     * @param errorMessage the error message
     * @param consecutiveFailures number of consecutive failures
     * @return an unhealthy result
     */
    public static RelayHealthResult unhealthy(String relayUrl, String errorMessage,
                                              int consecutiveFailures) {
        return RelayHealthResult.builder()
                .relayUrl(relayUrl)
                .connected(false)
                .healthy(false)
                .errorMessage(errorMessage)
                .consecutiveFailures(consecutiveFailures)
                .successRate(0.0)
                .checkedAt(Instant.now())
                .build();
    }

    /**
     * Creates a disconnected result for a relay.
     *
     * @param relayUrl the relay URL
     * @return a disconnected result
     */
    public static RelayHealthResult disconnected(String relayUrl) {
        return RelayHealthResult.builder()
                .relayUrl(relayUrl)
                .connected(false)
                .healthy(false)
                .errorMessage("Not connected")
                .checkedAt(Instant.now())
                .build();
    }
}
