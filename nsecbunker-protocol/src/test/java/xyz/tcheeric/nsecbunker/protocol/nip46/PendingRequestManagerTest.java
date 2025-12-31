package xyz.tcheeric.nsecbunker.protocol.nip46;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PendingRequestManager}.
 */
class PendingRequestManagerTest {

    private PendingRequestManager manager;

    @BeforeEach
    void setUp() {
        manager = new PendingRequestManager(Duration.ofSeconds(5));
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.shutdown();
        }
    }

    @Test
    void constructorCreatesValidManager() {
        assertNotNull(manager);
        assertEquals(Duration.ofSeconds(5), manager.getDefaultTimeout());
    }

    @Test
    void constructorWithDefaultTimeout() {
        PendingRequestManager defaultManager = new PendingRequestManager();
        assertEquals(Duration.ofSeconds(30), defaultManager.getDefaultTimeout());
        defaultManager.shutdown();
    }

    @Test
    void registerReturnsCompletableFuture() {
        Nip46Request request = Nip46Request.ping();

        CompletableFuture<Nip46Response> future = manager.register(request);

        assertNotNull(future);
        assertFalse(future.isDone());
    }

    @Test
    void registerWithNullRequestThrows() {
        assertThrows(NullPointerException.class, () ->
                manager.register(null));
    }

    @Test
    void registerDuplicateIdThrows() {
        Nip46Request request1 = Nip46Request.builder()
                .id("duplicate-id")
                .method("ping")
                .build();
        Nip46Request request2 = Nip46Request.builder()
                .id("duplicate-id")
                .method("sign_event")
                .build();

        manager.register(request1);

        assertThrows(IllegalArgumentException.class, () ->
                manager.register(request2));
    }

    @Test
    void completeMatchesResponseToRequest() throws Exception {
        Nip46Request request = Nip46Request.ping();
        CompletableFuture<Nip46Response> future = manager.register(request);

        Nip46Response response = Nip46Response.pong(request.getId());
        boolean completed = manager.complete(response);

        assertTrue(completed);
        assertTrue(future.isDone());
        assertEquals(response, future.get());
    }

    @Test
    void completeWithNullResponseThrows() {
        assertThrows(NullPointerException.class, () ->
                manager.complete(null));
    }

    @Test
    void completeWithUnknownIdReturnsFalse() {
        Nip46Response response = Nip46Response.pong("unknown-id");

        boolean completed = manager.complete(response);

        assertFalse(completed);
    }

    @Test
    void completeExceptionallyFailsRequest() {
        Nip46Request request = Nip46Request.ping();
        CompletableFuture<Nip46Response> future = manager.register(request);
        RuntimeException exception = new RuntimeException("Test error");

        boolean completed = manager.completeExceptionally(request.getId(), exception);

        assertTrue(completed);
        assertTrue(future.isCompletedExceptionally());
    }

    @Test
    void completeExceptionallyWithUnknownIdReturnsFalse() {
        boolean completed = manager.completeExceptionally("unknown-id", new RuntimeException());

        assertFalse(completed);
    }

    @Test
    void cancelRequest() {
        Nip46Request request = Nip46Request.ping();
        CompletableFuture<Nip46Response> future = manager.register(request);

        boolean cancelled = manager.cancel(request.getId());

        assertTrue(cancelled);
        assertTrue(future.isCancelled());
    }

    @Test
    void cancelUnknownIdReturnsFalse() {
        boolean cancelled = manager.cancel("unknown-id");

        assertFalse(cancelled);
    }

    @Test
    void cancelAllCancelsAllPendingRequests() {
        manager.register(Nip46Request.ping());
        manager.register(Nip46Request.getPublicKey());
        manager.register(Nip46Request.getRelays());

        int cancelled = manager.cancelAll();

        assertEquals(3, cancelled);
        assertEquals(0, manager.getPendingCount());
    }

    @Test
    void getReturnsPendingRequest() {
        Nip46Request request = Nip46Request.ping();
        manager.register(request);

        var pending = manager.get(request.getId());

        assertTrue(pending.isPresent());
        assertEquals(request.getId(), pending.get().getId());
    }

    @Test
    void getReturnsEmptyForUnknownId() {
        var pending = manager.get("unknown-id");

        assertTrue(pending.isEmpty());
    }

    @Test
    void isPendingReturnsTrueForPendingRequest() {
        Nip46Request request = Nip46Request.ping();
        manager.register(request);

        assertTrue(manager.isPending(request.getId()));
    }

    @Test
    void isPendingReturnsFalseForCompletedRequest() {
        Nip46Request request = Nip46Request.ping();
        manager.register(request);
        manager.complete(Nip46Response.pong(request.getId()));

        assertFalse(manager.isPending(request.getId()));
    }

    @Test
    void isPendingReturnsFalseForUnknownId() {
        assertFalse(manager.isPending("unknown-id"));
    }

    @Test
    void getPendingRequestsReturnsOnlyPending() {
        Nip46Request request1 = Nip46Request.ping();
        Nip46Request request2 = Nip46Request.getPublicKey();
        manager.register(request1);
        manager.register(request2);
        manager.complete(Nip46Response.pong(request1.getId()));

        Collection<PendingRequest> pending = manager.getPendingRequests();

        assertEquals(1, pending.size());
        assertEquals(request2.getId(), pending.iterator().next().getId());
    }

    @Test
    void getPendingCountReturnsCorrectCount() {
        manager.register(Nip46Request.ping());
        manager.register(Nip46Request.getPublicKey());

        assertEquals(2, manager.getPendingCount());
    }

    @Test
    void clearCompletedRemovesCompletedRequests() {
        Nip46Request request1 = Nip46Request.ping();
        Nip46Request request2 = Nip46Request.getPublicKey();
        manager.register(request1);
        manager.register(request2);
        manager.complete(Nip46Response.pong(request1.getId()));

        manager.clearCompleted();

        assertTrue(manager.get(request1.getId()).isEmpty());
        assertTrue(manager.get(request2.getId()).isPresent());
    }

    @Test
    void timeoutExpiresPendingRequest() throws Exception {
        PendingRequestManager shortTimeoutManager = new PendingRequestManager(Duration.ofMillis(100));
        try {
            Nip46Request request = Nip46Request.ping();
            CompletableFuture<Nip46Response> future = shortTimeoutManager.register(request);

            // Wait for timeout
            ExecutionException ex = assertThrows(ExecutionException.class, () ->
                    future.get(1, TimeUnit.SECONDS));
            assertInstanceOf(TimeoutException.class, ex.getCause());
        } finally {
            shortTimeoutManager.shutdown();
        }
    }

    @Test
    void registerWithCustomTimeout() throws Exception {
        Nip46Request request = Nip46Request.ping();
        CompletableFuture<Nip46Response> future = manager.register(request, Duration.ofMillis(50));

        // Should timeout quickly
        ExecutionException ex = assertThrows(ExecutionException.class, () ->
                future.get(1, TimeUnit.SECONDS));
        assertInstanceOf(TimeoutException.class, ex.getCause());
    }

    @Test
    void completedRequestIsRemovedFromTracking() throws Exception {
        Nip46Request request = Nip46Request.ping();
        CompletableFuture<Nip46Response> future = manager.register(request);

        manager.complete(Nip46Response.pong(request.getId()));

        // Wait for cleanup
        future.get();
        Thread.sleep(50);

        assertTrue(manager.get(request.getId()).isEmpty());
    }

    @Test
    void shutdownCancelsAllPending() {
        Nip46Request request = Nip46Request.ping();
        CompletableFuture<Nip46Response> future = manager.register(request);

        manager.shutdown();

        assertTrue(future.isCancelled());
    }

    @Test
    void multipleRequestsCorrelatedCorrectly() throws Exception {
        Nip46Request ping = Nip46Request.ping();
        Nip46Request getPubKey = Nip46Request.getPublicKey();
        Nip46Request getRelays = Nip46Request.getRelays();

        CompletableFuture<Nip46Response> pingFuture = manager.register(ping);
        CompletableFuture<Nip46Response> getPubKeyFuture = manager.register(getPubKey);
        CompletableFuture<Nip46Response> getRelaysFuture = manager.register(getRelays);

        // Complete in different order
        Nip46Response relaysResponse = Nip46Response.success(getRelays.getId(), "{}");
        Nip46Response pingResponse = Nip46Response.pong(ping.getId());
        Nip46Response pubKeyResponse = Nip46Response.success(getPubKey.getId(), "abc123");

        manager.complete(relaysResponse);
        manager.complete(pingResponse);
        manager.complete(pubKeyResponse);

        assertEquals(relaysResponse, getRelaysFuture.get());
        assertEquals(pingResponse, pingFuture.get());
        assertEquals(pubKeyResponse, getPubKeyFuture.get());
    }

    @Test
    void completingResponseCancelsTimeout() throws Exception {
        Nip46Request request = Nip46Request.ping();
        CompletableFuture<Nip46Response> future = manager.register(request, Duration.ofMillis(100));

        // Complete before timeout
        Nip46Response response = Nip46Response.pong(request.getId());
        manager.complete(response);

        // Wait past timeout period
        Thread.sleep(200);

        // Should still have the completed response, not a timeout
        assertEquals(response, future.get());
        assertFalse(future.isCompletedExceptionally());
    }
}
