package xyz.tcheeric.nsecbunker.connection;

import lombok.Builder;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/**
 * Represents the health status of a relay connection.
 *
 * <p>This class provides various metrics about connection health including
 * latency measurements, connection state, and timestamps of various events.
 *
 * <p>ConnectionHealth instances are immutable snapshots of health data at
 * a specific point in time.
 */
@Getter
@Builder(toBuilder = true)
public class ConnectionHealth {

    /**
     * The relay URL this health data pertains to.
     */
    private final String url;

    /**
     * The current connection state.
     */
    private final ConnectionState state;

    /**
     * Whether the connection is currently healthy.
     *
     * <p>A connection is considered healthy if it is connected and
     * responding to pings within the expected timeout.
     */
    private final boolean healthy;

    /**
     * The most recent round-trip latency measurement.
     */
    private final Duration latency;

    /**
     * The average latency over recent measurements.
     */
    private final Duration averageLatency;

    /**
     * The minimum latency observed.
     */
    private final Duration minLatency;

    /**
     * The maximum latency observed.
     */
    private final Duration maxLatency;

    /**
     * The timestamp when this health data was captured.
     */
    private final Instant timestamp;

    /**
     * The timestamp of the last successful ping.
     */
    private final Instant lastPingTime;

    /**
     * The timestamp of the last successful pong response.
     */
    private final Instant lastPongTime;

    /**
     * The timestamp when the connection was established.
     */
    private final Instant connectedSince;

    /**
     * The number of successful pings.
     */
    private final long successfulPings;

    /**
     * The number of failed pings (timeouts).
     */
    private final long failedPings;

    /**
     * The number of consecutive failed pings.
     */
    private final int consecutiveFailures;

    /**
     * The total number of messages sent.
     */
    private final long messagesSent;

    /**
     * The total number of messages received.
     */
    private final long messagesReceived;

    /**
     * Returns the ping success rate as a percentage (0.0 to 1.0).
     *
     * @return the success rate, or 1.0 if no pings have been sent
     */
    public double getPingSuccessRate() {
        long total = successfulPings + failedPings;
        if (total == 0) {
            return 1.0;
        }
        return (double) successfulPings / total;
    }

    /**
     * Returns the connection uptime duration.
     *
     * @return the uptime, or empty if not connected
     */
    public Optional<Duration> getUptime() {
        if (connectedSince == null || state != ConnectionState.CONNECTED) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(connectedSince, Instant.now()));
    }

    /**
     * Returns the time since the last successful pong.
     *
     * @return the duration since last pong, or empty if no pong received
     */
    public Optional<Duration> getTimeSinceLastPong() {
        if (lastPongTime == null) {
            return Optional.empty();
        }
        return Optional.of(Duration.between(lastPongTime, Instant.now()));
    }

    /**
     * Creates a health snapshot indicating a disconnected state.
     *
     * @param url the relay URL
     * @return a disconnected health snapshot
     */
    public static ConnectionHealth disconnected(String url) {
        return ConnectionHealth.builder()
                .url(url)
                .state(ConnectionState.DISCONNECTED)
                .healthy(false)
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Creates an initial health snapshot for a newly connected relay.
     *
     * @param url the relay URL
     * @return a connected health snapshot
     */
    public static ConnectionHealth connected(String url) {
        Instant now = Instant.now();
        return ConnectionHealth.builder()
                .url(url)
                .state(ConnectionState.CONNECTED)
                .healthy(true)
                .timestamp(now)
                .connectedSince(now)
                .successfulPings(0)
                .failedPings(0)
                .consecutiveFailures(0)
                .messagesSent(0)
                .messagesReceived(0)
                .build();
    }

    @Override
    public String toString() {
        return "ConnectionHealth{" +
                "url='" + url + '\'' +
                ", state=" + state +
                ", healthy=" + healthy +
                ", latency=" + latency +
                ", avgLatency=" + averageLatency +
                ", successRate=" + String.format("%.1f%%", getPingSuccessRate() * 100) +
                '}';
    }
}
