package xyz.tcheeric.nsecbunker.monitoring.metrics;

import java.util.concurrent.CompletableFuture;

/**
 * Collects usage metrics from nsecBunker.
 *
 * @see DefaultMetricsCollector
 */
public interface MetricsCollector {

    /**
     * Gets usage statistics for a specific key.
     *
     * @param keyName the key name
     * @return future with the metrics record
     */
    CompletableFuture<MetricsRecord> getKeyStatistics(String keyName);

    /**
     * Gets usage statistics for a specific token.
     *
     * @param tokenId the token ID
     * @return future with the metrics record
     */
    CompletableFuture<MetricsRecord> getTokenStatistics(String tokenId);

    /**
     * Gets usage statistics for a specific user.
     *
     * @param userPubkey the user's public key
     * @return future with the metrics record
     */
    CompletableFuture<MetricsRecord> getUserStatistics(String userPubkey);

    /**
     * Gets usage statistics for a specific policy.
     *
     * @param policyId the policy ID
     * @return future with the metrics record
     */
    CompletableFuture<MetricsRecord> getPolicyStatistics(String policyId);
}
