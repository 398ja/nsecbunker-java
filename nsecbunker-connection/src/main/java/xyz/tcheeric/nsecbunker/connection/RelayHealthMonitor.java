package xyz.tcheeric.nsecbunker.connection;

import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Health monitor implementation for a single relay connection.
 *
 * <p>This monitor tracks connection health by:
 * <ul>
 *   <li>Monitoring connection state changes</li>
 *   <li>Tracking message send/receive counts</li>
 *   <li>Recording latency measurements</li>
 *   <li>Detecting connection failures</li>
 * </ul>
 *
 * <p>The monitor uses OkHttp's built-in WebSocket ping/pong mechanism for
 * latency measurements when available. It also tracks application-level
 * message activity.
 *
 * <p>Usage example:
 * <pre>{@code
 * RelayConnection connection = new RelayConnection("wss://relay.example.com");
 * RelayHealthMonitor monitor = new RelayHealthMonitor(connection);
 * monitor.addHealthListener(health -> {
 *     System.out.println("Latency: " + health.getLatency());
 * });
 * monitor.start();
 * connection.connect();
 * }</pre>
 *
 * @see HealthMonitor
 * @see ConnectionHealth
 */
@Slf4j
public class RelayHealthMonitor implements HealthMonitor {

    private static final Duration DEFAULT_CHECK_INTERVAL = Duration.ofSeconds(30);
    private static final Duration DEFAULT_PING_TIMEOUT = Duration.ofSeconds(10);
    private static final int DEFAULT_UNHEALTHY_THRESHOLD = 3;
    private static final int LATENCY_SAMPLE_SIZE = 10;

    private final RelayConnection connection;
    private final List<Consumer<ConnectionHealth>> listeners;
    private final AtomicBoolean running;
    private final AtomicReference<ConnectionHealth> currentHealth;

    // Metrics tracking
    private final AtomicLong messagesSent;
    private final AtomicLong messagesReceived;
    private final AtomicLong successfulPings;
    private final AtomicLong failedPings;
    private final AtomicReference<Instant> connectedSince;
    private final AtomicReference<Instant> lastPingTime;
    private final AtomicReference<Instant> lastPongTime;

    // Latency tracking (circular buffer)
    private final long[] latencySamples;
    private int latencyIndex;
    private int latencyCount;
    private final Object latencyLock = new Object();

    // Configuration
    private volatile Duration checkInterval;
    private volatile Duration pingTimeout;
    private volatile int unhealthyThreshold;

    // Executor
    private volatile ScheduledExecutorService executor;
    private volatile ScheduledFuture<?> checkFuture;

    // Connection listener
    private final ConnectionListener connectionListener;

    /**
     * Creates a new health monitor for the given connection.
     *
     * @param connection the relay connection to monitor
     */
    public RelayHealthMonitor(RelayConnection connection) {
        this.connection = Objects.requireNonNull(connection, "Connection must not be null");
        this.listeners = new CopyOnWriteArrayList<>();
        this.running = new AtomicBoolean(false);

        this.messagesSent = new AtomicLong(0);
        this.messagesReceived = new AtomicLong(0);
        this.successfulPings = new AtomicLong(0);
        this.failedPings = new AtomicLong(0);
        this.connectedSince = new AtomicReference<>();
        this.lastPingTime = new AtomicReference<>();
        this.lastPongTime = new AtomicReference<>();

        this.latencySamples = new long[LATENCY_SAMPLE_SIZE];
        this.latencyIndex = 0;
        this.latencyCount = 0;

        this.checkInterval = DEFAULT_CHECK_INTERVAL;
        this.pingTimeout = DEFAULT_PING_TIMEOUT;
        this.unhealthyThreshold = DEFAULT_UNHEALTHY_THRESHOLD;

        this.currentHealth = new AtomicReference<>(
                ConnectionHealth.disconnected(connection.getUrl()));

        // Create connection listener to track state changes
        this.connectionListener = new ConnectionListener() {
            @Override
            public void onConnected(String url) {
                connectedSince.set(Instant.now());
                resetMetrics();
                updateHealth();
            }

            @Override
            public void onDisconnected(String url, int code, String reason) {
                connectedSince.set(null);
                updateHealth();
            }

            @Override
            public void onError(String url, Throwable error) {
                updateHealth();
            }

            @Override
            public void onStateChanged(String url, ConnectionState oldState, ConnectionState newState) {
                updateHealth();
            }
        };
    }

