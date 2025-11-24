package xyz.tcheeric.nsecbunker.client.request;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestExecutorTest {

    /**
     * Ensures successful execution completes the pending future.
     */
    @Test
    void shouldExecuteRequestSuccessfully() {
        // Arrange
        RequestQueue queue = new RequestQueue();
        RequestExecutor executor = new RequestExecutor(queue, req ->
                CompletableFuture.completedFuture(Nip46Response.pong(req.getId())), null);

        // Act
        Nip46Response response = executor.execute(Nip46Request.ping()).join();

        // Assert
        assertThat(response.getResult()).isEqualTo("pong");
    }

    /**
     * Ensures retries are attempted on failure before surfacing error.
     */
    @Test
    void shouldRetryOnFailure() {
        // Arrange
        RequestQueue queue = new RequestQueue();
        AtomicInteger calls = new AtomicInteger();
        RequestExecutor executor = new RequestExecutor(queue, req -> {
            if (calls.incrementAndGet() < 2) {
                CompletableFuture<Nip46Response> f = new CompletableFuture<>();
                f.completeExceptionally(new RuntimeException("fail"));
                return f;
            }
            return CompletableFuture.completedFuture(Nip46Response.pong(req.getId()));
        }, RequestExecutor.Config.builder()
                .maxRetries(2)
                .retryDelay(Duration.ofMillis(10))
                .timeout(Duration.ofSeconds(1))
                .build());

        // Act
        Nip46Response response = executor.execute(Nip46Request.ping()).join();

        // Assert
        assertThat(calls.get()).isEqualTo(2);
        assertThat(response.getResult()).isEqualTo("pong");
    }

    /**
     * Ensures exceeding retries surfaces the error.
     */
    @Test
    void shouldFailAfterRetries() {
        // Arrange
        RequestQueue queue = new RequestQueue();
        RequestExecutor executor = new RequestExecutor(queue, req -> {
            CompletableFuture<Nip46Response> f = new CompletableFuture<>();
            f.completeExceptionally(new RuntimeException("fail"));
            return f;
        }, RequestExecutor.Config.builder()
                .maxRetries(1)
                .retryDelay(Duration.ofMillis(5))
                .timeout(Duration.ofMillis(100))
                .build());

        // Act + Assert
        assertThatThrownBy(() -> executor.execute(Nip46Request.ping()).join())
                .hasCauseInstanceOf(RuntimeException.class)
                .hasRootCauseMessage("fail");
    }
}
