package xyz.tcheeric.nsecbunker.connection;

import java.time.Duration;
import java.util.Optional;

/**
 * A {@link ReconnectionStrategy} that never attempts reconnection.
 *
 * <p>Use this strategy when you want connections to fail permanently
 * after disconnection, requiring explicit reconnection by the application.
 *
 * <p>This is a singleton class; use {@link #INSTANCE} or
 * {@link ReconnectionStrategy#none()} to obtain the instance.
 */
public final class NoReconnectionStrategy implements ReconnectionStrategy {

    /**
     * The singleton instance.
     */
    public static final NoReconnectionStrategy INSTANCE = new NoReconnectionStrategy();

    private NoReconnectionStrategy() {
        // Singleton
    }

    @Override
    public boolean shouldReconnect(int attempt) {
        return false;
    }

    @Override
    public Optional<Duration> getDelay(int attempt) {
        return Optional.empty();
    }

    @Override
    public int getMaxAttempts() {
        return 0;
    }

    @Override
    public String toString() {
        return "NoReconnectionStrategy";
    }
}
