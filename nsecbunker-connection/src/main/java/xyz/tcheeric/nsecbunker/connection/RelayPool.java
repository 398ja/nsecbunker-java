package xyz.tcheeric.nsecbunker.connection;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import nostr.event.impl.GenericEvent;
import okhttp3.OkHttpClient;
import xyz.tcheeric.nsecbunker.core.exception.BunkerConnectionException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manages a pool of relay connections.
 *
 * <p>RelayPool provides:
 * <ul>
 *   <li>Management of multiple relay connections</li>
 *   <li>Message broadcasting to all or selected relays</li>
 *   <li>Event aggregation from all relays</li>
 *   <li>Automatic failover and reconnection</li>
 *   <li>Connection health tracking</li>
 * </ul>
 *
 * <p>Usage example:
 * <pre>{@code
 * RelayPool pool = RelayPool.builder()
 *     .relay("wss://relay1.example.com")
 *     .relay("wss://relay2.example.com")
 *     .connectTimeout(Duration.ofSeconds(30))
 *     .build();
 *
 * pool.addListener(new RelayPoolListener() {
 *     @Override
 *     public void onEvent(RelayConnection relay, String subId, GenericEvent event) {
 *         System.out.println("Event from " + relay.getUrl() + ": " + event.getId());
 *     }
 * });
 *
 * pool.connectAll();
 * pool.broadcast("[\"REQ\",\"sub1\",{\"kinds\":[1],\"limit\":10}]");
 * }</pre>
 */
@Slf4j
public final class RelayPool implements AutoCloseable {

    /**
     * Map of relay URL to connection.
     */
    private final Map<String, RelayConnection> relays;

    /**
     * Shared OkHttp client for all connections.
     */
    private final OkHttpClient client;

    /**
     * Connection timeout for relay connections.
     */
    @Getter
    private final Duration connectTimeout;

    /**
     * Minimum number of connected relays required for operations.
     */
    @Getter
    private final int minConnectedRelays;

    /**
     * Pool listeners.
     */
    private final List<RelayPoolListener> listeners;

    /**
     * Set of event IDs already seen (for deduplication).
     */
    private final Set<String> seenEventIds;

    /**
     * Whether to deduplicate events.
     */
    @Getter
    private final boolean deduplicateEvents;

    /**
     * Internal relay listener that forwards events to pool listeners.
     */
    private final RelayListener internalListener;

    /**
     * Whether the pool has been closed.
     */
    private volatile boolean closed;

    private RelayPool(Builder builder) {
        this.relays = new ConcurrentHashMap<>();
        this.client = builder.client != null ? builder.client : createDefaultClient(builder.connectTimeout);
        this.connectTimeout = builder.connectTimeout;
        this.minConnectedRelays = builder.minConnectedRelays;
        this.listeners = new CopyOnWriteArrayList<>();
        this.seenEventIds = new CopyOnWriteArraySet<>();
        this.deduplicateEvents = builder.deduplicateEvents;
        this.internalListener = new InternalRelayListener();
        this.closed = false;

        // Add initial relays
        for (String url : builder.relayUrls) {
            addRelay(url);
        }
    }

    /**
     * Creates a new builder for RelayPool.
     *
     * @return a new builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Adds a relay to the pool.
     *
     * @param url the relay URL
     * @return the created RelayConnection
     */
    public RelayConnection addRelay(String url) {
        Objects.requireNonNull(url, "URL must not be null");
        if (closed) {
            throw new IllegalStateException("Pool is closed");
        }

        return relays.computeIfAbsent(url, u -> {
            RelayConnection relay = new RelayConnection(u, client, connectTimeout);
            relay.addListener(internalListener);
            log.debug("Added relay to pool: {}", u);
            return relay;
        });
    }

    /**
     * Removes a relay from the pool.
     *
     * @param url the relay URL
     * @return true if the relay was removed
     */
    public boolean removeRelay(String url) {
        RelayConnection relay = relays.remove(url);
        if (relay != null) {
            relay.removeListener(internalListener);
            relay.close();
            log.debug("Removed relay from pool: {}", url);
            return true;
        }
        return false;
    }

    /**
     * Gets a relay connection by URL.
     *
     * @param url the relay URL
     * @return the relay connection, or null if not found
     */
    public RelayConnection getRelay(String url) {
        return relays.get(url);
    }

    /**
     * Returns all relay connections.
     *
     * @return unmodifiable collection of relay connections
     */
    public Collection<RelayConnection> getRelays() {
        return Collections.unmodifiableCollection(relays.values());
    }

    /**
     * Returns all relay URLs.
     *
     * @return unmodifiable set of relay URLs
     */
    public Set<String> getRelayUrls() {
        return Collections.unmodifiableSet(relays.keySet());
    }

