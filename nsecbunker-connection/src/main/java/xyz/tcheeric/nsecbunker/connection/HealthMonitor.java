package xyz.tcheeric.nsecbunker.connection;

import java.time.Duration;
import java.util.function.Consumer;

/**
 * Interface for monitoring connection health.
 *
 * <p>Health monitors track connection metrics like latency, ping success rate,
 * and connection state. They can notify listeners when health status changes.
 *
 * <p>Usage example:
 * <pre>{@code
 * HealthMonitor monitor = new RelayHealthMonitor(connection);
 * monitor.addHealthListener(health -> {
 *     if (!health.isHealthy()) {
 *         log.warn("Connection unhealthy: {}", health.getUrl());
 *     }
 * });
 * monitor.start();
 * }</pre>
 *
 * @see RelayHealthMonitor
 * @see ConnectionHealth
 */
public interface HealthMonitor {

    /**
     * Starts the health monitor.
     *
     * <p>Once started, the monitor will periodically check connection health
     * and notify listeners of any changes.
     */
    void start();

    /**
     * Stops the health monitor.
     *
     * <p>Any pending health checks will be cancelled.
     */
    void stop();

    /**
     * Returns whether the monitor is currently running.
     *
     * @return true if the monitor is running
     */
    boolean isRunning();

    /**
     * Returns the current health status.
     *
     * @return the current health snapshot
     */
    ConnectionHealth getHealth();

    /**
     * Returns whether the connection is currently healthy.
     *
     * <p>This is a convenience method equivalent to {@code getHealth().isHealthy()}.
     *
     * @return true if the connection is healthy
     */
    default boolean isHealthy() {
        return getHealth().isHealthy();
    }

    /**
     * Triggers an immediate health check.
     *
     * <p>This is useful for on-demand health verification outside of
     * the regular monitoring interval.
     */
    void checkNow();

    /**
     * Adds a listener to be notified of health changes.
     *
     * <p>The listener will be called whenever the health status changes,
     * including latency updates and state changes.
     *
     * @param listener the health listener
     */
    void addHealthListener(Consumer<ConnectionHealth> listener);

    /**
     * Removes a health listener.
     *
     * @param listener the listener to remove
     */
    void removeHealthListener(Consumer<ConnectionHealth> listener);

    /**
     * Sets the interval between health checks.
     *
     * @param interval the check interval
     */
    void setCheckInterval(Duration interval);

    /**
     * Returns the current check interval.
     *
     * @return the check interval
     */
    Duration getCheckInterval();

    /**
     * Sets the ping timeout duration.
     *
     * <p>If a ping does not receive a pong response within this duration,
     * it is considered failed.
     *
     * @param timeout the ping timeout
     */
    void setPingTimeout(Duration timeout);

    /**
     * Returns the current ping timeout.
     *
     * @return the ping timeout
     */
    Duration getPingTimeout();

    /**
     * Sets the number of consecutive failures before marking unhealthy.
     *
     * @param threshold the failure threshold
     */
    void setUnhealthyThreshold(int threshold);

    /**
     * Returns the unhealthy threshold.
     *
     * @return the failure threshold
     */
    int getUnhealthyThreshold();
}
