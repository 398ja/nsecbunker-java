package xyz.tcheeric.nsecbunker.client.signer;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default batch signer that uses a {@link RemoteSigner} to perform batched operations.
 */
public final class DefaultBatchSigner implements BatchSigner {

    private final RemoteSigner signer;

    public DefaultBatchSigner(RemoteSigner signer) {
        this.signer = Objects.requireNonNull(signer, "signer must not be null");
    }

    @Override
    public CompletableFuture<List<String>> signEvents(List<String> events) {
        if (events == null || events.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (String event : events) {
            futures.add(signer.signEvent(event));
        }
        return executeBatch(futures);
    }

    @Override
    public CompletableFuture<List<String>> executeBatch(List<CompletableFuture<String>> operations) {
        if (operations == null || operations.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        CompletableFuture<Void> allDone = CompletableFuture.allOf(operations.toArray(new CompletableFuture[0]));
        return allDone.thenApply(ignored -> operations.stream()
                .map(CompletableFuture::join)
                .toList());
    }
}
