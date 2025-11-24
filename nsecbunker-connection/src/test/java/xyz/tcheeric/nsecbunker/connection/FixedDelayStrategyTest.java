package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the {@link FixedDelayStrategy} class.
 */
class FixedDelayStrategyTest {

    @Test
    void constructorWithValidParametersCreatesStrategy() {
        FixedDelayStrategy strategy = new FixedDelayStrategy(Duration.ofSeconds(5), 10);

        assertEquals(Duration.ofSeconds(5), strategy.getDelay());
        assertEquals(10, strategy.getMaxAttempts());
    }

    @Test
    void constructorWithUnlimitedAttemptsCreatesStrategy() {
        FixedDelayStrategy strategy = new FixedDelayStrategy(Duration.ofSeconds(5), -1);

        assertEquals(Duration.ofSeconds(5), strategy.getDelay());
        assertEquals(-1, strategy.getMaxAttempts());
    }

    @Test
    void constructorWithNullDelayThrowsException() {
        assertThrows(NullPointerException.class, () ->
            new FixedDelayStrategy(null, 10));
    }

    @Test
    void constructorWithZeroDelayThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new FixedDelayStrategy(Duration.ZERO, 10));
    }

    @Test
    void constructorWithNegativeDelayThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new FixedDelayStrategy(Duration.ofSeconds(-1), 10));
    }

    @Test
    void constructorWithInvalidMaxAttemptsThrowsException() {
        assertThrows(IllegalArgumentException.class, () ->
            new FixedDelayStrategy(Duration.ofSeconds(5), -2));
    }

    @Test
    void shouldReconnectWithinMaxAttempts() {
        FixedDelayStrategy strategy = new FixedDelayStrategy(Duration.ofSeconds(5), 3);

        assertTrue(strategy.shouldReconnect(1));
        assertTrue(strategy.shouldReconnect(2));
        assertTrue(strategy.shouldReconnect(3));
        assertFalse(strategy.shouldReconnect(4));
    }

    @Test
    void shouldReconnectWithUnlimitedAttempts() {
        FixedDelayStrategy strategy = new FixedDelayStrategy(Duration.ofSeconds(5), -1);

        assertTrue(strategy.shouldReconnect(1));
        assertTrue(strategy.shouldReconnect(100));
        assertTrue(strategy.shouldReconnect(10000));
    }

    @Test
    void getDelayReturnsFixedValue() {
        Duration delay = Duration.ofSeconds(5);
        FixedDelayStrategy strategy = new FixedDelayStrategy(delay, 10);

        assertEquals(Optional.of(delay), strategy.getDelay(1));
        assertEquals(Optional.of(delay), strategy.getDelay(5));
        assertEquals(Optional.of(delay), strategy.getDelay(10));
    }

    @Test
    void getDelayReturnsEmptyBeyondMaxAttempts() {
        FixedDelayStrategy strategy = new FixedDelayStrategy(Duration.ofSeconds(5), 3);

        assertEquals(Optional.empty(), strategy.getDelay(4));
        assertEquals(Optional.empty(), strategy.getDelay(100));
    }

    @Test
    void toStringContainsRelevantInfo() {
        FixedDelayStrategy strategy = new FixedDelayStrategy(Duration.ofSeconds(5), 10);

        String str = strategy.toString();
        assertTrue(str.contains("FixedDelayStrategy"));
        assertTrue(str.contains("PT5S"));
        assertTrue(str.contains("10"));
    }

    @Test
    void toStringWithUnlimitedAttempts() {
        FixedDelayStrategy strategy = new FixedDelayStrategy(Duration.ofSeconds(5), -1);

        String str = strategy.toString();
        assertTrue(str.contains("unlimited"));
    }

    @Test
    void implementsReconnectionStrategy() {
        FixedDelayStrategy strategy = new FixedDelayStrategy(Duration.ofSeconds(5), 10);
        assertInstanceOf(ReconnectionStrategy.class, strategy);
    }
}
