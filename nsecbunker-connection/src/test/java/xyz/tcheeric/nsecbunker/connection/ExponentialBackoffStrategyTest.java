package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link ExponentialBackoffStrategy} class.
 */
class ExponentialBackoffStrategyTest {

    @Test
    void builderWithDefaultsCreatesValidStrategy() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder().build();

        assertEquals(Duration.ofSeconds(1), strategy.getInitialDelay());
        assertEquals(Duration.ofMinutes(5), strategy.getMaxDelay());
        assertEquals(2.0, strategy.getMultiplier());
        assertEquals(10, strategy.getMaxAttempts());
        assertEquals(0.1, strategy.getJitter());
    }

    @Test
    void builderWithCustomValuesCreatesStrategy() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .initialDelay(Duration.ofMillis(500))
            .maxDelay(Duration.ofSeconds(30))
            .multiplier(1.5)
            .maxAttempts(5)
            .jitter(0.2)
            .build();

        assertEquals(Duration.ofMillis(500), strategy.getInitialDelay());
        assertEquals(Duration.ofSeconds(30), strategy.getMaxDelay());
        assertEquals(1.5, strategy.getMultiplier());
        assertEquals(5, strategy.getMaxAttempts());
        assertEquals(0.2, strategy.getJitter());
    }

    @Test
    void builderWithUnlimitedAttempts() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .unlimitedAttempts()
            .build();

        assertEquals(-1, strategy.getMaxAttempts());
    }

    @Test
    void builderWithNoJitter() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .noJitter()
            .build();

        assertEquals(0.0, strategy.getJitter());
    }

    @Test
    void builderWithNullInitialDelayThrows() {
        assertThrows(NullPointerException.class, () ->
            ExponentialBackoffStrategy.builder().initialDelay(null));
    }

    @Test
    void builderWithZeroInitialDelayThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            ExponentialBackoffStrategy.builder().initialDelay(Duration.ZERO));
    }

    @Test
    void builderWithNegativeInitialDelayThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            ExponentialBackoffStrategy.builder().initialDelay(Duration.ofSeconds(-1)));
    }

    @Test
    void builderWithNullMaxDelayThrows() {
        assertThrows(NullPointerException.class, () ->
            ExponentialBackoffStrategy.builder().maxDelay(null));
    }

    @Test
    void builderWithZeroMaxDelayThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            ExponentialBackoffStrategy.builder().maxDelay(Duration.ZERO));
    }

    @Test
    void builderWithMultiplierLessThanOneThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            ExponentialBackoffStrategy.builder().multiplier(0.5));
    }

    @Test
    void builderWithInvalidMaxAttemptsThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            ExponentialBackoffStrategy.builder().maxAttempts(-2));
    }

    @Test
    void builderWithNegativeJitterThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            ExponentialBackoffStrategy.builder().jitter(-0.1));
    }

    @Test
    void builderWithJitterGreaterThanOneThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            ExponentialBackoffStrategy.builder().jitter(1.5));
    }

    @Test
    void builderWithMaxDelayLessThanInitialDelayThrows() {
        assertThrows(IllegalStateException.class, () ->
            ExponentialBackoffStrategy.builder()
                .initialDelay(Duration.ofMinutes(1))
                .maxDelay(Duration.ofSeconds(10))
                .build());
    }

    @Test
    void shouldReconnectWithinMaxAttempts() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .maxAttempts(3)
            .build();

        assertTrue(strategy.shouldReconnect(1));
        assertTrue(strategy.shouldReconnect(2));
        assertTrue(strategy.shouldReconnect(3));
        assertFalse(strategy.shouldReconnect(4));
    }

    @Test
    void shouldReconnectWithUnlimitedAttempts() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .unlimitedAttempts()
            .build();

        assertTrue(strategy.shouldReconnect(1));
        assertTrue(strategy.shouldReconnect(100));
        assertTrue(strategy.shouldReconnect(10000));
    }

    @Test
    void getDelayWithNoJitterFollowsExponentialPattern() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .initialDelay(Duration.ofSeconds(1))
            .multiplier(2.0)
            .maxAttempts(10)
            .noJitter()
            .build();

        assertEquals(Duration.ofSeconds(1), strategy.getDelay(1).orElseThrow());
        assertEquals(Duration.ofSeconds(2), strategy.getDelay(2).orElseThrow());
        assertEquals(Duration.ofSeconds(4), strategy.getDelay(3).orElseThrow());
        assertEquals(Duration.ofSeconds(8), strategy.getDelay(4).orElseThrow());
    }

    @Test
    void getDelayRespectsMaxDelay() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .initialDelay(Duration.ofSeconds(1))
            .maxDelay(Duration.ofSeconds(10))
            .multiplier(2.0)
            .maxAttempts(10)
            .noJitter()
            .build();

        assertEquals(Duration.ofSeconds(1), strategy.getDelay(1).orElseThrow());
        assertEquals(Duration.ofSeconds(2), strategy.getDelay(2).orElseThrow());
        assertEquals(Duration.ofSeconds(4), strategy.getDelay(3).orElseThrow());
        assertEquals(Duration.ofSeconds(8), strategy.getDelay(4).orElseThrow());
        assertEquals(Duration.ofSeconds(10), strategy.getDelay(5).orElseThrow()); // capped
        assertEquals(Duration.ofSeconds(10), strategy.getDelay(6).orElseThrow()); // still capped
    }

    @Test
    void getDelayReturnsEmptyBeyondMaxAttempts() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .maxAttempts(3)
            .build();

        assertEquals(Optional.empty(), strategy.getDelay(4));
        assertEquals(Optional.empty(), strategy.getDelay(100));
    }

    @RepeatedTest(10)
    void getDelayWithJitterAddsVariation() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .initialDelay(Duration.ofSeconds(10))
            .jitter(0.2) // 20% jitter
            .maxAttempts(10)
            .build();

        Duration delay = strategy.getDelay(1).orElseThrow();

        // With 20% jitter, delay should be between 8 and 12 seconds
        long delayMillis = delay.toMillis();
        assertTrue(delayMillis >= 8000, "Delay should be at least 8000ms but was " + delayMillis);
        assertTrue(delayMillis <= 12000, "Delay should be at most 12000ms but was " + delayMillis);
    }

    @Test
    void toStringContainsRelevantInfo() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .initialDelay(Duration.ofSeconds(1))
            .maxDelay(Duration.ofMinutes(5))
            .multiplier(2.0)
            .maxAttempts(10)
            .jitter(0.1)
            .build();

        String str = strategy.toString();
        assertTrue(str.contains("ExponentialBackoffStrategy"));
        assertTrue(str.contains("initialDelay=PT1S"));
        assertTrue(str.contains("maxDelay=PT5M"));
        assertTrue(str.contains("multiplier=2.0"));
        assertTrue(str.contains("10"));
        assertTrue(str.contains("jitter=0.1"));
    }

    @Test
    void toStringWithUnlimitedAttempts() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .unlimitedAttempts()
            .build();

        String str = strategy.toString();
        assertTrue(str.contains("unlimited"));
    }

    @Test
    void implementsReconnectionStrategy() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder().build();
        assertInstanceOf(ReconnectionStrategy.class, strategy);
    }

    @Test
    void multiplierOfOneCreatesConstantDelay() {
        ExponentialBackoffStrategy strategy = ExponentialBackoffStrategy.builder()
            .initialDelay(Duration.ofSeconds(5))
            .multiplier(1.0)
            .noJitter()
            .maxAttempts(5)
            .build();

        assertEquals(Duration.ofSeconds(5), strategy.getDelay(1).orElseThrow());
        assertEquals(Duration.ofSeconds(5), strategy.getDelay(2).orElseThrow());
        assertEquals(Duration.ofSeconds(5), strategy.getDelay(3).orElseThrow());
    }
}
