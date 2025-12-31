package xyz.tcheeric.nsecbunker.monitoring.health;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitors error rates over a sliding time window.
 *
 * <p>Tracks successes and failures to calculate error rates
 * and detect threshold violations.
 *
 * <p>Usage example:
 * <pre>{@code
 * ErrorRateMonitor monitor = ErrorRateMonitor.builder()
 *     .windowDuration(Duration.ofMinutes(5))
 *     .errorThreshold(0.5)
 *     .build();
 *
 * monitor.recordSuccess();
 * monitor.recordFailure();
 *
 * double errorRate = monitor.getErrorRate();
 * if (monitor.isThresholdExceeded()) {
 *     // Take action
 * }
 * }</pre>
 */
@Slf4j
public class ErrorRateMonitor {

    /**
     * Sliding window duration for rate calculation.
     */
    @Getter
    private final Duration windowDuration;

    /**
     * Error rate threshold (0.0 to 1.0).
     */
    @Getter
    private final double errorThreshold;

    /**
     * Minimum samples required before evaluating threshold.
     */
    @Getter
    private final int minSamples;

    /**
     * Records of events within the window.
     */
    private final Deque<EventRecord> events;

    /**
     * Total successes (all time).
     */
    private final AtomicLong totalSuccesses;

    /**
     * Total failures (all time).
     */
    private final AtomicLong totalFailures;

    @Builder
    private ErrorRateMonitor(Duration windowDuration, double errorThreshold, int minSamples) {
        this.windowDuration = windowDuration != null ? windowDuration : Duration.ofMinutes(5);
        this.errorThreshold = errorThreshold > 0 ? errorThreshold : 0.5;
        this.minSamples = minSamples > 0 ? minSamples : 10;
        this.events = new ConcurrentLinkedDeque<>();
        this.totalSuccesses = new AtomicLong(0);
        this.totalFailures = new AtomicLong(0);
    }

    /**
     * Records a successful operation.
     */
    public void recordSuccess() {
        events.addLast(new EventRecord(Instant.now(), true));
        totalSuccesses.incrementAndGet();
        pruneOldEvents();
    }

    /**
     * Records a failed operation.
     */
    public void recordFailure() {
        events.addLast(new EventRecord(Instant.now(), false));
        totalFailures.incrementAndGet();
        pruneOldEvents();
    }

    /**
     * Records an operation result.
     *
     * @param success true if successful, false if failed
     */
    public void record(boolean success) {
        if (success) {
            recordSuccess();
        } else {
            recordFailure();
        }
    }

    /**
     * Gets the current error rate within the window (0.0 to 1.0).
     *
     * @return the error rate
     */
    public double getErrorRate() {
        pruneOldEvents();

        long successes = 0;
        long failures = 0;

        for (EventRecord event : events) {
            if (event.success) {
                successes++;
            } else {
                failures++;
            }
        }

        long total = successes + failures;
        if (total == 0) {
            return 0.0;
        }

        return (double) failures / total;
    }

    /**
     * Gets the success rate within the window (0.0 to 1.0).
     *
     * @return the success rate
     */
    public double getSuccessRate() {
        return 1.0 - getErrorRate();
    }

    /**
     * Checks if the error rate exceeds the configured threshold.
     *
     * @return true if threshold is exceeded
     */
    public boolean isThresholdExceeded() {
        pruneOldEvents();

        // Need minimum samples before evaluating
        if (events.size() < minSamples) {
            return false;
        }

        return getErrorRate() > errorThreshold;
    }

    /**
     * Gets the number of events in the current window.
     *
     * @return the event count
     */
    public int getWindowEventCount() {
        pruneOldEvents();
        return events.size();
    }

    /**
     * Gets the number of successes in the current window.
     *
     * @return the success count
     */
    public long getWindowSuccesses() {
        pruneOldEvents();
        return events.stream().filter(e -> e.success).count();
    }

    /**
     * Gets the number of failures in the current window.
     *
     * @return the failure count
     */
    public long getWindowFailures() {
        pruneOldEvents();
        return events.stream().filter(e -> !e.success).count();
    }

    /**
     * Gets the total successes (all time).
     *
     * @return total successes
     */
    public long getTotalSuccesses() {
        return totalSuccesses.get();
    }

    /**
     * Gets the total failures (all time).
     *
     * @return total failures
     */
    public long getTotalFailures() {
        return totalFailures.get();
    }

    /**
     * Gets the all-time error rate.
     *
     * @return the all-time error rate
     */
    public double getAllTimeErrorRate() {
        long total = totalSuccesses.get() + totalFailures.get();
        if (total == 0) {
            return 0.0;
        }
        return (double) totalFailures.get() / total;
    }

    /**
     * Gets a snapshot of the current error rate statistics.
     *
     * @return the statistics snapshot
     */
    public ErrorRateSnapshot getSnapshot() {
        pruneOldEvents();

        long windowSuccesses = 0;
        long windowFailures = 0;

        for (EventRecord event : events) {
            if (event.success) {
                windowSuccesses++;
            } else {
                windowFailures++;
            }
        }

        long windowTotal = windowSuccesses + windowFailures;
        double windowErrorRate = windowTotal > 0 ? (double) windowFailures / windowTotal : 0.0;

        return ErrorRateSnapshot.builder()
                .windowDuration(windowDuration)
                .errorThreshold(errorThreshold)
                .windowEventCount((int) windowTotal)
                .windowSuccesses(windowSuccesses)
                .windowFailures(windowFailures)
                .windowErrorRate(windowErrorRate)
                .thresholdExceeded(windowTotal >= minSamples && windowErrorRate > errorThreshold)
                .totalSuccesses(totalSuccesses.get())
                .totalFailures(totalFailures.get())
                .allTimeErrorRate(getAllTimeErrorRate())
                .timestamp(Instant.now())
                .build();
    }

    /**
     * Resets all counters and clears the event history.
     */
    public void reset() {
        events.clear();
        totalSuccesses.set(0);
        totalFailures.set(0);
    }

    /**
     * Removes events older than the window duration.
     */
    private void pruneOldEvents() {
        Instant cutoff = Instant.now().minus(windowDuration);
        while (!events.isEmpty() && events.peekFirst().timestamp.isBefore(cutoff)) {
            events.pollFirst();
        }
    }

    /**
     * Record of a single event.
     */
    private static class EventRecord {
        final Instant timestamp;
        final boolean success;

        EventRecord(Instant timestamp, boolean success) {
            this.timestamp = timestamp;
            this.success = success;
        }
    }

    /**
     * Snapshot of error rate statistics.
     */
    @lombok.Value
    @Builder
    public static class ErrorRateSnapshot {
        Duration windowDuration;
        double errorThreshold;
        int windowEventCount;
        long windowSuccesses;
        long windowFailures;
        double windowErrorRate;
        boolean thresholdExceeded;
        long totalSuccesses;
        long totalFailures;
        double allTimeErrorRate;
        Instant timestamp;
    }
}
