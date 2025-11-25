package xyz.tcheeric.nsecbunker.monitoring.health;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Circuit breaker pattern implementation for protecting against cascading failures.
 *
 * <p>The circuit breaker has three states:
 * <ul>
 *   <li>CLOSED - Normal operation, requests pass through</li>
 *   <li>OPEN - Failure threshold exceeded, requests fail fast</li>
 *   <li>HALF_OPEN - Testing if the system has recovered</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * CircuitBreaker breaker = CircuitBreaker.builder()
 *     .name("bunker-requests")
 *     .failureThreshold(5)
 *     .resetTimeout(Duration.ofSeconds(30))
 *     .build();
 *
 * try {
 *     String result = breaker.execute(() -> {
 *         return remoteBunkerCall();
 *     });
 * } catch (CircuitBreakerOpenException e) {
 *     // Circuit is open, fail fast
 * }
 * }</pre>
 */
@Slf4j
public class CircuitBreaker {

    /**
     * Circuit breaker states.
     */
    public enum State {
        /** Normal operation, requests pass through. */
        CLOSED,
        /** Failure threshold exceeded, requests fail fast. */
        OPEN,
        /** Testing recovery, limited requests allowed. */
        HALF_OPEN
    }

    /**
     * Name of this circuit breaker (for logging).
     */
    @Getter
    private final String name;

    /**
     * Number of failures before opening the circuit.
     */
    @Getter
    private final int failureThreshold;

    /**
     * Number of successes required to close from half-open.
     */
    @Getter
    private final int successThreshold;

    /**
     * Duration to wait before transitioning from OPEN to HALF_OPEN.
     */
    @Getter
    private final Duration resetTimeout;

    /**
     * Current state.
     */
    private final AtomicReference<State> state;

    /**
     * Consecutive failure count.
     */
    private final AtomicInteger failureCount;

    /**
     * Success count in half-open state.
     */
    private final AtomicInteger halfOpenSuccessCount;

    /**
     * When the circuit was opened.
     */
    private volatile Instant openedAt;

    /**
     * Optional listener for state changes.
     */
    private volatile CircuitBreakerListener listener;

    @Builder
    private CircuitBreaker(String name, int failureThreshold, int successThreshold, Duration resetTimeout) {
        this.name = name != null ? name : "default";
        this.failureThreshold = failureThreshold > 0 ? failureThreshold : 5;
        this.successThreshold = successThreshold > 0 ? successThreshold : 3;
        this.resetTimeout = resetTimeout != null ? resetTimeout : Duration.ofSeconds(30);
        this.state = new AtomicReference<>(State.CLOSED);
        this.failureCount = new AtomicInteger(0);
        this.halfOpenSuccessCount = new AtomicInteger(0);
    }

    /**
     * Executes an action through the circuit breaker.
     *
     * @param action the action to execute
     * @param <T> the return type
     * @return the result of the action
     * @throws CircuitBreakerOpenException if the circuit is open
     * @throws Exception if the action throws
     */
    public <T> T execute(Supplier<T> action) throws Exception {
        if (!allowRequest()) {
            throw new CircuitBreakerOpenException(
                    "Circuit breaker '" + name + "' is open, rejecting request");
        }

        try {
            T result = action.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure();
            throw e;
        }
    }

    /**
     * Executes a void action through the circuit breaker.
     *
     * @param action the action to execute
     * @throws CircuitBreakerOpenException if the circuit is open
     * @throws Exception if the action throws
     */
    public void executeVoid(Runnable action) throws Exception {
        execute(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Checks if a request should be allowed.
     *
     * @return true if the request can proceed
     */
    public boolean allowRequest() {
        State currentState = state.get();

        switch (currentState) {
            case CLOSED:
                return true;

            case OPEN:
                // Check if reset timeout has passed
                if (openedAt != null && Instant.now().isAfter(openedAt.plus(resetTimeout))) {
                    if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                        halfOpenSuccessCount.set(0);
                        log.info("Circuit breaker '{}' transitioning to HALF_OPEN", name);
                        notifyStateChange(State.OPEN, State.HALF_OPEN);
                    }
                    return true;
                }
                return false;

            case HALF_OPEN:
                // Allow limited requests in half-open state
                return true;

            default:
                return false;
        }
    }

    /**
     * Records a successful request.
     */
    public void recordSuccess() {
        State currentState = state.get();

        if (currentState == State.HALF_OPEN) {
            int successes = halfOpenSuccessCount.incrementAndGet();
            if (successes >= successThreshold) {
                if (state.compareAndSet(State.HALF_OPEN, State.CLOSED)) {
                    failureCount.set(0);
                    openedAt = null;
                    log.info("Circuit breaker '{}' closed after {} successes", name, successes);
                    notifyStateChange(State.HALF_OPEN, State.CLOSED);
                }
            }
        } else if (currentState == State.CLOSED) {
            // Reset failure count on success in closed state
            failureCount.set(0);
        }
    }

    /**
     * Records a failed request.
     */
    public void recordFailure() {
        State currentState = state.get();

        if (currentState == State.HALF_OPEN) {
            // Any failure in half-open goes back to open
            if (state.compareAndSet(State.HALF_OPEN, State.OPEN)) {
                openedAt = Instant.now();
                log.warn("Circuit breaker '{}' re-opened due to failure in HALF_OPEN state", name);
                notifyStateChange(State.HALF_OPEN, State.OPEN);
            }
        } else if (currentState == State.CLOSED) {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                if (state.compareAndSet(State.CLOSED, State.OPEN)) {
                    openedAt = Instant.now();
                    log.warn("Circuit breaker '{}' opened after {} failures", name, failures);
                    notifyStateChange(State.CLOSED, State.OPEN);
                }
            }
        }
    }

