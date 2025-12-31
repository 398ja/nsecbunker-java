package xyz.tcheeric.nsecbunker.monitoring.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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

    // Tests for ErrorRateSnapshot nested class
    @Test
    @DisplayName("ErrorRateSnapshot has working equals and hashCode")
    void errorRateSnapshotHasWorkingEqualsAndHashCode() {
        // Create snapshots with same timestamp to test equality
        java.time.Instant fixedTime = java.time.Instant.now();
        ErrorRateMonitor.ErrorRateSnapshot snapshot1 = ErrorRateMonitor.ErrorRateSnapshot.builder()
                .windowDuration(Duration.ofMinutes(5))
                .errorThreshold(0.5)
                .windowEventCount(2)
                .windowSuccesses(1)
                .windowFailures(1)
                .totalSuccesses(1)
                .totalFailures(1)
                .windowErrorRate(0.5)
                .allTimeErrorRate(0.5)
                .thresholdExceeded(false)
                .timestamp(fixedTime)
                .build();
        ErrorRateMonitor.ErrorRateSnapshot snapshot2 = ErrorRateMonitor.ErrorRateSnapshot.builder()
                .windowDuration(Duration.ofMinutes(5))
                .errorThreshold(0.5)
                .windowEventCount(2)
                .windowSuccesses(1)
                .windowFailures(1)
                .totalSuccesses(1)
                .totalFailures(1)
                .windowErrorRate(0.5)
                .allTimeErrorRate(0.5)
                .thresholdExceeded(false)
                .timestamp(fixedTime)
                .build();

        assertThat(snapshot1).isEqualTo(snapshot2);
        assertThat(snapshot1.hashCode()).isEqualTo(snapshot2.hashCode());
        assertThat(snapshot1).isEqualTo(snapshot1);
        assertThat(snapshot1).isNotEqualTo(null);
        assertThat(snapshot1).isNotEqualTo("string");
    }

    @Test
    @DisplayName("ErrorRateSnapshot has working toString")
    void errorRateSnapshotHasWorkingToString() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder()
                .windowDuration(Duration.ofMinutes(10))
                .build();
        monitor.recordSuccess();

        ErrorRateMonitor.ErrorRateSnapshot snapshot = monitor.getSnapshot();
        String toString = snapshot.toString();

        assertThat(toString).contains("ErrorRateSnapshot");
    }

    @Test
    @DisplayName("ErrorRateSnapshot builder works correctly")
    void errorRateSnapshotBuilderWorksCorrectly() {
        ErrorRateMonitor.ErrorRateSnapshot snapshot = ErrorRateMonitor.ErrorRateSnapshot.builder()
                .windowDuration(Duration.ofMinutes(15))
                .errorThreshold(0.75)
                .windowEventCount(100)
                .windowSuccesses(70)
                .windowFailures(30)
                .totalSuccesses(1000)
                .totalFailures(500)
                .windowErrorRate(0.30)
                .allTimeErrorRate(0.33)
                .thresholdExceeded(false)
                .build();

        assertThat(snapshot.getWindowDuration()).isEqualTo(Duration.ofMinutes(15));
        assertThat(snapshot.getErrorThreshold()).isEqualTo(0.75);
        assertThat(snapshot.getWindowEventCount()).isEqualTo(100);
        assertThat(snapshot.getWindowSuccesses()).isEqualTo(70);
        assertThat(snapshot.getWindowFailures()).isEqualTo(30);
        assertThat(snapshot.getTotalSuccesses()).isEqualTo(1000);
        assertThat(snapshot.getTotalFailures()).isEqualTo(500);
        assertThat(snapshot.getWindowErrorRate()).isEqualTo(0.30);
        assertThat(snapshot.getAllTimeErrorRate()).isEqualTo(0.33);
        assertThat(snapshot.isThresholdExceeded()).isFalse();
    }

    @Test
    @DisplayName("ErrorRateSnapshot inequality works")
    void errorRateSnapshotInequalityWorks() {
        ErrorRateMonitor.ErrorRateSnapshot snapshot1 = ErrorRateMonitor.ErrorRateSnapshot.builder()
                .windowEventCount(10)
                .windowSuccesses(8)
                .windowFailures(2)
                .build();
        ErrorRateMonitor.ErrorRateSnapshot snapshot2 = ErrorRateMonitor.ErrorRateSnapshot.builder()
                .windowEventCount(20)
                .windowSuccesses(15)
                .windowFailures(5)
                .build();

        assertThat(snapshot1).isNotEqualTo(snapshot2);
    }

    @Test
    @DisplayName("Snapshot reflects threshold exceeded state correctly")
    void snapshotReflectsThresholdExceededStateCorrectly() {
        ErrorRateMonitor monitor = ErrorRateMonitor.builder()
                .errorThreshold(0.3)
                .minSamples(5)
                .build();

        // Record enough samples to exceed threshold
        for (int i = 0; i < 4; i++) {
            monitor.recordFailure();
        }
        monitor.recordSuccess();

        ErrorRateMonitor.ErrorRateSnapshot snapshot = monitor.getSnapshot();
        assertThat(snapshot.isThresholdExceeded()).isTrue();
    }
}
