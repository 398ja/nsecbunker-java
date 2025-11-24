package xyz.tcheeric.nsecbunker.connection;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link ConnectionListener} that delegates to multiple listeners.
 *
 * <p>This allows combining multiple listeners into a single listener instance.
 * Exceptions in individual listeners are caught and logged, allowing other
 * listeners to still receive the event.
 */
@Slf4j
public class CompositeConnectionListener implements ConnectionListener {

    private final List<ConnectionListener> listeners;

    /**
     * Creates a CompositeConnectionListener with the given listeners.
     *
     * @param listeners the listeners to delegate to
     */
    public CompositeConnectionListener(ConnectionListener... listeners) {
        this.listeners = new CopyOnWriteArrayList<>(Arrays.asList(listeners));
    }

    /**
     * Adds a listener to this composite.
     *
     * @param listener the listener to add
     */
    public void addListener(ConnectionListener listener) {
        if (listener != null && listener != this) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a listener from this composite.
     *
     * @param listener the listener to remove
     * @return true if the listener was removed
     */
    public boolean removeListener(ConnectionListener listener) {
        return listeners.remove(listener);
    }

    /**
     * Returns the number of listeners in this composite.
     *
     * @return the listener count
     */
    public int size() {
        return listeners.size();
    }

    @Override
    public void onConnected(String url) {
        for (ConnectionListener listener : listeners) {
            try {
                listener.onConnected(url);
            } catch (Exception e) {
                log.warn("Error in ConnectionListener.onConnected: {}", e.getMessage());
            }
        }
    }

    @Override
    public void onDisconnected(String url, int code, String reason) {
        for (ConnectionListener listener : listeners) {
            try {
                listener.onDisconnected(url, code, reason);
            } catch (Exception e) {
                log.warn("Error in ConnectionListener.onDisconnected: {}", e.getMessage());
            }
        }
    }

    @Override
    public void onError(String url, Throwable error) {
        for (ConnectionListener listener : listeners) {
            try {
                listener.onError(url, error);
            } catch (Exception e) {
                log.warn("Error in ConnectionListener.onError: {}", e.getMessage());
            }
        }
    }

    @Override
    public void onStateChanged(String url, ConnectionState oldState, ConnectionState newState) {
        for (ConnectionListener listener : listeners) {
            try {
                listener.onStateChanged(url, oldState, newState);
            } catch (Exception e) {
                log.warn("Error in ConnectionListener.onStateChanged: {}", e.getMessage());
            }
        }
    }

    @Override
    public void onReconnecting(String url, int attempt) {
        for (ConnectionListener listener : listeners) {
            try {
                listener.onReconnecting(url, attempt);
            } catch (Exception e) {
                log.warn("Error in ConnectionListener.onReconnecting: {}", e.getMessage());
            }
        }
    }
}