    /**
     * Gets the current state.
     *
     * @return the current state
     */
    public State getState() {
        // Check for state transition due to timeout
        if (state.get() == State.OPEN && openedAt != null &&
                Instant.now().isAfter(openedAt.plus(resetTimeout))) {
            if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                halfOpenSuccessCount.set(0);
                notifyStateChange(State.OPEN, State.HALF_OPEN);
            }
        }
        return state.get();
    }

    /**
     * Checks if the circuit is closed (normal operation).
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return getState() == State.CLOSED;
    }

    /**
     * Checks if the circuit is open (rejecting requests).
     *
     * @return true if open
     */
    public boolean isOpen() {
        return getState() == State.OPEN;
    }

    /**
     * Checks if the circuit is half-open (testing recovery).
     *
     * @return true if half-open
     */
    public boolean isHalfOpen() {
        return getState() == State.HALF_OPEN;
    }

    /**
     * Gets the current failure count.
     *
     * @return the failure count
     */
    public int getFailureCount() {
        return failureCount.get();
    }

    /**
     * Gets the duration since the circuit was opened.
     *
     * @return the duration, or null if not open
     */
    public Duration getOpenDuration() {
        if (openedAt == null) {
            return null;
        }
        return Duration.between(openedAt, Instant.now());
    }

    /**
     * Gets the time remaining until reset timeout.
     *
     * @return the remaining time, or null if not applicable
     */
    public Duration getTimeUntilReset() {
        if (openedAt == null || state.get() != State.OPEN) {
            return null;
        }
        Instant resetTime = openedAt.plus(resetTimeout);
        if (Instant.now().isAfter(resetTime)) {
            return Duration.ZERO;
        }
        return Duration.between(Instant.now(), resetTime);
    }

    /**
     * Forces the circuit to close (reset).
     */
    public void reset() {
        State oldState = state.getAndSet(State.CLOSED);
        failureCount.set(0);
        halfOpenSuccessCount.set(0);
        openedAt = null;
        log.info("Circuit breaker '{}' forcibly reset from {}", name, oldState);
        if (oldState != State.CLOSED) {
            notifyStateChange(oldState, State.CLOSED);
        }
    }

    /**
     * Forces the circuit to open.
     */
    public void trip() {
        State oldState = state.getAndSet(State.OPEN);
        openedAt = Instant.now();
        log.warn("Circuit breaker '{}' forcibly tripped from {}", name, oldState);
        if (oldState != State.OPEN) {
            notifyStateChange(oldState, State.OPEN);
        }
    }

    /**
     * Sets a listener for state changes.
     *
     * @param listener the listener
     */
    public void setListener(CircuitBreakerListener listener) {
        this.listener = listener;
    }

    /**
     * Gets a snapshot of the circuit breaker status.
     *
     * @return the status snapshot
     */
    public CircuitBreakerStatus getStatus() {
        return CircuitBreakerStatus.builder()
                .name(name)
                .state(getState())
                .failureCount(failureCount.get())
                .failureThreshold(failureThreshold)
                .successThreshold(successThreshold)
                .resetTimeout(resetTimeout)
                .openDuration(getOpenDuration())
                .timeUntilReset(getTimeUntilReset())
                .timestamp(Instant.now())
                .build();
    }

    private void notifyStateChange(State oldState, State newState) {
        CircuitBreakerListener l = listener;
        if (l != null) {
            try {
                l.onStateChange(this, oldState, newState);
            } catch (Exception e) {
                log.warn("Error in circuit breaker listener: {}", e.getMessage());
            }
        }
    }

    /**
     * Listener interface for circuit breaker state changes.
     */
    public interface CircuitBreakerListener {
        /**
         * Called when the circuit breaker state changes.
         *
         * @param breaker the circuit breaker
         * @param oldState the previous state
         * @param newState the new state
         */
        void onStateChange(CircuitBreaker breaker, State oldState, State newState);
    }

    /**
     * Exception thrown when the circuit breaker is open.
     */
    public static class CircuitBreakerOpenException extends Exception {
        public CircuitBreakerOpenException(String message) {
            super(message);
        }
    }

    /**
     * Status snapshot of the circuit breaker.
     */
    @lombok.Value
    @Builder
    public static class CircuitBreakerStatus {
        String name;
        State state;
        int failureCount;
        int failureThreshold;
        int successThreshold;
        Duration resetTimeout;
        Duration openDuration;
        Duration timeUntilReset;
        Instant timestamp;
    }
}
