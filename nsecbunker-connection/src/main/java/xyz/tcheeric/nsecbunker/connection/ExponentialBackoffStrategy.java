package xyz.tcheeric.nsecbunker.connection;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

/**
 * A {@link ReconnectionStrategy} that uses exponential backoff with optional jitter.
 *
 * <p>The delay between attempts increases exponentially according to:
 * <pre>
 * delay = min(initialDelay * multiplier^(attempt-1), maxDelay)
 * </pre>
 *
 * <p>With jitter enabled, random variation is added to prevent thundering herd:
 * <pre>
 * finalDelay = delay * (1 - jitter + random(0, 2*jitter))
 * </pre>
 *
 * <p>Example usage:
 * <pre>{@code
 * ReconnectionStrategy strategy = ExponentialBackoffStrategy.builder()
 *     .initialDelay(Duration.ofSeconds(1))
 *     .maxDelay(Duration.ofMinutes(5))
 *     .multiplier(2.0)
 *     .maxAttempts(10)
 *     .jitter(0.2) // 20% jitter
 *     .build();
 * }</pre>
 *
 * <p>Default values:
 * <ul>
 *   <li>Initial delay: 1 second</li>
 *   <li>Max delay: 5 minutes</li>
 *   <li>Multiplier: 2.0</li>
 *   <li>Max attempts: 10</li>
 *   <li>Jitter: 0.1 (10%)</li>
 * </ul>
 *
 * @see ReconnectionStrategy#exponentialBackoff()
 */
public final class ExponentialBackoffStrategy implements ReconnectionStrategy {

    private static final Duration DEFAULT_INITIAL_DELAY = Duration.ofSeconds(1);
    private static final Duration DEFAULT_MAX_DELAY = Duration.ofMinutes(5);
    private static final double DEFAULT_MULTIPLIER = 2.0;
    private static final int DEFAULT_MAX_ATTEMPTS = 10;
    private static final double DEFAULT_JITTER = 0.1;

    private final Duration initialDelay;
    private final Duration maxDelay;
    private final double multiplier;
    private final int maxAttempts;
    private final double jitter;

    private ExponentialBackoffStrategy(Builder builder) {
        this.initialDelay = builder.initialDelay;
        this.maxDelay = builder.maxDelay;
        this.multiplier = builder.multiplier;
        this.maxAttempts = builder.maxAttempts;
        this.jitter = builder.jitter;
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

        // Calculate base delay with exponential backoff
        double delayMillis = initialDelay.toMillis() * Math.pow(multiplier, attempt - 1);

        // Apply max delay cap
        delayMillis = Math.min(delayMillis, maxDelay.toMillis());

        // Apply jitter if configured
        if (jitter > 0) {
            double jitterFactor = 1 - jitter + (ThreadLocalRandom.current().nextDouble() * 2 * jitter);
            delayMillis = delayMillis * jitterFactor;
        }

        return Optional.of(Duration.ofMillis(Math.round(delayMillis)));
    }

    @Override
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Returns the initial delay before the first reconnection attempt.
     *
     * @return the initial delay
     */
    public Duration getInitialDelay() {
        return initialDelay;
    }

    /**
     * Returns the maximum delay between reconnection attempts.
     *
     * @return the maximum delay
     */
    public Duration getMaxDelay() {
        return maxDelay;
    }

    /**
     * Returns the multiplier applied to each subsequent delay.
     *
     * @return the multiplier
     */
    public double getMultiplier() {
        return multiplier;
    }

    /**
     * Returns the jitter factor (0.0 to 1.0).
     *
     * @return the jitter factor
     */
    public double getJitter() {
        return jitter;
    }

