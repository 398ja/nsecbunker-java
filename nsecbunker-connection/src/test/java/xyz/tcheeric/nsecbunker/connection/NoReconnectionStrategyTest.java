package xyz.tcheeric.nsecbunker.connection;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the {@link NoReconnectionStrategy} class.
 */
class NoReconnectionStrategyTest {

    @Test
    void instanceIsSingleton() {
        NoReconnectionStrategy instance1 = NoReconnectionStrategy.INSTANCE;
        NoReconnectionStrategy instance2 = NoReconnectionStrategy.INSTANCE;

        assertSame(instance1, instance2);
    }

    @Test
    void shouldReconnectReturnsFalseForAllAttempts() {
        NoReconnectionStrategy strategy = NoReconnectionStrategy.INSTANCE;

        assertFalse(strategy.shouldReconnect(1));
        assertFalse(strategy.shouldReconnect(2));
        assertFalse(strategy.shouldReconnect(100));
        assertFalse(strategy.shouldReconnect(0));
    }

    @Test
    void getDelayReturnsEmptyForAllAttempts() {
        NoReconnectionStrategy strategy = NoReconnectionStrategy.INSTANCE;

        assertEquals(Optional.empty(), strategy.getDelay(1));
        assertEquals(Optional.empty(), strategy.getDelay(2));
        assertEquals(Optional.empty(), strategy.getDelay(100));
    }

    @Test
    void getMaxAttemptsReturnsZero() {
        NoReconnectionStrategy strategy = NoReconnectionStrategy.INSTANCE;

        assertEquals(0, strategy.getMaxAttempts());
    }

    @Test
    void toStringReturnsExpectedValue() {
        NoReconnectionStrategy strategy = NoReconnectionStrategy.INSTANCE;

        assertEquals("NoReconnectionStrategy", strategy.toString());
    }

    @Test
    void implementsReconnectionStrategy() {
        assertInstanceOf(ReconnectionStrategy.class, NoReconnectionStrategy.INSTANCE);
    }
}
