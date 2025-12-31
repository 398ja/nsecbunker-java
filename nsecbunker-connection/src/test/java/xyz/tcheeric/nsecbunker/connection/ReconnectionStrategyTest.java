package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests for the {@link ReconnectionStrategy} interface.
 */
class ReconnectionStrategyTest {

    @Test
    void noneReturnsNoReconnectionStrategy() {
        ReconnectionStrategy strategy = ReconnectionStrategy.none();

        assertNotNull(strategy);
        assertInstanceOf(NoReconnectionStrategy.class, strategy);
    }

    @Test
    void fixedDelayWithMaxAttemptsCreatesStrategy() {
        ReconnectionStrategy strategy = ReconnectionStrategy.fixedDelay(Duration.ofSeconds(5), 10);

        assertNotNull(strategy);
        assertInstanceOf(FixedDelayStrategy.class, strategy);
        assertEquals(10, strategy.getMaxAttempts());
    }

    @Test
    void fixedDelayUnlimitedCreatesStrategy() {
        ReconnectionStrategy strategy = ReconnectionStrategy.fixedDelay(Duration.ofSeconds(5));

        assertNotNull(strategy);
        assertInstanceOf(FixedDelayStrategy.class, strategy);
        assertEquals(-1, strategy.getMaxAttempts());
    }

    @Test
    void exponentialBackoffReturnsBuilder() {
        ExponentialBackoffStrategy.Builder builder = ReconnectionStrategy.exponentialBackoff();

        assertNotNull(builder);
    }

    @Test
    void exponentialBackoffDefaultCreatesStrategyWithDefaults() {
        ReconnectionStrategy strategy = ReconnectionStrategy.exponentialBackoffDefault();

        assertNotNull(strategy);
        assertInstanceOf(ExponentialBackoffStrategy.class, strategy);

        ExponentialBackoffStrategy expStrategy = (ExponentialBackoffStrategy) strategy;
        assertEquals(Duration.ofSeconds(1), expStrategy.getInitialDelay());
        assertEquals(Duration.ofMinutes(5), expStrategy.getMaxDelay());
        assertEquals(2.0, expStrategy.getMultiplier());
        assertEquals(10, expStrategy.getMaxAttempts());
        assertEquals(0.1, expStrategy.getJitter());
    }

    @Test
    void defaultResetDoesNothing() {
        ReconnectionStrategy strategy = new ReconnectionStrategy() {
            @Override
            public boolean shouldReconnect(int attempt) {
                return false;
            }

            @Override
            public java.util.Optional<Duration> getDelay(int attempt) {
                return java.util.Optional.empty();
            }

            @Override
            public int getMaxAttempts() {
                return 0;
            }
        };

        assertDoesNotThrow(strategy::reset);
    }

    @Test
    void defaultOnAttemptFailedDoesNothing() {
        ReconnectionStrategy strategy = ReconnectionStrategy.none();

        assertDoesNotThrow(() -> strategy.onAttemptFailed(1, new RuntimeException("test")));
    }
}
