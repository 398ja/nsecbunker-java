package xyz.tcheeric.nsecbunker.client.request;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/**
 * Executes NIP-46 requests asynchronously with timeout, cancellation, and retry handling.
 */
@Slf4j
public final class RequestExecutor {

    private final RequestQueue queue;
    private final Function<Nip46Request, CompletableFuture<Nip46Response>> transport;
    private final int maxRetries;
    private final Duration retryDelay;
    private final Duration timeout;
    private final Executor scheduler;

    @Getter
    @Builder
    public static final class Config {
        @Builder.Default
        private final int maxRetries = 2;
        @Builder.Default
        private final Duration retryDelay = Duration.ofMillis(100);
        @Builder.Default
        private final Duration timeout = Duration.ofSeconds(30);
        @Builder.Default
        private final Executor scheduler = Executors.newSingleThreadExecutor();
    }

    public RequestExecutor(RequestQueue queue,
                           Function<Nip46Request, CompletableFuture<Nip46Response>> transport,
                           Config config) {
        this.queue = Objects.requireNonNull(queue, "queue must not be null");
        this.transport = Objects.requireNonNull(transport, "transport must not be null");
        Config cfg = config != null ? config : Config.builder().build();
        this.maxRetries = cfg.maxRetries;
        this.retryDelay = cfg.retryDelay;
        this.timeout = cfg.timeout;
        this.scheduler = cfg.scheduler;
    }

    /**
     * Executes a request with timeout and retry handling.
     *
     * @param request the request to execute
     * @return a future with the response
     */
    public CompletableFuture<Nip46Response> execute(Nip46Request request) {
        CompletableFuture<Nip46Response> future = queue.register(request, timeout);
        AtomicInteger attempts = new AtomicInteger(0);
        sendWithRetry(request, future, attempts);
        return future;
    }

    /**
     * Cancels all pending requests.
     */
    public void cancelAll() {
        queue.cancelAll();
    }

    private void sendWithRetry(Nip46Request request,
                               CompletableFuture<Nip46Response> future,
                               AtomicInteger attempts) {
        if (future.isDone()) {
            return;
        }
        int currentAttempt = attempts.incrementAndGet();
        transport.apply(request).whenCompleteAsync((response, error) -> {
            if (future.isDone()) {
                return;
            }
            if (error != null) {
                if (currentAttempt <= maxRetries) {
                    scheduleRetry(request, future, attempts);
                } else {
                    future.completeExceptionally(error);
                }
                return;
            }
            queue.complete(response);
        });
    }

    private void scheduleRetry(Nip46Request request,
                               CompletableFuture<Nip46Response> future,
                               AtomicInteger attempts) {
        CompletableFuture.runAsync(() -> sendWithRetry(request, future, attempts),
                CompletableFuture.delayedExecutor(retryDelay.toMillis(),
                        java.util.concurrent.TimeUnit.MILLISECONDS,
                        scheduler));
    }
}