    @Override
    public void start() {
        if (!running.compareAndSet(false, true)) {
            return;
        }

        log.debug("Starting health monitor for {}", connection.getUrl());

        // Register connection listener
        connection.addConnectionListener(connectionListener);

        // Initialize health based on current state
        if (connection.isConnected()) {
            connectedSince.set(Instant.now());
        }
        updateHealth();

        // Start periodic health checks
        executor = new ScheduledThreadPoolExecutor(1);
        scheduleNextCheck();
    }

    @Override
    public void stop() {
        if (!running.compareAndSet(true, false)) {
            return;
        }

        log.debug("Stopping health monitor for {}", connection.getUrl());

        // Remove connection listener
        connection.removeConnectionListener(connectionListener);

        // Cancel scheduled check
        if (checkFuture != null) {
            checkFuture.cancel(false);
            checkFuture = null;
        }

        // Shutdown executor
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public ConnectionHealth getHealth() {
        return currentHealth.get();
    }

    @Override
    public void checkNow() {
        if (running.get()) {
            performHealthCheck();
        }
    }

    @Override
    public void addHealthListener(Consumer<ConnectionHealth> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public void removeHealthListener(Consumer<ConnectionHealth> listener) {
        listeners.remove(listener);
    }

    @Override
    public void setCheckInterval(Duration interval) {
        Objects.requireNonNull(interval, "Interval must not be null");
        if (interval.isNegative() || interval.isZero()) {
            throw new IllegalArgumentException("Interval must be positive");
        }
        this.checkInterval = interval;
    }

    @Override
    public Duration getCheckInterval() {
        return checkInterval;
    }

    @Override
    public void setPingTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "Timeout must not be null");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        this.pingTimeout = timeout;
    }

    @Override
    public Duration getPingTimeout() {
        return pingTimeout;
    }

    @Override
    public void setUnhealthyThreshold(int threshold) {
        if (threshold < 1) {
            throw new IllegalArgumentException("Threshold must be at least 1");
        }
        this.unhealthyThreshold = threshold;
    }

    @Override
    public int getUnhealthyThreshold() {
        return unhealthyThreshold;
    }

    /**
     * Records a message being sent.
     *
     * <p>Call this when a message is sent to track outbound message count.
     */
    public void recordMessageSent() {
        messagesSent.incrementAndGet();
    }

    /**
     * Records a message being received.
     *
     * <p>Call this when a message is received to track inbound message count.
     */
    public void recordMessageReceived() {
        messagesReceived.incrementAndGet();
    }

    /**
     * Records a latency measurement.
     *
     * @param latency the measured latency
     */
    public void recordLatency(Duration latency) {
        if (latency == null || latency.isNegative()) {
            return;
        }

        synchronized (latencyLock) {
            latencySamples[latencyIndex] = latency.toMillis();
            latencyIndex = (latencyIndex + 1) % LATENCY_SAMPLE_SIZE;
            if (latencyCount < LATENCY_SAMPLE_SIZE) {
                latencyCount++;
            }
        }

        lastPongTime.set(Instant.now());
        successfulPings.incrementAndGet();
        updateHealth();
    }

    /**
     * Records a ping failure (timeout).
     */
    public void recordPingFailure() {
        failedPings.incrementAndGet();
        updateHealth();
    }

    /**
     * Records a ping being sent.
     */
    public void recordPingSent() {
        lastPingTime.set(Instant.now());
    }

    /**
     * Returns the connection being monitored.
     *
     * @return the relay connection
     */
    public RelayConnection getConnection() {
        return connection;
    }

    private void scheduleNextCheck() {
        if (!running.get() || executor == null) {
            return;
        }

        checkFuture = executor.schedule(() -> {
            try {
                performHealthCheck();
            } finally {
                scheduleNextCheck();
            }
        }, checkInterval.toMillis(), TimeUnit.MILLISECONDS);
    }

