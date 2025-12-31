package xyz.tcheeric.nsecbunker.protocol.nip46;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Manages pending NIP-46 requests and correlates them with responses.
 *
 * <p>This class handles:
 * <ul>
 *   <li>Registration of outgoing requests</li>
 *   <li>Matching incoming responses to pending requests</li>
 *   <li>Automatic timeout handling</li>
 *   <li>Request cancellation</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * PendingRequestManager manager = new PendingRequestManager();
 *
 * // Register a request and get a future for the response
 * Nip46Request request = Nip46Request.ping();
 * CompletableFuture<Nip46Response> future = manager.register(request);
 *
 * // When response arrives, complete the pending request
 * manager.complete(response);
 *
 * // Get the response
 * Nip46Response response = future.get(30, TimeUnit.SECONDS);
 *
 * // Cleanup
 * manager.shutdown();
 * }</pre>
 *
 * @see PendingRequest
 */
@Slf4j
public class PendingRequestManager {

    private final Map<String, PendingRequest> pendingRequests;
    private final ScheduledExecutorService scheduler;
    private final boolean ownsScheduler;

    @Getter
    private final Duration defaultTimeout;

    /**
     * Creates a new manager with default settings.
     */
    public PendingRequestManager() {
        this(Duration.ofSeconds(30));
    }

    /**
     * Creates a new manager with a custom default timeout.
     *
     * @param defaultTimeout the default timeout for requests
     */
    public PendingRequestManager(Duration defaultTimeout) {
        this(defaultTimeout, Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "nip46-timeout-scheduler");
            t.setDaemon(true);
            return t;
        }), true);
    }

    /**
     * Creates a new manager with a custom scheduler.
     *
     * @param defaultTimeout the default timeout for requests
     * @param scheduler      the scheduler for timeout tasks
     */
    public PendingRequestManager(Duration defaultTimeout, ScheduledExecutorService scheduler) {
        this(defaultTimeout, scheduler, false);
    }

    private PendingRequestManager(Duration defaultTimeout, ScheduledExecutorService scheduler,
                                  boolean ownsScheduler) {
        this.defaultTimeout = Objects.requireNonNull(defaultTimeout, "Default timeout must not be null");
        this.scheduler = Objects.requireNonNull(scheduler, "Scheduler must not be null");
        this.ownsScheduler = ownsScheduler;
        this.pendingRequests = new ConcurrentHashMap<>();
    }

    /**
     * Registers a request and returns a future for the response.
     *
     * @param request the request to register
     * @return a future that completes when the response arrives
     * @throws IllegalArgumentException if a request with the same ID is already pending
     */
    public CompletableFuture<Nip46Response> register(Nip46Request request) {
        return register(request, defaultTimeout);
    }

    /**
     * Registers a request with a custom timeout.
     *
     * @param request the request to register
     * @param timeout the timeout duration
     * @return a future that completes when the response arrives
     * @throws IllegalArgumentException if a request with the same ID is already pending
     */
    public CompletableFuture<Nip46Response> register(Nip46Request request, Duration timeout) {
        Objects.requireNonNull(request, "Request must not be null");
        Objects.requireNonNull(timeout, "Timeout must not be null");

        String id = request.getId();
        PendingRequest pending = new PendingRequest(request, timeout);

        PendingRequest existing = pendingRequests.putIfAbsent(id, pending);
        if (existing != null) {
            throw new IllegalArgumentException("Request with ID already pending: " + id);
        }

        // Schedule timeout
        ScheduledFuture<?> timeoutFuture = scheduler.schedule(
                () -> timeoutRequest(id),
                timeout.toMillis(),
                TimeUnit.MILLISECONDS);
        pending.setTimeoutFuture(timeoutFuture);

        // Clean up when future completes
        pending.getFuture().whenComplete((response, ex) -> pendingRequests.remove(id));

        log.debug("Registered pending request: {}", id);
        return pending.getFuture();
    }

    /**
     * Completes a pending request with a response.
     *
     * @param response the response
     * @return true if a matching request was found and completed
     */
    public boolean complete(Nip46Response response) {
        Objects.requireNonNull(response, "Response must not be null");

        String id = response.getId();
        PendingRequest pending = pendingRequests.get(id);

        if (pending == null) {
            log.debug("No pending request found for response ID: {}", id);
            return false;
        }

        return pending.complete(response);
    }

    /**
     * Completes a pending request with an error.
     *
     * @param requestId the request ID
     * @param exception the exception
     * @return true if a matching request was found and completed
     */
    public boolean completeExceptionally(String requestId, Throwable exception) {
        Objects.requireNonNull(requestId, "Request ID must not be null");
        Objects.requireNonNull(exception, "Exception must not be null");

        PendingRequest pending = pendingRequests.get(requestId);
        if (pending == null) {
            log.debug("No pending request found for ID: {}", requestId);
            return false;
        }

        return pending.completeExceptionally(exception);
    }

    /**
     * Cancels a pending request.
     *
     * @param requestId the request ID
     * @return true if the request was cancelled
     */
    public boolean cancel(String requestId) {
        Objects.requireNonNull(requestId, "Request ID must not be null");

        PendingRequest pending = pendingRequests.get(requestId);
        if (pending == null) {
            return false;
        }

        return pending.cancel();
    }

    /**
     * Cancels all pending requests.
     *
     * @return the number of cancelled requests
     */
    public int cancelAll() {
        int count = 0;
        for (PendingRequest pending : pendingRequests.values()) {
            if (pending.cancel()) {
                count++;
            }
        }
        log.debug("Cancelled {} pending requests", count);
        return count;
    }

    /**
     * Gets a pending request by ID.
     *
     * @param requestId the request ID
     * @return the pending request, or empty if not found
     */
    public Optional<PendingRequest> get(String requestId) {
        return Optional.ofNullable(pendingRequests.get(requestId));
    }

    /**
     * Checks if a request is pending.
     *
     * @param requestId the request ID
     * @return true if the request is pending
     */
    public boolean isPending(String requestId) {
        PendingRequest pending = pendingRequests.get(requestId);
        return pending != null && pending.isPending();
    }

    /**
     * Gets all pending requests.
     *
     * @return unmodifiable collection of pending requests
     */
    public Collection<PendingRequest> getPendingRequests() {
        return pendingRequests.values().stream()
                .filter(PendingRequest::isPending)
                .toList();
    }

    /**
     * Gets the number of pending requests.
     *
     * @return the count
     */
    public int getPendingCount() {
        return (int) pendingRequests.values().stream()
                .filter(PendingRequest::isPending)
                .count();
    }

    /**
     * Clears all completed requests from tracking.
     * Pending requests are not affected.
     */
    public void clearCompleted() {
        pendingRequests.entrySet().removeIf(entry -> !entry.getValue().isPending());
    }

    /**
     * Shuts down the manager and cancels all pending requests.
     */
    public void shutdown() {
        cancelAll();
        if (ownsScheduler) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        log.debug("PendingRequestManager shut down");
    }

    private void timeoutRequest(String requestId) {
        PendingRequest pending = pendingRequests.get(requestId);
        if (pending != null) {
            pending.timeout();
        }
    }
}
