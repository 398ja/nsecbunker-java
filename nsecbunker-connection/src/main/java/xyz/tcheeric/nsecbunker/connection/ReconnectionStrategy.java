package xyz.tcheeric.nsecbunker.connection;

import java.time.Duration;
import java.util.Optional;

/**
 * Strategy interface for determining reconnection behavior after connection failures.
 *
 * <p>Implementations control whether reconnection should be attempted and how long
 * to wait between attempts. Common strategies include exponential backoff,
 * fixed delay, and no reconnection.
 *
 * <p>Usage example:
 * <pre>{@code
 * // Exponential backoff with jitter
 * ReconnectionStrategy strategy = ReconnectionStrategy.exponentialBackoff()
 *     .initialDelay(Duration.ofSeconds(1))
 *     .maxDelay(Duration.ofMinutes(5))
 *     .maxAttempts(10)
 *     .withJitter(0.2)
 *     .build();
 *
 * // Fixed delay
 * ReconnectionStrategy strategy = ReconnectionStrategy.fixedDelay(Duration.ofSeconds(5), 5);
 *
 * // No reconnection
 * ReconnectionStrategy strategy = ReconnectionStrategy.none();
 * }</pre>
 *
 * @see ExponentialBackoffStrategy
 * @see FixedDelayStrategy
 * @see NoReconnectionStrategy
 */
public interface ReconnectionStrategy {

    /**
     * Determines whether a reconnection attempt should be made.
     *
     * @param attempt the current attempt number (1-based)
     * @return true if reconnection should be attempted
     */
    boolean shouldReconnect(int attempt);

    /**
     * Calculates the delay before the next reconnection attempt.
     *
     * @param attempt the current attempt number (1-based)
     * @return the delay duration, or empty if no reconnection should be attempted
     */
    Optional<Duration> getDelay(int attempt);

    /**
     * Returns the maximum number of reconnection attempts.
     *
     * @return the maximum attempts, or -1 for unlimited
     */
    int getMaxAttempts();

    /**
     * Resets the strategy state for a new connection cycle.
     *
     * <p>Called when a connection is successfully established to reset
     * any internal state tracking reconnection attempts.
     */
    default void reset() {
        // Default no-op for stateless strategies
    }

    /**
     * Called when a reconnection attempt fails.
     *
     * <p>Implementations can use this to track failure reasons and
     * adjust behavior accordingly.
     *
     * @param attempt the attempt number that failed
     * @param error   the error that caused the failure
     */
    default void onAttemptFailed(int attempt, Throwable error) {
        // Default no-op
    }

    /**
     * Creates a strategy that never reconnects.
     *
     * @return a no-reconnection strategy
     */
    static ReconnectionStrategy none() {
        return NoReconnectionStrategy.INSTANCE;
    }

    /**
     * Creates a strategy with fixed delay between attempts.
     *
     * @param delay       the delay between attempts
     * @param maxAttempts the maximum number of attempts
     * @return a fixed delay strategy
     */
    static ReconnectionStrategy fixedDelay(Duration delay, int maxAttempts) {
        return new FixedDelayStrategy(delay, maxAttempts);
    }

    /**
     * Creates a strategy with fixed delay and unlimited attempts.
     *
     * @param delay the delay between attempts
     * @return a fixed delay strategy with unlimited attempts
     */
    static ReconnectionStrategy fixedDelay(Duration delay) {
        return new FixedDelayStrategy(delay, -1);
    }

    /**
     * Creates a builder for exponential backoff strategy.
     *
     * @return a new builder
     */
    static ExponentialBackoffStrategy.Builder exponentialBackoff() {
        return ExponentialBackoffStrategy.builder();
    }

    /**
     * Creates an exponential backoff strategy with default settings.
     *
     * <p>Default settings:
     * <ul>
     *   <li>Initial delay: 1 second</li>
     *   <li>Max delay: 5 minutes</li>
     *   <li>Multiplier: 2.0</li>
     *   <li>Max attempts: 10</li>
     *   <li>Jitter: 0.1 (10%)</li>
     * </ul>
     *
     * @return an exponential backoff strategy with defaults
     */
    static ReconnectionStrategy exponentialBackoffDefault() {
        return ExponentialBackoffStrategy.builder().build();
    }
}
