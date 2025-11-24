package xyz.tcheeric.nsecbunker.client.signer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Batch signer that aggregates multiple remote signing operations.
 */
public interface BatchSigner {

    /**
     * Signs multiple events in one batch.
     *
     * @param events unsigned event JSON list
     * @return future with list of results
     */
    CompletableFuture<List<String>> signEvents(List<String> events);

    /**
     * Executes multiple requests and aggregates results.
     *
     * @param operations operations to execute
     * @return future with aggregated results
     */
    CompletableFuture<List<String>> executeBatch(List<CompletableFuture<String>> operations);
}