    /**
     * Returns the number of relays in the pool.
     *
     * @return the relay count
     */
    public int size() {
        return relays.size();
    }

    /**
     * Checks if the pool is empty.
     *
     * @return true if no relays are in the pool
     */
    public boolean isEmpty() {
        return relays.isEmpty();
    }

    /**
     * Returns connected relay connections.
     *
     * @return list of connected relays
     */
    public List<RelayConnection> getConnectedRelays() {
        return relays.values().stream()
                .filter(RelayConnection::isConnected)
                .collect(Collectors.toList());
    }

    /**
     * Returns the number of connected relays.
     *
     * @return connected relay count
     */
    public int getConnectedCount() {
        return (int) relays.values().stream()
                .filter(RelayConnection::isConnected)
                .count();
    }

    /**
     * Returns health information for all relays.
     *
     * @return map of relay URL to connection health
     */
    public Map<String, ConnectionHealth> getHealthMap() {
        return relays.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().getHealth()
                ));
    }

    /**
     * Returns only healthy relay connections.
     *
     * @return list of healthy relay connections
     */
    public List<RelayConnection> getHealthyRelays() {
        return relays.values().stream()
                .filter(r -> r.getHealth().isHealthy())
                .collect(Collectors.toList());
    }

    /**
     * Returns the number of healthy relays.
     *
     * @return healthy relay count
     */
    public int getHealthyCount() {
        return (int) relays.values().stream()
                .filter(r -> r.getHealth().isHealthy())
                .count();
    }

    /**
     * Starts health monitoring on all relays.
     *
     * <p>This creates and starts a health monitor for each relay in the pool.
     */
    public void startHealthMonitoring() {
        for (RelayConnection relay : relays.values()) {
            RelayHealthMonitor monitor = relay.getHealthMonitor();
            if (!monitor.isRunning()) {
                monitor.start();
            }
        }
    }

    /**
     * Stops health monitoring on all relays.
     */
    public void stopHealthMonitoring() {
        for (RelayConnection relay : relays.values()) {
            RelayHealthMonitor monitor = relay.getHealthMonitor();
            if (monitor.isRunning()) {
                monitor.stop();
            }
        }
    }

    /**
     * Checks if minimum required relays are connected.
     *
     * @return true if enough relays are connected
     */
    public boolean hasMinimumConnections() {
        return getConnectedCount() >= minConnectedRelays;
    }

    /**
     * Adds a pool listener.
     *
     * @param listener the listener to add
     */
    public void addListener(RelayPoolListener listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * Removes a pool listener.
     *
     * @param listener the listener to remove
     */
    public void removeListener(RelayPoolListener listener) {
        listeners.remove(listener);
    }

    /**
     * Connects all relays in the pool synchronously.
     *
     * @throws BunkerConnectionException if minimum connections cannot be established
     */
    public void connectAll() throws BunkerConnectionException {
        connectAll(connectTimeout);
    }

    /**
     * Connects all relays in the pool with custom timeout.
     *
     * @param timeout the connection timeout
     * @throws BunkerConnectionException if minimum connections cannot be established
     */
    public void connectAll(Duration timeout) throws BunkerConnectionException {
        if (closed) {
            throw new IllegalStateException("Pool is closed");
        }
        if (relays.isEmpty()) {
            throw new BunkerConnectionException("No relays in pool");
        }

        CountDownLatch latch = new CountDownLatch(relays.size());
        AtomicInteger successCount = new AtomicInteger(0);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (RelayConnection relay : relays.values()) {
            new Thread(() -> {
                try {
                    relay.connect(timeout);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    log.warn("Failed to connect to {}: {}", relay.getUrl(), e.getMessage());
                    errors.add(e);
                } finally {
                    latch.countDown();
                }
            }, "relay-connect-" + relay.getUrl()).start();
        }

        try {
            boolean completed = latch.await(timeout.toMillis() + 1000, TimeUnit.MILLISECONDS);
            if (!completed) {
                log.warn("Connection timeout - some relays did not respond");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BunkerConnectionException("Connection interrupted", e);
        }

        if (successCount.get() < minConnectedRelays) {
            String errorSummary = errors.isEmpty() ? "no error details"
                    : errors.stream()
                            .map(Throwable::getMessage)
                            .distinct()
                            .limit(3)
                            .reduce((a, b) -> a + "; " + b)
                            .orElse("unknown");
            throw new BunkerConnectionException(
                    String.format("Only %d of %d required relays connected (%s)",
                            successCount.get(), minConnectedRelays, errorSummary)
            );
        }

        log.info("relay_pool_connected success={} total={}", successCount.get(), relays.size());
    }

    /**
     * Connects all relays asynchronously.
     */
    public void connectAllAsync() {
        if (closed) {
            return;
        }
        for (RelayConnection relay : relays.values()) {
            relay.connectAsync();
        }
    }

    /**
     * Broadcasts a message to all connected relays.
     *
     * @param message the message to send
     * @return number of relays the message was sent to
     */
    public int broadcast(String message) {
        return sendTo(message, relay -> relay.isConnected());
    }

    /**
     * Sends a message to relays matching a predicate.
     *
     * @param message   the message to send
     * @param predicate filter for relays
     * @return number of relays the message was sent to
     */
    public int sendTo(String message, Predicate<RelayConnection> predicate) {
        int count = 0;
        for (RelayConnection relay : relays.values()) {
            if (predicate.test(relay)) {
                try {
                    if (relay.send(message)) {
                        count++;
                    }
                } catch (Exception e) {
                    log.warn("relay_pool_broadcast_failed relay={} error={}", relay.getUrl(), e.getMessage());
                }
            }
        }
        return count;
    }

    /**
     * Sends a message to a specific relay.
     *
     * @param url     the relay URL
     * @param message the message to send
     * @return true if sent successfully
     */
    public boolean sendTo(String url, String message) {
        RelayConnection relay = relays.get(url);
        if (relay != null && relay.isConnected()) {
            try {
                return relay.send(message);
            } catch (Exception e) {
                log.warn("relay_pool_send_failed relay={} error={}", url, e.getMessage());
            }
        }
        return false;
    }

    /**
     * Broadcasts a REQ message to subscribe on all relays.
     *
     * @param subscriptionId the subscription ID
     * @param filters        the filter JSON objects
     * @return number of relays the message was sent to
     */
    public int broadcastReq(String subscriptionId, String... filters) {
        StringBuilder sb = new StringBuilder("[\"REQ\",\"").append(subscriptionId).append("\"");
        for (String filter : filters) {
            sb.append(",").append(filter);
        }
        sb.append("]");
        return broadcast(sb.toString());
    }

    /**
     * Broadcasts a CLOSE message to unsubscribe on all relays.
     *
     * @param subscriptionId the subscription ID
     * @return number of relays the message was sent to
     */
    public int broadcastClose(String subscriptionId) {
        return broadcast("[\"CLOSE\",\"" + subscriptionId + "\"]");
    }

    /**
     * Broadcasts an EVENT message to publish on all relays.
     *
     * @param eventJson the event JSON
     * @return number of relays the message was sent to
     */
    public int broadcastEvent(String eventJson) {
        return broadcast("[\"EVENT\"," + eventJson + "]");
    }

    /**
     * Disconnects all relays.
     */
    public void disconnectAll() {
        for (RelayConnection relay : relays.values()) {
            try {
                relay.close();
            } catch (Exception e) {
                log.warn("Error closing {}: {}", relay.getUrl(), e.getMessage());
            }
        }
    }

    /**
     * Clears the seen event ID cache (for deduplication).
     */
    public void clearSeenEvents() {
        seenEventIds.clear();
    }

    /**
     * Returns the number of seen events.
     *
     * @return seen event count
     */
    public int getSeenEventCount() {
        return seenEventIds.size();
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        disconnectAll();
        relays.clear();
        listeners.clear();
        seenEventIds.clear();
        log.debug("RelayPool closed");
    }

    /**
     * Checks if the pool is closed.
     *
     * @return true if closed
     */
    public boolean isClosed() {
        return closed;
    }

    private OkHttpClient createDefaultClient(Duration timeout) {
        return new OkHttpClient.Builder()
                .connectTimeout(timeout)
                .readTimeout(Duration.ofMinutes(5))
                .writeTimeout(Duration.ofSeconds(30))
                .pingInterval(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Internal listener that forwards events to pool listeners.
     */
    private class InternalRelayListener implements RelayListener {

        @Override
        public void onStateChange(RelayConnection relay, ConnectionState oldState, ConnectionState newState) {
            for (RelayPoolListener listener : listeners) {
                try {
                    listener.onRelayStateChange(relay, oldState, newState);
                } catch (Exception e) {
                    log.warn("Error in pool listener onRelayStateChange: {}", e.getMessage());
                }
            }
        }

        @Override
        public void onConnect(RelayConnection relay) {
            for (RelayPoolListener listener : listeners) {
                try {
                    listener.onRelayConnect(relay);
                } catch (Exception e) {
                    log.warn("Error in pool listener onRelayConnect: {}", e.getMessage());
                }
            }
        }

        @Override
        public void onDisconnect(RelayConnection relay, int code, String reason) {
            for (RelayPoolListener listener : listeners) {
                try {
                    listener.onRelayDisconnect(relay, code, reason);
                } catch (Exception e) {
                    log.warn("Error in pool listener onRelayDisconnect: {}", e.getMessage());
                }
            }
        }

        @Override
        public void onEvent(RelayConnection relay, String subscriptionId, GenericEvent event) {
            // Deduplicate if enabled
            if (deduplicateEvents && event.getId() != null) {
                if (!seenEventIds.add(event.getId())) {
                    log.debug("Duplicate event {} from {}", event.getId(), relay.getUrl());
                    return;
                }
            }

            for (RelayPoolListener listener : listeners) {
                try {
                    listener.onEvent(relay, subscriptionId, event);
                } catch (Exception e) {
                    log.warn("Error in pool listener onEvent: {}", e.getMessage());
                }
            }
        }

        @Override
        public void onOk(RelayConnection relay, String eventId, boolean success, String message) {
            for (RelayPoolListener listener : listeners) {
                try {
                    listener.onOk(relay, eventId, success, message);
                } catch (Exception e) {
                    log.warn("Error in pool listener onOk: {}", e.getMessage());
                }
            }
        }

        @Override
        public void onEndOfStoredEvents(RelayConnection relay, String subscriptionId) {
            for (RelayPoolListener listener : listeners) {
                try {
                    listener.onEndOfStoredEvents(relay, subscriptionId);
                } catch (Exception e) {
                    log.warn("Error in pool listener onEndOfStoredEvents: {}", e.getMessage());
                }
            }
        }

        @Override
        public void onNotice(RelayConnection relay, String message) {
            for (RelayPoolListener listener : listeners) {
                try {
                    listener.onNotice(relay, message);
                } catch (Exception e) {
                    log.warn("Error in pool listener onNotice: {}", e.getMessage());
                }
            }
        }

        @Override
        public void onClosed(RelayConnection relay, String subscriptionId, String message) {
            for (RelayPoolListener listener : listeners) {
                try {
                    listener.onClosed(relay, subscriptionId, message);
                } catch (Exception e) {
                    log.warn("Error in pool listener onClosed: {}", e.getMessage());
                }
            }
        }

        @Override
        public void onAuth(RelayConnection relay, String challenge) {
            for (RelayPoolListener listener : listeners) {
                try {
                    listener.onAuth(relay, challenge);
                } catch (Exception e) {
                    log.warn("Error in pool listener onAuth: {}", e.getMessage());
                }
            }
        }

        @Override
        public void onError(RelayConnection relay, Throwable throwable) {
            for (RelayPoolListener listener : listeners) {
                try {
                    listener.onRelayError(relay, throwable);
                } catch (Exception e) {
                    log.warn("Error in pool listener onRelayError: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Builder for RelayPool.
     */
    public static class Builder {
        private final List<String> relayUrls = new ArrayList<>();
        private OkHttpClient client;
        private Duration connectTimeout = Duration.ofSeconds(30);
        private int minConnectedRelays = 1;
        private boolean deduplicateEvents = true;

        /**
         * Adds a relay URL to the pool.
         *
         * @param url the relay URL
         * @return this builder
         */
        public Builder relay(String url) {
            if (url != null && !url.isEmpty()) {
                relayUrls.add(url);
            }
            return this;
        }

        /**
         * Adds multiple relay URLs to the pool.
         *
         * @param urls the relay URLs
         * @return this builder
         */
        public Builder relays(Collection<String> urls) {
            if (urls != null) {
                urls.stream()
                        .filter(u -> u != null && !u.isEmpty())
                        .forEach(relayUrls::add);
            }
            return this;
        }

        /**
         * Sets a custom OkHttp client.
         *
         * @param client the OkHttp client
         * @return this builder
         */
        public Builder client(OkHttpClient client) {
            this.client = client;
            return this;
        }

        /**
         * Sets the connection timeout.
         *
         * @param timeout the timeout
         * @return this builder
         */
        public Builder connectTimeout(Duration timeout) {
            this.connectTimeout = timeout != null ? timeout : Duration.ofSeconds(30);
            return this;
        }

        /**
         * Sets the minimum number of connected relays required.
         *
         * @param min the minimum count
         * @return this builder
         */
        public Builder minConnectedRelays(int min) {
            this.minConnectedRelays = Math.max(1, min);
            return this;
        }

        /**
         * Sets whether to deduplicate events.
         *
         * @param deduplicate true to deduplicate
         * @return this builder
         */
        public Builder deduplicateEvents(boolean deduplicate) {
            this.deduplicateEvents = deduplicate;
            return this;
        }

        /**
         * Builds the RelayPool.
         *
         * @return the created RelayPool
         */
        public RelayPool build() {
            return new RelayPool(this);
        }
    }

    @Override
    public String toString() {
        return String.format("RelayPool{relays=%d, connected=%d, closed=%s}",
                relays.size(), getConnectedCount(), closed);
    }
}
