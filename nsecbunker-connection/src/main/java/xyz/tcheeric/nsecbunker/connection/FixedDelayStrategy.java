package xyz.tcheeric.nsecbunker.connection;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * A {@link ReconnectionStrategy} that uses a fixed delay between reconnection attempts.
 *
 * <p>This strategy waits the same amount of time between each attempt,
 * up to a maximum number of attempts (or unlimited if maxAttempts is -1).
 *
 * <p>Example usage:
 * <pre>{@code
 * // Reconnect every 5 seconds, up to 10 attempts
 * ReconnectionStrategy strategy = new FixedDelayStrategy(Duration.ofSeconds(5), 10);
 *
 * // Reconnect every 30 seconds, unlimited attempts
 * ReconnectionStrategy strategy = new FixedDelayStrategy(Duration.ofSeconds(30), -1);
 * }</pre>
 *
 * @see ReconnectionStrategy#fixedDelay(Duration, int)
 */
public final class FixedDelayStrategy implements ReconnectionStrategy {

    private final Duration delay;
    private final int maxAttempts;

    /**
     * Creates a new fixed delay strategy.
     *
     * @param delay       the delay between reconnection attempts (must be positive)
     * @param maxAttempts the maximum number of attempts, or -1 for unlimited
     * @throws NullPointerException     if delay is null
     * @throws IllegalArgumentException if delay is negative or zero, or maxAttempts is less than -1
     */
    public FixedDelayStrategy(Duration delay, int maxAttempts) {
        Objects.requireNonNull(delay, "Delay must not be null");
        if (delay.isNegative() || delay.isZero()) {
            throw new IllegalArgumentException("Delay must be positive");
        }
        if (maxAttempts < -1) {
            throw new IllegalArgumentException("Max attempts must be -1 (unlimited) or greater");
        }

        this.delay = delay;
        this.maxAttempts = maxAttempts;
    }

    @Override
    public boolean shouldReconnect(int attempt) {
        if (maxAttempts == -1) {
            return true;
        }
        return attempt <= maxAttempts;
    }

    @Override
    public Optional<Duration> getDelay(int attempt) {
        if (!shouldReconnect(attempt)) {
            return Optional.empty();
        }
        return Optional.of(delay);
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Returns the fixed delay between attempts.
     *
     * @return the delay duration
     */
    public Duration getDelay() {
        return delay;
    }

    @Override
    public String toString() {
        return "FixedDelayStrategy{delay=" + delay +
                ", maxAttempts=" + (maxAttempts == -1 ? "unlimited" : maxAttempts) + "}";
    }
}
