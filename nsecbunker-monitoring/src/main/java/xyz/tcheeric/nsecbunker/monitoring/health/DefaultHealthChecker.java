package xyz.tcheeric.nsecbunker.monitoring.health;

import xyz.tcheeric.nsecbunker.admin.NsecBunkerAdminClient;

import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class DefaultHealthChecker implements HealthChecker {

    private final NsecBunkerAdminClient adminClient;

    public DefaultHealthChecker(NsecBunkerAdminClient adminClient) {
        this.adminClient = Objects.requireNonNull(adminClient, "adminClient must not be null");
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
}