    private void performHealthCheck() {
        if (!running.get()) {
            return;
        }

        log.trace("Performing health check for {}", connection.getUrl());

        // Check if we've received a pong recently
        Instant lastPong = lastPongTime.get();
        if (lastPong != null && connection.isConnected()) {
            Duration sinceLastPong = Duration.between(lastPong, Instant.now());
            if (sinceLastPong.compareTo(checkInterval.multipliedBy(2)) > 0) {
                // No pong for too long, record as failure
                recordPingFailure();
            }
        }

        updateHealth();
    }

    private void updateHealth() {
        ConnectionHealth oldHealth = currentHealth.get();
        ConnectionHealth newHealth = buildHealthSnapshot();
        currentHealth.set(newHealth);

        // Notify listeners if health changed significantly
        if (healthChanged(oldHealth, newHealth)) {
            notifyListeners(newHealth);
        }
    }

    private ConnectionHealth buildHealthSnapshot() {
        ConnectionState state = connection.getState();
        boolean connected = state == ConnectionState.CONNECTED;

        // Calculate latency statistics
        Duration latency = null;
        Duration avgLatency = null;
        Duration minLatency = null;
        Duration maxLatency = null;

        synchronized (latencyLock) {
            if (latencyCount > 0) {
                long sum = 0;
                long min = Long.MAX_VALUE;
                long max = Long.MIN_VALUE;
                long last = latencySamples[(latencyIndex - 1 + LATENCY_SAMPLE_SIZE) % LATENCY_SAMPLE_SIZE];

                for (int i = 0; i < latencyCount; i++) {
                    long sample = latencySamples[i];
                    sum += sample;
                    min = Math.min(min, sample);
                    max = Math.max(max, sample);
                }

                latency = Duration.ofMillis(last);
                avgLatency = Duration.ofMillis(sum / latencyCount);
                minLatency = Duration.ofMillis(min);
                maxLatency = Duration.ofMillis(max);
            }
        }

        // Determine health status
        int consecutiveFailures = calculateConsecutiveFailures();
        boolean healthy = connected && consecutiveFailures < unhealthyThreshold;

        return ConnectionHealth.builder()
                .url(connection.getUrl())
                .state(state)
                .healthy(healthy)
                .latency(latency)
                .averageLatency(avgLatency)
                .minLatency(minLatency)
                .maxLatency(maxLatency)
                .timestamp(Instant.now())
                .lastPingTime(lastPingTime.get())
                .lastPongTime(lastPongTime.get())
                .connectedSince(connectedSince.get())
                .successfulPings(successfulPings.get())
                .failedPings(failedPings.get())
                .consecutiveFailures(consecutiveFailures)
                .messagesSent(messagesSent.get())
                .messagesReceived(messagesReceived.get())
                .build();
    }

    private int calculateConsecutiveFailures() {
        // Simple approximation: check time since last pong vs expected interval
        Instant lastPong = lastPongTime.get();
        if (lastPong == null) {
            return 0;
        }

        Duration sinceLastPong = Duration.between(lastPong, Instant.now());
        long missedChecks = sinceLastPong.toMillis() / checkInterval.toMillis();
        return (int) Math.max(0, missedChecks - 1);
    }

    private boolean healthChanged(ConnectionHealth oldHealth, ConnectionHealth newHealth) {
        if (oldHealth == null) {
            return true;
        }
        return oldHealth.isHealthy() != newHealth.isHealthy() ||
                oldHealth.getState() != newHealth.getState();
    }

    private void notifyListeners(ConnectionHealth health) {
        for (Consumer<ConnectionHealth> listener : listeners) {
            try {
                listener.accept(health);
            } catch (Exception e) {
                log.warn("Error in health listener: {}", e.getMessage());
            }
        }
    }

    private void resetMetrics() {
        synchronized (latencyLock) {
            latencyIndex = 0;
            latencyCount = 0;
        }
        // Don't reset cumulative counters, only latency samples
    }
}
