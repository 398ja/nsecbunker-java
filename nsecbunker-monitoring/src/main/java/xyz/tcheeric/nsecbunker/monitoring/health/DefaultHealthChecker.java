package xyz.tcheeric.nsecbunker.monitoring.health;

import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;
import xyz.tcheeric.nsecbunker.connection.ConnectionHealth;
import xyz.tcheeric.nsecbunker.connection.RelayConnection;
import xyz.tcheeric.nsecbunker.connection.RelayPool;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * Default implementation of {@link HealthChecker}.
 *
 * <p>Performs health checks on the bunker and relay connections.
 */
public class DefaultHealthChecker implements HealthChecker {

    private final NsecBunkerAdminClient adminClient;
    private final RelayPool relayPool;

    /**
     * Creates a health checker with admin client only.
     *
     * @param adminClient the admin client
     */
    public DefaultHealthChecker(NsecBunkerAdminClient adminClient) {
        this(adminClient, null);
    }

    /**
     * Creates a health checker with admin client and relay pool.
     *
     * @param adminClient the admin client
     * @param relayPool the relay pool (may be null)
     */
    public DefaultHealthChecker(NsecBunkerAdminClient adminClient, RelayPool relayPool) {
        this.adminClient = Objects.requireNonNull(adminClient, "adminClient must not be null");
        this.relayPool = relayPool;
    }

    @Override
    public CompletableFuture<HealthCheckResult> checkBunker() {
        long start = System.currentTimeMillis();
        return adminClient.ping()
                .thenApply(pong -> HealthCheckResult.builder()
                        .status("pong".equalsIgnoreCase(pong) ? HealthStatus.UP : HealthStatus.DOWN)
                        .latencyMs(System.currentTimeMillis() - start)
                        .message("pong".equalsIgnoreCase(pong) ? "OK" : "Unexpected pong: " + pong)
                        .checkedAt(Instant.now())
                        .build())
                .exceptionally(ex -> HealthCheckResult.builder()
                        .status(HealthStatus.DOWN)
                        .message(ex.getMessage())
                        .latencyMs(System.currentTimeMillis() - start)
                        .checkedAt(Instant.now())
                        .build());
    }

    @Override
    public CompletableFuture<Map<String, RelayHealthResult>> checkRelays() {
        return CompletableFuture.supplyAsync(() -> {
            Map<String, RelayHealthResult> results = new HashMap<>();

            if (relayPool == null) {
                return results;
            }

            Map<String, ConnectionHealth> healthMap = relayPool.getHealthMap();
            for (Map.Entry<String, ConnectionHealth> entry : healthMap.entrySet()) {
                String url = entry.getKey();
                ConnectionHealth health = entry.getValue();
                results.put(url, toRelayHealthResult(url, health));
            }

            return results;
        });
    }

    @Override
    public CompletableFuture<RelayHealthResult> checkRelay(String relayUrl) {
        return CompletableFuture.supplyAsync(() -> {
            if (relayPool == null) {
                return RelayHealthResult.disconnected(relayUrl);
            }

            RelayConnection relay = relayPool.getRelay(relayUrl);
            if (relay == null) {
                return RelayHealthResult.unhealthy(relayUrl, "Relay not found in pool", 0);
            }

            ConnectionHealth health = relay.getHealth();
            return toRelayHealthResult(relayUrl, health);
        });
    }

    @Override
    public CompletableFuture<AggregateHealthResult> checkAll() {
        CompletableFuture<HealthCheckResult> bunkerFuture = checkBunker();
        CompletableFuture<Map<String, RelayHealthResult>> relaysFuture = checkRelays();

        return bunkerFuture.thenCombine(relaysFuture, (bunkerResult, relayResults) -> {
            int totalRelays = relayResults.size();
            int healthyRelays = (int) relayResults.values().stream()
                    .filter(RelayHealthResult::isHealthy)
                    .count();
            int connectedRelays = (int) relayResults.values().stream()
                    .filter(RelayHealthResult::isConnected)
                    .count();

            // Overall status is UP only if bunker is UP and at least one relay is healthy
            HealthStatus overallStatus;
            String summary;

            if (bunkerResult.getStatus() == HealthStatus.DOWN) {
                overallStatus = HealthStatus.DOWN;
                summary = "Bunker is down: " + bunkerResult.getMessage();
            } else if (totalRelays > 0 && healthyRelays == 0) {
                overallStatus = HealthStatus.DOWN;
                summary = "No healthy relays available";
            } else {
                overallStatus = HealthStatus.UP;
                summary = String.format("Bunker OK, %d/%d relays healthy", healthyRelays, totalRelays);
            }

            return AggregateHealthResult.builder()
                    .overallStatus(overallStatus)
                    .bunkerHealth(bunkerResult)
                    .relayHealth(relayResults)
                    .totalRelays(totalRelays)
                    .healthyRelays(healthyRelays)
                    .connectedRelays(connectedRelays)
                    .summary(summary)
                    .checkedAt(Instant.now())
                    .build();
        });
    }

    /**
     * Converts ConnectionHealth to RelayHealthResult.
     */
    private RelayHealthResult toRelayHealthResult(String url, ConnectionHealth health) {
        if (health == null) {
            return RelayHealthResult.disconnected(url);
        }

        if (!health.isHealthy()) {
            return RelayHealthResult.builder()
                    .relayUrl(url)
                    .connected(health.getState() != null &&
                            health.getState() == xyz.tcheeric.nsecbunker.connection.ConnectionState.CONNECTED)
                    .healthy(false)
                    .latency(health.getLatency())
                    .averageLatency(health.getAverageLatency())
                    .successRate(health.getPingSuccessRate())
                    .consecutiveFailures(health.getConsecutiveFailures())
                    .errorMessage("Health check failed")
                    .checkedAt(Instant.now())
                    .build();
        }

        return RelayHealthResult.healthy(
                url,
                health.getLatency(),
                health.getAverageLatency(),
                health.getPingSuccessRate()
        );
    }
}
