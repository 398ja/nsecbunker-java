package xyz.tcheeric.nsecbunker.client.request;

import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks pending NIP-46 requests and their futures.
 */
public final class RequestQueue {

    private final Map<String, CompletableFuture<Nip46Response>> pending = new ConcurrentHashMap<>();

    /**
     * Registers a request and returns its future, applying the provided timeout.
     *
     * @param request the request
     * @param timeout timeout to apply
     * @return future for the response
     */
    public CompletableFuture<Nip46Response> register(Nip46Request request, Duration timeout) {
        CompletableFuture<Nip46Response> future = new CompletableFuture<>();
        pending.put(request.getId(), future);
        if (timeout != null) {
            future.orTimeout(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        }
        return future;
    }

    /**
     * Completes the future for the given response id.
     *
     * @param response the response
     * @return true if a pending request was completed
     */
    public boolean complete(Nip46Response response) {
        if (response == null || response.getId() == null) {
            return false;
        }
        CompletableFuture<Nip46Response> future = pending.remove(response.getId());
        if (future != null) {
            future.complete(response);
            return true;
        }
        return false;
    }

    /**
     * Cancels a pending request by id.
     *
     * @param requestId id to cancel
     */
    public void cancel(String requestId) {
        CompletableFuture<Nip46Response> future = pending.remove(requestId);
        if (future != null) {
            future.cancel(true);
        }
    }

    /**
     * Cancels all pending requests.
     */
    public void cancelAll() {
        pending.values().forEach(future -> future.cancel(true));
        pending.clear();
    }

    /**
     * Returns the count of pending requests.
     *
     * @return pending count
     */
    public int size() {
        return pending.size();
    }
}
