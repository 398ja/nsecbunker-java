package xyz.tcheeric.nsecbunker.monitoring.metrics;

import java.util.concurrent.CompletableFuture;

/**
 * Collects usage metrics from nsecBunker.
 */
public interface MetricsCollector {

    CompletableFuture<MetricsRecord> getKeyStatistics(String keyName);

    CompletableFuture<MetricsRecord> getTokenStatistics(String tokenId);

    CompletableFuture<MetricsRecord> getUserStatistics(String userPubkey);

    CompletableFuture<MetricsRecord> getPolicyStatistics(String policyId);
}
