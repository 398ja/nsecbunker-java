package xyz.tcheeric.nsecbunker.monitoring.health;

import java.util.concurrent.CompletableFuture;

public interface HealthChecker {

    CompletableFuture<HealthCheckResult> checkBunker();
}
