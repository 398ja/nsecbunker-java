package xyz.tcheeric.nsecbunker.monitoring.health;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for performing health checks on nsecBunker components.
 *
 * <p>Provides methods to check the health of the bunker itself
 * and individual relay connections.
 */
public interface HealthChecker {

    /**
     * Checks the health of the bunker by performing a ping.
     *
     * @return a future completing with the health check result
     */
    CompletableFuture<HealthCheckResult> checkBunker();

    /**
     * Checks the health of all relay connections.
     *
     * @return a future completing with a map of relay URL to health result
     */
    CompletableFuture<Map<String, RelayHealthResult>> checkRelays();

    /**
     * Checks the health of a specific relay.
     *
     * @param relayUrl the relay URL to check
     * @return a future completing with the relay health result
     */
    CompletableFuture<RelayHealthResult> checkRelay(String relayUrl);

    /**
     * Performs a comprehensive health check of all components.
     *
     * @return a future completing with the aggregate health result
     */
    CompletableFuture<AggregateHealthResult> checkAll();
}
