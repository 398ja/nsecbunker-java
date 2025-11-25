package xyz.tcheeric.nsecbunker.monitoring.health;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateHealthResultTest {

    @Test
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();
        HealthCheckResult bunkerHealth = HealthCheckResult.builder()
                .status(HealthStatus.UP)
                .build();
        Map<String, RelayHealthResult> relayHealth = Map.of(
                "relay1", RelayHealthResult.disconnected("relay1")
        );

        AggregateHealthResult result = AggregateHealthResult.builder()
                .overallStatus(HealthStatus.UP)
                .bunkerHealth(bunkerHealth)
                .relayHealth(relayHealth)
                .totalRelays(2)
                .healthyRelays(1)
                .connectedRelays(1)
                .summary("1/2 relays healthy")
                .checkedAt(now)
                .build();

        assertThat(result.getOverallStatus()).isEqualTo(HealthStatus.UP);
        assertThat(result.getBunkerHealth()).isEqualTo(bunkerHealth);
        assertThat(result.getRelayHealth()).hasSize(1);
        assertThat(result.getTotalRelays()).isEqualTo(2);
        assertThat(result.getHealthyRelays()).isEqualTo(1);
        assertThat(result.getConnectedRelays()).isEqualTo(1);
        assertThat(result.getSummary()).isEqualTo("1/2 relays healthy");
    }

    @Test
    void isFullyHealthyShouldReturnTrueWhenAllHealthy() {
        HealthCheckResult bunkerHealth = HealthCheckResult.builder()
                .status(HealthStatus.UP)
                .build();

        AggregateHealthResult result = AggregateHealthResult.builder()
                .overallStatus(HealthStatus.UP)
                .bunkerHealth(bunkerHealth)
                .totalRelays(3)
                .healthyRelays(3)
                .build();

        assertThat(result.isFullyHealthy()).isTrue();
    }

    @Test
    void isFullyHealthyShouldReturnFalseWhenOverallStatusDown() {
        HealthCheckResult bunkerHealth = HealthCheckResult.builder()
                .status(HealthStatus.UP)
                .build();

        AggregateHealthResult result = AggregateHealthResult.builder()
                .overallStatus(HealthStatus.DOWN)
                .bunkerHealth(bunkerHealth)
                .totalRelays(2)
                .healthyRelays(2)
                .build();

        assertThat(result.isFullyHealthy()).isFalse();
    }

    @Test
    void isFullyHealthyShouldReturnFalseWhenBunkerHealthNull() {
        AggregateHealthResult result = AggregateHealthResult.builder()
                .overallStatus(HealthStatus.UP)
                .bunkerHealth(null)
                .totalRelays(2)
                .healthyRelays(2)
                .build();

        assertThat(result.isFullyHealthy()).isFalse();
    }

    @Test
    void isFullyHealthyShouldReturnFalseWhenBunkerDown() {
        HealthCheckResult bunkerHealth = HealthCheckResult.builder()
                .status(HealthStatus.DOWN)
                .build();

        AggregateHealthResult result = AggregateHealthResult.builder()
                .overallStatus(HealthStatus.UP)
                .bunkerHealth(bunkerHealth)
                .totalRelays(2)
                .healthyRelays(2)
                .build();

        assertThat(result.isFullyHealthy()).isFalse();
    }

    @Test
    void isFullyHealthyShouldReturnFalseWhenSomeRelaysUnhealthy() {
        HealthCheckResult bunkerHealth = HealthCheckResult.builder()
                .status(HealthStatus.UP)
                .build();

        AggregateHealthResult result = AggregateHealthResult.builder()
                .overallStatus(HealthStatus.UP)
                .bunkerHealth(bunkerHealth)
                .totalRelays(3)
                .healthyRelays(2)
                .build();

        assertThat(result.isFullyHealthy()).isFalse();
    }

    @Test
    void getRelayHealthPercentageShouldReturnCorrectValue() {
        AggregateHealthResult result = AggregateHealthResult.builder()
                .totalRelays(4)
                .healthyRelays(3)
                .build();

        assertThat(result.getRelayHealthPercentage()).isEqualTo(0.75);
    }

    @Test
    void getRelayHealthPercentageShouldReturnOneWhenNoRelays() {
        AggregateHealthResult result = AggregateHealthResult.builder()
                .totalRelays(0)
                .healthyRelays(0)
                .build();

        assertThat(result.getRelayHealthPercentage()).isEqualTo(1.0);
    }

    @Test
    void getRelayHealthPercentageShouldReturnZeroWhenAllUnhealthy() {
        AggregateHealthResult result = AggregateHealthResult.builder()
                .totalRelays(3)
                .healthyRelays(0)
                .build();

        assertThat(result.getRelayHealthPercentage()).isZero();
    }

    @Test
    void builderWithTimestampShouldSetCheckedAt() {
        AggregateHealthResult result = AggregateHealthResult.builderWithTimestamp()
                .overallStatus(HealthStatus.UP)
                .build();

        assertThat(result.getCheckedAt()).isNotNull();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        AggregateHealthResult original = AggregateHealthResult.builder()
                .overallStatus(HealthStatus.UP)
                .totalRelays(3)
                .healthyRelays(3)
                .build();

        AggregateHealthResult copy = original.toBuilder()
                .overallStatus(HealthStatus.DOWN)
                .healthyRelays(1)
                .build();

        assertThat(copy.getOverallStatus()).isEqualTo(HealthStatus.DOWN);
        assertThat(copy.getTotalRelays()).isEqualTo(3);
        assertThat(copy.getHealthyRelays()).isEqualTo(1);
        assertThat(original.getOverallStatus()).isEqualTo(HealthStatus.UP);
    }

    @Test
    void equalsShouldWorkCorrectly() {
        Instant now = Instant.now();

        AggregateHealthResult result1 = AggregateHealthResult.builder()
                .overallStatus(HealthStatus.UP)
                .totalRelays(2)
                .healthyRelays(2)
                .checkedAt(now)
                .build();

        AggregateHealthResult result2 = AggregateHealthResult.builder()
                .overallStatus(HealthStatus.UP)
                .totalRelays(2)
                .healthyRelays(2)
                .checkedAt(now)
                .build();

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }
}
