package xyz.tcheeric.nsecbunker.monitoring.alerting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertTest {

    @Test
    void shouldBuildWithAllFields() {
        Alert alert = Alert.builder()
                .type("error_rate")
                .message("Error rate exceeded threshold")
                .build();

        assertThat(alert.getType()).isEqualTo("error_rate");
        assertThat(alert.getMessage()).isEqualTo("Error rate exceeded threshold");
    }

    @Test
    void shouldBuildWithNullFields() {
        Alert alert = Alert.builder().build();

        assertThat(alert.getType()).isNull();
        assertThat(alert.getMessage()).isNull();
    }

    @Test
    void toBuilderShouldCreateCopy() {
        Alert original = Alert.builder()
                .type("connection_failure")
                .message("Cannot connect to relay")
                .build();

        Alert copy = original.toBuilder()
                .message("Connection restored")
                .build();

        assertThat(copy.getType()).isEqualTo("connection_failure");
        assertThat(copy.getMessage()).isEqualTo("Connection restored");
        assertThat(original.getMessage()).isEqualTo("Cannot connect to relay");
    }

    @Test
    void equalsShouldWorkCorrectly() {
        Alert alert1 = Alert.builder()
                .type("health_check")
                .message("Health check failed")
                .build();

        Alert alert2 = Alert.builder()
                .type("health_check")
                .message("Health check failed")
                .build();

        assertThat(alert1).isEqualTo(alert2);
        assertThat(alert1.hashCode()).isEqualTo(alert2.hashCode());
    }

    @Test
    void toStringShouldContainFields() {
        Alert alert = Alert.builder()
                .type("error_rate")
                .message("High error rate detected")
                .build();

        String str = alert.toString();
        assertThat(str).contains("error_rate");
        assertThat(str).contains("High error rate detected");
    }
}
