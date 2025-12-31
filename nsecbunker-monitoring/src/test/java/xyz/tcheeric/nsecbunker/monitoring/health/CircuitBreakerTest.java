package xyz.tcheeric.nsecbunker.monitoring.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link CircuitBreaker}.
 */
class CircuitBreakerTest {

    @Test
    @DisplayName("Initial state is CLOSED")
    void initialStateIsClosed() {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .name("test")
                .build();

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(breaker.isClosed()).isTrue();
        assertThat(breaker.isOpen()).isFalse();
    }

    @Test
    @DisplayName("Allows requests when CLOSED")
    void allowsRequestsWhenClosed() {
        CircuitBreaker breaker = CircuitBreaker.builder().build();

        assertThat(breaker.allowRequest()).isTrue();
    }

    @Test
    @DisplayName("Opens circuit after failure threshold")
    void opensCircuitAfterFailureThreshold() {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(3)
                .build();

        breaker.recordFailure();
        assertThat(breaker.isClosed()).isTrue();

        breaker.recordFailure();
        assertThat(breaker.isClosed()).isTrue();

        breaker.recordFailure();
        assertThat(breaker.isOpen()).isTrue();
    }

    @Test
    @DisplayName("Rejects requests when OPEN")
    void rejectsRequestsWhenOpen() {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(1)
                .resetTimeout(Duration.ofMinutes(1))
                .build();

        breaker.recordFailure(); // Opens circuit

        assertThat(breaker.allowRequest()).isFalse();
    }

    @Test
    @DisplayName("Execute throws when circuit is open")
    void executeThrowsWhenCircuitIsOpen() {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(1)
                .resetTimeout(Duration.ofMinutes(1))
                .build();

        breaker.recordFailure(); // Opens circuit

        assertThatThrownBy(() -> breaker.execute(() -> "result"))
                .isInstanceOf(CircuitBreaker.CircuitBreakerOpenException.class)
                .hasMessageContaining("open");
    }

    @Test
    @DisplayName("Execute passes result on success")
    void executePassesResultOnSuccess() throws Exception {
        CircuitBreaker breaker = CircuitBreaker.builder().build();

        String result = breaker.execute(() -> "success");

        assertThat(result).isEqualTo("success");
    }

