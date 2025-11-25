package xyz.tcheeric.nsecbunker.monitoring.health;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RelayHealthResultTest {

    private static final String TEST_RELAY = "wss://relay.example.com";

    @Test
    void shouldBuildWithAllFields() {
        Instant now = Instant.now();
        Duration latency = Duration.ofMillis(50);
        Duration avgLatency = Duration.ofMillis(45);

        RelayHealthResult result = RelayHealthResult.builder()
                .relayUrl(TEST_RELAY)
                .connected(true)
                .healthy(true)
                .latency(latency)
                .averageLatency(avgLatency)
                .successRate(0.95)
                .consecutiveFailures(0)
                .errorMessage(null)
                .checkedAt(now)
                .build();

        assertThat(result.getRelayUrl()).isEqualTo(TEST_RELAY);
        assertThat(result.isConnected()).isTrue();
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getLatency()).isEqualTo(latency);
        assertThat(result.getAverageLatency()).isEqualTo(avgLatency);
        assertThat(result.getSuccessRate()).isEqualTo(0.95);
        assertThat(result.getConsecutiveFailures()).isZero();
        assertThat(result.getErrorMessage()).isNull();
    }

    @Test
    void healthyShouldCreateHealthyResult() {
        Duration latency = Duration.ofMillis(100);
        Duration avgLatency = Duration.ofMillis(80);

        RelayHealthResult result = RelayHealthResult.healthy(TEST_RELAY, latency, avgLatency, 0.99);

        assertThat(result.getRelayUrl()).isEqualTo(TEST_RELAY);
        assertThat(result.isConnected()).isTrue();
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getLatency()).isEqualTo(latency);
        assertThat(result.getAverageLatency()).isEqualTo(avgLatency);
        assertThat(result.getSuccessRate()).isEqualTo(0.99);
        assertThat(result.getConsecutiveFailures()).isZero();
        assertThat(result.getCheckedAt()).isNotNull();
    }

    @Test
    void unhealthyShouldCreateUnhealthyResult() {
        RelayHealthResult result = RelayHealthResult.unhealthy(
                TEST_RELAY,
                "Connection timeout",
                3
        );

        assertThat(result.getRelayUrl()).isEqualTo(TEST_RELAY);
        assertThat(result.isConnected()).isFalse();
        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Connection timeout");
        assertThat(result.getConsecutiveFailures()).isEqualTo(3);
        assertThat(result.getSuccessRate()).isZero();
        assertThat(result.getCheckedAt()).isNotNull();
    }

    @Test
    void disconnectedShouldCreateDisconnectedResult() {
        RelayHealthResult result = RelayHealthResult.disconnected(TEST_RELAY);

        assertThat(result.getRelayUrl()).isEqualTo(TEST_RELAY);
        assertThat(result.isConnected()).isFalse();
        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getErrorMessage()).isEqualTo("Not connected");
        assertThat(result.getCheckedAt()).isNotNull();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        RelayHealthResult original = RelayHealthResult.healthy(
                TEST_RELAY,
                Duration.ofMillis(50),
                Duration.ofMillis(45),
                0.98
        );

        RelayHealthResult copy = original.toBuilder()
                .healthy(false)
                .errorMessage("Latency too high")
                .build();

        assertThat(copy.getRelayUrl()).isEqualTo(TEST_RELAY);
        assertThat(copy.isHealthy()).isFalse();
        assertThat(copy.getErrorMessage()).isEqualTo("Latency too high");
        assertThat(original.isHealthy()).isTrue();
    }

    @Test
    void equalsShouldWorkCorrectly() {
        Instant now = Instant.now();
        Duration latency = Duration.ofMillis(50);

        RelayHealthResult result1 = RelayHealthResult.builder()
                .relayUrl(TEST_RELAY)
                .connected(true)
                .healthy(true)
                .latency(latency)
                .successRate(0.99)
                .checkedAt(now)
                .build();

        RelayHealthResult result2 = RelayHealthResult.builder()
                .relayUrl(TEST_RELAY)
                .connected(true)
                .healthy(true)
                .latency(latency)
                .successRate(0.99)
                .checkedAt(now)
                .build();

        assertThat(result1).isEqualTo(result2);
        assertThat(result1.hashCode()).isEqualTo(result2.hashCode());
    }
}
