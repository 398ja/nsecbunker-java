package xyz.tcheeric.nsecbunker.monitoring.health;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ErrorRateMonitor}.
 */
class ErrorRateMonitorTest {

    @Test
    @DisplayName("Initial state has zero error rate")
    void initialStateHasZeroErrorRate() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder().build();

        assertThat(monitor.getErrorRate()).isEqualTo(0.0);
        assertThat(monitor.getSuccessRate()).isEqualTo(1.0);
        assertThat(monitor.getWindowEventCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Records successes correctly")
    void recordsSuccessesCorrectly() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder().build();

        monitor.recordSuccess();
        monitor.recordSuccess();
        monitor.recordSuccess();

        assertThat(monitor.getWindowSuccesses()).isEqualTo(3);
        assertThat(monitor.getWindowFailures()).isEqualTo(0);
        assertThat(monitor.getErrorRate()).isEqualTo(0.0);
    }

    @Test
    @DisplayName("Records failures correctly")
    void recordsFailuresCorrectly() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder().build();

        monitor.recordFailure();
        monitor.recordFailure();

        assertThat(monitor.getWindowFailures()).isEqualTo(2);
        assertThat(monitor.getWindowSuccesses()).isEqualTo(0);
        assertThat(monitor.getErrorRate()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Calculates error rate correctly")
    void calculatesErrorRateCorrectly() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder().build();

        // 2 successes, 2 failures = 50% error rate
        monitor.recordSuccess();
        monitor.recordSuccess();
        monitor.recordFailure();
        monitor.recordFailure();

        assertThat(monitor.getErrorRate()).isEqualTo(0.5);
        assertThat(monitor.getSuccessRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Threshold not exceeded with insufficient samples")
    void thresholdNotExceededWithInsufficientSamples() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder()
                .minSamples(10)
                .errorThreshold(0.5)
                .build();

        // Only 5 failures - below min samples
        for (int i = 0; i < 5; i++) {
            monitor.recordFailure();
        }

        assertThat(monitor.isThresholdExceeded()).isFalse();
    }

    @Test
    @DisplayName("Threshold exceeded when error rate exceeds threshold")
    void thresholdExceededWhenErrorRateExceedsThreshold() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder()
                .minSamples(10)
                .errorThreshold(0.5)
                .build();

        // 8 failures, 4 successes = 66.6% error rate
        for (int i = 0; i < 8; i++) {
            monitor.recordFailure();
        }
        for (int i = 0; i < 4; i++) {
            monitor.recordSuccess();
        }

        assertThat(monitor.isThresholdExceeded()).isTrue();
    }

    @Test
    @DisplayName("Threshold not exceeded when error rate below threshold")
    void thresholdNotExceededWhenErrorRateBelowThreshold() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder()
                .minSamples(10)
                .errorThreshold(0.5)
                .build();

        // 3 failures, 10 successes = 23% error rate
        for (int i = 0; i < 3; i++) {
            monitor.recordFailure();
        }
        for (int i = 0; i < 10; i++) {
            monitor.recordSuccess();
        }

        assertThat(monitor.isThresholdExceeded()).isFalse();
    }

    @Test
    @DisplayName("Record method works for both success and failure")
    void recordMethodWorks() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder().build();

        monitor.record(true);
        monitor.record(false);

        assertThat(monitor.getWindowSuccesses()).isEqualTo(1);
        assertThat(monitor.getWindowFailures()).isEqualTo(1);
    }

    @Test
    @DisplayName("Total counters are cumulative")
    void totalCountersAreCumulative() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder().build();

        monitor.recordSuccess();
        monitor.recordSuccess();
        monitor.recordFailure();

        assertThat(monitor.getTotalSuccesses()).isEqualTo(2);
        assertThat(monitor.getTotalFailures()).isEqualTo(1);
    }

    @Test
    @DisplayName("All-time error rate calculated correctly")
    void allTimeErrorRateCalculatedCorrectly() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder().build();

        monitor.recordSuccess();
        monitor.recordSuccess();
        monitor.recordFailure();
        monitor.recordFailure();

        assertThat(monitor.getAllTimeErrorRate()).isEqualTo(0.5);
    }

    @Test
    @DisplayName("Reset clears all counters")
    void resetClearsAllCounters() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder().build();

        monitor.recordSuccess();
        monitor.recordFailure();
        monitor.reset();

        assertThat(monitor.getWindowEventCount()).isEqualTo(0);
        assertThat(monitor.getTotalSuccesses()).isEqualTo(0);
        assertThat(monitor.getTotalFailures()).isEqualTo(0);
    }

    @Test
    @DisplayName("Snapshot contains accurate data")
    void snapshotContainsAccurateData() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder()
                .windowDuration(Duration.ofMinutes(5))
                .errorThreshold(0.3)
                .minSamples(5)
                .build();

        monitor.recordSuccess();
        monitor.recordSuccess();
        monitor.recordFailure();

        ErrorRateMonitor.ErrorRateSnapshot snapshot = monitor.getSnapshot();

        assertThat(snapshot.getWindowDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(snapshot.getErrorThreshold()).isEqualTo(0.3);
        assertThat(snapshot.getWindowEventCount()).isEqualTo(3);
        assertThat(snapshot.getWindowSuccesses()).isEqualTo(2);
        assertThat(snapshot.getWindowFailures()).isEqualTo(1);
        assertThat(snapshot.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("Custom configuration is respected")
    void customConfigurationIsRespected() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder()
                .windowDuration(Duration.ofMinutes(10))
                .errorThreshold(0.75)
                .minSamples(20)
                .build();

        assertThat(monitor.getWindowDuration()).isEqualTo(Duration.ofMinutes(10));
        assertThat(monitor.getErrorThreshold()).isEqualTo(0.75);
        assertThat(monitor.getMinSamples()).isEqualTo(20);
    }

    @Test
    @DisplayName("Default configuration values are applied")
    void defaultConfigurationValuesAreApplied() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder().build();

        assertThat(monitor.getWindowDuration()).isEqualTo(Duration.ofMinutes(5));
        assertThat(monitor.getErrorThreshold()).isEqualTo(0.5);
        assertThat(monitor.getMinSamples()).isEqualTo(10);
    }
}
