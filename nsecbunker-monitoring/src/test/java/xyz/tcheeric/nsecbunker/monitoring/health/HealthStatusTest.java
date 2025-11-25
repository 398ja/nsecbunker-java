package xyz.tcheeric.nsecbunker.monitoring.health;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HealthStatusTest {

    @Test
    void shouldHaveUpStatus() {
        assertThat(HealthStatus.UP).isNotNull();
        assertThat(HealthStatus.UP.name()).isEqualTo("UP");
    }

    @Test
    void shouldHaveDownStatus() {
        assertThat(HealthStatus.DOWN).isNotNull();
        assertThat(HealthStatus.DOWN.name()).isEqualTo("DOWN");
    }

    @Test
    void shouldHaveExactlyTwoValues() {
        assertThat(HealthStatus.values()).hasSize(2);
        assertThat(HealthStatus.values()).containsExactlyInAnyOrder(HealthStatus.UP, HealthStatus.DOWN);
    }

    @Test
    void valueOfShouldReturnCorrectEnum() {
        assertThat(HealthStatus.valueOf("UP")).isEqualTo(HealthStatus.UP);
        assertThat(HealthStatus.valueOf("DOWN")).isEqualTo(HealthStatus.DOWN);
    }
}
