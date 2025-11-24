package xyz.tcheeric.nsecbunker.protocol.nip46;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeoutException;

/**
 * Represents a pending NIP-46 request awaiting a response.
 *
 * <p>This class tracks the state of an outstanding request including:
 * <ul>
 *   <li>The original request</li>
 *   <li>A CompletableFuture for the response</li>
 *   <li>Timing information</li>
 *   <li>Timeout handling</li>
 * </ul>
 *
 * <p>Usage is typically through {@link PendingRequestManager}.
 */
@Slf4j
@Getter
public class PendingRequest {

    private final Nip46Request request;
    private final CompletableFuture<Nip46Response> future;
    private final Instant createdAt;
    private final Duration timeout;

    private volatile ScheduledFuture<?> timeoutFuture;
    private volatile Instant completedAt;
    private volatile boolean timedOut;

    /**
     * Creates a new pending request.
     *
     * @param request the NIP-46 request
     * @param timeout the timeout duration
     */
    public PendingRequest(Nip46Request request, Duration timeout) {
        this.request = Objects.requireNonNull(request, "Request must not be null");
        this.timeout = Objects.requireNonNull(timeout, "Timeout must not be null");
        this.future = new CompletableFuture<>();
        this.createdAt = Instant.now();
    }

    /**
     * Creates a new pending request with default timeout.
     *
     * @param request the NIP-46 request
     */
    public PendingRequest(Nip46Request request) {
        this(request, Duration.ofSeconds(30));
    }

    /**
     * Gets the request ID.
     *
     * @return the request ID
     */
    public String getId() {
        return request.getId();
    }

    /**
     * Gets the request method.
     *
     * @return the method name
     */
    public String getMethod() {
        return request.getMethod();
    }

    /**
     * Checks if the request is still pending.
     *
     * @return true if not yet completed or timed out
     */
    public boolean isPending() {
        return !future.isDone();
    }

    /**
     * Checks if the request completed successfully.
     *
     * @return true if completed with a response
     */
    public boolean isCompleted() {
        return future.isDone() && !timedOut && !future.isCompletedExceptionally();
    }

    /**
     * Checks if the request timed out.
     *
     * @return true if timed out
     */
    public boolean isTimedOut() {
        return timedOut;
    }

    /**
     * Checks if the request failed.
     *
     * @return true if completed exceptionally
     */
    public boolean isFailed() {
        return future.isCompletedExceptionally();
    }

    /**
     * Completes the request with a response.
     *
     * @param response the response
     * @return true if successfully completed, false if already done
     */
    public boolean complete(Nip46Response response) {
        Objects.requireNonNull(response, "Response must not be null");

        if (future.complete(response)) {
            completedAt = Instant.now();
            cancelTimeoutTask();
            log.debug("Request {} completed with response", getId());
            return true;
        }
        return false;
    }

    /**
     * Completes the request exceptionally.
     *
     * @param exception the exception
     * @return true if successfully completed, false if already done
     */
    public boolean completeExceptionally(Throwable exception) {
        Objects.requireNonNull(exception, "Exception must not be null");

        if (future.completeExceptionally(exception)) {
            completedAt = Instant.now();
            cancelTimeoutTask();
            log.debug("Request {} completed exceptionally: {}", getId(), exception.getMessage());
            return true;
        }
        return false;
    }

    /**
     * Times out the request.
     *
     * @return true if successfully timed out, false if already done
     */
    public boolean timeout() {
        TimeoutException exception = new TimeoutException(
                String.format("Request %s timed out after %s", getId(), timeout));

        if (future.completeExceptionally(exception)) {
            timedOut = true;
            completedAt = Instant.now();
            log.debug("Request {} timed out", getId());
            return true;
        }
        return false;
    }

    /**
     * Cancels the request.
     *
     * @return true if successfully cancelled
     */
    public boolean cancel() {
        if (future.cancel(false)) {
            completedAt = Instant.now();
            cancelTimeoutTask();
            log.debug("Request {} cancelled", getId());
            return true;
        }
        return false;
    }

    /**
     * Gets the elapsed time since creation.
     *
     * @return the elapsed duration
     */
    public Duration getElapsed() {
        Instant end = completedAt != null ? completedAt : Instant.now();
        return Duration.between(createdAt, end);
    }

    /**
     * Gets the remaining time before timeout.
     *
     * @return the remaining duration, or zero if already timed out
     */
    public Duration getRemaining() {
        if (timedOut || future.isDone()) {
            return Duration.ZERO;
        }
        Duration elapsed = getElapsed();
        Duration remaining = timeout.minus(elapsed);
        return remaining.isNegative() ? Duration.ZERO : remaining;
    }

    /**
     * Sets the timeout task handle for cancellation.
     *
     * @param timeoutFuture the scheduled timeout task
     */
    void setTimeoutFuture(ScheduledFuture<?> timeoutFuture) {
        this.timeoutFuture = timeoutFuture;
    }

    private void cancelTimeoutTask() {
        if (timeoutFuture != null && !timeoutFuture.isDone()) {
            timeoutFuture.cancel(false);
        }
    }

    @Override
    public String toString() {
        return String.format("PendingRequest[id=%s, method=%s, pending=%s, elapsed=%s]",
                getId(), getMethod(), isPending(), getElapsed());
    }
}
