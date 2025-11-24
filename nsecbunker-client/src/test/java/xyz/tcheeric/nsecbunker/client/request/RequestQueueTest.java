package xyz.tcheeric.nsecbunker.client.request;

import org.junit.jupiter.api.Test;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Request;
import xyz.tcheeric.nsecbunker.protocol.nip46.Nip46Response;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RequestQueueTest {

    /**
     * Ensures register applies timeout and pending count reflects operations.
     */
    @Test
    void shouldRegisterAndTimeout() {
        // Arrange
        RequestQueue queue = new RequestQueue();
        Nip46Request req = Nip46Request.ping();

        // Act
        CompletableFuture<Nip46Response> future = queue.register(req, Duration.ofMillis(50));

        // Assert
        assertThat(queue.size()).isEqualTo(1);
        assertThatThrownBy(() -> future.get(200, TimeUnit.MILLISECONDS))
                .hasMessageContaining("Timeout");
        assertThat(queue.size()).isEqualTo(1); // still tracked until cancel
    }

    /**
     * Ensures completion removes from queue.
     */
    @Test
    void shouldCompleteResponse() {
        // Arrange
        RequestQueue queue = new RequestQueue();
        Nip46Request req = Nip46Request.ping();
        queue.register(req, Duration.ofSeconds(1));

        // Act
        Nip46Response response = Nip46Response.pong(req.getId());
        boolean completed = queue.complete(response);

        // Assert
        assertThat(completed).isTrue();
        assertThat(queue.size()).isZero();
    }

    /**
     * Ensures cancel removes pending future.
     */
    @Test
    void shouldCancelPending() {
        // Arrange
        RequestQueue queue = new RequestQueue();
        Nip46Request req = Nip46Request.ping();
        queue.register(req, Duration.ofSeconds(1));

        // Act
        queue.cancel(req.getId());

        // Assert
        assertThat(queue.size()).isZero();
    }
}
