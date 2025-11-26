package xyz.tcheeric.nsecbunker.chaos;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;
import xyz.tcheeric.nsecbunker.protocol.nip46.PendingRequestManager;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Chaos-style tests for {@link PendingRequestManager} under heavy churn and duplicates.
 */
class PendingRequestManagerChaosTest {

    private PendingRequestManager manager = new PendingRequestManager(Duration.ofMillis(200));

    @AfterEach
    void tearDown() {
        manager.shutdown();
    }

    /**
     * Ensures duplicate completions do not leak pending requests under load.
     */
    @Test
    @DisplayName("Should clear pending requests under duplicate completions")
    void shouldClearPendingRequestsUnderDuplicates() {
        List<CompletableFuture<Nip46Response>> futures = new ArrayList<>();

        for (int i = 0; i < 50; i++) {
            Nip46Request req = Nip46Request.builder()
                    .id("req-" + i)
                    .method("ping")
                    .build();
            CompletableFuture<Nip46Response> future = manager.register(req, Duration.ofSeconds(1));
            futures.add(future);

            // Complete the same request twice to simulate duplicate responses
            assertThat(manager.complete(Nip46Response.success(req.getId(), "pong"))).isTrue();
            assertThat(manager.complete(Nip46Response.success(req.getId(), "pong-dup"))).isFalse();
        }

        // All futures should be done and no pending requests should remain
        futures.forEach(f -> assertThat(f).isCompleted());
        assertThat(manager.getPendingCount()).isZero();
    }

    /**
     * Ensures mass cancellations do not leave stranded pending entries.
     */
    @Test
    @DisplayName("Should cancel all pending without leaks")
    void shouldCancelAllPendingWithoutLeaks() {
        for (int i = 0; i < 30; i++) {
            Nip46Request req = Nip46Request.builder()
                    .id("cancel-" + i)
                    .method("ping")
                    .build();
            manager.register(req, Duration.ofSeconds(1));
        }

        assertThat(manager.getPendingCount()).isEqualTo(30);

        assertThatCode(() -> manager.cancelAll()).doesNotThrowAnyException();
        assertThat(manager.getPendingCount()).isZero();
    }
}
