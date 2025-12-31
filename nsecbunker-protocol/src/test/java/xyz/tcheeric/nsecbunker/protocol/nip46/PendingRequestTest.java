package xyz.tcheeric.nsecbunker.protocol.nip46;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link PendingRequest}.
 */
class PendingRequestTest {

    @Test
    void constructorCreatesValidPendingRequest() {
        Nip46Request request = Nip46Request.ping();
        PendingRequest pending = new PendingRequest(request);

        assertEquals(request.getId(), pending.getId());
        assertEquals("ping", pending.getMethod());
        assertTrue(pending.isPending());
        assertFalse(pending.isCompleted());
        assertFalse(pending.isTimedOut());
        assertFalse(pending.isFailed());
    }

    @Test
    void constructorWithCustomTimeout() {
        Nip46Request request = Nip46Request.ping();
        Duration timeout = Duration.ofMinutes(5);

        PendingRequest pending = new PendingRequest(request, timeout);

        assertEquals(timeout, pending.getTimeout());
    }

    @Test
    void constructorWithNullRequestThrows() {
        assertThrows(NullPointerException.class, () ->
                new PendingRequest(null));
    }

    @Test
    void constructorWithNullTimeoutThrows() {
        assertThrows(NullPointerException.class, () ->
                new PendingRequest(Nip46Request.ping(), null));
    }

    @Test
    void completeWithResponse() throws Exception {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());
        Nip46Response response = Nip46Response.pong(pending.getId());

        boolean completed = pending.complete(response);

        assertTrue(completed);
        assertTrue(pending.isCompleted());
        assertFalse(pending.isPending());
        assertEquals(response, pending.getFuture().get());
    }

    @Test
    void completeWithNullResponseThrows() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());

        assertThrows(NullPointerException.class, () ->
                pending.complete(null));
    }

    @Test
    void completeAlreadyCompletedReturnsFalse() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());
        Nip46Response response1 = Nip46Response.pong(pending.getId());
        Nip46Response response2 = Nip46Response.success(pending.getId(), "other");

        pending.complete(response1);
        boolean secondComplete = pending.complete(response2);

        assertFalse(secondComplete);
    }

    @Test
    void completeExceptionallyWithException() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());
        RuntimeException exception = new RuntimeException("Test error");

        boolean completed = pending.completeExceptionally(exception);

        assertTrue(completed);
        assertTrue(pending.isFailed());
        assertFalse(pending.isPending());
        assertFalse(pending.isCompleted());

        ExecutionException ex = assertThrows(ExecutionException.class, () ->
                pending.getFuture().get());
        assertEquals(exception, ex.getCause());
    }

    @Test
    void completeExceptionallyWithNullExceptionThrows() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());

        assertThrows(NullPointerException.class, () ->
                pending.completeExceptionally(null));
    }

    @Test
    void timeoutSetsTimedOutFlag() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());

        boolean timedOut = pending.timeout();

        assertTrue(timedOut);
        assertTrue(pending.isTimedOut());
        assertTrue(pending.isFailed());
        assertFalse(pending.isPending());

        ExecutionException ex = assertThrows(ExecutionException.class, () ->
                pending.getFuture().get());
        assertInstanceOf(TimeoutException.class, ex.getCause());
    }

    @Test
    void timeoutAlreadyCompletedReturnsFalse() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());
        pending.complete(Nip46Response.pong(pending.getId()));

        boolean timedOut = pending.timeout();

        assertFalse(timedOut);
        assertFalse(pending.isTimedOut());
    }

    @Test
    void cancelPendingRequest() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());

        boolean cancelled = pending.cancel();

        assertTrue(cancelled);
        assertFalse(pending.isPending());
        assertTrue(pending.getFuture().isCancelled());
    }

    @Test
    void cancelAlreadyCompletedReturnsFalse() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());
        pending.complete(Nip46Response.pong(pending.getId()));

        boolean cancelled = pending.cancel();

        assertFalse(cancelled);
    }

    @Test
    void getElapsedReturnsPositiveDuration() throws InterruptedException {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());

        Thread.sleep(50);

        Duration elapsed = pending.getElapsed();
        assertTrue(elapsed.toMillis() >= 50);
    }

    @Test
    void getElapsedAfterCompletionIsFixed() throws InterruptedException {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());

        Thread.sleep(50);
        pending.complete(Nip46Response.pong(pending.getId()));
        Duration elapsedAtComplete = pending.getElapsed();

        Thread.sleep(50);
        Duration elapsedLater = pending.getElapsed();

        // Elapsed should be fixed after completion (within small margin)
        assertTrue(Math.abs(elapsedAtComplete.toMillis() - elapsedLater.toMillis()) < 10);
    }

    @Test
    void getRemainingReturnsPositiveDuration() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping(), Duration.ofSeconds(30));

        Duration remaining = pending.getRemaining();

        assertTrue(remaining.toSeconds() > 0);
        assertTrue(remaining.toSeconds() <= 30);
    }

    @Test
    void getRemainingReturnsZeroAfterCompletion() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());
        pending.complete(Nip46Response.pong(pending.getId()));

        Duration remaining = pending.getRemaining();

        assertEquals(Duration.ZERO, remaining);
    }

    @Test
    void getRemainingReturnsZeroAfterTimeout() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());
        pending.timeout();

        Duration remaining = pending.getRemaining();

        assertEquals(Duration.ZERO, remaining);
    }

    @Test
    void toStringContainsRelevantInfo() {
        Nip46Request request = Nip46Request.ping();
        PendingRequest pending = new PendingRequest(request);

        String str = pending.toString();

        assertTrue(str.contains(request.getId()));
        assertTrue(str.contains("ping"));
        assertTrue(str.contains("pending=true"));
    }

    @Test
    void getCreatedAtIsSet() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());

        assertNotNull(pending.getCreatedAt());
    }

    @Test
    void getCompletedAtIsNullBeforeCompletion() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());

        assertNull(pending.getCompletedAt());
    }

    @Test
    void getCompletedAtIsSetAfterCompletion() {
        PendingRequest pending = new PendingRequest(Nip46Request.ping());
        pending.complete(Nip46Response.pong(pending.getId()));

        assertNotNull(pending.getCompletedAt());
    }

    @Test
    void getRequestReturnsOriginalRequest() {
        Nip46Request request = Nip46Request.signEvent("{\"kind\":1}");
        PendingRequest pending = new PendingRequest(request);

        assertEquals(request, pending.getRequest());
    }
}
