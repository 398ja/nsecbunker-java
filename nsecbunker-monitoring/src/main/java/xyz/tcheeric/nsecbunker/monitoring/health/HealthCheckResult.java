package xyz.tcheeric.nsecbunker.monitoring.health;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder(toBuilder = true)
public class HealthCheckResult {
    HealthStatus status;
    Long latencyMs;
    String message;
    Instant checkedAt;
}
