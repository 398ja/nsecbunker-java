package xyz.tcheeric.nsecbunker.monitoring.health;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class HealthCheckResultTest {

    @Test
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();

        HealthCheckResult result = HealthCheckResult.builder()
                .status(HealthStatus.UP)
                .latencyMs(150L)
                .message("Connection successful")
                .checkedAt(now)
                .build();

        assertThat(result.getStatus()).isEqualTo(HealthStatus.UP);
        assertThat(result.getLatencyMs()).isEqualTo(150L);
        assertThat(result.getMessage()).isEqualTo("Connection successful");
        assertThat(result.getCheckedAt()).isEqualTo(now);
    }

    @Test
    void shouldBuildWithNullFields() {
        HealthCheckResult result = HealthCheckResult.builder()
                .status(HealthStatus.DOWN)
                .build();

        assertThat(result.getStatus()).isEqualTo(HealthStatus.DOWN);
        assertThat(result.getLatencyMs()).isNull();
        assertThat(result.getMessage()).isNull();
        assertThat(result.getCheckedAt()).isNull();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        HealthCheckResult original = HealthCheckResult.builder()
                .status(HealthStatus.UP)
                .latencyMs(100L)
                .build();

        HealthCheckResult copy = original.toBuilder()
                .status(HealthStatus.DOWN)
                .message("Connection lost")
                .build();

        assertThat(copy.getStatus()).isEqualTo(HealthStatus.DOWN);
        assertThat(copy.getLatencyMs()).isEqualTo(100L);
        assertThat(copy.getMessage()).isEqualTo("Connection lost");
        assertThat(original.getStatus()).isEqualTo(HealthStatus.UP);
    }

    @Test
    void equalsShouldWorkCorrectly() {
        Instant now = Instant.now();

        HealthCheckResult result1 = HealthCheckResult.builder()
                .status(HealthStatus.UP)
                .latencyMs(100L)
                .checkedAt(now)
                .build();

        HealthCheckResult result2 = HealthCheckResult.builder()
                .status(HealthStatus.UP)
                .latencyMs(100L)
                .checkedAt(now)
                .build();

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }

    @Test
    void toStringShouldContainFields() {
        HealthCheckResult result = HealthCheckResult.builder()
                .status(HealthStatus.UP)
                .latencyMs(150L)
                .message("OK")
                .build();

        String str = result.toString();
        assertThat(str).contains("UP");
        assertThat(str).contains("150");
    }
}