    /**
     * Creates a new builder for ExponentialBackoffStrategy.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    @Override
    public String toString() {
        return "ExponentialBackoffStrategy{" +
                "initialDelay=" + initialDelay +
                ", maxDelay=" + maxDelay +
                ", multiplier=" + multiplier +
                ", maxAttempts=" + (maxAttempts == -1 ? "unlimited" : maxAttempts) +
                ", jitter=" + jitter +
                '}';
    }

    /**
     * Builder for {@link ExponentialBackoffStrategy}.
     */
    public static final class Builder {
        private Duration initialDelay = DEFAULT_INITIAL_DELAY;
        private Duration maxDelay = DEFAULT_MAX_DELAY;
        private double multiplier = DEFAULT_MULTIPLIER;
        private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
        private double jitter = DEFAULT_JITTER;

        private Builder() {
        }

        /**
         * Sets the initial delay before the first reconnection attempt.
         *
         * @param initialDelay the initial delay (must be positive)
         * @return this builder
         * @throws NullPointerException     if initialDelay is null
         * @throws IllegalArgumentException if initialDelay is not positive
         */
        public Builder initialDelay(Duration initialDelay) {
            Objects.requireNonNull(initialDelay, "Initial delay must not be null");
            if (initialDelay.isNegative() || initialDelay.isZero()) {
                throw new IllegalArgumentException("Initial delay must be positive");
            }
            this.initialDelay = initialDelay;
            return this;
        }

        /**
         * Sets the maximum delay between reconnection attempts.
         *
         * @param maxDelay the maximum delay (must be positive)
         * @return this builder
         * @throws NullPointerException     if maxDelay is null
         * @throws IllegalArgumentException if maxDelay is not positive
         */
        public Builder maxDelay(Duration maxDelay) {
            Objects.requireNonNull(maxDelay, "Max delay must not be null");
            if (maxDelay.isNegative() || maxDelay.isZero()) {
                throw new IllegalArgumentException("Max delay must be positive");
            }
            this.maxDelay = maxDelay;
            return this;
        }

        /**
         * Sets the multiplier applied to each subsequent delay.
         *
         * @param multiplier the multiplier (must be >= 1.0)
         * @return this builder
         * @throws IllegalArgumentException if multiplier is less than 1.0
         */
        public Builder multiplier(double multiplier) {
            if (multiplier < 1.0) {
                throw new IllegalArgumentException("Multiplier must be >= 1.0");
            }
            this.multiplier = multiplier;
            return this;
        }

        /**
         * Sets the maximum number of reconnection attempts.
         *
         * @param maxAttempts the maximum attempts, or -1 for unlimited
         * @return this builder
         * @throws IllegalArgumentException if maxAttempts is less than -1
         */
        public Builder maxAttempts(int maxAttempts) {
            if (maxAttempts < -1) {
                throw new IllegalArgumentException("Max attempts must be -1 (unlimited) or greater");
            }
            this.maxAttempts = maxAttempts;
            return this;
        }

        /**
         * Sets unlimited reconnection attempts.
         *
         * @return this builder
         */
        public Builder unlimitedAttempts() {
            this.maxAttempts = -1;
            return this;
        }

        /**
         * Sets the jitter factor to add randomness to delays.
         *
         * <p>A jitter of 0.2 means the actual delay will be within ±20% of the
         * calculated delay.
         *
         * @param jitter the jitter factor (0.0 to 1.0)
         * @return this builder
         * @throws IllegalArgumentException if jitter is not in [0.0, 1.0]
         */
        public Builder jitter(double jitter) {
            if (jitter < 0.0 || jitter > 1.0) {
                throw new IllegalArgumentException("Jitter must be between 0.0 and 1.0");
            }
            this.jitter = jitter;
            return this;
        }

        /**
         * Disables jitter (sets jitter to 0).
         *
         * @return this builder
         */
        public Builder noJitter() {
            this.jitter = 0.0;
            return this;
        }

        /**
         * Builds the ExponentialBackoffStrategy.
         *
         * @return a new ExponentialBackoffStrategy
         * @throws IllegalStateException if maxDelay is less than initialDelay
         */
        public ExponentialBackoffStrategy build() {
            if (maxDelay.compareTo(initialDelay) < 0) {
                throw new IllegalStateException("Max delay must be >= initial delay");
            }
            return new ExponentialBackoffStrategy(this);
        }
    }
}