    @Test
    @DisplayName("Execute records success")
    void executeRecordsSuccess() throws Exception {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(3)
                .build();

        // Record some failures
        breaker.recordFailure();
        breaker.recordFailure();
        assertThat(breaker.getFailureCount()).isEqualTo(2);

        // Successful execute should reset
        breaker.execute(() -> "ok");

        assertThat(breaker.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Execute records failure and rethrows")
    void executeRecordsFailureAndRethrows() {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(5)
                .build();

        assertThatThrownBy(() -> breaker.execute(() -> {
            throw new RuntimeException("fail");
        })).isInstanceOf(RuntimeException.class);

        assertThat(breaker.getFailureCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Transitions to HALF_OPEN after reset timeout")
    void transitionsToHalfOpenAfterResetTimeout() throws InterruptedException {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(1)
                .resetTimeout(Duration.ofMillis(100))
                .build();

        breaker.recordFailure(); // Opens circuit
        assertThat(breaker.isOpen()).isTrue();

        Thread.sleep(150); // Wait for reset timeout

        assertThat(breaker.getState()).isEqualTo(CircuitBreaker.State.HALF_OPEN);
    }

    @Test
    @DisplayName("Closes circuit after successes in HALF_OPEN")
    void closesCircuitAfterSuccessesInHalfOpen() throws InterruptedException {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(1)
                .successThreshold(2)
                .resetTimeout(Duration.ofMillis(50))
                .build();

        breaker.recordFailure(); // Opens circuit
        Thread.sleep(100); // Wait for HALF_OPEN

        breaker.allowRequest(); // Triggers transition to HALF_OPEN
        breaker.recordSuccess();
        breaker.recordSuccess();

        assertThat(breaker.isClosed()).isTrue();
    }

    @Test
    @DisplayName("Re-opens circuit on failure in HALF_OPEN")
    void reOpensCircuitOnFailureInHalfOpen() throws InterruptedException {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(1)
                .resetTimeout(Duration.ofMillis(50))
                .build();

        breaker.recordFailure(); // Opens circuit
        Thread.sleep(100); // Wait for HALF_OPEN

        breaker.allowRequest(); // Triggers transition to HALF_OPEN
        assertThat(breaker.isHalfOpen()).isTrue();

        breaker.recordFailure(); // Re-opens

        assertThat(breaker.isOpen()).isTrue();
    }

    @Test
    @DisplayName("Reset forces circuit to CLOSED")
    void resetForcesCircuitToClosed() {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(1)
                .build();

        breaker.recordFailure(); // Opens circuit
        assertThat(breaker.isOpen()).isTrue();

        breaker.reset();

        assertThat(breaker.isClosed()).isTrue();
        assertThat(breaker.getFailureCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("Trip forces circuit to OPEN")
    void tripForcesCircuitToOpen() {
        CircuitBreaker breaker = CircuitBreaker.builder().build();

        assertThat(breaker.isClosed()).isTrue();

        breaker.trip();

        assertThat(breaker.isOpen()).isTrue();
    }

    @Test
    @DisplayName("Listener notified on state change")
    void listenerNotifiedOnStateChange() {
        AtomicInteger notifications = new AtomicInteger(0);

        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(1)
                .build();

        breaker.setListener((cb, oldState, newState) -> {
            notifications.incrementAndGet();
            assertThat(oldState).isEqualTo(CircuitBreaker.State.CLOSED);
            assertThat(newState).isEqualTo(CircuitBreaker.State.OPEN);
        });

        breaker.recordFailure();

        assertThat(notifications.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Status snapshot contains accurate data")
    void statusSnapshotContainsAccurateData() {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .name("test-breaker")
                .failureThreshold(5)
                .successThreshold(3)
                .resetTimeout(Duration.ofSeconds(30))
                .build();

        breaker.recordFailure();
        breaker.recordFailure();

        CircuitBreaker.CircuitBreakerStatus status = breaker.getStatus();

        assertThat(status.getName()).isEqualTo("test-breaker");
        assertThat(status.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
        assertThat(status.getFailureCount()).isEqualTo(2);
        assertThat(status.getFailureThreshold()).isEqualTo(5);
        assertThat(status.getSuccessThreshold()).isEqualTo(3);
        assertThat(status.getResetTimeout()).isEqualTo(Duration.ofSeconds(30));
        assertThat(status.getTimestamp()).isNotNull();
    }

    @Test
    @DisplayName("ExecuteVoid works correctly")
    void executeVoidWorksCorrectly() throws Exception {
        CircuitBreaker breaker = CircuitBreaker.builder().build();
        AtomicInteger counter = new AtomicInteger(0);

        breaker.executeVoid(counter::incrementAndGet);

        assertThat(counter.get()).isEqualTo(1);
    }

    @Test
    @DisplayName("Default configuration values are applied")
    void defaultConfigurationValuesAreApplied() {
        CircuitBreaker breaker = CircuitBreaker.builder().build();

        assertThat(breaker.getName()).isEqualTo("default");
        assertThat(breaker.getFailureThreshold()).isEqualTo(5);
        assertThat(breaker.getSuccessThreshold()).isEqualTo(3);
        assertThat(breaker.getResetTimeout()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("Open duration is tracked")
    void openDurationIsTracked() throws InterruptedException {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(1)
                .resetTimeout(Duration.ofMinutes(1))
                .build();

        assertThat(breaker.getOpenDuration()).isNull();

        breaker.recordFailure();
        Thread.sleep(50);

        Duration openDuration = breaker.getOpenDuration();
        assertThat(openDuration).isNotNull();
        assertThat(openDuration.toMillis()).isGreaterThanOrEqualTo(50);
    }

    @Test
    @DisplayName("Time until reset is calculated")
    void timeUntilResetIsCalculated() {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .failureThreshold(1)
                .resetTimeout(Duration.ofSeconds(30))
                .build();

        breaker.recordFailure();

        Duration timeUntilReset = breaker.getTimeUntilReset();
        assertThat(timeUntilReset).isNotNull();
        assertThat(timeUntilReset.toSeconds()).isLessThanOrEqualTo(30);
        assertThat(timeUntilReset.toSeconds()).isGreaterThan(25);
    }

    // Tests for CircuitBreakerStatus nested class
    @Test
    @DisplayName("CircuitBreakerStatus has working equals and hashCode")
    void circuitBreakerStatusHasWorkingEqualsAndHashCode() {
        // Create statuses with same timestamp to test equality
        java.time.Instant fixedTime = java.time.Instant.now();
        CircuitBreaker.CircuitBreakerStatus status1 = CircuitBreaker.CircuitBreakerStatus.builder()
                .name("test")
                .state(CircuitBreaker.State.CLOSED)
                .failureCount(0)
                .failureThreshold(5)
                .successThreshold(3)
                .resetTimeout(Duration.ofSeconds(30))
                .timestamp(fixedTime)
                .build();
        CircuitBreaker.CircuitBreakerStatus status2 = CircuitBreaker.CircuitBreakerStatus.builder()
                .name("test")
                .state(CircuitBreaker.State.CLOSED)
                .failureCount(0)
                .failureThreshold(5)
                .successThreshold(3)
                .resetTimeout(Duration.ofSeconds(30))
                .timestamp(fixedTime)
                .build();

        assertThat(status1).isEqualTo(status2);
        assertThat(status1.hashCode()).isEqualTo(status2.hashCode());
        assertThat(status1).isEqualTo(status1);
        assertThat(status1).isNotEqualTo(null);
        assertThat(status1).isNotEqualTo("string");
    }

    @Test
    @DisplayName("CircuitBreakerStatus has working toString")
    void circuitBreakerStatusHasWorkingToString() {
        CircuitBreaker breaker = CircuitBreaker.builder()
                .name("test-breaker")
                .build();

        CircuitBreaker.CircuitBreakerStatus status = breaker.getStatus();
        String toString = status.toString();

        assertThat(toString).contains("CircuitBreakerStatus");
        assertThat(toString).contains("test-breaker");
        assertThat(toString).contains("CLOSED");
    }

    @Test
    @DisplayName("CircuitBreakerStatus builder works correctly")
    void circuitBreakerStatusBuilderWorksCorrectly() {
        CircuitBreaker.CircuitBreakerStatus status = CircuitBreaker.CircuitBreakerStatus.builder()
                .name("custom")
                .state(CircuitBreaker.State.OPEN)
                .failureCount(3)
                .failureThreshold(5)
                .successThreshold(3)
                .resetTimeout(Duration.ofMinutes(1))
                .build();

        assertThat(status.getName()).isEqualTo("custom");
        assertThat(status.getState()).isEqualTo(CircuitBreaker.State.OPEN);
        assertThat(status.getFailureCount()).isEqualTo(3);
        assertThat(status.getFailureThreshold()).isEqualTo(5);
        assertThat(status.getSuccessThreshold()).isEqualTo(3);
        assertThat(status.getResetTimeout()).isEqualTo(Duration.ofMinutes(1));
    }

    @Test
    @DisplayName("CircuitBreakerStatus inequality works")
    void circuitBreakerStatusInequalityWorks() {
        CircuitBreaker.CircuitBreakerStatus status1 = CircuitBreaker.CircuitBreakerStatus.builder()
                .name("test1")
                .state(CircuitBreaker.State.CLOSED)
                .build();
        CircuitBreaker.CircuitBreakerStatus status2 = CircuitBreaker.CircuitBreakerStatus.builder()
                .name("test2")
                .state(CircuitBreaker.State.OPEN)
                .build();

        assertThat(status1).isNotEqualTo(status2);
    }
}
