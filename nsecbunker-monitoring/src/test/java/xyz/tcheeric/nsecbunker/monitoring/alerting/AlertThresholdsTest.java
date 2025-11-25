package xyz.tcheeric.nsecbunker.monitoring.alerting;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertThresholdsTest {

    @Test
    void shouldBuildWithDefaultValues() {
        AlertThresholds thresholds = AlertThresholds.builder().build();

        assertThat(thresholds.getFailureThreshold()).isZero();
        assertThat(thresholds.getErrorRateThreshold()).isEqualTo(0.1);
    }

    @Test
    void shouldBuildWithCustomValues() {
        AlertThresholds thresholds = AlertThresholds.builder()
                .failureThreshold(5)
                .errorRateThreshold(0.25)
                .build();

        assertThat(thresholds.getFailureThreshold()).isEqualTo(5);
        assertThat(thresholds.getErrorRateThreshold()).isEqualTo(0.25);
    }

    @Test
    void toBuilderShouldCreateCopy() {
        AlertThresholds original = AlertThresholds.builder()
                .failureThreshold(10)
                .errorRateThreshold(0.5)
                .build();

        AlertThresholds copy = original.toBuilder()
                .failureThreshold(20)
                .build();

        assertThat(copy.getFailureThreshold()).isEqualTo(20);
        assertThat(copy.getErrorRateThreshold()).isEqualTo(0.5);
        assertThat(original.getFailureThreshold()).isEqualTo(10);
    }

    @Test
    void equalsShouldWorkCorrectly() {
        AlertThresholds thresholds1 = AlertThresholds.builder()
                .failureThreshold(5)
                .errorRateThreshold(0.2)
                .build();

        AlertThresholds thresholds2 = AlertThresholds.builder()
                .failureThreshold(5)
                .errorRateThreshold(0.2)
                .build();

        assertThat(thresholds1).isEqualTo(thresholds2);
        assertThat(thresholds1.hashCode()).isEqualTo(thresholds2.hashCode());
    }

    @Test
    void toStringShouldContainFields() {
        AlertThresholds thresholds = AlertThresholds.builder()
                .failureThreshold(10)
                .errorRateThreshold(0.15)
                .build();

        String str = thresholds.toString();
        assertThat(str).contains("10");
        assertThat(str).contains("0.15");
    }
}
